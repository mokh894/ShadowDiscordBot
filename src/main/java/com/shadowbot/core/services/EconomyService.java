package com.shadowbot.core.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EconomyService {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Path economyPath;
    private final Map<String, UserEconomy> userEconomies = new ConcurrentHashMap<>();

    public EconomyService(Path economyPath) {
        this.economyPath = economyPath;
        try {
            load();
        } catch (IOException ignored) { }
    }

    public synchronized long getBalance(String userId) {
        return userEconomies.computeIfAbsent(userId, k -> new UserEconomy()).balance;
    }

    public synchronized boolean canClaimDaily(String userId) {
        UserEconomy user = userEconomies.computeIfAbsent(userId, k -> new UserEconomy());
        return !LocalDate.now().equals(user.lastDaily);
    }

    public synchronized long claimDaily(String userId) throws IOException {
        UserEconomy user = userEconomies.computeIfAbsent(userId, k -> new UserEconomy());
        if (LocalDate.now().equals(user.lastDaily)) {
            return 0; // Already claimed today
        }
        
        long dailyAmount = 100;
        user.balance += dailyAmount;
        user.lastDaily = LocalDate.now();
        persist();
        return dailyAmount;
    }

    public synchronized boolean deductCoins(String userId, long amount) throws IOException {
        UserEconomy user = userEconomies.computeIfAbsent(userId, k -> new UserEconomy());
        if (user.balance < amount) {
            return false;
        }
        user.balance -= amount;
        persist();
        return true;
    }

    public synchronized void addCoins(String userId, long amount) throws IOException {
        UserEconomy user = userEconomies.computeIfAbsent(userId, k -> new UserEconomy());
        user.balance += amount;
        persist();
    }

    public List<Map.Entry<String, Long>> getLeaderboard(int limit) {
        return userEconomies.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().balance))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public synchronized void load() throws IOException {
        if (!Files.exists(economyPath)) {
            return;
        }
        Map<String, UserEconomy> data = MAPPER.readValue(Files.readAllBytes(economyPath), new TypeReference<Map<String, UserEconomy>>(){});
        userEconomies.clear();
        userEconomies.putAll(data);
    }

    public synchronized void persist() throws IOException {
        if (economyPath.getParent() != null) {
            Files.createDirectories(economyPath.getParent());
        }
        byte[] out = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(userEconomies);
        Files.write(economyPath, out);
    }

        public synchronized void recordGamble(String userId, long amount, long winnings, boolean won) throws IOException {
        UserEconomy user = userEconomies.computeIfAbsent(userId, k -> new UserEconomy());
        user.totalGambles++;
        user.totalGambled += amount;
        if (won) {
            user.gamblesWon++;
            user.totalWinnings += winnings;
        } else {
            user.totalLosses += amount;
        }
        persist();
    }

    public GambleStats getGambleStats(String userId) {
        UserEconomy user = userEconomies.get(userId);
        if (user == null) {
            return new GambleStats();
        }
        return new GambleStats(user.totalGambles, user.gamblesWon, user.totalGambled, user.totalWinnings, user.totalLosses);
    }

    public static class UserEconomy {
        public long balance = 0;
        public LocalDate lastDaily = null;
        public long totalGambles = 0;
        public long gamblesWon = 0;
        public long totalGambled = 0;
        public long totalWinnings = 0;
        public long totalLosses = 0;

        public UserEconomy() {}
    }

    public static class GambleStats {
        public final long totalGambles;
        public final long gamblesWon;
        public final long totalGambled;
        public final long totalWinnings;
        public final long totalLosses;
        public final double winRate;
        public final long netProfit;

        public GambleStats() {
            this(0, 0, 0, 0, 0);
        }

        public GambleStats(long totalGambles, long gamblesWon, long totalGambled, long totalWinnings, long totalLosses) {
            this.totalGambles = totalGambles;
            this.gamblesWon = gamblesWon;
            this.totalGambled = totalGambled;
            this.totalWinnings = totalWinnings;
            this.totalLosses = totalLosses;
            this.winRate = totalGambles > 0 ? (double) gamblesWon / totalGambles * 100 : 0;
            this.netProfit = totalWinnings - totalLosses;
        }
    }
}
