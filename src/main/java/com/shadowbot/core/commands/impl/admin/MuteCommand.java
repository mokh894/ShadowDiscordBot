package com.shadowbot.core.commands.impl.admin;

import com.shadowbot.core.util.ArgumentParser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.Duration;

public class MuteCommand extends AdminBase {
    @Override
    public String name() { return "mute"; }

    @Override
    public String description() { return "Timeout a member. Usage: !mute @user <duration> [reason]; e.g., 10m, 2h, 1d"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (event.getGuild() == null) return;
        if (args.length < 2) {
            event.getChannel().sendMessage("Usage: !mute @user <duration> [reason]").queue();
            return;
        }
        Member target = ArgumentParser.resolveMember(event.getGuild(), args[0], event.getMessage().getMentions().getMembers());
        if (target == null) { event.getChannel().sendMessage("Could not resolve member.").queue(); return; }
        Duration duration = ArgumentParser.parseDuration(args[1]);
        if (duration == null) { event.getChannel().sendMessage("Invalid duration.").queue(); return; }
        if (!precheckMemberAction(event, target, Permission.MODERATE_MEMBERS)) { return; }
        String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "No reason provided";
        target.timeoutFor(duration).reason(reason).queue(
                v -> event.getChannel().sendMessage("Timed out " + target.getUser().getAsTag() + " for " + duration).queue(),
                e -> event.getChannel().sendMessage("Failed to timeout: " + e.getMessage()).queue()
        );
    }
}
