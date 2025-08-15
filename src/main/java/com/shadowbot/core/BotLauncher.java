package com.shadowbot.core;

import com.shadowbot.core.commands.CommandManager;
import com.shadowbot.core.commands.impl.admin.*;
import com.shadowbot.core.commands.impl.fun.*;
import com.shadowbot.core.commands.impl.fun.economy.*;
import com.shadowbot.core.commands.impl.admin.protection.AntiNukeCommand;
import com.shadowbot.core.commands.impl.admin.protection.AntiNukeLogsCommand;
import com.shadowbot.core.config.Config;
import com.shadowbot.core.events.BotEventListener;
import com.shadowbot.core.events.admin.antinuke.AntiNukeListener;
import com.shadowbot.core.services.AntiNukeService;
import com.shadowbot.core.services.AutoModService;
import com.shadowbot.core.services.BlocklistService;
import com.shadowbot.core.services.EconomyService;
import com.shadowbot.dashboard.DashboardApplication;
import com.shadowbot.dashboard.config.DashboardConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.nio.file.Path;
import java.util.EnumSet;

public class BotLauncher {
    public static void main(String[] args) throws Exception {
        Config config = Config.load(Path.of("config.json"));

        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_MODERATION
        );

        BlocklistService blocklistService = new BlocklistService(Path.of("data","blocklist.json"));
        AutoModService autoModService = new AutoModService();
        EconomyService economyService = new EconomyService(Path.of("data","economy.json"));
        AntiNukeService antiNukeService = new AntiNukeService(Path.of("data","antinuke.json"));

        CommandManager commandManager = new CommandManager(config.getPrefix());
        commandManager.registerBuiltins();
        
        // Admin commands
        commandManager.register(new BanCommand());
        commandManager.register(new KickCommand());
        commandManager.register(new PurgeCommand());
        commandManager.register(new NukeCommand());
        commandManager.register(new GiveRoleCommand());
        commandManager.register(new TakeRoleCommand());
        commandManager.register(new MuteCommand());
        commandManager.register(new UnmuteCommand());
        commandManager.register(new VoiceKickCommand());
        commandManager.register(new BlockWordCommand(blocklistService, autoModService));
        commandManager.register(new AutoModCommand(autoModService, blocklistService));
        
        // Protection commands
        commandManager.register(new AntiNukeCommand(antiNukeService));
        commandManager.register(new AntiNukeLogsCommand(antiNukeService));
        
        // Fun commands
        commandManager.register(new CatCommand());
        commandManager.register(new DogCommand());
        commandManager.register(new WeatherCommand());
        commandManager.register(new CoinFlipCommand());
        commandManager.register(new EightBallCommand());
        
        // Economy commands
        commandManager.register(new BalanceCommand(economyService));
        commandManager.register(new DailyCommand(economyService));
        commandManager.register(new GambleCommand(economyService));
        commandManager.register(new LeaderboardCommand(economyService));
        commandManager.register(new StatsCommand(economyService));

        JDA jda = JDABuilder.createDefault(config.getToken())
                .enableIntents(intents)
                .addEventListeners(
                    new BotEventListener(commandManager, blocklistService),
                    new AntiNukeListener(antiNukeService)
                )
                .build();

        jda.awaitReady();
        
        // Initialize anti-nuke data for all guilds
        jda.getGuilds().forEach(guild -> {
            antiNukeService.initializeGuild(guild.getIdLong());
        });
        
        System.out.println("Bot is ready. Logged in as: " + jda.getSelfUser().getAsTag());
        System.out.println("Anti-nuke system initialized for " + jda.getGuilds().size() + " guilds.");
        
        // Start dashboard if enabled
        if (config.getDashboard() != null && config.getDashboard().isEnabled()) {
            // Set system properties for Spring Boot
            System.setProperty("discord.client-id", config.getClientId());
            System.setProperty("discord.client-secret", config.getClientSecret());
            System.setProperty("server.port", String.valueOf(config.getDashboard().getPort()));
            
            // Inject services into dashboard configuration
            DashboardConfig.setJDA(jda);
            DashboardConfig.setAntiNukeService(antiNukeService);
            DashboardConfig.setBlocklistService(blocklistService);
            DashboardConfig.setEconomyService(economyService);
            
            // Start the dashboard
            DashboardApplication.startDashboard(args);
        } else {
            System.out.println("Dashboard is disabled in configuration.");
        }
    }
}
