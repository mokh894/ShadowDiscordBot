package com.shadowbot.core.commands.impl.admin.protection;

import com.shadowbot.core.commands.Command;
import com.shadowbot.core.services.AntiNukeLogger;
import com.shadowbot.core.services.AntiNukeService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class AntiNukeLogsCommand implements Command {
    private final AntiNukeService antiNukeService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public AntiNukeLogsCommand(AntiNukeService antiNukeService) {
        this.antiNukeService = antiNukeService;
    }
    
    @Override
    public String name() {
        return "antilog";
    }
    
    @Override
    public String description() {
        return "View anti-nuke logs and user statistics (Server Owner only)";
    }
    
    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (!event.isFromGuild()) {
            event.getChannel().sendMessage("❌ This command can only be used in servers.").queue();
            return;
        }
        
        Guild guild = event.getGuild();
        Member executor = event.getMember();
        
        // Check permissions - only server owner or bot developer
        if (!antiNukeService.canManageAntiNuke(guild, executor)) {
            event.getChannel().sendMessage("❌ Only the server owner can view anti-nuke logs.").queue();
            return;
        }
        
        if (args.length == 0) {
            showOverview(event, guild);
            return;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "user":
                if (args.length >= 2) {
                    showUserStats(event, guild, args[1]);
                } else {
                    showUserUsage(event);
                }
                break;
            case "top":
                showTopUsers(event, guild);
                break;
            case "recent":
                showRecentViolations(event, guild);
                break;
            default:
                showHelp(event);
        }
    }
    
    private void showOverview(MessageReceivedEvent event, Guild guild) {
        AntiNukeLogger logger = antiNukeService.getLogger();
        Map<Long, AntiNukeLogger.UserActionData> guildData = logger.getGuildData(guild.getIdLong());
        
        int totalUsers = guildData.size();
        int totalViolations = guildData.values().stream()
                .mapToInt(AntiNukeLogger.UserActionData::getViolationCount)
                .sum();
        
        int totalActions = guildData.values().stream()
                .mapToInt(userData -> userData.actionCounts.values().stream()
                        .mapToInt(Integer::intValue).sum())
                .sum();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🛡️ Anti-Nuke Overview - " + guild.getName())
                .setColor(Color.BLUE)
                .addField("📊 Statistics", 
                    "**Users Tracked:** " + totalUsers + "\n" +
                    "**Total Actions:** " + totalActions + "\n" +
                    "**Total Violations:** " + totalViolations, true)
                .addField("⚙️ Status", 
                    "**Protection:** " + (antiNukeService.isEnabled(guild.getIdLong()) ? "✅ Enabled" : "❌ Disabled") + "\n" +
                    "**Log Files:** `data/" + guild.getId() + "/antinuke/`", true)
                .addField("📋 Commands", 
                    "`!antilog user <@user>` - View user stats\n" +
                    "`!antilog top` - Show top users by actions\n" +
                    "`!antilog recent` - Show recent violations", false)
                .setFooter("Use !antilog help for more options");
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
    
    private void showUserStats(MessageReceivedEvent event, Guild guild, String userInput) {
        // Parse user mention or ID
        String userId = userInput.replaceAll("[<@!>]", "");
        
        AntiNukeLogger logger = antiNukeService.getLogger();
        AntiNukeLogger.UserActionData userData = logger.getUserData(guild.getIdLong(), Long.parseLong(userId));
        
        if (userData == null) {
            event.getChannel().sendMessage("❌ No anti-nuke data found for that user.").queue();
            return;
        }
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👤 Anti-Nuke User Stats")
                .setColor(Color.ORANGE)
                .addField("User", userData.username + "\n`" + userData.userId + "`", true)
                .addField("First Seen", userData.firstSeen != null ? userData.firstSeen.format(FORMATTER) : "Unknown", true)
                .addField("Last Seen", userData.lastSeen != null ? userData.lastSeen.format(FORMATTER) : "Unknown", true);
        
        // Add action counts
        StringBuilder actions = new StringBuilder();
        for (Map.Entry<String, Integer> entry : userData.actionCounts.entrySet()) {
            String actionType = entry.getKey().replace("_", " ");
            actions.append("**").append(actionType).append(":** ").append(entry.getValue()).append("\n");
        }
        
        if (actions.length() > 0) {
            embed.addField("📊 Action Counts", actions.toString(), false);
        }
        
        // Add violation info
        if (userData.getViolationCount() > 0) {
            embed.addField("⚠️ Violations", String.valueOf(userData.getViolationCount()), true);
            
            // Show most recent violation
            AntiNukeLogger.ViolationRecord lastViolation = userData.violations.get(userData.violations.size() - 1);
            embed.addField("🕐 Last Violation", 
                lastViolation.actionType + " - " + lastViolation.punishment + "\n" +
                lastViolation.timestamp.format(FORMATTER), true);
        }
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
    
    private void showTopUsers(MessageReceivedEvent event, Guild guild) {
        AntiNukeLogger logger = antiNukeService.getLogger();
        Map<Long, AntiNukeLogger.UserActionData> guildData = logger.getGuildData(guild.getIdLong());
        
        if (guildData.isEmpty()) {
            event.getChannel().sendMessage("📊 No anti-nuke data available yet.").queue();
            return;
        }
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏆 Top Users by Activity")
                .setColor(Color.YELLOW);
        
        guildData.entrySet().stream()
                .sorted((a, b) -> {
                    int aTotal = a.getValue().actionCounts.values().stream().mapToInt(Integer::intValue).sum();
                    int bTotal = b.getValue().actionCounts.values().stream().mapToInt(Integer::intValue).sum();
                    return Integer.compare(bTotal, aTotal);
                })
                .limit(10)
                .forEach(entry -> {
                    AntiNukeLogger.UserActionData userData = entry.getValue();
                    int totalActions = userData.actionCounts.values().stream().mapToInt(Integer::intValue).sum();
                    
                    embed.addField(userData.username,
                        "**Actions:** " + totalActions + "\n" +
                        "**Violations:** " + userData.getViolationCount(), true);
                });
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
    
    private void showRecentViolations(MessageReceivedEvent event, Guild guild) {
        event.getChannel().sendMessage("📝 Recent violations are stored in: `data/" + guild.getId() + "/antinuke/violations_YYYY-MM.json`\n" +
                "Use the file system to view detailed violation logs with timestamps and punishment details.").queue();
    }
    
    private void showHelp(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🛡️ Anti-Nuke Logs Commands")
                .setColor(Color.BLUE)
                .setDescription("View anti-nuke logs and statistics")
                .addField("Basic Commands", 
                    "`!antilog` - Show overview\n" +
                    "`!antilog user <@user>` - View user statistics\n" +
                    "`!antilog top` - Show most active users\n" +
                    "`!antilog recent` - Show recent violations info", false)
                .addField("File Locations", 
                    "`data/SERVERID/antinuke/user_data.json` - User statistics\n" +
                    "`data/SERVERID/antinuke/violations_YYYY-MM.json` - Monthly violation logs", false)
                .setFooter("Only server owners can access anti-nuke logs");
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
    
    private void showUserUsage(MessageReceivedEvent event) {
        event.getChannel().sendMessage(
            "❌ **Usage:** `!antilog user <@user|user_id>`\n" +
            "**Example:** `!antilog user @sa1_` or `!antilog user 473500213072494594`"
        ).queue();
    }
}
