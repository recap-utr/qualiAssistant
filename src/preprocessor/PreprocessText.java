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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static utils.CSVFiles.defaultCSVFormat;
import static utils.CSVFiles.readCSVFile;

public class PreprocessText {

    private static final Pattern regex_signalWords = Pattern.compile("\\(??([A-Z]+)/(.[a-zA-Z0-9]+)\\)?");
    private static final List<String> signalWords = new ArrayList<>();


    public void preprocessTextForQualiaSearch(File selectedFileToPreprocess, String columnNameInCSVFileWithTextToProceed, boolean fastRun, Path pathToFileWithQualiaRolesForQuery, File pathToFilePreprocessed, LanguageManager languageManager) {
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

            if (fastRun) {
                initFastRun(pathToFileWithQualiaRolesForQuery);
            }

            int idCnt = 0;
            for (CSVRecord csvRecord : csvRecords) {

                String plainText = csvRecord.get(columnNameInCSVFileWithTextToProceed);

                List<String> sentences = POSTools.getSentences(plainText, languageManager);
                for (String sentence : sentences) {

                    if (fastRun && !plainTextContainsAtLeaseOneSignalWord(plainText)) {
                        continue;
                    }

                    CoreDocument document = languageManager.getPosTagging_pipeline().processToCoreDocument(sentence);
                    List<Tree> constituencyTrees = new LinkedList<>();
                    for (CoreSentence coreSentence : document.sentences()) {
                        constituencyTrees.add(coreSentence.constituencyParse());
                    }

                    List<Object> csvEntry = new ArrayList<>();
                    for (String column : headerOld) {
                        csvEntry.add(csvRecord.get(column));
                    }

                    csvEntry.add(++idCnt);
                    csvEntry.add(sentence);
                    csvEntry.add(constituencyTrees);
                    csvEntry.add(POSTools.getPennStrings(constituencyTrees));

                    csvPrinter.printRecord(csvEntry.toArray(new Object[0]));
                }

                if (++idCnt % 100000 == 0) {
                    csvPrinter.flush();
                }
            }

            csvPrinter.flush();
            csvPrinter.close();

            System.err.println("The preprocessing is completed. " + idCnt + " sentences were processed.");

            csvParser_fileToPreprocess.close();

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }


    private void initFastRun (Path pathToFileWithQualiaRolesForQuery) {
        CSVParser csvParser_patternAndRoles = readCSVFile(pathToFileWithQualiaRolesForQuery);
        for (CSVRecord csvRecord : Objects.requireNonNull(csvParser_patternAndRoles)) {
            String[] tokens = csvRecord.get("pattern").split("\\s+");
            for (String token : tokens) {
                if (!token.contains("<qualia>") && !token.contains("query") && token.contains("/")) {
                    Matcher matcher = regex_signalWords.matcher(token);
                    if (matcher.find()) {
                        signalWords.add(matcher.group(2));
                    }
                }
            }
        }
    }


    public boolean plainTextContainsAtLeaseOneSignalWord (String plainText) {

        for (String signalWord : signalWords) {
            if (plainText.contains(signalWord)) {
                return true;
            }
        }

        return false;
    }

}
