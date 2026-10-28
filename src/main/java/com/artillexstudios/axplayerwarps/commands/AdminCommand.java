package com.artillexstudios.axplayerwarps.commands;

import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.commands.annotations.AllWarps;
import com.artillexstudios.axplayerwarps.commands.annotations.SelectWarp;
import com.artillexstudios.axplayerwarps.commands.subcommands.Converter;
import com.artillexstudios.axplayerwarps.commands.subcommands.Reload;
import com.artillexstudios.axplayerwarps.enums.AccessList;
import com.artillexstudios.axplayerwarps.enums.Converters;
import com.artillexstudios.axplayerwarps.input.DialogSelect;
import com.artillexstudios.axplayerwarps.input.InputManager;
import com.artillexstudios.axplayerwarps.utils.PlayerLookup;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.exception.CommandErrorException;
import revxrsal.commands.orphan.OrphanCommand;

import java.util.Map;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.LANG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class AdminCommand implements OrphanCommand {

    @DefaultFor({"~", "~ help"})
    @CommandPermission("axplayerwarps.admin.help")
    public void help(@NotNull CommandSender sender) {
        for (String m : LANG.getStringList("admin-help")) {
            sender.sendMessage(StringUtils.formatToString(m));
        }
    }

    @Subcommand("reload")
    @CommandPermission("axplayerwarps.admin.reload")
    public void reload(@NotNull CommandSender sender) {
        Reload.INSTANCE.execute(sender);
    }

    @Subcommand("delete")
    @CommandPermission("axplayerwarps.admin.delete")
    public void delete(@NotNull CommandSender sender, @Optional @SelectWarp @AllWarps Warp warp) {
        if (warp != null) {
            performDelete(sender, warp);
            return;
        }

        if (!(sender instanceof Player player)) {
            missing(sender, "warp");
            return;
        }

        DialogSelect.warp(player, null, WarpManager.getWarps(), selected -> performDelete(sender, selected));
    }

    @Subcommand("deleteid")
    @CommandPermission("axplayerwarps.admin.delete")
    public void deleteId(@NotNull CommandSender sender, int id) {
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            Warp warp = null;
            for (Warp w : WarpManager.getWarps()) {
                if (id != w.getId()) continue;
                warp = w;
                break;
            }
            if (warp == null) {
                MESSAGEUTILS.sendLang(sender, "errors.not-found", Map.of("%warp%", String.valueOf(id)));
                throw new CommandErrorException();
            }
            AxPlayerWarps.getDatabase().deleteWarp(warp);
            MESSAGEUTILS.sendLang(sender, "admin.deleted", Map.of("%warp%", warp.getName()));
        });
    }

    @Subcommand("setowner")
    @CommandPermission("axplayerwarps.admin.setowner")
    public void setOwner(@NotNull CommandSender sender, @Optional @SelectWarp @AllWarps Warp warp, @Optional OfflinePlayer player) {
        if (warp == null) {
            if (!(sender instanceof Player playerSender)) {
                missing(sender, "warp");
                return;
            }

            DialogSelect.warp(playerSender, null, WarpManager.getWarps(), selected -> {
                if (player != null) {
                    performSetOwner(sender, selected, player);
                    return;
                }
                InputManager.getInput(playerSender, "transfer", result ->
                        PlayerLookup.find(playerSender, result, target -> performSetOwner(sender, selected, target)));
            });
            return;
        }

        if (player != null) {
            performSetOwner(sender, warp, player);
            return;
        }

        if (!(sender instanceof Player playerSender)) {
            missing(sender, "player");
            return;
        }

        InputManager.getInput(playerSender, "transfer", result ->
                PlayerLookup.find(playerSender, result, target -> performSetOwner(sender, warp, target)));
    }

    @Subcommand("converter")
    @CommandPermission("axplayerwarps.admin.converter")
    public void converter(CommandSender sender, Converters converters) {
        Converter.INSTANCE.execute(sender, converters);
    }

    private void performDelete(CommandSender sender, Warp warp) {
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            AxPlayerWarps.getDatabase().deleteWarp(warp);
            MESSAGEUTILS.sendLang(sender, "admin.deleted", Map.of("%warp%", warp.getName()));
        });
    }

    private void performSetOwner(CommandSender sender, Warp warp, OfflinePlayer player) {
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            warp.setOwner(player.getUniqueId());
            AxPlayerWarps.getDatabase().updateWarp(warp);
            AxPlayerWarps.getDatabase().removeFromList(warp, AccessList.WHITELIST, player);
            AxPlayerWarps.getDatabase().removeFromList(warp, AccessList.BLACKLIST, player);

            MESSAGEUTILS.sendLang(sender, "admin.setowner", Map.of("%warp%", warp.getName(), "%player%", player.getName() == null ? "---" : player.getName()));
        });
    }

    private void missing(CommandSender sender, String what) {
        sender.sendMessage(StringUtils.formatToString("&#FF4444You must specify a " + what + "!"));
    }
}
