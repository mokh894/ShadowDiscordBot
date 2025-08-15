package com.shadowbot.core.commands.impl.fun.economy;

import com.shadowbot.core.commands.Command;
import com.shadowbot.core.services.EconomyService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;

public class StatsCommand implements Command {
    private final EconomyService economyService;

    public StatsCommand(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public String name() { return "stats"; }

    @Override
    public String description() { return "View your gambling statistics."; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String userId = event.getAuthor().getId();
        EconomyService.GambleStats stats = economyService.getGambleStats(userId);
        long balance = economyService.getBalance(userId);
        
        if (stats.totalGambles == 0) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📊 Your Statistics")
                    .setDescription("You haven't gambled yet!\nUse `!gamble <amount>` to start.")
                    .addField("💰 Current Balance", balance + " coins", true)
                    .setColor(Color.BLUE)
                    .setThumbnail(event.getAuthor().getAvatarUrl())
                    .setFooter("Economy System", null);
            
            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            return;
        }

        String winRateColor = stats.winRate >= 50 ? "🟢" : stats.winRate >= 30 ? "🟡" : "🔴";
        String profitColor = stats.netProfit >= 0 ? "📈" : "📉";
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📊 Your Gambling Statistics")
                .setColor(stats.netProfit >= 0 ? Color.GREEN : Color.RED)
                .setThumbnail(event.getAuthor().getAvatarUrl())
                .setFooter("Economy System", null);

        embed.addField("💰 Current Balance", balance + " coins", true);
        embed.addField("🎰 Total Gambles", String.valueOf(stats.totalGambles), true);
        embed.addField(winRateColor + " Win Rate", String.format("%.1f%% (%d/%d)", 
                stats.winRate, stats.gamblesWon, stats.totalGambles), true);

        embed.addField("💸 Total Gambled", stats.totalGambled + " coins", true);
        embed.addField("🎉 Total Winnings", stats.totalWinnings + " coins", true);
        embed.addField(profitColor + " Net Profit", (stats.netProfit >= 0 ? "+" : "") + stats.netProfit + " coins", true);

        if (stats.netProfit >= 0) {
            embed.setDescription("🎊 **You're in profit!** Keep it up!");
        } else {
            embed.setDescription("💔 **You're at a loss.** Gamble responsibly!");
        }

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
}
