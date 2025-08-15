package com.shadowbot.core.commands.impl.fun;

import com.shadowbot.core.commands.Command;
import com.shadowbot.core.services.ApiService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;

public class DogCommand implements Command {
    @Override
    public String name() { return "dog"; }

    @Override
    public String description() { return "Get a random dog picture."; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        event.getChannel().sendMessage("🐶 Fetching a good boy...").queue(msg -> {
            ApiService.getRandomDog()
                .thenAccept(dogUrl -> {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("🐶 Random Dog")
                            .setImage(dogUrl)
                            .setColor(Color.CYAN)
                            .setFooter("Powered by Dog CEO API", null);
                    
                    msg.editMessageEmbeds(embed.build()).setContent("").queue();
                })
                .exceptionally(throwable -> {
                    msg.editMessage("❌ Failed to fetch dog picture: " + throwable.getMessage()).queue();
                    return null;
                });
        });
    }
}
