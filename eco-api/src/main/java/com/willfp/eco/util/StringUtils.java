package com.willfp.eco.util;

import com.google.common.collect.ImmutableList;
import com.willfp.eco.core.integrations.placeholder.PlaceholderManager;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

/**
 * Utilities / API methods for strings.
 */
@UtilityClass
public class StringUtils {
    /**
     * Regexes for gradients.
     */
    private static final List<Pattern> GRADIENT_PATTERNS = new ImmutableList.Builder<Pattern>()
            .add(Pattern.compile("<GRADIENT:([0-9A-Fa-f]{6})>(.*?)</GRADIENT:([0-9A-Fa-f]{6})>", Pattern.CASE_INSENSITIVE))
            .add(Pattern.compile("<GRADIENT:#([0-9A-Fa-f]{6})>(.*?)</GRADIENT:#([0-9A-Fa-f]{6})>", Pattern.CASE_INSENSITIVE))
            .add(Pattern.compile("<G:([0-9A-Fa-f]{6})>(.*?)</G:([0-9A-Fa-f]{6})>", Pattern.CASE_INSENSITIVE))
            .add(Pattern.compile("<#:([0-9A-Fa-f]{6})>(.*?)</#:([0-9A-Fa-f]{6})>"))
            .add(Pattern.compile("\\{#:([0-9A-Fa-f]{6})}(.*?)\\{/#:([0-9A-Fa-f]{6})}"))
            .build();

    /**
     * Regexes for hex codes.
     */
    private static final List<Pattern> HEX_PATTERNS = new ImmutableList.Builder<Pattern>()
            .add(Pattern.compile("&#" + "([A-Fa-f0-9]{6})" + ""))
            .add(Pattern.compile("\\{#" + "([A-Fa-f0-9]{6})" + "}"))
            .add(Pattern.compile("<#" + "([A-Fa-f0-9]{6})" + ">"))
            .build();

    /**
     * Format a list of strings - converts Placeholders and Color codes.
     *
     * @param list   The messages to format.
     * @param player The player to translate placeholders with respect to.
     * @return The message, format.
     */
    public List<String> formatList(@NotNull final List<String> list,
                                   @Nullable final Player player) {
        List<String> translated = new ArrayList<>();
        for (String string : list) {
            translated.add(format(string, player));
        }

        return translated;
    }

    /**
     * Format a list of strings - converts Placeholders and Color codes.
     *
     * @param list The messages to format.
     * @return The message, formatted.
     */
    public List<String> formatList(@NotNull final List<String> list) {
        return formatList(list, null);
    }

    /**
     * Format a string - converts Placeholders and Color codes.
     *
     * @param message The message to format.
     * @param player  The player to translate placeholders with respect to.
     * @return The message, formatted.
     */
    public String format(@NotNull final String message,
                         @Nullable final Player player) {
        String processedMessage = message;
        processedMessage = translateGradients(processedMessage);
        processedMessage = PlaceholderManager.translatePlaceholders(processedMessage, player);
        processedMessage = translateHexColorCodes(processedMessage);
        processedMessage = ChatColor.translateAlternateColorCodes('&', processedMessage);
        return processedMessage;
    }

    /**
     * Format a string without respect to a player.
     *
     * @param message The message to translate.
     * @return The message, formatted.
     * @see StringUtils#format(String, Player)
     */
    public String format(@NotNull final String message) {
        return format(message, null);
    }

    private static String translateHexColorCodes(@NotNull final String message) {
        String processedMessage = message;
        for (Pattern pattern : HEX_PATTERNS) {
            processedMessage = translateHexColorCodes(processedMessage, pattern);
        }
        return processedMessage;
    }

    private static String translateHexColorCodes(@NotNull final String message,
                                                 @NotNull final Pattern pattern) {
        Matcher matcher = pattern.matcher(message);

        StringBuilder builder = new StringBuilder(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(builder, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5));
        }

