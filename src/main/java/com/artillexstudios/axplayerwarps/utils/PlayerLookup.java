package com.artillexstudios.axplayerwarps.utils;

import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public final class PlayerLookup {
    private PlayerLookup() {
    }

    public static void find(Player sender, String name, Consumer<OfflinePlayer> callback) {
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            UUID uuid = AxPlayerWarps.getDatabase().getUUIDFromName(name);
            if (uuid == null) {
                Scheduler.get().run(sender, task -> MESSAGEUTILS.sendLang(sender, "errors.player-not-found"), () -> {});
                return;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            Scheduler.get().run(sender, task -> callback.accept(player), () -> {});
        });
    }
}
