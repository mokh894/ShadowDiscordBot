package com.shadowbot.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private String token;
    private String prefix = "!";
    private String ownerId;
    private String devGuildId;
    private String clientId;
    private String clientSecret;
    private DashboardConfig dashboard;

    public static Config load(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Missing config.json. Copy config.json to config.json and fill it in.");
        }
        try {
            return MAPPER.readValue(Files.readAllBytes(path), Config.class);
        } catch (IOException e) {
            throw new IOException("Failed to parse config at " + path.toAbsolutePath(), e);
        }
    }

    public String getToken() { return token; }
    public String getPrefix() { return prefix; }
    public String getOwnerId() { return ownerId; }
    public String getDevGuildId() { return devGuildId; }
    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public DashboardConfig getDashboard() { return dashboard; }

    public void setToken(String token) { this.token = token; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setDevGuildId(String devGuildId) { this.devGuildId = devGuildId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public void setDashboard(DashboardConfig dashboard) { this.dashboard = dashboard; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DashboardConfig {
        private boolean enabled = true;
        private String host = "localhost";
        private int port = 3000;
        private String baseUrl = "http://localhost:3000";
        private String sessionSecret = "default-secret";

        public boolean isEnabled() { return enabled; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getBaseUrl() { return baseUrl; }
        public String getSessionSecret() { return sessionSecret; }

        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setHost(String host) { this.host = host; }
        public void setPort(int port) { this.port = port; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public void setSessionSecret(String sessionSecret) { this.sessionSecret = sessionSecret; }
    }
}
