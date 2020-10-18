package com.nhl;

import com.google.common.collect.ImmutableMap;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.collections.list.UnmodifiableList;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.nhl.Helpers.tryGetOrDefault;
import static com.nhl.Helpers.tryParse;

public class CardStatsExtractor {
    private static final int INCHES_PER_FOOT = 12;
    private static final double CM_PER_INCH = 2.54;
    private static final Rectangle REQUIRED_INPUT_SIZE = new Rectangle(1920, 1080);

    //Card stats are laid out on a grid, with 10 rows and 3 columns
    private static int ROW_1_PX = 430;
    private static int ROW_2_PX = 460;
    private static int ROW_3_PX = 495;
    private static int ROW_4_PX = 525;
    private static int ROW_5_PX = 550;
    private static int ROW_6_PX = 694;
    private static int ROW_7_PX = 725;
    private static int ROW_8_PX = 752;
    private static int ROW_9_PX = 786;
    private static int ROW_10_PX = 812;

    private static int COL_1_PX = 1085;
    private static int COL_2_PX = 1377;
    private static int COL_3_PX = 1670;

    private static Bounds FIRST_CATEGORY_BOUNDS = Bounds.text("FIRST_CATEGORY", 937, 376, 1137, 407);
    private static String GOALIE_EXPECTED_FIRST_CATEGORY = "HIGH";

    private static Bounds NAME_BOUNDS = Bounds.text("NAME", 693, 175, 1800, 246);
    private static Bounds HEIGHT_FT_BOUNDS = Bounds.numeric("HEIGHT_FT", 700, 277, 723, 308);
    private static Bounds HEIGHT_INCHES_BOUNDS = Bounds.height("HEIGHT_INCHES", 730, 277, 767, 315);
    private static Bounds METADATA_BOUNDS = Bounds.all("METADATA", 695, 281, 1913, 320);
    private static Bounds SYNERGY_1_BOUNDS = Bounds.all("SYNERGY_1", 771, 430, 905, 460);
    private static Bounds SYNERGY_2_BOUNDS = Bounds.all("SYNERGY_2", 771, 527, 905, 556);

    private static Map<String, String> SYNERGIES_MAP = ImmutableMap
            .<String, String>builder()
            .put("gladiator","GL")
            .put("howitzer","HW")
            .put("passing playmaker","PP")
            .put("speedster","SP")
            .put("light the lamp","LL")
            .put("magician","M")
            .put("thief","T")
            .put("wingman","WM")
            .put("shutdown","SD")
            .put("workhorse","WH")
            .put("balanced","BA")
            .put("barrage","BR")
            .put("distributor","DB")
            .put("heart and soul","HS")
            .put("spark","S")
            .put("swarm","SW")
            .build();

    private static final java.util.List<Bounds> GOALIE_STAT_BOUNDS = java.util.List.of(
            //HIGH
            Bounds.numeric("glove_high", COL_1_PX, ROW_1_PX),
            Bounds.numeric("stick_high", COL_1_PX, ROW_2_PX),

            //LOW
            Bounds.numeric("glove_low", COL_2_PX, ROW_1_PX),
            Bounds.numeric("poke_check", COL_2_PX, ROW_2_PX),
            Bounds.numeric("stick_low", COL_2_PX, ROW_3_PX),
            Bounds.numeric("passing", COL_2_PX, ROW_4_PX),

            //QUICKNESS
            Bounds.numeric("speed", COL_3_PX, ROW_1_PX),
            Bounds.numeric("vision", COL_3_PX, ROW_2_PX),
            Bounds.numeric("agility", COL_3_PX, ROW_3_PX),

            //POSITION
            Bounds.numeric("positioning", COL_1_PX, ROW_6_PX),
            Bounds.numeric("five_hole", COL_1_PX, ROW_7_PX),
            Bounds.numeric("breakaway", COL_1_PX, ROW_8_PX),

            //REBOUND CONTROL
            Bounds.numeric("aggression", COL_2_PX, ROW_6_PX),
            Bounds.numeric("rebound_control", COL_2_PX, ROW_7_PX),
            Bounds.numeric("shot_recovery", COL_2_PX, ROW_8_PX)
    );

