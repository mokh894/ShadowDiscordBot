package com.shadowbot.core.events;

import com.shadowbot.core.commands.CommandManager;
import com.shadowbot.core.services.BlocklistService;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class BotEventListener extends ListenerAdapter {
    private final CommandManager commandManager;
    private final BlocklistService blocklistService;

    public BotEventListener(CommandManager commandManager, BlocklistService blocklistService) {
        this.commandManager = commandManager;
        this.blocklistService = blocklistService;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        
        // Check for blocked words and delete messages if found
        if (event.isFromGuild()) {
            var patterns = blocklistService.getPatterns(event.getGuild().getIdLong());
            String content = event.getMessage().getContentRaw();
            boolean matched = patterns.stream().anyMatch(p -> p.matcher(content).find());
            if (matched) {
                event.getMessage().delete().queue(
                    success -> event.getChannel().sendMessage("🚫 " + event.getAuthor().getAsMention() + 
                        ", your message was removed for containing blocked content.").queue(),
                    error -> event.getChannel().sendMessage("🚫 Message contains blocked content but couldn't be deleted.").queue()
                );
                return; // Don't process as command if it was blocked
            }
        }
        
        commandManager.handleMessage(event);
    }
}
