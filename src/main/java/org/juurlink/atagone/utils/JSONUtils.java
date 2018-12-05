package org.juurlink.atagone.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON utils.
 */
@UtilityClass
@Log
public class JSONUtils {

    /**
     * Convert Java Map to JSON String, pretty printed.
     *
     * @param map Java Map
     * @return JSON as String
     */
    @Nonnull
    public static String toJSON(@Nonnull @NonNull Map map) {
        StringBuilder result = new StringBuilder();
        result.append("{");

        boolean first = true;
        @SuppressWarnings("unchecked")
        final Set<Entry> entrySet = map.entrySet();
        for (Entry entry : entrySet) {
            if (!first) {
                result.append(",");
            }
            first = false;
            result.append("\n    \"");
            result.append(entry.getKey()).append("\": ");
            if (entry.getValue() instanceof String) {
                result.append("\"").append(entry.getValue()).append("\"");
            } else {
                result.append(entry.getValue());
            }
        }
        result.append("\n}");
        return result.toString();
    }

    /**
     * Get value from JSON data blob. Only get primitive values like strings and numbers at any level. First value found is returned.
     *
     * @param json  Full JSON as String
     * @param clazz Class type of value; [String, BigInteger, Boolean]
     * @param name  Variable name to search for at any level
     * @return Value or null when not found
     * @throws IllegalStateException When requested value class not supported
     */
    @Nullable
    public static <T> T getJSONValueByName(@Nonnull @NonNull final String json,
                                           @Nonnull @NonNull final Class<T> clazz,
                                           @Nonnull @NonNull final String name) {

        // Real world JSON example.
        // {"isHeating":false,"targetTemp":"17.0","currentTemp":"16.9","vacationPlanned":false,"currentMode":"manual"}
        String escapedName = name.replaceAll("[^a-zA-Z0-9_]+", "");

        // We need to double escape the backslash in RegEx.
        // http://stackoverflow.com/questions/11769555/java-regular-expression-to-match-a-backslash-followed-by-a-quote
        Pattern pattern;
        if (clazz == String.class) {
            // String value has quotes arount the value.
            pattern = Pattern
                .compile("(?:\"|\\\\\"|)" + escapedName + "(?:\"|\\\\\"|)\\s*:\\s*((?:\"|\\\\\").*?(?:\"|\\\\\"))[,}\\s]", Pattern.DOTALL);
        } else {
            pattern = Pattern.compile("(?:\"|\\\\\"|)" + escapedName + "(?:\"|\\\\\"|)\\s*:\\s*(.+?)[,}\\s]", Pattern.DOTALL);
        }
        final Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            final String value = matcher.group(1);
            if (!value.isEmpty()) {
                // Replace Dutch decimal separator.
                if (clazz == String.class) {
                    if (value.length() >= 2) {
                        // Strip comments from string.
                        return clazz.cast(value.substring(1, value.length() - 1));
                    } else {
                        return null;
                    }
                }
                if (clazz == Boolean.class) {
                    return clazz.cast(Boolean.valueOf(value));
                }
                if (clazz == Integer.class) {
                    Integer anInt;
                    try {
                        anInt = Integer.parseInt(value, 10);
                    } catch (NumberFormatException e) {
                        // Parse error.
                        log.fine("Error parsing value '" + value + "' as Integer.");
                        anInt = null;
                    }
                    return clazz.cast(anInt);
                }
                if (clazz == BigDecimal.class) {
                    BigDecimal bigDecimal;
                    try {
                        bigDecimal = new BigDecimal(value);
                    } catch (Exception e) {
                        // Parse error.
                        log.fine("Error parsing value '" + value + "' as BigDecimal.");
                        bigDecimal = null;
                    }
                    return clazz.cast(bigDecimal);
                }
                throw new IllegalStateException("Unknown return type requested: '" + clazz + "'");
            }
        }
        return null;
    }
}