        return matcher.appendTail(builder).toString();
    }

    private static String processGradients(@NotNull final String string,
                                           @NotNull final Color start,
                                           @NotNull final Color end) {
        String processedString = string;
        List<ChatColor> modifiers = new ArrayList<>();
        if (processedString.contains("&l")) {
            modifiers.add(ChatColor.BOLD);
        }
        if (processedString.contains("&o")) {
            modifiers.add(ChatColor.ITALIC);
        }
        if (processedString.contains("&n")) {
            modifiers.add(ChatColor.UNDERLINE);
        }
        if (processedString.contains("&m")) {
            modifiers.add(ChatColor.STRIKETHROUGH);
        }
        if (processedString.contains("&k")) {
            modifiers.add(ChatColor.MAGIC);
        }
        processedString = processedString.replace("&l", "");
        processedString = processedString.replace("&o", "");
        processedString = processedString.replace("&n", "");
        processedString = processedString.replace("&k", "");
        processedString = processedString.replace("&m", "");

        StringBuilder stringBuilder = new StringBuilder();
        ChatColor[] colors = getGradientColors(start, end, processedString.length());
        String[] characters = processedString.split("");
        for (int i = 0; i < processedString.length(); i++) {
            stringBuilder.append(colors[i]);
            modifiers.forEach(stringBuilder::append);
            stringBuilder.append(characters[i]);
        }
        return stringBuilder.toString();
    }

    private static ChatColor[] getGradientColors(@NotNull final Color start,
                                                 @NotNull final Color end,
                                                 final int step) {
        ChatColor[] colors = new ChatColor[step];
        int stepR = Math.abs(start.getRed() - end.getRed()) / (step - 1);
        int stepG = Math.abs(start.getGreen() - end.getGreen()) / (step - 1);
        int stepB = Math.abs(start.getBlue() - end.getBlue()) / (step - 1);
        int[] direction = new int[]{
                start.getRed() < end.getRed() ? +1 : -1,
                start.getGreen() < end.getGreen() ? +1 : -1,
                start.getBlue() < end.getBlue() ? +1 : -1
        };

        for (int i = 0; i < step; i++) {
            Color color = new Color(start.getRed() + ((stepR * i) * direction[0]), start.getGreen() + ((stepG * i) * direction[1]), start.getBlue() + ((stepB * i) * direction[2]));
            colors[i] = ChatColor.of(color);
        }
        return colors;
    }

    private static String translateGradients(@NotNull final String string) {
        String processedString = string;
        for (Pattern pattern : GRADIENT_PATTERNS) {
            Matcher matcher = pattern.matcher(string);
            while (matcher.find()) {
                String start = matcher.group(1);
                String end = matcher.group(3);
                String content = matcher.group(2);
                processedString = processedString.replace(matcher.group(), processGradients(content, new Color(Integer.parseInt(start, 16)), new Color(Integer.parseInt(end, 16))));
            }
        }
        return processedString;
    }

    /**
     * Internal implementation of {@link String#valueOf}.
     * Formats collections and doubles better.
     *
     * @param object The object to convert to string.
     * @return The object stringified.
     */
    public String internalToString(@Nullable final Object object) {
        if (object == null) {
            return "null";
        }

        if (object instanceof Integer) {
            return ((Integer) object).toString();
        } else if (object instanceof String) {
            return (String) object;
        } else if (object instanceof Double) {
            return NumberUtils.format((Double) object);
        } else if (object instanceof Collection<?> c) {
            return c.stream().map(StringUtils::internalToString).collect(Collectors.joining(", "));
        } else {
            return String.valueOf(object);
        }
    }

    /**
     * Remove a string of characters from the start of a string.
     *
     * @param string The string to remove the prefix from.
     * @param prefix The substring to remove.
     * @return The string with the prefix removed.
     */
    public String removePrefix(@NotNull final String string,
                               @NotNull final String prefix) {
        if (string.startsWith(prefix)) {
            return string.substring(prefix.length());
        }
        return string;
    }

    /**
     * Convert legacy string to JSON.
     *
     * @param legacy The legacy string.
     * @return The JSON String.
     */
    public String legacyToJson(@NotNull final String legacy) {
        return GsonComponentSerializer.gson().serialize(
                Component.empty().decoration(TextDecoration.ITALIC, false).append(
                        LegacyComponentSerializer.legacySection().deserialize(legacy)
                )
        );
    }

    /**
     * Convert JSON string to legacy.
     *
     * @param json The JSON string.
     * @return The legacy string.
     */
    public String jsonToLegacy(@NotNull final String json) {
        return LegacyComponentSerializer.legacySection().serialize(
                GsonComponentSerializer.gson().deserialize(json)
        );
    }
}
