package buildcraft.resources;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResourceJsonTester {
    @Test
    void everySourceResourceJsonParses() throws IOException {
        List<String> failures = new ArrayList<>();
        for (Path root : List.of(Path.of("src/main/resources"), Path.of("src/generated/resources"))) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(path -> path.toString().endsWith(".json")).forEach(path -> parse(path, failures));
            }
        }
        Assertions.assertTrue(failures.isEmpty(), () -> "Invalid JSON resources:\n" + String.join("\n", failures));
    }

    @Test
    void packMetadataIsPinnedToMinecraft1192() throws IOException {
        Path path = Path.of("src/main/resources/pack.mcmeta");
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            Assertions.assertEquals(9, root.getAsJsonObject().getAsJsonObject("pack").get("pack_format").getAsInt());
        }
    }

    private static void parse(Path path, List<String> failures) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || element.isJsonNull()) {
                failures.add(path + ": empty JSON document");
            }
        } catch (Exception exception) {
            failures.add(path + ": " + exception.getMessage());
        }
    }
}
