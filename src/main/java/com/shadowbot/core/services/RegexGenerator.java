package com.shadowbot.core.services;

public final class RegexGenerator {
    private RegexGenerator() {}

    public static String generate(String word, BlocklistService.RegexOptions options) {
        if (word == null) return "";
        String sanitized = word.trim();
        if (sanitized.isEmpty()) return "";

        String[] leet = buildLeetMap(options);
        StringBuilder sb = new StringBuilder();
        if (options.useWordBoundaries) sb.append("\\b");

        for (int i = 0; i < sanitized.length(); i++) {
            char c = Character.toLowerCase(sanitized.charAt(i));
            String cls = characterClass(c, leet, options);
            sb.append(cls);
            if (i < sanitized.length() - 1) {
                // Allow a limited number of non-word separators (spaces/punctuations) between letters
                if (options.maxNonWordSeparatorsBetweenLetters > 0) {
                    sb.append("[\\W_]{0,").append(options.maxNonWordSeparatorsBetweenLetters).append("}");
                }
            }
        }

        if (options.useWordBoundaries) sb.append("\\b");
        return sb.toString();
    }

    private static String[] buildLeetMap(BlocklistService.RegexOptions options) {
        // index by letter 'a'..'z'
        String[] map = new String[26];
        for (int i = 0; i < 26; i++) map[i] = "";
        if (!options.useLeetSubstitutions) {
            return map;
        }
        map['a' - 'a'] = "a@4";
        map['b' - 'a'] = "b8";
        map['c' - 'a'] = "c(";
        map['d' - 'a'] = "d";
        map['e' - 'a'] = "e3";
        map['f' - 'a'] = "f";
        map['g' - 'a'] = "g9";
        map['h' - 'a'] = "h";
        map['i' - 'a'] = "i1!";
        map['j' - 'a'] = "j";
        map['k' - 'a'] = "k";
        map['l' - 'a'] = "l1!";
        map['m' - 'a'] = "m";
        map['n' - 'a'] = "n";
        map['o' - 'a'] = "o0";
        map['p' - 'a'] = "p";
        map['q' - 'a'] = "q";
        map['r' - 'a'] = "r";
        map['s' - 'a'] = "s5$";
        map['t' - 'a'] = "t7";
        map['u' - 'a'] = "u";
        map['v' - 'a'] = "v";
        map['w' - 'a'] = "w";
        map['x' - 'a'] = "x";
        map['y' - 'a'] = "y";
        map['z' - 'a'] = "z2";
        return map;
    }

    private static String characterClass(char c, String[] leet, BlocklistService.RegexOptions options) {
        if (c < 'a' || c > 'z') {
            return escape(Character.toString(c));
        }
        String variants = leet[c - 'a'];
        String base = Character.toString(c);
        if (variants == null || variants.isEmpty()) {
            return "[" + base + Character.toUpperCase(c) + "]";
        }
        String chars = base + Character.toUpperCase(c) + variants;
        return "[" + escape(chars) + "]";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("-", "\\-").replace("]", "\\]").replace("^", "\\^");
    }
}
