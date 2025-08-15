package com.shadowbot.core.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.Arrays;

public final class ModerationUtils {
    private ModerationUtils() {}

    public static boolean hasPermissions(Member member, GuildChannel channel, Permission... permissions) {
        if (member == null) {
            return false;
        }
        return member.hasPermission(channel, permissions);
    }

    public static boolean hasPermissions(Member member, Permission... permissions) {
        if (member == null) {
            return false;
        }
        return member.hasPermission(permissions);
    }

    public static String checkCanInteract(Member executor, Member target, Member selfBot) {
        if (target == null) {
            return "Target member not found.";
        }
        if (executor == null) {
            return "Executor not found.";
        }
        if (executor.getIdLong() == executor.getGuild().getOwnerIdLong()) {
            // Owner can always interact except when bot cannot
            if (selfBot != null && !selfBot.canInteract(target)) {
                return "I cannot interact with that member due to role hierarchy.";
            }
            return null;
        }
        if (!executor.canInteract(target)) {
            return "You cannot interact with that member due to role hierarchy.";
        }
        if (selfBot != null && !selfBot.canInteract(target)) {
            return "I cannot interact with that member due to role hierarchy.";
        }
        return null;
    }

    public static String checkCanInteractRole(Member executor, Role role, Member selfBot) {
        if (role == null) {
            return "Role not found.";
        }
        if (executor == null) {
            return "Executor not found.";
        }
        if (!executor.canInteract(role)) {
            return "You cannot interact with that role due to role hierarchy.";
        }
        if (selfBot != null && !selfBot.canInteract(role)) {
            return "I cannot interact with that role due to role hierarchy.";
        }
        return null;
    }

    public static String requirePermissions(Member member, Permission... permissions) {
        if (member == null) {
            return "Not in a guild context.";
        }
        if (!member.hasPermission(permissions)) {
            return "Missing required permissions: " + Arrays.toString(permissions);
        }
        return null;
    }

    public static String requirePermissions(Member member, GuildChannel channel, Permission... permissions) {
        if (member == null) {
            return "Not in a guild context.";
        }
        if (!member.hasPermission(channel, permissions)) {
            return "Missing required channel permissions: " + Arrays.toString(permissions);
        }
        return null;
    }
}
