package main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import preprocessor.PreprocessText;
import qualiaIdentifier.QualiaIdentifier;
import utils.LanguageManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class Main {


    public static void main(String[] args) {
        executeQualiaStructuresAppWithJSON(args[0]);
    }


    public static void executeQualiaStructuresAppWithJSON(String filename) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(Files.readString(Path.of(filename)));

            String version = root.get("version").asText();
            String requestedVersion = "1.1.0";
            qualiAssistantVersionCheck(version, requestedVersion);

            Path pathToQualiaPatternsFile = Path.of(root.get("qualiaPatternsFile").asText());

            String language = root.get("language").asText();
            LanguageManager languageManager = new LanguageManager(language);

            boolean enableParallelization = root.get("enableParallelization").asBoolean();


            JsonNode node_datasetPreparation = root.get("datasetPreparation");
            boolean prepareFile = node_datasetPreparation.get("prepareFile").asBoolean();

            JsonNode node_queryProcessing = root.get("queryProcessing");
            boolean processQuery = node_queryProcessing.get("processQuery").asBoolean();
            Path pathToFilePreprocessed = Path.of(node_queryProcessing.get("filePreprocessed").asText());

            if (prepareFile) {
                Path pathToFileToPreprocess = Path.of(node_datasetPreparation.get("fileToPreprocess").asText());
                String columnWithTextToProceed = node_datasetPreparation.get("columnWithTextToProceed").asText();

                PreprocessText preprocessText = new PreprocessText(
                        new File(String.valueOf(pathToFileToPreprocess)),
                        columnWithTextToProceed,
                        pathToQualiaPatternsFile,
                        new File(String.valueOf(pathToFilePreprocessed)),
                        languageManager,
                        enableParallelization);
                preprocessText.preprocessTextForQualiaSearch();
            }

            if (processQuery) {
                List<String> queries = new ArrayList<>();
                JsonNode reduceSearchToQueries = node_queryProcessing.get("reduceSearchToQueries");
                for (JsonNode query : reduceSearchToQueries) {
                    queries.add(query.asText());
                }

                boolean deepSearch = node_queryProcessing.get("deepSearch").asBoolean();
                Path pathToFileWithOutput = Paths.get(node_queryProcessing.get("outputDirectory").asText(), pathToFilePreprocessed.getFileName().toString().replace(".csv", "_withQualiaStructures.csv"));
                boolean useStemming = node_queryProcessing.get("useStemming").asBoolean();

                if (queries.size() == 0) {
                    QualiaIdentifier qualiaIdentifier = new QualiaIdentifier(
                            new File(String.valueOf(pathToFilePreprocessed)),
                            pathToQualiaPatternsFile,
                            null,
                            useStemming,
                            pathToFileWithOutput,
                            languageManager,
                            deepSearch,
                            enableParallelization);
                    qualiaIdentifier.computeQualiaStructures();
                }
                for (String query : queries) {
                    QualiaIdentifier qualiaIdentifier = new QualiaIdentifier(
                            new File(String.valueOf(pathToFilePreprocessed)),
                            pathToQualiaPatternsFile,
                            query,
                            useStemming,
                            pathToFileWithOutput,
                            languageManager,
                            deepSearch,
                            enableParallelization);
                    qualiaIdentifier.computeQualiaStructures();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void qualiAssistantVersionCheck(String versionInSpecification, String requestedVersion) {
        if (!versionInSpecification.equals(requestedVersion)) {
            System.err.println("The required qualia structure app version should be " + requestedVersion + " but is " + versionInSpecification + " instead.");
        }
    }

}
