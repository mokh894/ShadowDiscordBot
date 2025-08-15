package com.shadowbot.core.commands.impl.general;

import com.shadowbot.core.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.time.Instant;

public class HelpCommand implements Command {
    @Override
    public String name() { return "help"; }

    @Override
    public String description() { return "Shows available commands and their usage."; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🤖 Shadow Bot Commands")
                .setColor(Color.BLUE)
                .setTimestamp(Instant.now())
                .setFooter("Shadow Bot", event.getJDA().getSelfUser().getAvatarUrl());

        embed.addField("📋 General Commands", 
                "`!help` - Show this help message\n" +
                "`!about` - Bot information\n" +
                "`!ping` - Check bot latency\n" +
                "`!serverinfo` - Server information", false);

        embed.addField("🎮 Fun Commands", 
                "`!cat` - Get a random cat picture\n" +
                "`!dog` - Get a random dog picture\n" +
                "`!weather <city>` - Get weather information\n" +
                "`!coinflip` - Flip a coin\n" +
                "`!8ball <question>` - Ask the magic 8-ball", false);

        embed.addField("💰 Economy Commands", 
                "`!balance` - Check your coin balance\n" +
                "`!daily` - Claim daily coins\n" +
                "`!gamble <amount>` - Gamble your coins\n" +
                "`!stats` - View your gambling statistics\n" +
                "`!leaderboard` - View top coin holders", false);

        embed.addField("🔨 Moderation Commands", 
                "`!ban @user [reason]` - Ban a member\n" +
                "`!kick @user [reason]` - Kick a member\n" +
                "`!mute @user <duration> [reason]` - Timeout a member\n" +
                "`!unmute @user [reason]` - Remove timeout\n" +
                "`!purge <amount>` - Delete recent messages (2-100)\n" +
                "`!nuke` - Clone and delete current channel", false);

        embed.addField("👥 Role Management", 
                "`!giverole @user @role` - Give a role to user\n" +
                "`!takerole @user @role` - Remove role from user", false);

        embed.addField("🔇 Voice & AutoMod", 
                "`!voicekick @user` - Disconnect from voice\n" +
                "`!blockword <word>` - Add word to filter\n" +
                "`!automod [status|clear]` - Manage word filters", false);

        embed.addField("🛡️ Protection", 
                "`!antinuke` - Anti-nuke protection (Owner only)\n" +
                "`!antilog` - View anti-nuke logs (Owner only)", false);

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
}
