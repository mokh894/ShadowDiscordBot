package com.shadowbot.core.commands.impl.admin;

import com.shadowbot.core.services.AutoModService;
import com.shadowbot.core.services.BlocklistService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.time.Instant;
import java.util.List;

public class AutoModCommand extends AdminBase {
    private final AutoModService autoModService;
    private final BlocklistService blocklistService;

    public AutoModCommand(AutoModService autoModService, BlocklistService blocklistService) {
        this.autoModService = autoModService;
        this.blocklistService = blocklistService;
    }

    @Override
    public String name() { return "automod"; }

    @Override
    public String description() { return "Manage word filter settings. Usage: !automod [status|clear]"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (event.getGuild() == null) return;
        if (!precheckChannelPermissions(event, Permission.MANAGE_SERVER)) { return; }

        if (!autoModService.hasManageGuildPermission(event.getGuild())) {
            event.getChannel().sendMessage("❌ I need `Manage Server` permission to manage word filters.").queue();
            return;
        }

        String action = args.length > 0 ? args[0].toLowerCase() : "status";

        switch (action) {
            case "status":
                showAutoModStatus(event);
                break;
            case "clear":
                clearAutoModRules(event);
                break;
            default:
                event.getChannel().sendMessage("Usage: `!automod [status|clear]`\n" +
                        "• `status` - View current word filter status\n" +
                        "• `clear` - Clear ShadowBot word filters").queue();
        }
    }

    private void showAutoModStatus(MessageReceivedEvent event) {
        event.getChannel().sendMessage("⏳ Retrieving filter status...").queue(msg -> {
            autoModService.getAutoModRules(event.getGuild())
                .thenAccept(rules -> {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("🛡️ Word Filter Status")
                            .setColor(Color.ORANGE)
                            .setTimestamp(Instant.now())
                            .setFooter("ShadowBot Filter", event.getGuild().getIconUrl());

                    BlocklistService.GuildBlockConfig config = blocklistService.getConfig(event.getGuild().getIdLong());
                    
                    if (config.words.isEmpty()) {
                        embed.setDescription("No word filters are currently active.");
                    } else {
                        embed.setDescription("**Active Filter:** ShadowBot Word Filter\n" +
                                           "**Status:** ✅ Enabled\n" +
                                           "**Type:** Keyword Filter");
                        
                        embed.addField("📋 Filtered Words", 
                                "**Count:** " + config.words.size() + "\n" +
                                "**Words:** " + String.join(", ", config.words.subList(0, Math.min(config.words.size(), 10))) +
                                (config.words.size() > 10 ? "..." : ""), false);
                    }

                    msg.editMessageEmbeds(embed.build()).setContent("").queue();
                })
                .exceptionally(throwable -> {
                    msg.editMessage("❌ Failed to retrieve filter status: " + throwable.getMessage()).queue();
                    return null;
                });
        });
    }

    private void clearAutoModRules(MessageReceivedEvent event) {
        event.getChannel().sendMessage("⏳ Clearing ShadowBot word filters...").queue(msg -> {
            autoModService.deleteWordFilter(event.getGuild())
                .thenRun(() -> {
                    try {
                        // Clear local storage
                        blocklistService.getConfig(event.getGuild().getIdLong()).words.clear();
                        blocklistService.persist();
                        msg.editMessage("✅ Cleared ShadowBot word filters and local word list.").queue();
                    } catch (Exception e) {
                        msg.editMessage("❌ Failed to clear local storage: " + e.getMessage()).queue();
                    }
                })
                .exceptionally(throwable -> {
                    msg.editMessage("❌ Failed to clear word filters: " + throwable.getMessage()).queue();
                    return null;
                });
        });
    }
}
