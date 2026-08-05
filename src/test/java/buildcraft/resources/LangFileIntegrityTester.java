package buildcraft.resources;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LangFileIntegrityTester {
    private static final String GUIDE_KEY_PREFIX = "buildcraft.guide.";

    @Test
    void everyExistingTranslationContainsAllNonGuideEnglishKeys() throws IOException {
        Path assets = Path.of("src/main/resources/assets");
        List<String> failures = new ArrayList<>();
        List<Path> languageFiles;

        try (Stream<Path> paths = Files.walk(assets)) {
            languageFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getParent() != null && "lang".equals(path.getParent().getFileName().toString()))
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .toList();
        }

        Set<Path> languageDirectories = new LinkedHashSet<>();
        for (Path language : languageFiles) {
            languageDirectories.add(language.getParent());
        }

        for (Path languageDirectory : languageDirectories) {
            Path english = languageDirectory.resolve("en_us.json");
            if (!Files.isRegularFile(english)) {
                failures.add(languageDirectory + " contains JSON translations but has no en_us.json fallback");
                continue;
            }

            JsonObject englishObject = readStringMap(english, failures);
            if (englishObject == null) {
                continue;
            }
            Set<String> requiredEnglishKeys = new HashSet<>(englishObject.keySet());
            // Guide Book text is currently authored in English and deliberately falls back to en_us.
            requiredEnglishKeys.removeIf(key -> key.startsWith(GUIDE_KEY_PREFIX));

            for (Path language : languageFiles) {
                if (!languageDirectory.equals(language.getParent()) || language.equals(english)) {
                    continue;
                }
                JsonObject translatedObject = readStringMap(language, failures);
                if (translatedObject == null) {
                    continue;
                }
                Set<String> missing = new HashSet<>(requiredEnglishKeys);
                missing.removeAll(translatedObject.keySet());
                if (!missing.isEmpty()) {
                    failures.add(language + " is missing " + missing.size() + " non-guide keys: "
                        + missing.stream().sorted().limit(12).toList());
                }
            }
        }

        Assertions.assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    private static JsonObject readStringMap(Path path, List<String> failures) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                failures.add(path + " must contain a JSON object");
                return null;
            }
            JsonObject object = parsed.getAsJsonObject();
            for (var entry : object.entrySet()) {
                JsonElement value = entry.getValue();
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    failures.add(path + " has a non-string value for key " + entry.getKey());
                }
            }
            return object;
        } catch (RuntimeException | IOException exception) {
            failures.add(path + " is not valid JSON: " + exception.getMessage());
            return null;
        }
    }
}
