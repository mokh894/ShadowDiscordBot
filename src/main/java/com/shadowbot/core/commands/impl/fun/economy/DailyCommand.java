package com.shadowbot.core.commands.impl.fun.economy;

import com.shadowbot.core.commands.Command;
import com.shadowbot.core.services.EconomyService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;

public class DailyCommand implements Command {
    private final EconomyService economyService;

    public DailyCommand(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public String name() { return "daily"; }

    @Override
    public String description() { return "Claim your daily coins (100 coins)."; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String userId = event.getAuthor().getId();
        
        if (!economyService.canClaimDaily(userId)) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("⏰ Daily Reward")
                    .setDescription("You've already claimed your daily reward today!\nCome back tomorrow for more coins.")
                    .setColor(Color.RED)
                    .setFooter("Economy System", null);
            
            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            return;
        }
        
        try {
            long claimed = economyService.claimDaily(userId);
            long newBalance = economyService.getBalance(userId);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🎁 Daily Reward Claimed!")
                    .setDescription("You received **" + claimed + "** coins!\n" +
                                   "Your new balance: **" + newBalance + "** coins")
                    .setColor(Color.GREEN)
                    .setThumbnail(event.getAuthor().getAvatarUrl())
                    .setFooter("Come back tomorrow for more!", null);
            
            event.getChannel().sendMessageEmbeds(embed.build()).queue();
        } catch (Exception e) {
            event.getChannel().sendMessage("❌ Failed to claim daily reward: " + e.getMessage()).queue();
        }
    }
}
