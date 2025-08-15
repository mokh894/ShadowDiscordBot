package com.shadowbot.core.commands.impl.fun;

import com.shadowbot.core.commands.Command;
import com.shadowbot.core.services.ApiService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;

public class CatCommand implements Command {
    @Override
    public String name() { return "cat"; }

    @Override
    public String description() { return "Get a random cat picture."; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        event.getChannel().sendMessage("🐱 Fetching a cute cat...").queue(msg -> {
            ApiService.getRandomCat()
                .thenAccept(catUrl -> {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("🐱 Random Cat")
                            .setImage(catUrl)
                            .setColor(Color.ORANGE)
                            .setFooter("Powered by The Cat API", null);
                    
                    msg.editMessageEmbeds(embed.build()).setContent("").queue();
                })
                .exceptionally(throwable -> {
                    msg.editMessage("❌ Failed to fetch cat picture: " + throwable.getMessage()).queue();
                    return null;
                });
        });
    }
}
