package com.shadowbot.core.commands.impl.admin;

import com.shadowbot.core.commands.Command;
import com.shadowbot.core.moderation.ModerationUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class AdminBase implements Command {
    protected boolean precheckMemberAction(MessageReceivedEvent event, Member target, Permission... required) {
        Member self = event.getGuild().getSelfMember();
        Member executor = event.getMember();
        String missing = ModerationUtils.requirePermissions(executor, required);
        if (missing != null) {
            event.getChannel().sendMessage(missing).queue();
            return false;
        }
        String selfMissing = ModerationUtils.requirePermissions(self, required);
        if (selfMissing != null) {
            event.getChannel().sendMessage("Bot " + selfMissing).queue();
            return false;
        }
        String hier = ModerationUtils.checkCanInteract(executor, target, self);
        if (hier != null) {
            event.getChannel().sendMessage(hier).queue();
            return false;
        }
        return true;
    }

    protected boolean precheckChannelPermissions(MessageReceivedEvent event, Permission... required) {
        if (!event.isFromGuild()) {
            event.getChannel().sendMessage("Not in a guild channel.").queue();
            return false;
        }
        Member self = event.getGuild().getSelfMember();
        Member executor = event.getMember();
        GuildMessageChannel channel = event.getChannel().asGuildMessageChannel();

        String missing = ModerationUtils.requirePermissions(executor, channel, required);
        if (missing != null) {
            event.getChannel().sendMessage(missing).queue();
            return false;
        }
        String selfMissing = ModerationUtils.requirePermissions(self, channel, required);
        if (selfMissing != null) {
            event.getChannel().sendMessage("Bot " + selfMissing).queue();
            return false;
        }
        return true;
    }
}
