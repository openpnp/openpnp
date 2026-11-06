package org.openpnp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Messages with placeholders are filled in with {@link MessageFormat} by
 * {@link Translations#format}. In that syntax a single apostrophe starts quoted text, so
 * "Can't move {0}" prints as "Cant move {0}" with the placeholder left in. An apostrophe has to be
 * written as two ('').
 */
public class TranslationsFormatTest {
    private static final File DIRECTORY = new File("src/main/resources/org/openpnp");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)[,}]");
    private static final Pattern SINGLE_APOSTROPHE = Pattern.compile("(?<!')'(?!')");
    /** Enough arguments for the message with the most placeholders. */
    private static final Object[] ARGUMENTS = new Object[30];

    static {
        for (int i = 0; i < ARGUMENTS.length; i++) {
            ARGUMENTS[i] = i + 1;
        }
    }

    @Test
    void everyPlaceholderIsFilledIn() throws Exception {
        List<String> problems = new ArrayList<>();
        File[] files = DIRECTORY.listFiles((dir, name) -> name.matches("translations.*\\.properties"));
        assertTrue(files != null && files.length > 0, "no translations files found");
        for (File file : files) {
            Properties properties = new Properties();
            try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()),
                    StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            for (String key : new TreeSet<>(properties.stringPropertyNames())) {
                String value = properties.getProperty(key);
                Matcher matcher = PLACEHOLDER.matcher(value);
                if (!matcher.find()) {
                    continue;
                }
                if (SINGLE_APOSTROPHE.matcher(value).find()) {
                    problems.add(file.getName() + " " + key
                            + ": single apostrophe, which MessageFormat drops; write ''");
                    continue;
                }
                String result;
                try {
                    result = MessageFormat.format(value, ARGUMENTS);
                }
                catch (IllegalArgumentException e) {
                    problems.add(file.getName() + " " + key + ": " + e.getMessage());
                    continue;
                }
                matcher.reset();
                while (matcher.find()) {
                    if (result.contains("{" + matcher.group(1))) {
                        problems.add(file.getName() + " " + key
                                + ": placeholder not filled in (single apostrophe?)");
                        break;
                    }
                }
            }
        }
        assertEquals("", String.join("\n", problems));
    }

    @Test
    void softLimitMessageShowsTheValuesAndTheApostrophe() throws Exception {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(
                Files.newInputStream(new File(DIRECTORY, "translations.properties").toPath()),
                StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        String message = MessageFormat.format(
                properties.getProperty("AbstractMotionPlanner.Error.SoftLimitHigh"), "Z", "5.000",
                "0.000");
        assertEquals("Can't move Z to 5.000, higher than soft limit 0.000.", message);
    }
}
