package com.shadowbot.core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ApiService {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // The Cat API - free, no key required
    public static CompletableFuture<String> getRandomCat() {
        return makeRequest("https://api.thecatapi.com/v1/images/search")
                .thenApply(response -> {
                    try {
                        JsonNode json = MAPPER.readTree(response);
                        return json.get(0).get("url").asText();
                    } catch (Exception e) {
                        return "https://cataas.com/cat"; // Fallback API
                    }
                });
    }

    // Dog CEO API - free, no key required
    public static CompletableFuture<String> getRandomDog() {
        return makeRequest("https://dog.ceo/api/breeds/image/random")
                .thenApply(response -> {
                    try {
                        JsonNode json = MAPPER.readTree(response);
                        return json.get("message").asText();
                    } catch (Exception e) {
                        return "https://random.dog/woof.json"; // Fallback would need different parsing
                    }
                });
    }

    // wttr.in - free weather API, no key required
    public static CompletableFuture<String> getWeather(String city) {
        String url = "https://wttr.in/" + city.replace(" ", "+") + "?format=j1";
        return makeRequest(url)
                .thenApply(response -> {
                    try {
                        JsonNode json = MAPPER.readTree(response);
                        JsonNode current = json.get("current_condition").get(0);
                        JsonNode area = json.get("nearest_area").get(0);
                        
                        String location = area.get("areaName").get(0).get("value").asText();
                        String country = area.get("country").get(0).get("value").asText();
                        String temp = current.get("temp_C").asText();
                        String feelsLike = current.get("FeelsLikeC").asText();
                        String desc = current.get("weatherDesc").get(0).get("value").asText();
                        String humidity = current.get("humidity").asText();
                        String windSpeed = current.get("windspeedKmph").asText();
                        
                        return String.format("🌤️ **%s, %s**\n" +
                                "🌡️ **Temperature:** %s°C (feels like %s°C)\n" +
                                "☁️ **Condition:** %s\n" +
                                "💧 **Humidity:** %s%%\n" +
                                "💨 **Wind Speed:** %s km/h",
                                location, country, temp, feelsLike, desc, humidity, windSpeed);
                    } catch (Exception e) {
                        return "❌ Could not fetch weather data for: " + city;
                    }
                });
    }

    private static CompletableFuture<String> makeRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "ShadowBot/1.0")
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .exceptionally(throwable -> {
                    System.err.println("API request failed: " + throwable.getMessage());
                    return "{}";
                });
    }
}
