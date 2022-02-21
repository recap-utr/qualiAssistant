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


public class Main {


    private static final QualiaIdentifier QUALIA_IDENTIFIER = new QualiaIdentifier();
    private static final PreprocessText PREPROCESS_TEXT = new PreprocessText();



    public static void main(String[] args) {
        executeQualiaStructuresAppWithJSON(args[0]);
    }


    public static void executeQualiaStructuresAppWithJSON(String filename) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(Files.readString(Path.of(filename)));

            String version = root.get("version").asText();
            String requestedVersion = "2.0";
            qualiAssistantVersionCheck(version, requestedVersion);

            Path pathToPatternAndRoles = Path.of(root.get("patternAndRoles").asText());

            String language = root.get("language").asText();
            LanguageManager languageManager = new LanguageManager(language);


            JsonNode node_datasetPreparation = root.get("datasetPreparation");
            boolean prepareFile = node_datasetPreparation.get("prepareFile").asBoolean();

            JsonNode node_queryProcessing = root.get("queryProcessing");
            boolean processQuery = node_queryProcessing.get("processQuery").asBoolean();
            Path pathToFilePreprocessed = Path.of(node_queryProcessing.get("filePreprocessed").asText());

            if (prepareFile) {
                Path pathToFileToPreprocess = Path.of(node_datasetPreparation.get("fileToPreprocess").asText());
                String columnWithTextToProceed = node_datasetPreparation.get("columnWithTextToProceed").asText();
                boolean onlyWithSignalWords = node_datasetPreparation.get("onlyWithSignalWords").asBoolean();

                PREPROCESS_TEXT.preprocessTextForQualiaSearch(
                        new File(String.valueOf(pathToFileToPreprocess)),
                        columnWithTextToProceed,
                        onlyWithSignalWords,
                        pathToPatternAndRoles,
                        new File(String.valueOf(pathToFilePreprocessed)),
                        languageManager);
            }

            if (processQuery) {
                String reduceSearchToQuery = node_queryProcessing.get("reduceSearchToQuery").asText();
                boolean deepSearch = node_queryProcessing.get("deepSearch").asBoolean();
                Path pathToFileWithOutput = Paths.get(node_queryProcessing.get("outputDirectory").asText(), pathToFilePreprocessed.getFileName().toString().replace(".csv", "_withQualiaRolesForQuery.csv"));
                boolean useStemming = node_queryProcessing.get("useStemming").asBoolean();

                QUALIA_IDENTIFIER.computeQualiaStructures(
                        new File(String.valueOf(pathToFilePreprocessed)),
                        pathToPatternAndRoles,
                        reduceSearchToQuery,
                        useStemming,
                        pathToFileWithOutput,
                        languageManager,
                        deepSearch);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void qualiAssistantVersionCheck(String versionInSpecification, String requestedVersion) {
        if (!versionInSpecification.equals(requestedVersion)) {
            System.err.println("The required qualia structure app version should " + requestedVersion + " but is " + versionInSpecification + " instead.");
            System.exit(0);
        }
    }

}
