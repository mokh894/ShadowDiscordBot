package com.shadowbot.core.commands.impl.admin;

import com.shadowbot.core.util.ArgumentParser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class VoiceKickCommand extends AdminBase {
    @Override
    public String name() { return "voicekick"; }

    @Override
    public String description() { return "Disconnect a member from voice. Usage: !voicekick @user"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (event.getGuild() == null) return;
        if (args.length < 1) { event.getChannel().sendMessage("Usage: !voicekick @user").queue(); return; }
        Member target = ArgumentParser.resolveMember(event.getGuild(), args[0], event.getMessage().getMentions().getMembers());
        if (target == null) { event.getChannel().sendMessage("Could not resolve member.").queue(); return; }
        if (!precheckMemberAction(event, target, Permission.VOICE_MOVE_OTHERS)) { return; }
        if (target.getVoiceState() == null || !Boolean.TRUE.equals(target.getVoiceState().inAudioChannel())) {
            event.getChannel().sendMessage("Target is not in a voice channel.").queue();
            return;
        }
        event.getGuild().kickVoiceMember(target).queue(
                v -> event.getChannel().sendMessage("Disconnected " + target.getUser().getAsTag() + " from voice.").queue(),
                e -> event.getChannel().sendMessage("Failed: " + e.getMessage()).queue()
        );
    }
}
