package com.shadowbot.core.events.admin.antinuke;

import com.shadowbot.core.services.AntiNukeService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;

public class AntiNukeListener extends ListenerAdapter {
    private final AntiNukeService antiNukeService;
    
    public AntiNukeListener(AntiNukeService antiNukeService) {
        this.antiNukeService = antiNukeService;
        System.out.println("[ANTI-NUKE] AntiNukeListener initialized and ready!");
    }
    
    @Override
    public void onReady(@NotNull net.dv8tion.jda.api.events.session.ReadyEvent event) {
        System.out.println("[ANTI-NUKE] Bot is ready! Anti-nuke listener is active.");
    }
    
    @Override
    public void onMessageReceived(@NotNull net.dv8tion.jda.api.events.message.MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getMessage().getContentRaw().equals("!testantinuke")) {
            System.out.println("[ANTI-NUKE TEST] Message event received! Listener is working.");
            event.getChannel().sendMessage("✅ Anti-nuke listener is active and working!").queue();
        }
    }
    
    @Override
    public void onChannelCreate(@NotNull ChannelCreateEvent event) {
        if (!event.isFromGuild()) return;
        
        Guild guild = event.getGuild();
        
        // Get the user who created the channel from audit logs
        guild.retrieveAuditLogs().queue(auditLogs -> {
            auditLogs.forEach(auditLog -> {
                if (auditLog.getType() == net.dv8tion.jda.api.audit.ActionType.CHANNEL_CREATE &&
                    auditLog.getTargetIdLong() == event.getChannel().getIdLong()) {
                    
                    User creator = auditLog.getUser();
                    if (creator != null && !creator.isBot()) {
                        Member member = guild.getMember(creator);
                        if (member != null) {
                            checkViolation(guild, member, AntiNukeService.ActionType.CHANNEL_CREATE, 
                                "Mass channel creation detected");
                        }
                    }
                }
            });
        });
    }
    
    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        if (!event.isFromGuild()) return;
        
        Guild guild = event.getGuild();
        
        // Get the user who deleted the channel from audit logs
        guild.retrieveAuditLogs().queue(auditLogs -> {
            auditLogs.forEach(auditLog -> {
                if (auditLog.getType() == net.dv8tion.jda.api.audit.ActionType.CHANNEL_DELETE &&
                    auditLog.getTargetIdLong() == event.getChannel().getIdLong()) {
                    
                    User deleter = auditLog.getUser();
                    if (deleter != null && !deleter.isBot()) {
                        Member member = guild.getMember(deleter);
                        if (member != null) {
                            checkViolation(guild, member, AntiNukeService.ActionType.CHANNEL_DELETE, 
                                "Mass channel deletion detected");
                        }
                    }
                }
            });
        });
    }
    
    @Override
    public void onRoleCreate(@NotNull RoleCreateEvent event) {
        Guild guild = event.getGuild();
        
        System.out.println("[ANTI-NUKE DEBUG] *** ROLE CREATE EVENT TRIGGERED ***");
        System.out.println("[ANTI-NUKE DEBUG] Role created: " + event.getRole().getName() + " in guild: " + guild.getName());
        
        // Get the user who created the role from audit logs with a slight delay
        guild.retrieveAuditLogs().queueAfter(500, java.util.concurrent.TimeUnit.MILLISECONDS, auditLogs -> {
            System.out.println("[ANTI-NUKE DEBUG] Retrieved " + auditLogs.size() + " audit log entries for role creation");
            
            // Look for recent role creation entries (within last 10 seconds)
            long tenSecondsAgo = System.currentTimeMillis() - 10000;
            
            auditLogs.forEach(auditLog -> {
                if (auditLog.getType() == net.dv8tion.jda.api.audit.ActionType.ROLE_CREATE &&
                    auditLog.getTimeCreated().toEpochSecond() * 1000 > tenSecondsAgo) {
                    
                    User creator = auditLog.getUser();
                    System.out.println("[ANTI-NUKE DEBUG] Found role creation by: " + (creator != null ? creator.getAsTag() : "null"));
                    
                    if (creator != null) {
                        System.out.println("[ANTI-NUKE DEBUG] Creator is bot: " + creator.isBot());
                        if (!creator.isBot()) {
                            // Try cache first, then retrieve from API
                            Member member = guild.getMember(creator);
                            if (member != null) {
                                System.out.println("[ANTI-NUKE DEBUG] Member found in cache: " + member.getUser().getAsTag());
                                checkViolation(guild, member, AntiNukeService.ActionType.ROLE_CREATE, 
                                    "Mass role creation detected");
                            } else {
                                System.out.println("[ANTI-NUKE DEBUG] Member not in cache, retrieving from API...");
                                guild.retrieveMemberById(creator.getId()).queue(
                                    retrievedMember -> {
                                        System.out.println("[ANTI-NUKE DEBUG] Member retrieved from API: " + retrievedMember.getUser().getAsTag());
                                        checkViolation(guild, retrievedMember, AntiNukeService.ActionType.ROLE_CREATE, 
                                            "Mass role creation detected");
                                    },
                                    error -> {
                                        System.out.println("[ANTI-NUKE DEBUG] Failed to retrieve member from API: " + error.getMessage());
                                        // Member might have left the guild
                                    }
                                );
                            }
                        }
                    }
                }
            });
        }, error -> {
            System.err.println("[ANTI-NUKE ERROR] Failed to retrieve audit logs for role creation: " + error.getMessage());
            error.printStackTrace();
        });
    }
    
    @Override
    public void onRoleDelete(@NotNull RoleDeleteEvent event) {
        Guild guild = event.getGuild();
        
        System.out.println("[ANTI-NUKE DEBUG] *** ROLE DELETE EVENT TRIGGERED ***");
        System.out.println("[ANTI-NUKE DEBUG] Role deleted: " + event.getRole().getName() + " in guild: " + guild.getName());
        System.out.println("[ANTI-NUKE DEBUG] Bot has VIEW_AUDIT_LOG permission: " + guild.getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS));
        
        // Get the user who deleted the role from audit logs with a slight delay to ensure audit log is available
        guild.retrieveAuditLogs().queueAfter(500, java.util.concurrent.TimeUnit.MILLISECONDS, auditLogs -> {
            System.out.println("[ANTI-NUKE DEBUG] Retrieved " + auditLogs.size() + " audit log entries");
            
            // Look for recent role deletion entries (within last 10 seconds)
            long tenSecondsAgo = System.currentTimeMillis() - 10000;
            
            auditLogs.forEach(auditLog -> {
                if (auditLog.getType() == net.dv8tion.jda.api.audit.ActionType.ROLE_DELETE &&
                    auditLog.getTimeCreated().toEpochSecond() * 1000 > tenSecondsAgo) {
                    
                    User deleter = auditLog.getUser();
                    System.out.println("[ANTI-NUKE DEBUG] Found role deletion by: " + (deleter != null ? deleter.getAsTag() : "null"));
                    
                    if (deleter != null) {
                        System.out.println("[ANTI-NUKE DEBUG] Deleter is bot: " + deleter.isBot());
                        if (!deleter.isBot()) {
                            // Try cache first, then retrieve from API
                            Member member = guild.getMember(deleter);
                            if (member != null) {
                                System.out.println("[ANTI-NUKE DEBUG] Member found in cache: " + member.getUser().getAsTag());
                                checkViolation(guild, member, AntiNukeService.ActionType.ROLE_DELETE, 
                                    "Mass role deletion detected");
                            } else {
                                System.out.println("[ANTI-NUKE DEBUG] Member not in cache, retrieving from API...");
                                guild.retrieveMemberById(deleter.getId()).queue(
                                    retrievedMember -> {
                                        System.out.println("[ANTI-NUKE DEBUG] Member retrieved from API: " + retrievedMember.getUser().getAsTag());
                                        checkViolation(guild, retrievedMember, AntiNukeService.ActionType.ROLE_DELETE, 
                                            "Mass role deletion detected");
                                    },
                                    error -> {
                                        System.out.println("[ANTI-NUKE DEBUG] Failed to retrieve member from API: " + error.getMessage());
                                        // Member might have left the guild
                                    }
                                );
                            }
                        }
                    }
                }
            });
        }, error -> {
            System.err.println("[ANTI-NUKE ERROR] Failed to retrieve audit logs: " + error.getMessage());
            error.printStackTrace();
        });
    }
    
    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        User removedUser = event.getUser();
        
        System.out.println("[ANTI-NUKE DEBUG] *** MEMBER REMOVE EVENT TRIGGERED ***");
        System.out.println("[ANTI-NUKE DEBUG] Member removed: " + removedUser.getAsTag() + " from guild: " + guild.getName());
        
        // Get the user who kicked/banned the member from audit logs with a slight delay
        guild.retrieveAuditLogs().queueAfter(500, java.util.concurrent.TimeUnit.MILLISECONDS, auditLogs -> {
            System.out.println("[ANTI-NUKE DEBUG] Retrieved " + auditLogs.size() + " audit log entries for member removal");
            
            // Look for recent kick/ban entries (within last 10 seconds)
            long tenSecondsAgo = System.currentTimeMillis() - 10000;
            
            auditLogs.forEach(auditLog -> {
                boolean isKick = auditLog.getType() == net.dv8tion.jda.api.audit.ActionType.KICK;
                boolean isBan = auditLog.getType() == net.dv8tion.jda.api.audit.ActionType.BAN;
                
                if ((isKick || isBan) && auditLog.getTimeCreated().toEpochSecond() * 1000 > tenSecondsAgo &&
                    auditLog.getTargetIdLong() == removedUser.getIdLong()) {
                    
                    User moderator = auditLog.getUser();
                    System.out.println("[ANTI-NUKE DEBUG] Found " + (isKick ? "kick" : "ban") + " by: " + 
                                     (moderator != null ? moderator.getAsTag() : "null"));
                    
                    if (moderator != null) {
                        System.out.println("[ANTI-NUKE DEBUG] Moderator is bot: " + moderator.isBot());
                        if (!moderator.isBot()) {
                            // Try cache first, then retrieve from API
                            Member member = guild.getMember(moderator);
                            if (member != null) {
                                System.out.println("[ANTI-NUKE DEBUG] Member found in cache: " + member.getUser().getAsTag());
                                AntiNukeService.ActionType actionType = isKick ? AntiNukeService.ActionType.MEMBER_KICK : AntiNukeService.ActionType.MEMBER_BAN;
                                String reason = "Mass " + (isKick ? "kick" : "ban") + " detected";
                                checkViolation(guild, member, actionType, reason);
                            } else {
                                System.out.println("[ANTI-NUKE DEBUG] Member not in cache, retrieving from API...");
                                guild.retrieveMemberById(moderator.getId()).queue(
                                    retrievedMember -> {
                                        System.out.println("[ANTI-NUKE DEBUG] Member retrieved from API: " + retrievedMember.getUser().getAsTag());
                                        AntiNukeService.ActionType actionType = isKick ? AntiNukeService.ActionType.MEMBER_KICK : AntiNukeService.ActionType.MEMBER_BAN;
                                        String reason = "Mass " + (isKick ? "kick" : "ban") + " detected";
                                        checkViolation(guild, retrievedMember, actionType, reason);
                                    },
                                    error -> {
                                        System.out.println("[ANTI-NUKE DEBUG] Failed to retrieve member from API: " + error.getMessage());
                                        // Member might have left the guild
                                    }
                                );
                            }
                        }
                    }
                }
            });
        }, error -> {
            System.err.println("[ANTI-NUKE ERROR] Failed to retrieve audit logs for member removal: " + error.getMessage());
            error.printStackTrace();
        });
    }

    
    private void checkViolation(Guild guild, Member violator, AntiNukeService.ActionType actionType, String reason) {
        // Skip if it's the server owner (they can't nuke their own server maliciously)
        if (guild.getOwner() != null && guild.getOwner().equals(violator)) {
            return;
        }
        
        // Skip if it's a bot developer (temporarily disabled for testing)
        // if (violator.getUser().getId().equals("1075533908613021727")) {
        //     return;
        // }
        
        boolean violated = antiNukeService.checkAndRecordAction(
            guild.getIdLong(), 
            violator.getIdLong(), 
            actionType,
            guild,
            violator
        );
        
        if (violated) {
            handleViolation(guild, violator, actionType, reason);
        }
    }
    
    private void handleViolation(Guild guild, Member violator, AntiNukeService.ActionType actionType, String reason) {
        AntiNukeService.AntiNukeConfig config = antiNukeService.getConfig(guild.getIdLong());
        
        // Log the violation
        logViolation(guild, violator, actionType, reason, config.punishment);
        
        // Execute punishment
        antiNukeService.executePunishment(guild, violator, config.punishment, reason, actionType);
        
        // Send notification to log channel
        sendLogNotification(guild, violator, actionType, reason, config);
    }
    
    private void logViolation(Guild guild, Member violator, AntiNukeService.ActionType actionType, 
                             String reason, AntiNukeService.PunishmentType punishment) {
        System.out.println(String.format(
            "[ANTI-NUKE] Guild: %s (%s) | User: %s (%s) | Action: %s | Punishment: %s | Reason: %s",
            guild.getName(), guild.getId(),
            violator.getUser().getAsTag(), violator.getId(),
            actionType.name(), punishment.name(), reason
        ));
    }
    
    private void sendLogNotification(Guild guild, Member violator, AntiNukeService.ActionType actionType,
                                   String reason, AntiNukeService.AntiNukeConfig config) {
        if (config.logChannelId == null) {
            return; // No log channel configured
        }
        
        TextChannel logChannel = guild.getTextChannelById(config.logChannelId);
        if (logChannel == null || !logChannel.canTalk()) {
            return; // Log channel not found or no permission
        }
        
        Color embedColor = getColorForAction(actionType);
        String actionName = getActionDisplayName(actionType);
        String punishmentEmoji = getPunishmentEmoji(config.punishment);
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🚨 Anti-Nuke Violation Detected")
                .setColor(embedColor)
                .setThumbnail(violator.getUser().getAvatarUrl())
                .addField("👤 Violator", violator.getAsMention() + "\n`" + violator.getUser().getAsTag() + "`", true)
                .addField("⚡ Action", actionName, true)
                .addField(punishmentEmoji + " Punishment", config.punishment.getDisplayName(), true)
                .addField("📝 Reason", reason, false)
                .addField("⚙️ Limits", String.format(
                    "Channels: %d | Roles: %d | Webhooks: %d\nTime Window: %d minutes",
                    config.channelLimit, config.roleLimit, config.webhookLimit, config.timeWindowMinutes
                ), false)
                .setTimestamp(Instant.now())
                .setFooter("Anti-Nuke Protection • " + guild.getName(), guild.getIconUrl());
        
        logChannel.sendMessageEmbeds(embed.build()).queue();
    }
    
    private Color getColorForAction(AntiNukeService.ActionType actionType) {
        switch (actionType) {
            case CHANNEL_CREATE:
            case CHANNEL_DELETE:
                return Color.ORANGE;
            case ROLE_CREATE:
            case ROLE_DELETE:
                return Color.RED;
            case MEMBER_KICK:
                return Color.YELLOW;
            case MEMBER_BAN:
                return Color.MAGENTA;
            default:
                return Color.GRAY;
        }
    }
    
    private String getActionDisplayName(AntiNukeService.ActionType actionType) {
        switch (actionType) {
            case CHANNEL_CREATE:
                return "Mass Channel Creation";
            case CHANNEL_DELETE:
                return "Mass Channel Deletion";
            case ROLE_CREATE:
                return "Mass Role Creation";
            case ROLE_DELETE:
                return "Mass Role Deletion";
            case MEMBER_KICK:
                return "Mass Member Kicks";
            case MEMBER_BAN:
                return "Mass Member Bans";
            default:
                return actionType.name();
        }
    }
    
    private String getPunishmentEmoji(AntiNukeService.PunishmentType punishment) {
        switch (punishment) {
            case STRIP_ROLES:
                return "🔒";
            case KICK:
                return "👢";
            case BAN:
                return "🔨";
            default:
                return "⚡";
        }
    }
}