    private static final java.util.List<Bounds> SKATER_STAT_BOUNDS = List.of(
            //SKATING
            Bounds.numeric("acceleration", COL_1_PX, ROW_1_PX),
            Bounds.numeric("agility", COL_1_PX, ROW_2_PX),
            Bounds.numeric("balance", COL_1_PX, ROW_3_PX),
            Bounds.numeric("endurance", COL_1_PX, ROW_4_PX),
            Bounds.numeric("speed", COL_1_PX, ROW_5_PX),

            //SHOOTING
            Bounds.numeric("slap_shot_accuracy", COL_2_PX, ROW_1_PX),
            Bounds.numeric("slap_shot_power", COL_2_PX, ROW_2_PX),
            Bounds.numeric("wrist_shot_accuracy", COL_2_PX, ROW_3_PX),
            Bounds.numeric("wrist_shot_power", COL_2_PX, ROW_4_PX),

            //HANDS
            Bounds.numeric("deking", COL_3_PX, ROW_1_PX),
            Bounds.numeric("off_awareness", COL_3_PX, ROW_2_PX),
            Bounds.numeric("hand_eye", COL_3_PX, ROW_3_PX),
            Bounds.numeric("passing", COL_3_PX, ROW_4_PX),
            Bounds.numeric("puck_control", COL_3_PX, ROW_5_PX),

            //DEFENSE
            Bounds.numeric("body_checking", COL_1_PX, ROW_6_PX),
            Bounds.numeric("strength", COL_1_PX, ROW_7_PX),
            Bounds.numeric("aggression", COL_1_PX, ROW_8_PX),
            Bounds.numeric("durability", COL_1_PX, ROW_9_PX),
            Bounds.numeric("fighting_skill", COL_1_PX, ROW_10_PX),

            //DEFENSE
            Bounds.numeric("off_awareness", COL_2_PX, ROW_6_PX),
            Bounds.numeric("shot_blocking", COL_2_PX, ROW_7_PX),
            Bounds.numeric("stick_checking", COL_2_PX, ROW_8_PX),
            Bounds.numeric("faceoffs", COL_2_PX, ROW_9_PX),
            Bounds.numeric("discipline", COL_2_PX, ROW_10_PX)
    );

    private enum BoundType {
        TEXT,
        ANY,
        NUMERIC,
        HEIGHT
    }

    private Tesseract ocr;

    public CardStatsExtractor(final Tesseract ocr) {
        this.ocr = ocr;
    }

    public Optional<ExtractedCardStats> extract(final BufferedImage buf) {
        if (buf.getWidth() != REQUIRED_INPUT_SIZE.width && buf.getHeight() != REQUIRED_INPUT_SIZE.height) {
            return Optional.empty();
        }

        final var category = extract(ocr, buf, FIRST_CATEGORY_BOUNDS);

        final List<Pair<String, String>> properties = new ArrayList<>();
        final List<Bounds> statBounds;

        ExtractedCardStats.CardType type;
        if (category.getRight().equalsIgnoreCase(GOALIE_EXPECTED_FIRST_CATEGORY)) {
            type = ExtractedCardStats.CardType.GOALIE;
            statBounds = GOALIE_STAT_BOUNDS;
        } else {
            type = ExtractedCardStats.CardType.SKATER;
            statBounds = SKATER_STAT_BOUNDS;
        }

        final var fullName = extract(ocr, buf, NAME_BOUNDS).getRight();
        final var fullNameSplit = fullName != null ? fullName.split(" ") : new String[]{};
        properties.add(Pair.of("first_name", tryGetOrDefault(fullNameSplit, 0, "")));
        properties.add(Pair.of("last_name", tryGetOrDefault(fullNameSplit, 1, "")));
        properties.add(Pair.of("height", extractHeightInCm(buf)));
        properties.addAll(extractNonHeightMetadata(ocr, buf));
        properties.add(Pair.of("synergies", extractSynergies(buf)));
        statBounds.forEach(bounds -> {
            var extracted = extract(ocr, buf, bounds);
            //hacking it so that extracted stats equals just "7" implies "77" due to poor ocr
            if (extracted.getRight().equals("7")) {
                extracted = Pair.of(extracted.getLeft(), "77");
            }
            properties.add(extracted);
        });

        return Optional.of(new ExtractedCardStats(type, properties.stream().map(p -> p.getRight()).collect(Collectors.toList())));
    }

    private String extractHeightInCm(final BufferedImage buf) {
        var heightFt = (extract(ocr, buf, HEIGHT_FT_BOUNDS)).getRight();
        var heightInches = extract(ocr, buf, HEIGHT_INCHES_BOUNDS).getRight();

        //can't fix this, hacking it for now
        if (heightInches.equalsIgnoreCase("111")) {
            heightInches = "11";
        }

        return String.valueOf(toCentimeters(heightFt, heightInches));
    }

    private int toCentimeters(final String ftStr, final String inchesStr) {
        final int feet = tryParse(ftStr, 0);
        final int inches = tryParse(inchesStr, 0);
        return (int)((feet * INCHES_PER_FOOT + inches) * CM_PER_INCH);
    }

