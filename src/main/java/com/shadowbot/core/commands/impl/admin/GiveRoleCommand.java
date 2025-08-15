package com.shadowbot.core.commands.impl.admin;

import com.shadowbot.core.moderation.ModerationUtils;
import com.shadowbot.core.util.ArgumentParser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class GiveRoleCommand extends AdminBase {
    @Override
    public String name() { return "giverole"; }

    @Override
    public String description() { return "Give a role to a user. Usage: !giverole @user @role"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (event.getGuild() == null) return;
        if (args.length < 2) {
            event.getChannel().sendMessage("Usage: !giverole @user @role").queue();
            return;
        }
        Member target = ArgumentParser.resolveMember(event.getGuild(), args[0], event.getMessage().getMentions().getMembers());
        Role role = ArgumentParser.resolveRole(event.getGuild(), args[1], event.getMessage().getMentions().getRoles());
        if (target == null || role == null) {
            event.getChannel().sendMessage("Could not resolve target or role.").queue();
            return;
        }
        if (!precheckMemberAction(event, target, Permission.MANAGE_ROLES)) {
            return;
        }
        String hier = ModerationUtils.checkCanInteractRole(event.getMember(), role, event.getGuild().getSelfMember());
        if (hier != null) { event.getChannel().sendMessage(hier).queue(); return; }
        event.getGuild().addRoleToMember(target, role).queue(
                v -> event.getChannel().sendMessage("Gave role " + role.getName() + " to " + target.getUser().getAsTag()).queue(),
                e -> event.getChannel().sendMessage("Failed: " + e.getMessage()).queue()
        );
    }
}
