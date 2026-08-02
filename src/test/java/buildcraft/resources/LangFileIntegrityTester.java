package buildcraft.resources;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LangFileIntegrityTester {
    @Test
    void everyExistingTranslationContainsAllEnglishKeys() throws IOException {
        Path assets = Path.of("src/main/resources/assets");
        List<String> failures = new ArrayList<>();

        try (Stream<Path> namespaces = Files.list(assets)) {
            for (Path namespace : namespaces.filter(Files::isDirectory).toList()) {
                Path langDir = namespace.resolve("lang");
                Path english = langDir.resolve("en_us.json");
                if (!Files.isRegularFile(english)) {
                    continue;
                }
                Set<String> englishKeys = readKeys(english);
                try (Stream<Path> languages = Files.list(langDir)) {
                    for (Path language : languages
                        .filter(path -> path.toString().endsWith(".json"))
                        .filter(path -> !path.equals(english))
                        .toList()) {
                        Set<String> missing = new HashSet<>(englishKeys);
                        missing.removeAll(readKeys(language));
                        if (!missing.isEmpty()) {
                            failures.add(language + " is missing " + missing.size() + " keys: "
                                + missing.stream().sorted().limit(12).toList());
                        }
                    }
                }
            }
        }

        Assertions.assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    private static Set<String> readKeys(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            return object.keySet();
        }
    }
}
