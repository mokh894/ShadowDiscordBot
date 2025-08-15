package com.shadowbot.core.commands.impl.fun;

import com.shadowbot.core.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.Random;

public class EightBallCommand implements Command {
    private static final Random RANDOM = new Random();
    private static final String[] RESPONSES = {
            "It is certain", "It is decidedly so", "Without a doubt", "Yes definitely",
            "You may rely on it", "As I see it, yes", "Most likely", "Outlook good",
            "Yes", "Signs point to yes", "Reply hazy, try again", "Ask again later",
            "Better not tell you now", "Cannot predict now", "Concentrate and ask again",
            "Don't count on it", "My reply is no", "My sources say no",
            "Outlook not so good", "Very doubtful"
    };

    @Override
    public String name() { return "8ball"; }

    @Override
    public String description() { return "Ask the magic 8-ball a question. Usage: !8ball <question>"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (args.length < 1) {
            event.getChannel().sendMessage("Usage: `!8ball <question>`\nExample: `!8ball Will it rain tomorrow?`").queue();
            return;
        }

        String question = String.join(" ", args);
        String response = RESPONSES[RANDOM.nextInt(RESPONSES.length)];
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎱 Magic 8-Ball")
                .addField("Question", question, false)
                .addField("Answer", "**" + response + "**", false)
                .setColor(Color.MAGENTA)
                .setFooter("Asked by " + event.getAuthor().getAsTag(), event.getAuthor().getAvatarUrl());
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
}
