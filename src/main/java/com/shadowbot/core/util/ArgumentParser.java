package com.shadowbot.core.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArgumentParser {
    private static final Pattern ID_EXTRACTOR = Pattern.compile("\\d+");

    private ArgumentParser() {}

    public static Member resolveMember(Guild guild, String token, List<Member> mentioned) {
        if (!mentioned.isEmpty()) {
            return mentioned.get(0);
        }
        String id = extractId(token);
        if (id == null) {
            return null;
        }
        try {
            return guild.retrieveMemberById(id).complete();
        } catch (Exception e) {
            return null;
        }
    }

    public static Role resolveRole(Guild guild, String token, List<Role> mentioned) {
        if (!mentioned.isEmpty()) {
            return mentioned.get(0);
        }
        String id = extractId(token);
        if (id == null) {
            return null;
        }
        return guild.getRoleById(id);
    }

    public static String extractId(String token) {
        if (token == null) return null;
        Matcher m = ID_EXTRACTOR.matcher(token);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    public static Duration parseDuration(String token) {
        // Supports formats like 10m, 2h, 3d; default minutes if no unit
        if (token == null || token.isEmpty()) return null;
        try {
            char last = Character.toLowerCase(token.charAt(token.length() - 1));
            String numPart = (Character.isDigit(last)) ? token : token.substring(0, token.length() - 1);
            long value = Long.parseLong(numPart);
            switch (last) {
                case 's': return Duration.ofSeconds(value);
                case 'm': return Duration.ofMinutes(value);
                case 'h': return Duration.ofHours(value);
                case 'd': return Duration.ofDays(value);
                default: return Duration.ofMinutes(Long.parseLong(token));
            }
        } catch (Exception e) {
            return null;
        }
    }
}
