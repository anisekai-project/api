package fr.anisekai.utils;


import fr.anisekai.core.internal.plannifier.interfaces.entities.Planifiable;

import java.util.function.Function;

/**
 * Utility class providing helper methods for string formatting and parsing.
 */
public final class StringUtils {

    private StringUtils() {}

    /**
     * Cut the provided {@link CharSequence} to size, appending '...', if its length is above the provided threshold.
     *
     * @param charSequence
     *         The {@link CharSequence} to truncate if necessary
     * @param threshold
     *         Amount of character max allowed (including '...')
     *
     * @return The {@link CharSequence}, truncated if necessary
     */
    public static String truncate(CharSequence charSequence, int threshold) {

        if (threshold < 3) {
            throw new IllegalArgumentException("Threshold must be at least 3 to accommodate '...'");
        }

        if (charSequence.length() > threshold) {
            return charSequence.subSequence(0, threshold - 3) + "...";
        }
        return charSequence.toString();
    }

    /**
     * Cut the provided {@link CharSequence} to size, appending '...' within the string to leave {@code after}
     * characters afterward if its length is above the provided threshold.
     *
     * @param charSequence
     *         The {@link CharSequence} to truncate if necessary
     * @param threshold
     *         Amount of character max allowed (including '...')
     * @param after
     *         Amount of character to leave after the cut with '...'
     *
     * @return The {@link CharSequence}, truncated if necessary
     */
    public static String truncate(CharSequence charSequence, int threshold, int after) {

        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold must be non-negative");
        }

        if (after < 0) {
            throw new IllegalArgumentException("After must be non-negative");
        }

        if (threshold < 3) {
            throw new IllegalArgumentException("Threshold must be at least 3 to accommodate '...'");
        }

        if (after >= threshold - 3) {
            throw new IllegalArgumentException("After must be less than threshold - 3");
        }

        int length = charSequence.length();
        if (length <= threshold) {
            return charSequence.toString();
        }

        int    prefixLength = threshold - 3 - after;
        String prefix       = charSequence.subSequence(0, prefixLength).toString();
        String suffix       = charSequence.subSequence(length - after, length).toString();

        return prefix + "..." + suffix;
    }

    /**
     * Returns a formatted string based on the value of the count.
     *
     * @param count
     *         The value to evaluate
     * @param none
     *         The format string to use when {@code count == 0}
     * @param one
     *         The format string to use when {@code count == 1}
     * @param multiple
     *         The format string to use when {@code count > 1}
     *
     * @return The formatted string corresponding to the count
     */
    public static String count(int count, String none, String one, String multiple) {

        if (count == 0) {
            return String.format(none, count);
        } else if (count == 1) {
            return String.format(one, count);
        } else {
            return String.format(multiple, count);
        }
    }

    /**
     * Retrieve a formatted description of the given {@link Planifiable} object. Emphasizes the label by default.
     *
     * @param planifiable
     *         The planifiable object
     *
     * @return The formatted string description
     */
    public static String getPlanifiableDescription(Planifiable<?> planifiable) {

        return getPlanifiableDescription(planifiable, true);
    }

    /**
     * Retrieve a formatted description of the given {@link Planifiable} object.
     *
     * @param planifiable
     *         The planifiable object
     * @param emphasize
     *         Whether to emphasize the episode label (e.g., bold)
     *
     * @return The formatted string description
     */
    public static String getPlanifiableDescription(Planifiable<?> planifiable, boolean emphasize) {

        long amount = planifiable.getEpisodeCount();

        if (amount == 1) {
            String prefix = emphasize ? "**Épisode**" : "épisode";
            return String.format("%s %02d", prefix, planifiable.getFirstEpisode());
        } else if (amount == 2) {
            String prefix = emphasize ? "**Épisodes**" : "épisodes";
            return String.format(
                    "%s %02d et %02d",
                    prefix,
                    planifiable.getFirstEpisode(),
                    planifiable.getLastEpisode()
            );
        } else {
            String prefix = emphasize ? "**Épisodes**" : "épisodes";
            return String.format(
                    "%s %02d à %02d",
                    prefix,
                    planifiable.getFirstEpisode(),
                    planifiable.getLastEpisode()
            );
        }
    }

    /**
     * Attempts to parse an integer from the input string. If parsing fails, the given exception supplier is used to
     * throw a custom exception.
     *
     * @param input
     *         The input string to parse
     * @param exFunc
     *         The function that maps the {@link NumberFormatException} to a custom {@link RuntimeException}
     *
     * @return The parsed integer
     *
     * @throws RuntimeException
     *         Threw if parsing fails
     */
    public static int parseIntOrThrow(String input, Function<NumberFormatException, ? extends RuntimeException> exFunc) {

        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw exFunc.apply(e);
        }
    }

}
