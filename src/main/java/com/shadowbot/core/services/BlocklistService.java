package com.shadowbot.core.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class BlocklistService {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Path storePath;
    private final Map<Long, GuildBlockConfig> guildConfigById = new ConcurrentHashMap<>();
    private final Map<Long, List<Pattern>> compiledPatternsByGuild = new ConcurrentHashMap<>();

    public BlocklistService(Path storePath) {
        this.storePath = storePath;
        try {
            load();
        } catch (IOException ignored) { }
    }

    public synchronized void addWord(long guildId, String word) throws IOException {
        GuildBlockConfig cfg = guildConfigById.computeIfAbsent(guildId, id -> GuildBlockConfig.createDefault());
        cfg.words.add(word);
        String generated = RegexGenerator.generate(word, cfg.options);
        cfg.generatedRegex.add(generated);
        recompile(guildId);
        persist();
    }

    public synchronized void addPattern(long guildId, String regex) throws IOException {
        GuildBlockConfig cfg = guildConfigById.computeIfAbsent(guildId, id -> GuildBlockConfig.createDefault());
        cfg.customRegex.add(regex);
        recompile(guildId);
        persist();
    }

    public List<Pattern> getPatterns(long guildId) {
        return compiledPatternsByGuild.getOrDefault(guildId, Collections.emptyList());
    }

    public GuildBlockConfig getConfig(long guildId) {
        return guildConfigById.computeIfAbsent(guildId, id -> GuildBlockConfig.createDefault());
    }

    public synchronized void updateOptions(long guildId, RegexOptions options) throws IOException {
        GuildBlockConfig cfg = guildConfigById.computeIfAbsent(guildId, id -> GuildBlockConfig.createDefault());
        cfg.options = options;
        // Regenerate from words when options change
        cfg.generatedRegex.clear();
        for (String w : cfg.words) {
            cfg.generatedRegex.add(RegexGenerator.generate(w, options));
        }
        recompile(guildId);
        persist();
    }

    private void recompile(long guildId) {
        GuildBlockConfig cfg = guildConfigById.get(guildId);
        if (cfg == null) {
            compiledPatternsByGuild.remove(guildId);
            return;
        }
        List<Pattern> compiled = new ArrayList<>();
        for (String r : cfg.generatedRegex) {
            try { compiled.add(Pattern.compile(r, Pattern.CASE_INSENSITIVE)); } catch (Exception ignored) { }
        }
        for (String r : cfg.customRegex) {
            try { compiled.add(Pattern.compile(r, Pattern.CASE_INSENSITIVE)); } catch (Exception ignored) { }
        }
        compiledPatternsByGuild.put(guildId, compiled);
    }

    public synchronized void load() throws IOException {
        if (!Files.exists(storePath)) {
            return;
        }
        Map<Long, GuildBlockConfig> data = MAPPER.readValue(Files.readAllBytes(storePath), new TypeReference<Map<Long, GuildBlockConfig>>(){});
        guildConfigById.clear();
        compiledPatternsByGuild.clear();
        guildConfigById.putAll(data);
        for (Long gid : guildConfigById.keySet()) {
            recompile(gid);
        }
    }

    public synchronized void persist() throws IOException {
        if (storePath.getParent() != null) {
            Files.createDirectories(storePath.getParent());
        }
        byte[] out = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(guildConfigById);
        Files.write(storePath, out);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GuildBlockConfig {
        public List<String> words = new ArrayList<>();
        public List<String> generatedRegex = new ArrayList<>();
        public List<String> customRegex = new ArrayList<>();
        public RegexOptions options = RegexOptions.defaultOptions();

        public static GuildBlockConfig createDefault() {
            return new GuildBlockConfig();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RegexOptions {
        public boolean useLeetSubstitutions = true;
        public boolean useWordBoundaries = true;
        public int maxNonWordSeparatorsBetweenLetters = 2;

        public static RegexOptions defaultOptions() {
            return new RegexOptions();
        }
    }
}
