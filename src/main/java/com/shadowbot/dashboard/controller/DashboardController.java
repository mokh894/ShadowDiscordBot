package com.shadowbot.dashboard.controller;

import com.shadowbot.dashboard.service.DiscordService;
import com.shadowbot.dashboard.service.GuildConfigService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {
    
    @Autowired
    private DiscordService discordService;
    
    @Autowired
    private GuildConfigService guildConfigService;
    
    @Autowired
    private JDA jda;
    
    @GetMapping
    public String dashboard(@AuthenticationPrincipal OAuth2User user, Model model) {
        List<DiscordService.DiscordGuild> userGuilds = discordService.getUserGuilds(user);
        
        // Filter guilds where the bot is present
        List<DiscordService.DiscordGuild> botGuilds = userGuilds.stream()
                .filter(guild -> jda.getGuildById(guild.getId()) != null)
                .collect(Collectors.toList());
        
        model.addAttribute("user", user);
        model.addAttribute("guilds", botGuilds);
        return "dashboard";
    }
    
    @GetMapping("/{guildId}")
    public String guildDashboard(@PathVariable String guildId, 
                                @AuthenticationPrincipal OAuth2User user, 
                                Model model) {
        
        // Verify user has access to this guild
        List<DiscordService.DiscordGuild> userGuilds = discordService.getUserGuilds(user);
        DiscordService.DiscordGuild userGuild = userGuilds.stream()
                .filter(guild -> guild.getId().equals(guildId))
                .findFirst()
                .orElse(null);
        
        if (userGuild == null) {
            return "error/403";
        }
        
        // Get JDA guild info
        Guild jdaGuild = jda.getGuildById(guildId);
        if (jdaGuild == null) {
            return "error/404";
        }
        
        // Get guild configuration
        GuildConfigService.GuildConfig config = guildConfigService.getGuildConfig(guildId);
        
        model.addAttribute("user", user);
        model.addAttribute("guild", userGuild);
        model.addAttribute("jdaGuild", jdaGuild);
        model.addAttribute("config", config);
        
        return "guild-dashboard";
    }
    
    @GetMapping("/{guildId}/config")
    public String guildConfig(@PathVariable String guildId, 
                             @AuthenticationPrincipal OAuth2User user, 
                             Model model) {
        
        // Verify user has access to this guild
        List<DiscordService.DiscordGuild> userGuilds = discordService.getUserGuilds(user);
        DiscordService.DiscordGuild userGuild = userGuilds.stream()
                .filter(guild -> guild.getId().equals(guildId))
                .findFirst()
                .orElse(null);
        
        if (userGuild == null) {
            return "error/403";
        }
        
        // Get JDA guild info
        Guild jdaGuild = jda.getGuildById(guildId);
        if (jdaGuild == null) {
            return "error/404";
        }
        
        // Get guild configuration
        GuildConfigService.GuildConfig config = guildConfigService.getGuildConfig(guildId);
        
        model.addAttribute("user", user);
        model.addAttribute("guild", userGuild);
        model.addAttribute("jdaGuild", jdaGuild);
        model.addAttribute("config", config);
        
        return "config";
    }
    
    @GetMapping("/{guildId}/antinuke")
    public String antiNukeConfig(@PathVariable String guildId, 
                                @AuthenticationPrincipal OAuth2User user, 
                                Model model) {
        
        // Verify user has access to this guild
        List<DiscordService.DiscordGuild> userGuilds = discordService.getUserGuilds(user);
        DiscordService.DiscordGuild userGuild = userGuilds.stream()
                .filter(guild -> guild.getId().equals(guildId))
                .findFirst()
                .orElse(null);
        
        if (userGuild == null) {
            return "error/403";
        }
        
        // Get JDA guild info
        Guild jdaGuild = jda.getGuildById(guildId);
        if (jdaGuild == null) {
            return "error/404";
        }
        
        // Get guild configuration
        GuildConfigService.GuildConfig config = guildConfigService.getGuildConfig(guildId);
        
        model.addAttribute("user", user);
        model.addAttribute("guild", userGuild);
        model.addAttribute("jdaGuild", jdaGuild);
        model.addAttribute("config", config);
        model.addAttribute("antiNuke", config.getAntiNuke());
        
        return "antinuke";
    }
}
