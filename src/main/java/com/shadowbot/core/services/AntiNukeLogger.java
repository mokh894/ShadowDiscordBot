package com.shadowbot.core.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AntiNukeLogger {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    
    private final Path baseDataPath;
    private final Map<Long, Map<Long, UserActionData>> serverUserData = new ConcurrentHashMap<>();
    
    public AntiNukeLogger(Path baseDataPath) {
        this.baseDataPath = baseDataPath;
    }
    
    // Record a user action
    public void recordAction(Guild guild, Member member, AntiNukeService.ActionType actionType) {
        long guildId = guild.getIdLong();
        long userId = member.getIdLong();
        LocalDateTime now = LocalDateTime.now();
        
        // Get or create server data
        Map<Long, UserActionData> guildData = serverUserData.computeIfAbsent(guildId, k -> new ConcurrentHashMap<>());
        
        // Get or create user data
        UserActionData userData = guildData.computeIfAbsent(userId, k -> new UserActionData(
            member.getUser().getAsTag(),
            member.getUser().getId()
        ));
        
        // Update action count and last action time
        userData.incrementAction(actionType, now);
        
        // Save to file
        saveUserData(guildId, userData);
        
        System.out.println("[ANTI-NUKE LOG] Recorded " + actionType + " for " + member.getUser().getAsTag() + 
                          " in " + guild.getName() + " (Total: " + userData.getTotalActions(actionType) + ")");
    }
    
    // Record a violation and punishment
    public void recordViolation(Guild guild, Member violator, AntiNukeService.ActionType actionType, 
                               AntiNukeService.PunishmentType punishment, String reason) {
        long guildId = guild.getIdLong();
        long userId = violator.getIdLong();
        LocalDateTime now = LocalDateTime.now();
        
        ViolationRecord violation = new ViolationRecord(
            violator.getUser().getAsTag(),
            violator.getUser().getId(),
            actionType.name(),
            punishment.name(),
            reason,
            now
        );
        
        // Save violation log
        saveViolationLog(guildId, violation);
        
        // Update user data with violation
        Map<Long, UserActionData> guildData = serverUserData.computeIfAbsent(guildId, k -> new ConcurrentHashMap<>());
        UserActionData userData = guildData.get(userId);
        if (userData != null) {
            userData.addViolation(violation);
            saveUserData(guildId, userData);
        }
        
        System.out.println("[ANTI-NUKE LOG] Violation recorded for " + violator.getUser().getAsTag() + 
                          " - " + punishment + " (" + reason + ")");
    }
    
    // Get user action data
    public UserActionData getUserData(long guildId, long userId) {
        return serverUserData.getOrDefault(guildId, new HashMap<>()).get(userId);
    }
    
    // Get all users for a guild
    public Map<Long, UserActionData> getGuildData(long guildId) {
        return serverUserData.getOrDefault(guildId, new HashMap<>());
    }
    
    // Load user data from file
    public void loadUserData(long guildId) {
        Path guildDir = getGuildDirectory(guildId);
        Path userDataFile = guildDir.resolve("user_data.json");
        
        if (Files.exists(userDataFile)) {
            try {
                Map<String, UserActionData> loaded = MAPPER.readValue(
                    userDataFile.toFile(),
                    new TypeReference<Map<String, UserActionData>>() {}
                );
                
                Map<Long, UserActionData> guildData = new ConcurrentHashMap<>();
                for (Map.Entry<String, UserActionData> entry : loaded.entrySet()) {
                    guildData.put(Long.parseLong(entry.getKey()), entry.getValue());
                }
                
                serverUserData.put(guildId, guildData);
                System.out.println("[ANTI-NUKE LOG] Loaded user data for guild " + guildId + " (" + guildData.size() + " users)");
                
            } catch (IOException | NumberFormatException e) {
                System.err.println("[ANTI-NUKE LOG] Failed to load user data for guild " + guildId + ": " + e.getMessage());
            }
        }
    }
    
    // Save user data to file
    private void saveUserData(long guildId, UserActionData userData) {
        try {
            Path guildDir = getGuildDirectory(guildId);
            Files.createDirectories(guildDir);
            
            Path userDataFile = guildDir.resolve("user_data.json");
            
            // Convert to string keys for JSON
            Map<String, UserActionData> toSave = new HashMap<>();
            Map<Long, UserActionData> guildData = serverUserData.get(guildId);
            if (guildData != null) {
                for (Map.Entry<Long, UserActionData> entry : guildData.entrySet()) {
                    toSave.put(entry.getKey().toString(), entry.getValue());
                }
            }
            
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(userDataFile.toFile(), toSave);
            
        } catch (IOException e) {
            System.err.println("[ANTI-NUKE LOG] Failed to save user data: " + e.getMessage());
        }
    }
    
    // Save violation log
    private void saveViolationLog(long guildId, ViolationRecord violation) {
        try {
            Path guildDir = getGuildDirectory(guildId);
            Files.createDirectories(guildDir);
            
            // Create monthly log files
            String monthKey = violation.timestamp.format(FILE_FORMATTER);
            Path violationFile = guildDir.resolve("violations_" + monthKey + ".json");
            
            List<ViolationRecord> violations = new ArrayList<>();
            
            // Load existing violations if file exists
            if (Files.exists(violationFile)) {
                try {
                    violations = MAPPER.readValue(
                        violationFile.toFile(),
                        new TypeReference<List<ViolationRecord>>() {}
                    );
                } catch (IOException e) {
                    System.err.println("[ANTI-NUKE LOG] Failed to load existing violations: " + e.getMessage());
                }
            }
            
            // Add new violation
            violations.add(violation);
            
            // Save updated violations
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(violationFile.toFile(), violations);
            
        } catch (IOException e) {
            System.err.println("[ANTI-NUKE LOG] Failed to save violation log: " + e.getMessage());
        }
    }
    
    // Get guild directory path
    private Path getGuildDirectory(long guildId) {
        return baseDataPath.resolve(String.valueOf(guildId)).resolve("antinuke");
    }
    
    // User action data class
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserActionData {
        public String username;
        public String userId;
        public Map<String, Integer> actionCounts = new HashMap<>();
        public Map<String, LocalDateTime> lastActionTimes = new HashMap<>();
        public List<ViolationRecord> violations = new ArrayList<>();
        public LocalDateTime firstSeen;
        public LocalDateTime lastSeen;
        
        public UserActionData() {}
        
        public UserActionData(String username, String userId) {
            this.username = username;
            this.userId = userId;
            this.firstSeen = LocalDateTime.now();
            this.lastSeen = LocalDateTime.now();
        }
        
        public void incrementAction(AntiNukeService.ActionType actionType, LocalDateTime timestamp) {
            String actionKey = actionType.name();
            actionCounts.put(actionKey, actionCounts.getOrDefault(actionKey, 0) + 1);
            lastActionTimes.put(actionKey, timestamp);
            lastSeen = timestamp;
        }
        
        public int getTotalActions(AntiNukeService.ActionType actionType) {
            return actionCounts.getOrDefault(actionType.name(), 0);
        }
        
        public LocalDateTime getLastActionTime(AntiNukeService.ActionType actionType) {
            return lastActionTimes.get(actionType.name());
        }
        
        public void addViolation(ViolationRecord violation) {
            violations.add(violation);
        }
        
        public int getViolationCount() {
            return violations.size();
        }
    }
    
    // Violation record class
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ViolationRecord {
        public String username;
        public String userId;
        public String actionType;
        public String punishment;
        public String reason;
        public LocalDateTime timestamp;
        
        public ViolationRecord() {}
        
        public ViolationRecord(String username, String userId, String actionType, 
                              String punishment, String reason, LocalDateTime timestamp) {
            this.username = username;
            this.userId = userId;
            this.actionType = actionType;
            this.punishment = punishment;
            this.reason = reason;
            this.timestamp = timestamp;
        }
    }
}
