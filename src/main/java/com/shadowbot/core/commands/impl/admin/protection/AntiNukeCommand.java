package com.shadowbot.core.commands.impl.admin.protection;

import com.shadowbot.core.commands.Command;
import com.shadowbot.core.services.AntiNukeService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.Arrays;

public class AntiNukeCommand implements Command {
    private final AntiNukeService antiNukeService;
    
    public AntiNukeCommand(AntiNukeService antiNukeService) {
        this.antiNukeService = antiNukeService;
    }
    
    @Override
    public String name() {
        return "antinuke";
    }
    
    @Override
    public String description() {
        return "Configure anti-nuke protection settings (Server Owner only)";
    }
    
    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (!event.isFromGuild()) {
            event.getChannel().sendMessage("❌ This command can only be used in servers.").queue();
            return;
        }
        
        Guild guild = event.getGuild();
        Member executor = event.getMember();
        
        // Check permissions - only server owner or bot developer
        if (!antiNukeService.canManageAntiNuke(guild, executor)) {
            event.getChannel().sendMessage("❌ Only the server owner can manage anti-nuke settings.").queue();
            return;
        }
        
        if (args.length == 0) {
            showStatus(event, guild);
            return;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "enable":
            case "on":
                enableAntiNuke(event, guild);
                break;
            case "disable":
            case "off":
                disableAntiNuke(event, guild);
                break;
            case "limits":
                if (args.length >= 6) {
                    setLimits(event, guild, args);
                } else {
                    showLimitsUsage(event);
                }
                break;
            case "punishment":
                if (args.length >= 2) {
                    setPunishment(event, guild, args[1]);
                } else {
                    showPunishmentUsage(event);
                }
                break;
            case "logchannel":
                if (args.length >= 2) {
                    setLogChannel(event, guild, args[1]);
                } else {
                    showLogChannelUsage(event);
                }
                break;
            case "webhook":
                if (args.length >= 2) {
                    setWebhookProtection(event, guild, args[1]);
                } else {
                    showWebhookUsage(event);
                }
                break;
            case "setup":
                setupAntiNuke(event, guild);
                break;
            default:
                showHelp(event);
        }
    }
    
    private void showStatus(MessageReceivedEvent event, Guild guild) {
        AntiNukeService.AntiNukeConfig config = antiNukeService.getConfig(guild.getIdLong());
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🛡️ Anti-Nuke Protection Status")
                .setColor(config.enabled ? Color.GREEN : Color.RED)
                .addField("Status", config.enabled ? "✅ Enabled" : "❌ Disabled", true)
                .addField("Channel Limit", String.valueOf(config.channelLimit), true)
                .addField("Role Limit", String.valueOf(config.roleLimit), true)
                .addField("Webhook Limit", String.valueOf(config.webhookLimit), true)
                .addField("Kick Limit", String.valueOf(config.kickLimit), true)
                .addField("Ban Limit", String.valueOf(config.banLimit), true)
                .addField("Time Window", config.timeWindowMinutes + " minutes", true)
                .addField("Punishment", config.punishment.getDisplayName(), true)
                .addField("Webhook Protection", config.webhookProtection ? "✅ Enabled" : "❌ Disabled", true)
                .addField("Log Channel", config.logChannelId != null ? "<#" + config.logChannelId + ">" : "Not set", true)
                .setFooter("Use !antinuke help for configuration options");
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
    
    private void enableAntiNuke(MessageReceivedEvent event, Guild guild) {
        antiNukeService.setEnabled(guild.getIdLong(), true);
        event.getChannel().sendMessage("✅ Anti-nuke protection has been **enabled**.").queue();
    }
    
    private void disableAntiNuke(MessageReceivedEvent event, Guild guild) {
        antiNukeService.setEnabled(guild.getIdLong(), false);
        event.getChannel().sendMessage("❌ Anti-nuke protection has been **disabled**.").queue();
    }
    
    private void setLimits(MessageReceivedEvent event, Guild guild, String[] args) {
        try {
            int channelLimit = Integer.parseInt(args[1]);
            int roleLimit = Integer.parseInt(args[2]);
            int webhookLimit = Integer.parseInt(args[3]);
            int kickLimit = Integer.parseInt(args[4]);
            int banLimit = Integer.parseInt(args[5]);
            int timeWindow = args.length >= 7 ? Integer.parseInt(args[6]) : 5;
            
            AntiNukeService.AntiNukeConfig config = antiNukeService.getConfig(guild.getIdLong());
            config.channelLimit = Math.max(1, channelLimit);
            config.roleLimit = Math.max(1, roleLimit);
            config.webhookLimit = Math.max(1, webhookLimit);
            config.kickLimit = Math.max(1, kickLimit);
            config.banLimit = Math.max(1, banLimit);
            config.timeWindowMinutes = Math.max(1, timeWindow);
            
            antiNukeService.updateConfig(guild.getIdLong(), config);
            
            event.getChannel().sendMessage(String.format(
                "✅ Limits updated:\n" +
                "• Channels: %d per %d minutes\n" +
                "• Roles: %d per %d minutes\n" +
                "• Webhooks: %d per %d minutes\n" +
                "• Kicks: %d per %d minutes\n" +
                "• Bans: %d per %d minutes",
                config.channelLimit, config.timeWindowMinutes,
                config.roleLimit, config.timeWindowMinutes,
                config.webhookLimit, config.timeWindowMinutes,
                config.kickLimit, config.timeWindowMinutes,
                config.banLimit, config.timeWindowMinutes
            )).queue();
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Invalid number format. Please use integers only.").queue();
        }
    }
    
    private void setPunishment(MessageReceivedEvent event, Guild guild, String punishment) {
        try {
            AntiNukeService.PunishmentType punishmentType = AntiNukeService.PunishmentType.valueOf(punishment.toUpperCase());
            
            AntiNukeService.AntiNukeConfig config = antiNukeService.getConfig(guild.getIdLong());
            config.punishment = punishmentType;
            antiNukeService.updateConfig(guild.getIdLong(), config);
            
            event.getChannel().sendMessage("✅ Punishment set to: **" + punishmentType.getDisplayName() + "**").queue();
        } catch (IllegalArgumentException e) {
            event.getChannel().sendMessage("❌ Invalid punishment type. Use: strip_roles, kick, or ban").queue();
        }
    }
    
    private void setLogChannel(MessageReceivedEvent event, Guild guild, String channelMention) {
        TextChannel logChannel = null;
        
        if (channelMention.equals("none") || channelMention.equals("disable")) {
            // Disable logging
            AntiNukeService.AntiNukeConfig config = antiNukeService.getConfig(guild.getIdLong());
            config.logChannelId = null;
            antiNukeService.updateConfig(guild.getIdLong(), config);
            event.getChannel().sendMessage("✅ Anti-nuke logging disabled.").queue();
            return;
        }
        
        // Try to parse channel mention or ID
        if (channelMention.startsWith("<#") && channelMention.endsWith(">")) {
            String channelId = channelMention.substring(2, channelMention.length() - 1);
            logChannel = guild.getTextChannelById(channelId);
        } else {
            logChannel = guild.getTextChannelById(channelMention);
        }
        
        if (logChannel == null) {
            event.getChannel().sendMessage("❌ Invalid channel. Please mention a valid text channel or use the channel ID.").queue();
            return;
        }
        
        // Check bot permissions
        if (!logChannel.canTalk()) {
            event.getChannel().sendMessage("❌ I don't have permission to send messages in " + logChannel.getAsMention()).queue();
            return;
        }
        
        AntiNukeService.AntiNukeConfig config = antiNukeService.getConfig(guild.getIdLong());
        config.logChannelId = logChannel.getId();
        antiNukeService.updateConfig(guild.getIdLong(), config);
        
        event.getChannel().sendMessage("✅ Anti-nuke log channel set to " + logChannel.getAsMention()).queue();
    }
    
    private void setWebhookProtection(MessageReceivedEvent event, Guild guild, String toggle) {
        boolean enabled = toggle.equalsIgnoreCase("on") || toggle.equalsIgnoreCase("enable") || toggle.equalsIgnoreCase("true");
        
        AntiNukeService.AntiNukeConfig config = antiNukeService.getConfig(guild.getIdLong());
        config.webhookProtection = enabled;
        antiNukeService.updateConfig(guild.getIdLong(), config);
        
        event.getChannel().sendMessage("✅ Webhook protection " + (enabled ? "**enabled**" : "**disabled**") + ".").queue();
    }
    
    private void setupAntiNuke(MessageReceivedEvent event, Guild guild) {
        // Create hidden log channel and enable anti-nuke with default settings
        guild.createTextChannel("antinuke-logs")
                .addPermissionOverride(guild.getPublicRole(), null, Arrays.asList(Permission.VIEW_CHANNEL))
                .addPermissionOverride(guild.getSelfMember(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS), null)
                .queue(
                    channel -> {
                        AntiNukeService.AntiNukeConfig config = antiNukeService.getConfig(guild.getIdLong());
                        config.enabled = true;
                        config.logChannelId = channel.getId();
                        antiNukeService.updateConfig(guild.getIdLong(), config);
                        
                        event.getChannel().sendMessage(
                            "✅ **Anti-nuke protection setup complete!**\n" +
                            "• Protection: **Enabled**\n" +
                            "• Log Channel: " + channel.getAsMention() + " (hidden)\n" +
                            "• Default limits applied\n" +
                            "• Punishment: Strip roles\n\n" +
                            "Use `!antinuke` to view current settings."
                        ).queue();
                        
                        // Send initial message to log channel
                        EmbedBuilder logEmbed = new EmbedBuilder()
                                .setTitle("🛡️ Anti-Nuke Protection Activated")
                                .setDescription("This channel will log all anti-nuke events.")
                                .setColor(Color.GREEN)
                                .setTimestamp(java.time.Instant.now());
                        
                        channel.sendMessageEmbeds(logEmbed.build()).queue();
                    },
                    error -> event.getChannel().sendMessage("❌ Failed to create log channel: " + error.getMessage()).queue()
                );
    }
    
    private void showHelp(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🛡️ Anti-Nuke Commands")
                .setColor(Color.BLUE)
                .setDescription("Configure server protection against mass deletions/creations")
                .addField("Basic Commands", 
                    "`!antinuke` - Show current status\n" +
                    "`!antinuke enable/disable` - Toggle protection\n" +
                    "`!antinuke setup` - Quick setup with hidden log channel", false)
                .addField("Configuration",
                    "`!antinuke limits <channels> <roles> <webhooks> <kicks> <bans> [minutes]` - Set rate limits\n" +
                    "`!antinuke punishment <strip_roles|kick|ban>` - Set punishment type\n" +
                    "`!antinuke logchannel <#channel|none>` - Set log channel\n" +
                    "`!antinuke webhook <on|off>` - Toggle webhook protection", false)
                .addField("Examples",
                    "`!antinuke limits 5 5 3 5 3 10` - Max 5 channels/roles, 3 webhooks, 5 kicks, 3 bans per 10 minutes\n" +
                    "`!antinuke punishment ban` - Ban violators\n" +
                    "`!antinuke logchannel #security` - Set log channel", false)
                .setFooter("Only server owners can manage anti-nuke settings");
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
    
    private void showLimitsUsage(MessageReceivedEvent event) {
        event.getChannel().sendMessage(
            "❌ **Usage:** `!antinuke limits <channels> <roles> <webhooks> <kicks> <bans> [minutes]`\n" +
            "**Example:** `!antinuke limits 5 5 3 5 3 10`\n" +
            "• 5 channels per 10 minutes\n" +
            "• 5 roles per 10 minutes\n" +
            "• 3 webhooks per 10 minutes\n" +
            "• 5 kicks per 10 minutes\n" +
            "• 3 bans per 10 minutes"
        ).queue();
    }
    
    private void showPunishmentUsage(MessageReceivedEvent event) {
        event.getChannel().sendMessage(
            "❌ **Usage:** `!antinuke punishment <type>`\n" +
            "**Types:** `strip_roles`, `kick`, `ban`\n" +
            "**Example:** `!antinuke punishment ban`"
        ).queue();
    }
    
    private void showLogChannelUsage(MessageReceivedEvent event) {
        event.getChannel().sendMessage(
            "❌ **Usage:** `!antinuke logchannel <#channel|channel_id|none>`\n" +
            "**Examples:**\n" +
            "• `!antinuke logchannel #security-logs`\n" +
            "• `!antinuke logchannel none` (disable logging)"
        ).queue();
    }
    
    private void showWebhookUsage(MessageReceivedEvent event) {
        event.getChannel().sendMessage(
            "❌ **Usage:** `!antinuke webhook <on|off>`\n" +
            "**Example:** `!antinuke webhook on`"
        ).queue();
    }
}
