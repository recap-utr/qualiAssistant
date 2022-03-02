package preprocessor;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.trees.Tree;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import utils.LanguageManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static utils.CSVFiles.defaultCSVFormat;
import static utils.CSVFiles.readCSVFile;

public class PreprocessText {

    private static int sentenceIdCounter = 0;

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
            List<String> headerNewElements = Arrays.asList("sentence_id", "extractedSentence", "constituency_tree", "penn_string");

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
            int maximumNumberOfThreads = Runtime.getRuntime().availableProcessors() * 10;  // TODO: what is a good number of threads?
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
                            new Thread(() -> processCSVRecord(headerOld, csvRecord, finalCsvEntries, availableThreads, freeThreadPosition)).start();
                            break;
                        }
                    }
                } else {
                    processCSVRecord(headerOld, csvRecord, csvEntries, availableThreads, 0);
                }

                if (++csvRecordId % 1000000 == 0) {
                    while (availableThreads.size() < maximumNumberOfThreads) {
                        Thread.sleep(1000);
                    }
                    csvPrinter.printRecords(csvEntries.toArray(new Object[0]));
                    csvEntries = new ArrayList<>();
                }
            }

            while (availableThreads.size() < maximumNumberOfThreads) {
                Thread.sleep(1000);
            }
            csvPrinter.printRecords(csvEntries.toArray(new Object[0]));
            csvPrinter.flush();
            csvPrinter.close();

            System.err.println("The preprocessing is completed. " + sentenceIdCounter + " sentences were processed.");

            csvParser_fileToPreprocess.close();

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }


    private void processCSVRecord (List<String> headerOld, CSVRecord csvRecord, List<List<Object>> csvEntries, Stack<Integer> availableThreads, int freeThreadPosition) {

        try {
            String plainText = csvRecord.get(columnNameInCSVFileWithTextToProceed);
            List<String> sentences = POSTools.getSentences(plainText, languageManager);
            for (String sentence : sentences) {

                List<Object> csvEntry = new ArrayList<>();
                List<Tree> constituencyTrees = new LinkedList<>();

                CoreDocument document = languageManager.getPosTagging_pipeline().processToCoreDocument(sentence);
                for (CoreSentence coreSentence : document.sentences()) {
                    constituencyTrees.add(coreSentence.constituencyParse());
                }

                for (String column : headerOld) {
                    csvEntry.add(csvRecord.get(column));
                }

                csvEntry.add(++sentenceIdCounter);
                csvEntry.add(sentence);
                csvEntry.add(constituencyTrees);
                csvEntry.add(POSTools.getPennStrings(constituencyTrees));

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
