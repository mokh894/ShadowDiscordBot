package com.shadowbot.core.commands.impl.general;

import com.shadowbot.core.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.time.Instant;

public class AboutCommand implements Command {
    @Override
    public String name() { return "about"; }

    @Override
    public String description() { return "Shows information about the bot."; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        JDA jda = event.getJDA();
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        String uptimeStr = formatUptime(uptime);

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🤖 About Shadow Bot")
                .setColor(Color.CYAN)
                .setThumbnail(jda.getSelfUser().getAvatarUrl())
                .setTimestamp(Instant.now())
                .setFooter("Shadow Bot v0.1.0", jda.getSelfUser().getAvatarUrl());

        embed.addField("📊 Statistics", 
                "**Servers:** " + jda.getGuilds().size() + "\n" +
                "**Users:** " + jda.getUsers().size() + "\n" +
                "**Uptime:** " + uptimeStr, true);

        embed.addField("⚙️ Technical Info", 
                "**Library:** JDA 5.0.0\n" +
                "**Language:** Java 17\n" +
                "**Memory:** " + getMemoryUsage(), true);

        embed.setDescription("A multi-purpose Discord bot with moderation, AutoMod, and utility features.");

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private String formatUptime(long uptime) {
        long seconds = uptime / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        if (days > 0) {
            return String.format("%dd %02dh %02dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        } else {
            return String.format("%dm %02ds", minutes, seconds);
        }
    }

    private String getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        return String.format("%.1f/%.1f MB", used / 1024.0 / 1024.0, max / 1024.0 / 1024.0);
    }
}
