package com.nhl;

public class Helpers {
    public static <T> T tryGetOrDefault(final T[] arr, final int index, final T defaultVal) {
        if (arr.length < index) {
            return defaultVal;
        }

        return arr[index];
    }

    public static int tryParse(final String string, final int defaultVal) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
