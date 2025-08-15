package com.shadowbot.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DiscordService {
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public List<DiscordGuild> getUserGuilds(OAuth2User user) {
        List<DiscordGuild> guilds = new ArrayList<>();
        
        try {
            // Get access token from user attributes
            String accessToken = getAccessToken(user);
            if (accessToken == null) {
                return guilds;
            }
            
            // Make request to Discord API
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://discord.com/api/users/@me/guilds"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode guildsJson = objectMapper.readTree(response.body());
                
                for (JsonNode guildJson : guildsJson) {
                    DiscordGuild guild = new DiscordGuild();
                    guild.setId(guildJson.get("id").asText());
                    guild.setName(guildJson.get("name").asText());
                    guild.setIcon(guildJson.has("icon") && !guildJson.get("icon").isNull() ? 
                                 guildJson.get("icon").asText() : null);
                    guild.setPermissions(guildJson.get("permissions").asLong());
                    
                    // Only include guilds where user has admin or manage server permissions
                    if (hasRequiredPermissions(guild.getPermissions())) {
                        guilds.add(guild);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error fetching user guilds: " + e.getMessage());
        }
        
        return guilds;
    }
    
    private String getAccessToken(OAuth2User user) {
        // Try to get access token from user attributes
        Map<String, Object> attributes = user.getAttributes();
        
        // The access token might be stored differently depending on the OAuth2 implementation
        if (attributes.containsKey("access_token")) {
            return (String) attributes.get("access_token");
        }
        
        // If not directly available, we might need to implement token storage
        // For now, return null to handle gracefully
        return null;
    }
    
    private boolean hasRequiredPermissions(long permissions) {
        // Check for Administrator (0x8) or Manage Server (0x20) permissions
        return (permissions & 0x8L) == 0x8L || (permissions & 0x20L) == 0x20L;
    }
    
    public static class DiscordGuild {
        private String id;
        private String name;
        private String icon;
        private long permissions;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        
        public long getPermissions() { return permissions; }
        public void setPermissions(long permissions) { this.permissions = permissions; }
        
        public boolean hasAdminPermissions() {
            return (permissions & 0x8L) == 0x8L;
        }
        
        public boolean hasManageServerPermissions() {
            return (permissions & 0x20L) == 0x20L;
        }
        
        public String getIconUrl() {
            if (icon != null) {
                return "https://cdn.discordapp.com/icons/" + id + "/" + icon + ".png";
            }
            return null;
        }
    }
}
