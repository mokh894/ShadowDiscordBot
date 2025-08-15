package com.shadowbot.core.commands.impl.fun;

import com.shadowbot.core.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.Random;

public class CoinFlipCommand implements Command {
    private static final Random RANDOM = new Random();

    @Override
    public String name() { return "coinflip"; }

    @Override
    public String description() { return "Flip a coin - heads or tails!"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        boolean isHeads = RANDOM.nextBoolean();
        String result = isHeads ? "Heads" : "Tails";
        String emoji = isHeads ? "🪙" : "🔄";
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(emoji + " Coin Flip Result")
                .setDescription("**" + result + "**")
                .setColor(isHeads ? Color.YELLOW : Color.GRAY)
                .setFooter("Flipped by " + event.getAuthor().getAsTag(), event.getAuthor().getAvatarUrl());
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
}
