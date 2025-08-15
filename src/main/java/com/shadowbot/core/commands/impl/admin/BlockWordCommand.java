package com.shadowbot.core.commands.impl.admin;

import com.shadowbot.core.services.AutoModService;
import com.shadowbot.core.services.BlocklistService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockWordCommand extends AdminBase {
    private final BlocklistService blocklistService;
    private final AutoModService autoModService;

    public BlockWordCommand(BlocklistService blocklistService, AutoModService autoModService) {
        this.blocklistService = blocklistService;
        this.autoModService = autoModService;
    }

    @Override
    public String name() { return "blockword"; }

    @Override
    public String description() { return "Add blockwords to filter. Usage: !blockword <word ...>"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (event.getGuild() == null) return;
        if (args.length < 1) { 
            event.getChannel().sendMessage("Usage: !blockword <word ...>").queue(); 
            return; 
        }
        if (!precheckChannelPermissions(event, Permission.MANAGE_SERVER)) { 
            return; 
        }

        long gid = event.getGuild().getIdLong();
        
        // Check if bot has permission to manage server
        if (!autoModService.hasManageGuildPermission(event.getGuild())) {
            event.getChannel().sendMessage("❌ I need `Manage Server` permission to manage word filters.").queue();
            return;
        }

        try {
            // Add words to local storage for tracking
            List<String> newWords = Arrays.asList(args);
            for (String word : newWords) {
                blocklistService.addWord(gid, word);
            }

            // Get all words for this guild and update Discord AutoMod
            BlocklistService.GuildBlockConfig config = blocklistService.getConfig(gid);
            List<String> allWords = new ArrayList<>(config.words);
            
            if (allWords.isEmpty()) {
                event.getChannel().sendMessage("❌ No words to add to filter.").queue();
                return;
            }

            event.getChannel().sendMessage("⏳ Updating word filter...").queue(msg -> {
                autoModService.createOrUpdateWordFilter(event.getGuild(), allWords)
                    .thenAccept(result -> {
                        msg.editMessage("✅ Added **" + newWords.size() + "** word(s) to filter.\n" +
                                       "📋 Total filtered words: **" + allWords.size() + "**\n" +
                                       "🔒 Messages containing these words will be filtered by the bot.").queue();
                    })
                    .exceptionally(throwable -> {
                        msg.editMessage("❌ Failed to update filter: " + throwable.getMessage()).queue();
                        return null;
                    });
            });

        } catch (Exception e) {
            event.getChannel().sendMessage("❌ Failed to update blocklist: " + e.getMessage()).queue();
        }
    }
}
