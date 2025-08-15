package com.shadowbot.core.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Command {
    String name();
    String description();
    void execute(MessageReceivedEvent event, String[] args);
}
