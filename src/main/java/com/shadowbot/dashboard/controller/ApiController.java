package com.shadowbot.dashboard.controller;

import com.shadowbot.dashboard.service.DiscordService;
import com.shadowbot.dashboard.service.GuildConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    
    @Autowired
    private DiscordService discordService;
    
    @Autowired
    private GuildConfigService guildConfigService;
    
    @PostMapping("/guild/{guildId}/config")
    public ResponseEntity<Map<String, Object>> saveGuildConfig(
            @PathVariable String guildId,
            @RequestBody Map<String, Object> configData,
            @AuthenticationPrincipal OAuth2User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Verify user has access to this guild
        List<DiscordService.DiscordGuild> userGuilds = discordService.getUserGuilds(user);
        boolean hasAccess = userGuilds.stream()
                .anyMatch(guild -> guild.getId().equals(guildId));
        
        if (!hasAccess) {
            response.put("success", false);
            response.put("message", "Access denied");
            return ResponseEntity.status(403).body(response);
        }
        
        try {
            // Get current config
            GuildConfigService.GuildConfig config = guildConfigService.getGuildConfig(guildId);
            
            // Update prefix
            if (configData.containsKey("prefix")) {
                String prefix = (String) configData.get("prefix");
                if (prefix != null && !prefix.trim().isEmpty() && prefix.length() <= 5) {
                    config.setPrefix(prefix.trim());
                }
            }
            
            // Update enabled commands
            if (configData.containsKey("enabledCommands")) {
                @SuppressWarnings("unchecked")
                Map<String, Boolean> enabledCommands = (Map<String, Boolean>) configData.get("enabledCommands");
                config.setEnabledCommands(enabledCommands);
            }
            
            // Update command aliases
            if (configData.containsKey("commandAliases")) {
                @SuppressWarnings("unchecked")
                Map<String, String> commandAliases = (Map<String, String>) configData.get("commandAliases");
                config.setCommandAliases(commandAliases);
            }
            
            // Save configuration
            boolean saved = guildConfigService.saveGuildConfig(guildId, config);
            
            if (saved) {
                response.put("success", true);
                response.put("message", "Configuration saved successfully!");
            } else {
                response.put("success", false);
                response.put("message", "Failed to save configuration");
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/guild/{guildId}/antinuke")
    public ResponseEntity<Map<String, Object>> saveAntiNukeConfig(
            @PathVariable String guildId,
            @RequestBody Map<String, Object> antiNukeData,
            @AuthenticationPrincipal OAuth2User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Verify user has access to this guild
        List<DiscordService.DiscordGuild> userGuilds = discordService.getUserGuilds(user);
        boolean hasAccess = userGuilds.stream()
                .anyMatch(guild -> guild.getId().equals(guildId));
        
        if (!hasAccess) {
            response.put("success", false);
            response.put("message", "Access denied");
            return ResponseEntity.status(403).body(response);
        }
        
        try {
            // Get current config
            GuildConfigService.GuildConfig config = guildConfigService.getGuildConfig(guildId);
            GuildConfigService.AntiNukeConfig antiNuke = config.getAntiNuke();
            
            // Update anti-nuke settings
            if (antiNukeData.containsKey("enabled")) {
                antiNuke.setEnabled((Boolean) antiNukeData.get("enabled"));
            }
            
            if (antiNukeData.containsKey("channelLimit")) {
                antiNuke.setChannelLimit(((Number) antiNukeData.get("channelLimit")).intValue());
            }
            
            if (antiNukeData.containsKey("roleLimit")) {
                antiNuke.setRoleLimit(((Number) antiNukeData.get("roleLimit")).intValue());
            }
            
            if (antiNukeData.containsKey("webhookLimit")) {
                antiNuke.setWebhookLimit(((Number) antiNukeData.get("webhookLimit")).intValue());
            }
            
            if (antiNukeData.containsKey("kickLimit")) {
                antiNuke.setKickLimit(((Number) antiNukeData.get("kickLimit")).intValue());
            }
            
            if (antiNukeData.containsKey("banLimit")) {
                antiNuke.setBanLimit(((Number) antiNukeData.get("banLimit")).intValue());
            }
            
            if (antiNukeData.containsKey("timeWindowMinutes")) {
                antiNuke.setTimeWindowMinutes(((Number) antiNukeData.get("timeWindowMinutes")).intValue());
            }
            
            if (antiNukeData.containsKey("punishment")) {
                antiNuke.setPunishment((String) antiNukeData.get("punishment"));
            }
            
            if (antiNukeData.containsKey("logChannelId")) {
                antiNuke.setLogChannelId((String) antiNukeData.get("logChannelId"));
            }
            
            if (antiNukeData.containsKey("webhookProtection")) {
                antiNuke.setWebhookProtection((Boolean) antiNukeData.get("webhookProtection"));
            }
            
            // Save configuration
            boolean saved = guildConfigService.saveGuildConfig(guildId, config);
            
            if (saved) {
                response.put("success", true);
                response.put("message", "Anti-nuke configuration saved successfully!");
            } else {
                response.put("success", false);
                response.put("message", "Failed to save anti-nuke configuration");
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/guild/{guildId}/status")
    public ResponseEntity<Map<String, Object>> getGuildStatus(
            @PathVariable String guildId,
            @AuthenticationPrincipal OAuth2User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Verify user has access to this guild
        List<DiscordService.DiscordGuild> userGuilds = discordService.getUserGuilds(user);
        boolean hasAccess = userGuilds.stream()
                .anyMatch(guild -> guild.getId().equals(guildId));
        
        if (!hasAccess) {
            response.put("success", false);
            response.put("message", "Access denied");
            return ResponseEntity.status(403).body(response);
        }
        
        try {
            GuildConfigService.GuildConfig config = guildConfigService.getGuildConfig(guildId);
            
            response.put("success", true);
            response.put("prefix", config.getPrefix());
            response.put("antiNukeEnabled", config.getAntiNuke().isEnabled());
            response.put("commandCount", config.getEnabledCommands().size());
            response.put("enabledCommandCount", config.getEnabledCommands().values().stream()
                    .mapToInt(enabled -> enabled ? 1 : 0).sum());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
}
