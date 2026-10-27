package com.artillexstudios.axplayerwarps.commands.subcommands;

import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.enums.IconAction;
import com.artillexstudios.axplayerwarps.warps.Warp;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public enum Icon {
    INSTANCE;

    public void execute(Player sender, IconAction action, Warp warp) {
        if (!canEdit(sender, warp)) return;

        switch (action) {
            case SET -> set(sender, warp);
            case REMOVE -> remove(sender, warp);
        }
    }

    private void set(Player sender, Warp warp) {
        ItemStack item = sender.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            MESSAGEUTILS.sendLang(sender, "errors.no-item-in-hand");
            return;
        }

        warp.setIcon(item);
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            AxPlayerWarps.getDatabase().updateWarp(warp);
            MESSAGEUTILS.sendLang(sender, "editor.update-icon");
        });
    }

    private void remove(Player sender, Warp warp) {
        warp.setIcon(null);
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            AxPlayerWarps.getDatabase().updateWarp(warp);
            MESSAGEUTILS.sendLang(sender, "editor.remove-icon");
        });
    }

    private boolean canEdit(Player sender, Warp warp) {
        if (warp.getOwner().equals(sender.getUniqueId())) return true;
        if (sender.hasPermission("axplayerwarps.admin.bypass")) return true;
        MESSAGEUTILS.sendLang(sender, "errors.not-your-warp");
        return false;
    }
}
