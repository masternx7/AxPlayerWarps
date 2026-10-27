package com.artillexstudios.axplayerwarps.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class PlaceholderUtils {
    private static final Method SET_PLACEHOLDERS = findParser();

    private PlaceholderUtils() {
    }

    private static Method findParser() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return null;
        try {
            Class<?> placeholderApi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return placeholderApi.getMethod("setPlaceholders", Player.class, String.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String parse(Player player, String text) {
        if (SET_PLACEHOLDERS == null || player == null || text == null || text.isEmpty()) return text;
        try {
            Object parsed = SET_PLACEHOLDERS.invoke(null, player, text);
            return parsed instanceof String string ? string : text;
        } catch (Throwable ignored) {
            return text;
        }
    }

    public static List<String> parse(Player player, List<String> lines) {
        List<String> parsed = new ArrayList<>(lines.size());
        for (String line : lines) {
            parsed.add(parse(player, line));
        }
        return parsed;
    }
}
