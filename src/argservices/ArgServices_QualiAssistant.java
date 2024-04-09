package argservices;

import de.uni_trier.recap.arg_services.quality.v1beta.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;
import org.apache.commons.csv.CSVPrinter;
import preprocessor.PreprocessText;
import qualiaIdentifier.QualiaIdentifier;
import utils.LanguageManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static utils.CSVFiles.defaultCSVFormat;

public class ArgServices_QualiAssistant {

    private static final Logger logger = Logger.getLogger(ArgServices_QualiAssistant.class.getName());
    private static final int rpcPort = 50200;

    private Server server;

    private void start() throws IOException {
        /* The port on which the server should run */
        server =
                ServerBuilder.forPort(rpcPort)
                        .addService(new QualiAssistantServiceImpl())
                        .addService(ProtoReflectionService.newInstance())
                        .build()
                        .start();
        logger.info("Server started, listening on " + rpcPort);
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    // Use stderr here since the logger may have been reset by its JVM shutdown
                                    // hook.
                                    System.err.println("*** shutting down gRPC server since JVM is shutting down");
                                    try {
                                        ArgServices_QualiAssistant.this.stop();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace(System.err);
                                    }
                                    System.err.println("*** server shut down");
                                }));
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /** Await termination on the main thread since the grpc library uses daemon threads. */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /** Main launches the server from the command line. */
    public static void main(String[] args) throws IOException, InterruptedException {
        final var server = new ArgServices_QualiAssistant();
        server.start();
        server.blockUntilShutdown();
    }


    static class QualiAssistantServiceImpl extends QualiaServiceGrpc.QualiaServiceImplBase {
        public QualiAssistantServiceImpl() {
            super();
        }

        @Override
        public void qualiaAnnotations(QualiaAnnotationsRequest request, StreamObserver<QualiaAnnotationsResponse> responseObserver)
                throws IllegalArgumentException {

            // input
            List<QualiaInputPattern> pattern = request.getPatternsList();
            String text = request.getText();
            String language = "english"; // TODO: immutable at the moment. Can be extended with next Arg services update.
            List<String> queries = List.of(); // TODO: immutable at the moment. Can be extended with next Arg services update.

            // intern variables and methods
            Path pathToTmpQualiaPatternsFile = Paths.get("tmp_qualiaPatterns.csv");
            Path pathToTmpFileToPreprocess = Path.of("tmp_fileToPreprocess.csv");
            String columnWithTextToProceed = "text";
            Path pathToTmpFilePreprocessed = Path.of("tmp_fileToPreprocessed.csv");
            Path pathToTmpOutputDirectory = Path.of("");

            try {
                writeCSVFileWithQualiaPatterns(pathToTmpQualiaPatternsFile, pattern);
                writeCSVFileWithFileToPreprocess(pathToTmpFileToPreprocess, columnWithTextToProceed, text);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            List<List<Object>> csvEntries = executeQualiaStructuresForArgServices(
                    pathToTmpQualiaPatternsFile,
                    pathToTmpFileToPreprocess,
                    pathToTmpFilePreprocessed,
                    pathToTmpOutputDirectory,
                    language,
                    columnWithTextToProceed,
                    queries);

            int offset = 1;
            var response = QualiaAnnotationsResponse.newBuilder();

            for (List<Object> csvEntry : csvEntries) {

                var aduBuilder = de.uni_trier.recap.arg_services.quality.v1beta.QualiaOutputPattern.newBuilder();

                aduBuilder.setRole(QualiaRole.valueOf(String.valueOf(csvEntry.get(offset))));
                aduBuilder.setInputPatternMatch(String.valueOf(csvEntry.get(1 + offset)));
                aduBuilder.setQueryText(String.valueOf(csvEntry.get(9 + offset)));
                aduBuilder.setQualiaText(String.valueOf(csvEntry.get(11 + offset)));

                response.addPatterns(
                        aduBuilder.build()
                );
            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }

        private static void writeCSVFileWithFileToPreprocess(Path pathToTmpFileToPreprocess, String columnWithTextToProceed, String text) throws IOException {
            CSVPrinter csvPrinter_fileToPreprocess = new CSVPrinter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(
                                            String.valueOf(pathToTmpFileToPreprocess)
                                    ),
                                    StandardCharsets.UTF_8)
                    ),
                    defaultCSVFormat.withHeader(Arrays.asList(columnWithTextToProceed).toArray(new String[0])));

            List<Object> csvRecords_fileToPreprocess = new ArrayList<>();
            String[] csvEntry = List.of(text).toArray(new String[0]);
            csvRecords_fileToPreprocess.add(csvEntry);

            csvPrinter_fileToPreprocess.printRecords(csvRecords_fileToPreprocess);
        }

        private static void writeCSVFileWithQualiaPatterns(Path pathToTmpQualiaPatternsFile, List<QualiaInputPattern> pattern) throws IOException {
            CSVPrinter csvPrinter_qualiaPatterns = new CSVPrinter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(
                                            String.valueOf(pathToTmpQualiaPatternsFile)
                                    ),
                                    StandardCharsets.UTF_8)
                    ),
                    defaultCSVFormat.withHeader(Arrays.asList("role", "pattern", "queryEnvironmentDelimiterPOSTags").toArray(new String[0])));

            List<Object> csvRecords_qualiaPatterns = new ArrayList<>();
            for (QualiaInputPattern qualiaInputPattern : pattern) {
                String[] csvEntry = Arrays.asList(
                        qualiaInputPattern.getRole(),
                        qualiaInputPattern.getPattern(),
                        String.valueOf(qualiaInputPattern.getAllowedPosTagsList())
                ).toArray(new String[0]);

                csvRecords_qualiaPatterns.add(csvEntry);
            }

            csvPrinter_qualiaPatterns.printRecords(csvRecords_qualiaPatterns);
        }

        public static List<List<Object>> executeQualiaStructuresForArgServices(
                Path pathToQualiaPatternsFile,
                Path pathToFileToPreprocess,
                Path pathToFilePreprocessed,
                Path pathToOutputDirectory,
                String language,
                String columnWithTextToProceed,
                List<String> queries) {

            LanguageManager languageManager = new LanguageManager(language);

            PreprocessText preprocessText = new PreprocessText(
                    new File(String.valueOf(pathToFileToPreprocess)),
                    columnWithTextToProceed,
                    pathToQualiaPatternsFile,
                    new File(String.valueOf(pathToFilePreprocessed)),
                    languageManager,
                    true);
            preprocessText.preprocessTextForQualiaSearch();

            Path pathToFileWithOutput = Paths.get(
                    String.valueOf(pathToOutputDirectory),
                    pathToFilePreprocessed.getFileName().toString().replace(".csv", "_withQualiaStructures.csv")
            );

            List<List<Object>> csvEntries = new ArrayList<>();

            if (queries.size() == 0) {
                QualiaIdentifier qualiaIdentifier = new QualiaIdentifier(
                        new File(String.valueOf(pathToFilePreprocessed)),
                        pathToQualiaPatternsFile,
                        null,
                        true,
                        pathToFileWithOutput,
                        languageManager,
                        true,
                        true);
                csvEntries = qualiaIdentifier.computeQualiaStructures();
            }
            for (String query : queries) {
                QualiaIdentifier qualiaIdentifier = new QualiaIdentifier(
                        new File(String.valueOf(pathToFilePreprocessed)),
                        pathToQualiaPatternsFile,
                        query,
                        true,
                        pathToFileWithOutput,
                        languageManager,
                        true,
                        true);
                csvEntries = qualiaIdentifier.computeQualiaStructures();
            }

            return csvEntries;
        }

    }
}