    private static List<Pair<String, String>> extractNonHeightMetadata(final Tesseract ocr, final BufferedImage img) {
        final List<Pair<String, String>> properties = new ArrayList<>();
        final var metadata = extract(ocr, img, METADATA_BOUNDS);
        final var splitByPipe = metadata.getRight().replaceAll("\\[", "|").replaceAll("]", "|").split("\\|");
        Arrays.stream(splitByPipe).skip(1).forEach(subPipe -> {
            final var splitByColon = subPipe.split(":");
            if (splitByColon.length == 1) {
                properties.add(Pair.of("WEIGHT", splitByColon[0].trim().replaceAll("LBS", "").replaceAll("B", "8").replaceAll("18S", "")));
            } else {
                properties.add(Pair.of(splitByColon[0].trim(), splitByColon[1].trim()));
            }
        });

        return properties;
    }

    private String extractSynergies(final BufferedImage buf) {
        return Stream
                .of(
                    extractSynergyCode(buf, SYNERGY_1_BOUNDS),
                    extractSynergyCode(buf, SYNERGY_2_BOUNDS))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));

    }
    private String extractSynergyCode(final BufferedImage img, final Bounds bounds) {
        final var synergy = extract(ocr, img, bounds);
        final var synergySplit = synergy.getRight().split("\\(|\\[");

        var result = synergySplit.length <= 1 ? synergy.getRight() : synergySplit[0];
        result = result != null ? result.trim() : "";

        final String synergyCode = SYNERGIES_MAP.getOrDefault(result.toLowerCase(), null);
        return synergyCode;
    }

    private static Pair<String, String> extract(final Tesseract ocr, final BufferedImage img, final Bounds bounds) {
        try {
            switch (bounds.boundType) {
                case TEXT -> ocr.setTessVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZ- ");
                case NUMERIC -> ocr.setTessVariable("tessedit_char_whitelist", "0123456789");
                case HEIGHT -> ocr.setTessVariable("tessedit_char_whitelist", "0123456789");
                case ANY -> ocr.setTessVariable("tessedit_char_whitelist", "");
                default -> throw new IllegalStateException();
            }

            return Pair.of(bounds.category, ocr.doOCR(img, bounds.toRect()).replaceAll("\n", "").trim());
        } catch (TesseractException e) {
            e.printStackTrace();
            return null;
        }

    }

    private static class Bounds {
        private static int NUMERIC_WIDTH = 40;
        private static int NUMERIC_HEIGHT = 30;

        private String category;
        private BoundType boundType;
        private int startX;
        private int startY;
        private int endX;
        private int endY;

        private Bounds(String category, BoundType boundType, int startX, int startY, int endX, int endY) {
            this.category = category;
            this.boundType = boundType;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }

        public static Bounds numeric(String category, int startX, int startY) {
            return new Bounds(category, BoundType.NUMERIC, startX, startY, startX + NUMERIC_WIDTH, startY + NUMERIC_HEIGHT);
        }

        public static Bounds numeric(String category, int startX, int startY, int endX, int endY) {
            return new Bounds(category, BoundType.NUMERIC, startX, startY, endX, endY);
        }

        public static Bounds all(String category, int startX, int startY, int endX, int endY) {
            return new Bounds(category, BoundType.ANY, startX, startY, endX, endY);
        }

        public static Bounds text(String category, int startX, int startY, int endX, int endY) {
            return new Bounds(category, BoundType.TEXT, startX, startY, endX, endY);
        }

        public static Bounds height(String category, int startX, int startY, int endX, int endY) {
            return new Bounds(category, BoundType.HEIGHT, startX, startY, endX, endY);
        }

        private Rectangle toRect() {
            return new Rectangle(startX, startY, endX - startX, endY - startY);
        }
    }

    public static class ExtractedCardStats {
        private static final List<String> SHARED_PROPERTIES = Stream.of("first_name", "last_name", "height", "weight", "handedness", "nationality", "age", "salary", "synergies").collect(Collectors.toList());
        public static final List<String> GOALIE_PROPERTIES = UnmodifiableList.decorate(Stream.concat(SHARED_PROPERTIES.stream(), GOALIE_STAT_BOUNDS.stream().map(b -> b.category)).collect(Collectors.toList()));
        public static final List<String> SKATER_PROPERTIES = UnmodifiableList.decorate(Stream.concat(SHARED_PROPERTIES.stream(), SKATER_STAT_BOUNDS.stream().map(b -> b.category)).collect(Collectors.toList()));

        public enum CardType {
            GOALIE,
            SKATER
        }

        private CardType cardType;
        private List<String> stats;

        public ExtractedCardStats(final CardType cardType, final List<String> stats) {
            this.cardType = cardType;
            this.stats = stats;
        }

        public CardType getCardType() {
            return cardType;
        }

        public List<String> getStats() {
            return stats;
        }

    }
}

