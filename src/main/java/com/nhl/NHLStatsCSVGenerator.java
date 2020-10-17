package com.nhl;

import com.nhl.CardStatsExtractor.ExtractedCardStats;
import com.nhl.CardStatsExtractor.ExtractedCardStats.CardType;
import net.sourceforge.tess4j.Tesseract;
import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NHLStatsCSVGenerator {
    public static void main(String[] args) {
        final var dirAndTessPaths = getDirAndTessPaths(args);

        final var directory = dirAndTessPaths.getLeft();
        final var tessDataPath = dirAndTessPaths.getRight();

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

        outputToCsv(ExtractedCardStats.GOALIE_PROPERTIES, goalies.map(c -> c.getStats()).collect(Collectors.toList()), Paths.get(directory, "goalies.csv").toString());
        outputToCsv(ExtractedCardStats.SKATER_PROPERTIES, skaters.map(c -> c.getStats()).collect(Collectors.toList()), Paths.get(directory, "skaters.csv").toString());
    }

    private static Pair<String, String> getDirAndTessPaths(final String[] args) {
        final var options = new Options();

        final var dirOption = new Option("d", "directory", true, "Input directory (e.g., C:\\NHLData\\Anaheim Ducks)");
        dirOption.setRequired(true);
        options.addOption(dirOption);

        final var tessDataOption = new Option("t", "tessDataPath", true, "Tesseract data path (e.g., C:\\tessdata)");
        tessDataOption.setRequired(true);
        options.addOption(tessDataOption);

        HelpFormatter formatter = new HelpFormatter();

        try {
            DefaultParser parser = new DefaultParser();
            var result = parser.parse(options, args);
            return Pair.of(result.getOptionValue("directory"), result.getOptionValue("tessDataPath"));
        } catch (ParseException e) {
            System.out.println("Invalid arguments:");
            formatter.printHelp("utility-name", options);
            throw new RuntimeException(e);
        }
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
