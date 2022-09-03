package preprocessor;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.trees.Tree;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import utils.LanguageManager;
import utils.ThreadChecker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static utils.CSVFiles.defaultCSVFormat;
import static utils.CSVFiles.readCSVFile;

public class PreprocessText {

    private static int sentenceIdCounter = 0;
    public static int counter = 0;

    File selectedFileToPreprocess;
    String columnNameInCSVFileWithTextToProceed;
    Path pathToFileWithQualiaRolesForQuery;
    File pathToFilePreprocessed;
    LanguageManager languageManager;
    boolean enableParallelization;


    public PreprocessText (
            File selectedFileToPreprocess,
            String columnNameInCSVFileWithTextToProceed,
            Path pathToFileWithQualiaRolesForQuery,
            File pathToFilePreprocessed,
            LanguageManager languageManager,
            boolean enableParallelization) {

        this.selectedFileToPreprocess = selectedFileToPreprocess;
        this.columnNameInCSVFileWithTextToProceed = columnNameInCSVFileWithTextToProceed;
        this.pathToFileWithQualiaRolesForQuery = pathToFileWithQualiaRolesForQuery;
        this.pathToFilePreprocessed = pathToFilePreprocessed;
        this.languageManager = languageManager;
        this.enableParallelization = enableParallelization;
    }


    public void preprocessTextForQualiaSearch () {
        try {
            CSVParser csvParser_fileToPreprocess = readCSVFile(Paths.get(String.valueOf(selectedFileToPreprocess)));
            List<String> headerOld = Objects.requireNonNull(csvParser_fileToPreprocess).getHeaderNames();
            List<String> headerNewElements = Arrays.asList("sentence_id", "extracted_sentence", "constituency_tree", "penn_string");

            List<String> headerNew = new ArrayList<>();
            headerNew.addAll(headerOld);
            headerNew.addAll(headerNewElements);

            CSVPrinter csvPrinter = new CSVPrinter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(
                                            pathToFilePreprocessed
                                    ),
                                    StandardCharsets.UTF_8)
                    ),
                    defaultCSVFormat.withHeader(headerNew.toArray(new String[0])));

            List<CSVRecord> csvRecords = csvParser_fileToPreprocess.getRecords();

            List<List<Object>> csvEntries = new ArrayList<>();
            int csvRecordId = 0;
            int maximumNumberOfThreads = (int) Math.floor(ThreadChecker.count*0.5d);//Runtime.getRuntime().availableProcessors() * 10;  // TODO: what is a good number of threads?
            Stack<Integer> availableThreads = new Stack<>();
            for (int i=0; i<maximumNumberOfThreads; i++) {
                availableThreads.add(i);
            }
            for (CSVRecord csvRecord : csvRecords) {
                if (enableParallelization) {
                    while (true) {
                        if (availableThreads.isEmpty()) {
                            Thread.sleep(1000);
                        } else {
                            int freeThreadPosition = availableThreads.pop();
                            List<List<Object>> finalCsvEntries = csvEntries;
                            Thread t = new Thread(() -> processCSVRecord(headerOld, csvRecord, finalCsvEntries, availableThreads, freeThreadPosition));

                            // https://stackoverflow.com/a/6662631
                            t.setUncaughtExceptionHandler((t1, e) -> {
                                System.err.println("Failed to process csv record: " + csvRecord);
                                e.printStackTrace();
                                availableThreads.add(freeThreadPosition);
                            });

                            t.start();
                            break;
                        }
                    }
                } else {
                    processCSVRecord(headerOld, csvRecord, csvEntries, availableThreads, 0);
                }

                if (++csvRecordId % 1000000 == 0) {
                    printProgress(availableThreads, maximumNumberOfThreads, csvPrinter, csvEntries);
                    csvEntries = new ArrayList<>();
                }

                if (csvRecordId % 1000 == 0) {
                    System.err.print("Progress: " + (100d * ((int)(10000*(1d*csvRecordId / csvRecords.size())))/10000) + " %" + "\r");
                }
            }

            printProgress(availableThreads, maximumNumberOfThreads, csvPrinter, csvEntries);
            csvPrinter.close();

            System.err.println("The preprocessing is completed. " + sentenceIdCounter + " sentences were processed.");

            csvParser_fileToPreprocess.close();

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void printProgress(Stack<Integer> availableThreads, int maximumNumberOfThreads, CSVPrinter csvPrinter, List<List<Object>> csvEntries) throws InterruptedException, IOException {
        while (availableThreads.size() < maximumNumberOfThreads) {
            Thread.sleep(1000);
        }
        csvPrinter.printRecords(csvEntries.toArray(new Object[0]));
        csvPrinter.flush();
    }


    private void processCSVRecord (List<String> headerOld, CSVRecord csvRecord, List<List<Object>> csvEntries, Stack<Integer> availableThreads, int freeThreadPosition) {

        try {
            int MAX_PLAIN_TEXT_LENGTH = 15000;
            int DELTA = 500;
            String plainText = csvRecord.get(columnNameInCSVFileWithTextToProceed);
            List<String> sentences = new ArrayList<>();
            if (plainText.length() > MAX_PLAIN_TEXT_LENGTH) {
                for (int pos=0; pos<plainText.length(); pos += MAX_PLAIN_TEXT_LENGTH - DELTA) {
                    String plainTextChunk = plainText.substring(pos, Math.min(pos+MAX_PLAIN_TEXT_LENGTH, plainText.length()));
                    sentences.addAll(POSTools.getSentences(plainTextChunk, languageManager));
                }
            } else {
                sentences = POSTools.getSentences(plainText, languageManager);
            }

            for (String sentence : sentences) {
                List<Object> csvEntry = new ArrayList<>();
                List<Tree> constituencyTrees = new LinkedList<>();

                CoreDocument document = languageManager.getPosTagging_pipeline().processToCoreDocument(sentence);
                for (CoreSentence coreSentence : document.sentences()) {
                    //the following code will search in any single tree created if it has the pattern (NP (NN <word>) (NN <word>))
                    //this pattern seems very common with the words in question, eg.: (NP (NN health) (NN care))
                    Tree consitutTree = coreSentence.constituencyParse();
                    consitutTree = Tree.valueOf("(ROOT (ABBA (LOL test) (LOL wort)))");
                    if(PreproQualiaPatternChecker.checkForPattern(consitutTree.toString())){
                        constituencyTrees.add(Tree.valueOf(PreproQualiaPatternChecker.multiWordTree));
                    }else {
                        constituencyTrees.add(consitutTree);
                    }
                    //TODO("Long term idea: Try to check for all patterns from the qualia file if they are contained within a tree")
                }

                for (String column : headerOld) {
                    csvEntry.add(csvRecord.get(column).length() > 1000 ? "Text suppressed as it is too long. If required, check the original input file..." : csvRecord.get(column));
                }

                csvEntry.add(++sentenceIdCounter);
                csvEntry.add(sentence);
                csvEntry.add(constituencyTrees);
                csvEntry.add(POSTools.getPennStrings(constituencyTrees));

                counter++;
                System.out.println(counter);
                csvEntries.add(csvEntry);
            }

            if (enableParallelization) {
                availableThreads.add(freeThreadPosition);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
