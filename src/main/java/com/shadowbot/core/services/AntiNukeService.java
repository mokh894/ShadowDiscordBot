package com.shadowbot.core.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiNukeService {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    private final Path configPath;
    private final Map<Long, AntiNukeConfig> guildConfigs = new ConcurrentHashMap<>();
    private final Map<String, List<LocalDateTime>> actionHistory = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastPunishment = new ConcurrentHashMap<>(); // Track last punishment time
    private final AntiNukeLogger logger;
    
    public AntiNukeService(Path configPath) {
        this.configPath = configPath;
        this.logger = new AntiNukeLogger(configPath.getParent().resolve("data"));
        load();
    }
    
    // Configuration methods
    public AntiNukeConfig getConfig(long guildId) {
        return guildConfigs.getOrDefault(guildId, new AntiNukeConfig());
    }
    
    public void updateConfig(long guildId, AntiNukeConfig config) {
        guildConfigs.put(guildId, config);
        persist();
    }
    
    public boolean isEnabled(long guildId) {
        return getConfig(guildId).enabled;
    }
    
    public void setEnabled(long guildId, boolean enabled) {
        AntiNukeConfig config = getConfig(guildId);
        config.enabled = enabled;
        updateConfig(guildId, config);
    }
    
    // Get logger for external access
    public AntiNukeLogger getLogger() {
        return logger;
    }
    
    // Initialize guild data (load user data when guild is accessed)
    public void initializeGuild(long guildId) {
        logger.loadUserData(guildId);
    }
    
    // Permission checking
    public boolean canManageAntiNuke(Guild guild, Member member) {
        // Only server owner or bot developer can manage anti-nuke
        return guild.getOwner() != null && guild.getOwner().equals(member) || 
               isBotDeveloper(member.getUser());
    }
    
    private boolean isBotDeveloper(User user) {
       if(user.getId().equals("1075533908613021727")) {
              return true; // Replace with actual developer ID check
       }
       return false;
    }
    
    // Rate limiting and violation detection
    public boolean checkAndRecordAction(long guildId, long userId, ActionType actionType, 
                                       net.dv8tion.jda.api.entities.Guild guild, 
                                       net.dv8tion.jda.api.entities.Member member) {
        if (!isEnabled(guildId)) {
            return false; // Not enabled, allow action
        }
        
        // Record action in logger
        logger.recordAction(guild, member, actionType);
        
        AntiNukeConfig config = getConfig(guildId);
        String key = guildId + ":" + userId + ":" + actionType.name();
        
        LocalDateTime now = LocalDateTime.now();
        List<LocalDateTime> history = actionHistory.computeIfAbsent(key, k -> new ArrayList<>());
        
        // Clean old entries (older than time window)
        history.removeIf(time -> time.isBefore(now.minusMinutes(config.timeWindowMinutes)));
        
        // Add current action
        history.add(now);
        
        // Check if limit exceeded
        int limit = getLimitForAction(config, actionType);
        boolean violated = history.size() > limit;
        
        // Only log when violation is detected or when close to limit
        if (violated || history.size() > (limit * 0.8)) {
            System.out.println("[ANTI-NUKE] " + actionType + " count: " + history.size() + "/" + limit + 
                " for user " + userId + " - " + (violated ? "VIOLATION!" : "Warning"));
        }
        
        return violated;
    }
    
    private int getLimitForAction(AntiNukeConfig config, ActionType actionType) {
        switch (actionType) {
            case CHANNEL_CREATE:
            case CHANNEL_DELETE:
                return config.channelLimit;
            case ROLE_CREATE:
            case ROLE_DELETE:
                return config.roleLimit;
            case WEBHOOK_CREATE:
                return config.webhookLimit;
            case MEMBER_KICK:
                return config.kickLimit;
            case MEMBER_BAN:
                return config.banLimit;
            default:
                return Integer.MAX_VALUE;
        }
    }
    
    // Punishment execution with cooldown and logging
    public void executePunishment(Guild guild, Member violator, PunishmentType punishment, String reason, ActionType actionType) {
        String punishmentKey = guild.getId() + ":" + violator.getId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastPunishmentTime = lastPunishment.get(punishmentKey);
        
        // Check if punishment was executed recently (within 30 seconds)
        if (lastPunishmentTime != null && lastPunishmentTime.isAfter(now.minusSeconds(30))) {
            System.out.println("[ANTI-NUKE DEBUG] Punishment cooldown active - skipping repeated punishment for " + violator.getUser().getAsTag());
            return;
        }
        
        // Record punishment time
        lastPunishment.put(punishmentKey, now);
        
        // Log violation and punishment
        logger.recordViolation(guild, violator, actionType, punishment, reason);
        
        System.out.println("[ANTI-NUKE DEBUG] Executing punishment: " + punishment + " for " + violator.getUser().getAsTag());
        
        switch (punishment) {
            case STRIP_ROLES:
                stripAllRoles(guild, violator, reason);
                break;
            case KICK:
                kickMember(guild, violator, reason);
                break;
            case BAN:
                banMember(guild, violator, reason);
                break;
        }
    }
    
    private void stripAllRoles(Guild guild, Member member, String reason) {
        List<net.dv8tion.jda.api.entities.Role> currentRoles = new ArrayList<>(member.getRoles());
        List<net.dv8tion.jda.api.entities.Role> rolesToRemove = new ArrayList<>();
        
        // Only remove roles that the bot can interact with
        for (net.dv8tion.jda.api.entities.Role role : currentRoles) {
            if (guild.getSelfMember().canInteract(role)) {
                rolesToRemove.add(role);
            }
        }
        
        if (!rolesToRemove.isEmpty()) {
            guild.modifyMemberRoles(member, Collections.emptyList(), rolesToRemove)
                    .reason("Anti-Nuke: " + reason)
                    .queue();
        }
    }
    
    private void kickMember(Guild guild, Member member, String reason) {
        if (guild.getSelfMember().canInteract(member)) {
            guild.kick(member)
                    .reason("Anti-Nuke: " + reason)
                    .queue();
        }
    }
    
    private void banMember(Guild guild, Member member, String reason) {
        if (guild.getSelfMember().canInteract(member)) {
            guild.ban(member.getUser(), 0, java.util.concurrent.TimeUnit.DAYS)
                    .reason("Anti-Nuke: " + reason)
                    .queue();
        }
    }
    
    // JSON persistence
    private void load() {
        try {
            if (Files.exists(configPath)) {
                Map<String, AntiNukeConfig> loaded = MAPPER.readValue(
                    configPath.toFile(),
                    new TypeReference<Map<String, AntiNukeConfig>>() {}
                );
                
                // Convert string keys to long keys
                for (Map.Entry<String, AntiNukeConfig> entry : loaded.entrySet()) {
                    guildConfigs.put(Long.parseLong(entry.getKey()), entry.getValue());
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Failed to load anti-nuke config: " + e.getMessage());
        }
    }
    
    private void persist() {
        try {
            Files.createDirectories(configPath.getParent());
            
            // Convert long keys to string keys for JSON
            Map<String, AntiNukeConfig> toSave = new HashMap<>();
            for (Map.Entry<Long, AntiNukeConfig> entry : guildConfigs.entrySet()) {
                toSave.put(entry.getKey().toString(), entry.getValue());
            }
            
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), toSave);
        } catch (IOException e) {
            System.err.println("Failed to save anti-nuke config: " + e.getMessage());
        }
    }
    
    // Configuration class
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AntiNukeConfig {
        public boolean enabled = false;
        public int channelLimit = 5;        // Max channels created/deleted per time window
        public int roleLimit = 5;           // Max roles created/deleted per time window
        public int webhookLimit = 3;        // Max webhooks created per time window
        public int kickLimit = 5;           // Max members kicked per time window
        public int banLimit = 3;            // Max members banned per time window
        public int timeWindowMinutes = 5;   // Time window for rate limiting
        public PunishmentType punishment = PunishmentType.STRIP_ROLES;
        public String logChannelId = null;  // ID of the log channel
        public boolean webhookProtection = true; // Toggle webhook protection
        
        public AntiNukeConfig() {}
    }
    
    // Enums
    public enum ActionType {
        CHANNEL_CREATE,
        CHANNEL_DELETE,
        ROLE_CREATE,
        ROLE_DELETE,
        WEBHOOK_CREATE,
        MEMBER_KICK,
        MEMBER_BAN
    }
    
    public enum PunishmentType {
        STRIP_ROLES("Strip all roles"),
        KICK("Kick from server"),
        BAN("Ban from server");
        
        private final String displayName;
        
        PunishmentType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
