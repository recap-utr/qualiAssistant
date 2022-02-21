package utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.QuoteMode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class CSVFiles {


    public static final CSVFormat defaultCSVFormat = CSVFormat.DEFAULT
            .withDelimiter(';')
            .withQuoteMode(QuoteMode.MINIMAL)
            .withEscape('\\')
            ;

    public static CSVParser readCSVFile (Path pathToCSVFile) {

        try {
            return defaultCSVFormat
                    .withFirstRecordAsHeader()
                    .parse(
                            new BufferedReader(new InputStreamReader(new FileInputStream(String.valueOf(pathToCSVFile)), StandardCharsets.UTF_8))
                    );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
