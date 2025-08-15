package com.shadowbot.core.commands.impl.fun.economy;

import com.shadowbot.core.commands.Command;
import com.shadowbot.core.services.EconomyService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class LeaderboardCommand implements Command {
    private final EconomyService economyService;

    public LeaderboardCommand(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public String name() { return "leaderboard"; }

    @Override
    public String description() { return "View the top coin holders."; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        List<Map.Entry<String, Long>> leaderboard = economyService.getLeaderboard(10);
        
        if (leaderboard.isEmpty()) {
            event.getChannel().sendMessage("📊 No users found in the economy system yet!").queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏆 Coin Leaderboard")
                .setColor(Color.YELLOW)
                .setFooter("Economy System", null);

        StringBuilder description = new StringBuilder();
        for (int i = 0; i < leaderboard.size(); i++) {
            Map.Entry<String, Long> entry = leaderboard.get(i);
            String emoji = switch (i) {
                case 0 -> "🥇";
                case 1 -> "🥈";
                case 2 -> "🥉";
                default -> (i + 1) + ".";
            };
            
            // Try to get username, fallback to ID
            String username = "Unknown User";
            try {
                User user = event.getJDA().getUserById(entry.getKey());
                if (user != null) {
                    username = user.getName();
                }
            } catch (Exception ignored) { }
            
            description.append(emoji).append(" **").append(username).append("** - ")
                       .append(entry.getValue()).append(" coins\n");
        }

        embed.setDescription(description.toString());
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
}
