package com.shadowbot.core.commands.impl;

import com.shadowbot.core.commands.Command;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PingCommand implements Command {
    @Override
    public String name() {
        return "ping";
    }

    @Override
    public String description() {
        return "Responds with Pong and latency.";
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        long before = System.currentTimeMillis();
        event.getChannel().sendMessage("Pong!").queue(msg -> {
            long latency = System.currentTimeMillis() - before;
            msg.editMessage("Pong! (" + latency + " ms)").queue();
        });
    }
}
