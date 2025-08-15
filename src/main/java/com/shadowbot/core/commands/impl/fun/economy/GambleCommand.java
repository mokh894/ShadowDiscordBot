package com.shadowbot.core.commands.impl.fun.economy;

import com.shadowbot.core.commands.Command;
import com.shadowbot.core.services.EconomyService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.Random;

public class GambleCommand implements Command {
    private final EconomyService economyService;
    private static final Random RANDOM = new Random();

    public GambleCommand(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public String name() { return "gamble"; }

    @Override
    public String description() { return "Gamble your coins. Usage: !gamble <amount>"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (args.length < 1) {
            event.getChannel().sendMessage("Usage: `!gamble <amount>`\nExample: `!gamble 50`").queue();
            return;
        }

        String userId = event.getAuthor().getId();
        long currentBalance = economyService.getBalance(userId);
        
        long amount;
        try {
            if ("all".equalsIgnoreCase(args[0])) {
                amount = currentBalance;
            } else {
                amount = Long.parseLong(args[0]);
            }
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Please enter a valid number or 'all'.").queue();
            return;
        }

        if (amount <= 0) {
            event.getChannel().sendMessage("❌ You must gamble at least 1 coin!").queue();
            return;
        }

        if (amount > currentBalance) {
            event.getChannel().sendMessage("❌ You don't have enough coins! Your balance: **" + currentBalance + "**").queue();
            return;
        }

        try {
            if (!economyService.deductCoins(userId, amount)) {
                event.getChannel().sendMessage("❌ Failed to deduct coins.").queue();
                return;
            }

            // Gambling logic: 45% chance to win, 55% chance to lose
            boolean won = RANDOM.nextDouble() < 0.45;
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setThumbnail(event.getAuthor().getAvatarUrl())
                    .setFooter("Economy System", null);

            if (won) {
                // Win 1.5x to 2.5x the amount
                double multiplier = 1.5 + (RANDOM.nextDouble() * 1.0); // 1.5 to 2.5
                long winnings = (long) (amount * multiplier);
                economyService.addCoins(userId, winnings);
                economyService.recordGamble(userId, amount, winnings, true);
                long profit = winnings - amount;
                long newBalance = economyService.getBalance(userId);

                embed.setTitle("🎉 You Won!")
                        .setDescription("**Bet:** " + amount + " coins\n" +
                                       "**Won:** " + winnings + " coins\n" +
                                       "**Profit:** +" + profit + " coins\n" +
                                       "**New Balance:** " + newBalance + " coins")
                        .setColor(Color.GREEN);
            } else {
                economyService.recordGamble(userId, amount, 0, false);
                long newBalance = economyService.getBalance(userId);
                
                embed.setTitle("💸 You Lost!")
                        .setDescription("**Lost:** " + amount + " coins\n" +
                                       "**New Balance:** " + newBalance + " coins\n" +
                                       "Better luck next time!")
                        .setColor(Color.RED);
            }

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
        } catch (Exception e) {
            event.getChannel().sendMessage("❌ Failed to process gamble: " + e.getMessage()).queue();
        }
    }
}
