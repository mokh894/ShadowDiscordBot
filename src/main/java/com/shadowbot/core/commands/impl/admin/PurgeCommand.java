package com.shadowbot.core.commands.impl.admin;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.OffsetDateTime;
import java.util.List;

public class PurgeCommand extends AdminBase {
    @Override
    public String name() { return "purge"; }

    @Override
    public String description() { return "Delete N recent messages (2-100). Usage: !purge <amount>"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (args.length < 1) {
            event.getChannel().sendMessage("Usage: !purge <amount>").queue();
            return;
        }
        if (!precheckChannelPermissions(event, Permission.MESSAGE_MANAGE)) {
            return;
        }
        int amount;
        try { amount = Integer.parseInt(args[0]); } catch (Exception e) { amount = -1; }
        if (amount < 2 || amount > 100) {
            event.getChannel().sendMessage("Amount must be between 2 and 100.").queue();
            return;
        }
        event.getChannel().getHistory().retrievePast(amount).queue(messages -> {
            OffsetDateTime cutoff = OffsetDateTime.now().minusDays(14);
            List<Message> deletable = messages.stream().filter(m -> m.getTimeCreated().isAfter(cutoff)).toList();
            if (deletable.isEmpty()) {
                event.getChannel().sendMessage("No messages eligible for bulk delete.").queue();
                return;
            }
            if (deletable.size() == 1) {
                deletable.get(0).delete().queue(
                        v -> event.getChannel().sendMessage("Deleted 1 message.").queue(),
                        err -> event.getChannel().sendMessage("Delete failed: " + err.getMessage()).queue()
                );
            } else {
                event.getChannel().asTextChannel().deleteMessages(deletable).queue(
                        v -> event.getChannel().sendMessage("Deleted " + deletable.size() + " messages.").queue(),
                        err -> event.getChannel().sendMessage("Bulk delete failed: " + err.getMessage()).queue()
                );
            }
        });
    }
}
