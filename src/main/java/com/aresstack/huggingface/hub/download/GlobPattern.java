package com.aresstack.huggingface.hub.download;

import java.util.regex.Pattern;

/**
 * A minimal glob matcher for repository file paths using {@code /}-separated paths.
 * Supports {@code *} (any run within a path segment), {@code **} (any run across segments)
 * and {@code ?} (a single character).
 */
final class GlobPattern {

    private final Pattern pattern;

    private GlobPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    static GlobPattern compile(String glob) {
        return new GlobPattern(Pattern.compile(toRegex(glob)));
    }

    boolean matches(String path) {
        return pattern.matcher(path).matches();
    }

    private static String toRegex(String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (int index = 0; index < glob.length(); index++) {
            char c = glob.charAt(index);
            switch (c) {
                case '*':
                    if (index + 1 < glob.length() && glob.charAt(index + 1) == '*') {
                        regex.append(".*");
                        index++;
                    } else {
                        regex.append("[^/]*");
                    }
                    break;
                case '?':
                    regex.append("[^/]");
                    break;
                case '.': case '(': case ')': case '+': case '|': case '^':
                case '$': case '@': case '%': case '{': case '}': case '[':
                case ']': case '\\':
                    regex.append('\\').append(c);
                    break;
                default:
                    regex.append(c);
            }
        }
        return regex.append('$').toString();
    }
}
