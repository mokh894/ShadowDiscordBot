package com.shadowbot.core.commands.impl.fun;

import com.shadowbot.core.commands.Command;
import com.shadowbot.core.services.ApiService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;

public class WeatherCommand implements Command {
    @Override
    public String name() { return "weather"; }

    @Override
    public String description() { return "Get weather information for a city. Usage: !weather <city>"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (args.length < 1) {
            event.getChannel().sendMessage("Usage: `!weather <city>`\nExample: `!weather London`").queue();
            return;
        }

        String city = String.join(" ", args);
        event.getChannel().sendMessage("🌤️ Fetching weather for " + city + "...").queue(msg -> {
            ApiService.getWeather(city)
                .thenAccept(weatherInfo -> {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("🌤️ Weather Information")
                            .setDescription(weatherInfo)
                            .setColor(Color.BLUE)
                            .setFooter("Powered by wttr.in", null);
                    
                    msg.editMessageEmbeds(embed.build()).setContent("").queue();
                })
                .exceptionally(throwable -> {
                    msg.editMessage("❌ Failed to fetch weather: " + throwable.getMessage()).queue();
                    return null;
                });
        });
    }
}
