package com.shadowbot.core.commands.impl.fun.economy;

import com.shadowbot.core.commands.Command;
import com.shadowbot.core.services.EconomyService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;

public class BalanceCommand implements Command {
    private final EconomyService economyService;

    public BalanceCommand(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public String name() { return "balance"; }

    @Override
    public String description() { return "Check your coin balance."; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String userId = event.getAuthor().getId();
        long balance = economyService.getBalance(userId);
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("💰 Your Balance")
                .setDescription("**" + balance + "** coins")
                .setColor(Color.GREEN)
                .setThumbnail(event.getAuthor().getAvatarUrl())
                .setFooter("Economy System", null);
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
}
