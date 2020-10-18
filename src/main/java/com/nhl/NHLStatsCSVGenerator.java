package com.nhl;

import com.nhl.CardStatsExtractor.ExtractedCardStats;
import com.nhl.CardStatsExtractor.ExtractedCardStats.CardType;
import net.sourceforge.tess4j.Tesseract;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.nhl.Helpers.concat;

public class NHLStatsCSVGenerator {
    private static final List<String> SHARED_CSV_COLUMNS = List.of(
            "card_type_id",
            "league_id",
            "approved",
            "card_level"
    );

    public static void main(String[] args) throws IOException {
        final var directory = prompt("Directory for card images? E.g., C:/NHLData/Anaheim Ducks/");
        final var tessDataPath = prompt("Directory for tesseract training data? E.g., C:/tessdata");
        final List<String> sharedCsvColumnData = List.of(
                prompt("card_type_id?"),
                prompt("league_id?"),
                prompt("approved?"),
                prompt("card_level?")
        );

        final var dir = new File(directory);
        final var files = dir.listFiles((dir1, name) -> name.endsWith(".png"));

        final List<ExtractedCardStats> allCards = Stream
                .of(files)
                .parallel()
                .map(file -> file.getAbsolutePath())
                .map(path -> {
                    System.out.println("Processing \"" + path + "\"...");
                    return BufferedImageUtils.readPath(path);
                })
                .flatMap(Optional::stream)
                .map(img -> BufferedImageUtils.invert(img))
                .map(img -> createExtractor(tessDataPath).extract(img))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        final var goalies = allCards.stream().filter(c -> c.getCardType() == CardType.GOALIE);
        final var skaters = allCards.stream().filter(c -> c.getCardType() == CardType.SKATER);

        outputToCsv(
                concat(SHARED_CSV_COLUMNS, ExtractedCardStats.GOALIE_PROPERTIES),
                goalies.map(c -> concat(sharedCsvColumnData, c.getStats())).collect(Collectors.toList()),
                Paths.get(directory, "goalies.csv").toString());
        outputToCsv(
                concat(SHARED_CSV_COLUMNS, ExtractedCardStats.SKATER_PROPERTIES),
                skaters.map(c -> concat(sharedCsvColumnData, c.getStats())).collect(Collectors.toList()),
                Paths.get(directory, "skaters.csv").toString());
    }

    private static String prompt(final String message) throws IOException {
        final var reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(message);
        return reader.readLine();
    }

    private static void outputToCsv(final List<String> header, final List<List<String>> values, final String outputPath) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(header.toArray(new String[0])));
        ) {
            for (List<String> value : values) {
                csvPrinter.printRecord(value);
            }
            csvPrinter.flush();
            System.out.println("Wrote " + values.size() + " entries to " + outputPath);
        } catch (IOException e) {
            System.out.println("Problem writing CSV to " + outputPath + ":" + e.getMessage());
        }
    }

    private static CardStatsExtractor createExtractor(final String tessDataPath) {
        final var tes = new Tesseract();
        tes.setDatapath(tessDataPath);
        tes.setTessVariable("user_defined_dpi", "70");
        return new CardStatsExtractor(tes);
    }
}
