package com.shadowbot.core.commands.impl.admin;

import com.shadowbot.core.util.ArgumentParser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.concurrent.TimeUnit;

public class BanCommand extends AdminBase {
    @Override
    public String name() { return "ban"; }

    @Override
    public String description() { return "Ban a member. Usage: !ban @user [reason]"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (event.getGuild() == null) { return; }
        if (args.length < 1) {
            event.getChannel().sendMessage("Usage: !ban @user [reason]").queue();
            return;
        }
        Member target = ArgumentParser.resolveMember(event.getGuild(), args[0], event.getMessage().getMentions().getMembers());
        if (target == null) {
            event.getChannel().sendMessage("Could not resolve target member.").queue();
            return;
        }
        if (!precheckMemberAction(event, target, Permission.BAN_MEMBERS)) {
            return;
        }
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        event.getGuild().ban(target, 0, TimeUnit.valueOf(reason)).queue(
                v -> event.getChannel().sendMessage("Banned " + target.getUser().getAsTag()).queue(),
                e -> event.getChannel().sendMessage("Failed to ban: " + e.getMessage()).queue()
        );
    }
}
