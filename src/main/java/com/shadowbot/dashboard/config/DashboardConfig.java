package com.shadowbot.dashboard.config;

import com.shadowbot.core.services.AntiNukeService;
import com.shadowbot.core.services.BlocklistService;
import com.shadowbot.core.services.EconomyService;
import net.dv8tion.jda.api.JDA;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DashboardConfig {
    
    private static JDA jda;
    private static AntiNukeService antiNukeService;
    private static BlocklistService blocklistService;
    private static EconomyService economyService;
    
    // Static setters to inject services from BotLauncher
    public static void setJDA(JDA jda) {
        DashboardConfig.jda = jda;
    }
    
    public static void setAntiNukeService(AntiNukeService antiNukeService) {
        DashboardConfig.antiNukeService = antiNukeService;
    }
    
    public static void setBlocklistService(BlocklistService blocklistService) {
        DashboardConfig.blocklistService = blocklistService;
    }
    
    public static void setEconomyService(EconomyService economyService) {
        DashboardConfig.economyService = economyService;
    }
    
    @Bean
    public JDA jda() {
        return jda;
    }
    
    @Bean
    public AntiNukeService antiNukeService() {
        return antiNukeService;
    }
    
    @Bean
    public BlocklistService blocklistService() {
        return blocklistService;
    }
    
    @Bean
    public EconomyService economyService() {
        return economyService;
    }
}
