package com.shadowbot.core.commands.impl.admin;

import com.shadowbot.core.util.ArgumentParser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class KickCommand extends AdminBase {
    @Override
    public String name() { return "kick"; }

    @Override
    public String description() { return "Kick a member. Usage: !kick @user [reason]"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (event.getGuild() == null) { return; }
        if (args.length < 1) {
            event.getChannel().sendMessage("Usage: !kick @user [reason]").queue();
            return;
        }
        Member target = ArgumentParser.resolveMember(event.getGuild(), args[0], event.getMessage().getMentions().getMembers());
        if (target == null) {
            event.getChannel().sendMessage("Could not resolve target member.").queue();
            return;
        }
        if (!precheckMemberAction(event, target, Permission.KICK_MEMBERS)) {
            return;
        }
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        event.getGuild().kick(target, reason).queue(
                v -> event.getChannel().sendMessage("Kicked " + target.getUser().getAsTag()).queue(),
                e -> event.getChannel().sendMessage("Failed to kick: " + e.getMessage()).queue()
        );
    }
}
