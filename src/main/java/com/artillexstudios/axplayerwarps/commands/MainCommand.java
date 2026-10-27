package com.artillexstudios.axplayerwarps.commands;

import com.artillexstudios.axplayerwarps.commands.annotations.AllWarps;
import com.artillexstudios.axplayerwarps.commands.annotations.OwnWarps;
import com.artillexstudios.axplayerwarps.commands.subcommands.Create;
import com.artillexstudios.axplayerwarps.commands.subcommands.Delete;
import com.artillexstudios.axplayerwarps.commands.subcommands.Edit;
import com.artillexstudios.axplayerwarps.commands.subcommands.Help;
import com.artillexstudios.axplayerwarps.commands.subcommands.Icon;
import com.artillexstudios.axplayerwarps.commands.subcommands.Info;
import com.artillexstudios.axplayerwarps.commands.subcommands.Open;
import com.artillexstudios.axplayerwarps.commands.subcommands.WarpSettings;
import com.artillexstudios.axplayerwarps.enums.Access;
import com.artillexstudios.axplayerwarps.enums.AccessList;
import com.artillexstudios.axplayerwarps.enums.DescriptionAction;
import com.artillexstudios.axplayerwarps.enums.IconAction;
import com.artillexstudios.axplayerwarps.enums.ListAction;
import com.artillexstudios.axplayerwarps.warps.Warp;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;

public class MainCommand implements OrphanCommand {

    @DefaultFor({"~"})
    @CommandPermission("axplayerwarps.open")
    public void open(@NotNull CommandSender sender, @Optional @CommandPermission("axplayerwarps.use") Warp warp) {
        Open.INSTANCE.execute(sender, warp);
    }

    @Subcommand({"help"})
    @CommandPermission("axplayerwarps.help")
    public void help(@NotNull CommandSender sender) {
        Help.INSTANCE.execute(sender);
    }

    @Subcommand({"open"})
    @CommandPermission("axplayerwarps.open")
    public void open2(@NotNull CommandSender sender, @CommandPermission("axplayerwarps.open.other") @Optional Player player) {
        Open.INSTANCE.execute(sender, player);
    }

    @Subcommand({"warp", "go"})
    @CommandPermission("axplayerwarps.use")
    public void warp(@NotNull Player sender, @AllWarps Warp warp) {
        warp.teleportPlayer(sender);
    }

    @Subcommand({"create"})
    @CommandPermission("axplayerwarps.create")
    public void create(@NotNull Player sender, String warpName) {
        Create.INSTANCE.execute(sender, warpName);
    }

    @Subcommand({"set", "setlocation", "update"})
    @CommandPermission("axplayerwarps.edit")
    public void setLocation(@NotNull Player sender, @OwnWarps Warp warp) {
        WarpSettings.INSTANCE.setLocation(sender, warp);
    }

    @Subcommand({"rename", "name"})
    @CommandPermission("axplayerwarps.edit")
    public void rename(@NotNull Player sender, @OwnWarps Warp warp, String newName) {
        WarpSettings.INSTANCE.rename(sender, warp, newName);
    }

    @Subcommand({"desc", "description"})
    @CommandPermission("axplayerwarps.edit")
    public void description(@NotNull Player sender, DescriptionAction action, @OwnWarps Warp warp, @Optional String text) {
        WarpSettings.INSTANCE.setDescription(sender, warp, action, text);
    }

    @Subcommand({"access"})
    @CommandPermission("axplayerwarps.edit")
    public void access(@NotNull Player sender, @OwnWarps Warp warp, Access access) {
        WarpSettings.INSTANCE.setAccess(sender, warp, access);
    }

    @Subcommand({"price"})
    @CommandPermission("axplayerwarps.edit")
    public void price(@NotNull Player sender, @OwnWarps Warp warp, double price, @Optional String currency) {
        WarpSettings.INSTANCE.setPrice(sender, warp, price, currency);
    }

    @Subcommand({"free"})
    @CommandPermission("axplayerwarps.edit")
    public void free(@NotNull Player sender, @OwnWarps Warp warp) {
        WarpSettings.INSTANCE.setPrice(sender, warp, 0, null);
    }

    @Subcommand({"transfer"})
    @CommandPermission("axplayerwarps.edit")
    public void transfer(@NotNull Player sender, @OwnWarps Warp warp, Player target) {
        WarpSettings.INSTANCE.transfer(sender, warp, target);
    }

    @Subcommand({"whitelist", "wl"})
    @CommandPermission("axplayerwarps.edit")
    public void whitelist(@NotNull Player sender, ListAction action, @OwnWarps Warp warp, @Optional OfflinePlayer target) {
        WarpSettings.INSTANCE.editList(sender, warp, AccessList.WHITELIST, action, target);
    }

    @Subcommand({"blacklist", "bl"})
    @CommandPermission("axplayerwarps.edit")
    public void blacklist(@NotNull Player sender, ListAction action, @OwnWarps Warp warp, @Optional OfflinePlayer target) {
        WarpSettings.INSTANCE.editList(sender, warp, AccessList.BLACKLIST, action, target);
    }

    @Subcommand({"bank", "withdraw"})
    @CommandPermission("axplayerwarps.edit")
    public void bank(@NotNull Player sender, @OwnWarps Warp warp) {
        WarpSettings.INSTANCE.withdraw(sender, warp);
    }

    @Subcommand({"delete"})
    @CommandPermission("axplayerwarps.delete")
    public void delete(@NotNull Player sender, @OwnWarps Warp warp) {
        Delete.INSTANCE.execute(sender, warp);
    }

    @Subcommand({"edit", "settings"})
    @CommandPermission("axplayerwarps.edit")
    public void edit(@NotNull Player sender, @OwnWarps Warp warp) {
        Edit.INSTANCE.execute(sender, warp);
    }

    @Subcommand({"info"})
    @CommandPermission("axplayerwarps.info")
    public void info(@NotNull CommandSender sender, @AllWarps Warp warp) {
        Info.INSTANCE.execute(sender, warp);
    }

    @Subcommand({"icon"})
    @CommandPermission("axplayerwarps.icon")
    public void icon(@NotNull Player sender, IconAction action, @OwnWarps Warp warp) {
        Icon.INSTANCE.execute(sender, action, warp);
    }
}
