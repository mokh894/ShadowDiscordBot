package com.shadowbot.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class GuildConfigService {
    
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    private final Path dataPath = Paths.get("data", "data");
    
    public GuildConfigService() {
        try {
            Files.createDirectories(dataPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create data directory", e);
        }
    }
    
    public GuildConfig getGuildConfig(String guildId) {
        Path guildPath = dataPath.resolve(guildId);
        Path configFile = guildPath.resolve("config.json");
        
        try {
            if (Files.exists(configFile)) {
                return MAPPER.readValue(configFile.toFile(), GuildConfig.class);
            }
        } catch (IOException e) {
            System.err.println("Error reading guild config for " + guildId + ": " + e.getMessage());
        }
        
        // Return default config
        return new GuildConfig();
    }
    
    public boolean saveGuildConfig(String guildId, GuildConfig config) {
        Path guildPath = dataPath.resolve(guildId);
        Path configFile = guildPath.resolve("config.json");
        
        try {
            Files.createDirectories(guildPath);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(configFile.toFile(), config);
            return true;
        } catch (IOException e) {
            System.err.println("Error saving guild config for " + guildId + ": " + e.getMessage());
            return false;
        }
    }
    
    public static class GuildConfig {
        private String prefix = "!";
        private Map<String, Boolean> enabledCommands = new HashMap<>();
        private Map<String, String> commandAliases = new HashMap<>();
        private AntiNukeConfig antiNuke = new AntiNukeConfig();
        
        public GuildConfig() {
            // Initialize default enabled commands
            initializeDefaultCommands();
        }
        
        private void initializeDefaultCommands() {
            // General commands
            enabledCommands.put("help", true);
            enabledCommands.put("about", true);
            enabledCommands.put("ping", true);
            enabledCommands.put("serverinfo", true);
            
            // Moderation commands
            enabledCommands.put("ban", true);
            enabledCommands.put("kick", true);
            enabledCommands.put("mute", true);
            enabledCommands.put("unmute", true);
            enabledCommands.put("purge", true);
            enabledCommands.put("nuke", true);
            enabledCommands.put("giverole", true);
            enabledCommands.put("takerole", true);
            enabledCommands.put("voicekick", true);
            enabledCommands.put("blockword", true);
            
            // Fun commands
            enabledCommands.put("cat", true);
            enabledCommands.put("dog", true);
            enabledCommands.put("weather", true);
            enabledCommands.put("coinflip", true);
            enabledCommands.put("8ball", true);
            
            // Economy commands
            enabledCommands.put("balance", true);
            enabledCommands.put("daily", true);
            enabledCommands.put("gamble", true);
            enabledCommands.put("leaderboard", true);
            enabledCommands.put("stats", true);
            
            // Protection commands
            enabledCommands.put("antinuke", true);
            enabledCommands.put("antilog", true);
        }
        
        // Getters and setters
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        
        public Map<String, Boolean> getEnabledCommands() { return enabledCommands; }
        public void setEnabledCommands(Map<String, Boolean> enabledCommands) { this.enabledCommands = enabledCommands; }
        
        public Map<String, String> getCommandAliases() { return commandAliases; }
        public void setCommandAliases(Map<String, String> commandAliases) { this.commandAliases = commandAliases; }
        
        public AntiNukeConfig getAntiNuke() { return antiNuke; }
        public void setAntiNuke(AntiNukeConfig antiNuke) { this.antiNuke = antiNuke; }
    }
    
    public static class AntiNukeConfig {
        private boolean enabled = false;
        private int channelLimit = 5;
        private int roleLimit = 5;
        private int webhookLimit = 3;
        private int kickLimit = 5;
        private int banLimit = 3;
        private int timeWindowMinutes = 5;
        private String punishment = "STRIP_ROLES";
        private String logChannelId = null;
        private boolean webhookProtection = true;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public int getChannelLimit() { return channelLimit; }
        public void setChannelLimit(int channelLimit) { this.channelLimit = channelLimit; }
        
        public int getRoleLimit() { return roleLimit; }
        public void setRoleLimit(int roleLimit) { this.roleLimit = roleLimit; }
        
        public int getWebhookLimit() { return webhookLimit; }
        public void setWebhookLimit(int webhookLimit) { this.webhookLimit = webhookLimit; }
        
        public int getKickLimit() { return kickLimit; }
        public void setKickLimit(int kickLimit) { this.kickLimit = kickLimit; }
        
        public int getBanLimit() { return banLimit; }
        public void setBanLimit(int banLimit) { this.banLimit = banLimit; }
        
        public int getTimeWindowMinutes() { return timeWindowMinutes; }
        public void setTimeWindowMinutes(int timeWindowMinutes) { this.timeWindowMinutes = timeWindowMinutes; }
        
        public String getPunishment() { return punishment; }
        public void setPunishment(String punishment) { this.punishment = punishment; }
        
        public String getLogChannelId() { return logChannelId; }
        public void setLogChannelId(String logChannelId) { this.logChannelId = logChannelId; }
        
        public boolean isWebhookProtection() { return webhookProtection; }
        public void setWebhookProtection(boolean webhookProtection) { this.webhookProtection = webhookProtection; }
    }
}
