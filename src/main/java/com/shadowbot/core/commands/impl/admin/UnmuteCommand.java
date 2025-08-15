package com.shadowbot.core.commands.impl.admin;

import com.shadowbot.core.util.ArgumentParser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class UnmuteCommand extends AdminBase {
    @Override
    public String name() { return "unmute"; }

    @Override
    public String description() { return "Remove timeout from a member. Usage: !unmute @user [reason]"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (event.getGuild() == null) return;
        if (args.length < 1) { event.getChannel().sendMessage("Usage: !unmute @user [reason]").queue(); return; }
        Member target = ArgumentParser.resolveMember(event.getGuild(), args[0], event.getMessage().getMentions().getMembers());
        if (target == null) { event.getChannel().sendMessage("Could not resolve member.").queue(); return; }
        if (!precheckMemberAction(event, target, Permission.MODERATE_MEMBERS)) { return; }
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        target.removeTimeout().reason(reason).queue(
                v -> event.getChannel().sendMessage("Removed timeout for " + target.getUser().getAsTag()).queue(),
                e -> event.getChannel().sendMessage("Failed to remove timeout: " + e.getMessage()).queue()
        );
    }
}
