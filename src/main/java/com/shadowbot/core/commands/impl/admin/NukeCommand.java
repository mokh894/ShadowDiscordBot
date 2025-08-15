package com.shadowbot.core.commands.impl.admin;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class NukeCommand extends AdminBase {
    @Override
    public String name() { return "nuke"; }

    @Override
    public String description() { return "Clones the current channel and deletes the old one. Usage: !nuke"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (!(event.getChannel() instanceof TextChannel)) {
            event.getChannel().sendMessage("This command can only be used in text channels.").queue();
            return;
        }
        if (!precheckChannelPermissions(event, Permission.MANAGE_CHANNEL)) {
            return;
        }
        TextChannel tc = (TextChannel) event.getChannel();
        tc.createCopy().queue(newChannel -> {
            newChannel.sendMessage("Channel nuked by " + event.getAuthor().getAsTag()).queue();
            tc.delete().queue();
        }, err -> event.getChannel().sendMessage("Failed to clone: " + err.getMessage()).queue());
    }
}
