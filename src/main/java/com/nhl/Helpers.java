package com.nhl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static <T> List<T> concat(final List<T> first, final List<T> second) {
        return Stream.concat(first.stream(), second.stream()).collect(Collectors.toList());
    }
}
