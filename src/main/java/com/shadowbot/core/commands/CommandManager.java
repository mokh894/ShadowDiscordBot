package com.shadowbot.core.commands;

import com.shadowbot.core.commands.impl.PingCommand;
import com.shadowbot.core.commands.impl.general.HelpCommand;
import com.shadowbot.core.commands.impl.general.AboutCommand;
import com.shadowbot.core.commands.impl.general.ServerInfoCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CommandManager {
    private final Map<String, Command> commandByName = new HashMap<>();
    private final String prefix;

    public CommandManager(String prefix) {
        this.prefix = prefix;
    }

    public void registerBuiltins() {
        register(new PingCommand());
        register(new HelpCommand());
        register(new AboutCommand());
        register(new ServerInfoCommand());
    }

    public void register(Command command) {
        commandByName.put(command.name().toLowerCase(Locale.ROOT), command);
    }

    public void handleMessage(MessageReceivedEvent event) {
        String raw = event.getMessage().getContentRaw();
        if (!raw.startsWith(prefix)) {
            return;
        }
        String withoutPrefix = raw.substring(prefix.length()).trim();
        if (withoutPrefix.isEmpty()) {
            return;
        }
        String[] parts = withoutPrefix.split("\\s+");
        String commandName = parts[0].toLowerCase(Locale.ROOT);
        String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        Command command = commandByName.get(commandName);
        if (command == null) {
            event.getChannel().sendMessage("Unknown command: `" + commandName + "`").queue();
            return;
        }
        command.execute(event, args);
    }
}
