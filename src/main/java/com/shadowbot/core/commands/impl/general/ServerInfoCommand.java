package com.shadowbot.core.commands.impl.general;

import com.shadowbot.core.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;

public class ServerInfoCommand implements Command {
    @Override
    public String name() { return "serverinfo"; }

    @Override
    public String description() { return "Shows information about the current server."; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (!event.isFromGuild()) {
            event.getChannel().sendMessage("This command can only be used in a server.").queue();
            return;
        }

        Guild guild = event.getGuild();
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 Server Information")
                .setColor(Color.GREEN)
                .setThumbnail(guild.getIconUrl())
                .setTimestamp(guild.getTimeCreated())
                .setFooter("Server created", guild.getIconUrl());

        embed.addField("🏷️ Basic Info",
                "**Name:** " + guild.getName() + "\n" +
                "**ID:** " + guild.getId() + "\n" +
                "**Owner:** " + (guild.getOwner() != null ? guild.getOwner().getUser().getAsTag() : "Unknown"), true);

        embed.addField("📊 Statistics",
                "**Members:** " + guild.getMemberCount() + "\n" +
                "**Channels:** " + guild.getChannels().size() + "\n" +
                "**Roles:** " + guild.getRoles().size(), true);

        embed.addField("🔧 Features",
                "**Boost Tier:** " + guild.getBoostTier().name() + "\n" +
                "**Boosts:** " + guild.getBoostCount() + "\n" +
                "**Verification:** " + guild.getVerificationLevel().name(), true);

        if (guild.getDescription() != null) {
            embed.setDescription(guild.getDescription());
        }

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
}
