package com.shadowbot.core.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JsonDataStore {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Map<String, Object> read(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new HashMap<>();
        }
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length == 0) {
            return new HashMap<>();
        }
        return MAPPER.readValue(bytes, new TypeReference<Map<String, Object>>() {});
    }

    public static void write(Path path, Map<String, Object> data) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        byte[] bytes = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
        Files.write(path, bytes);
    }
}
