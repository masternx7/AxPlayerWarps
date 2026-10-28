package com.artillexstudios.axplayerwarps.commands;

import com.artillexstudios.axplayerwarps.commands.annotations.AllWarps;
import com.artillexstudios.axplayerwarps.commands.annotations.OwnWarps;
import com.artillexstudios.axplayerwarps.commands.annotations.SelectWarp;
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
import com.artillexstudios.axplayerwarps.input.DialogSelect;
import com.artillexstudios.axplayerwarps.input.InputManager;
import com.artillexstudios.axplayerwarps.utils.PlayerLookup;
import com.artillexstudios.axplayerwarps.warps.Warp;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;

import java.util.List;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.LANG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class MainCommand implements OrphanCommand {

    @DefaultFor({"~"})
    @CommandPermission("axplayerwarps.open")
    public void open(@NotNull CommandSender sender, @Optional @AllWarps @CommandPermission("axplayerwarps.use") Warp warp) {
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

    @Subcommand({"create"})
    @CommandPermission("axplayerwarps.create")
    public void create(@NotNull Player sender, String warpName) {
        Create.INSTANCE.execute(sender, warpName);
    }

    @Subcommand({"set", "setlocation", "update"})
    @CommandPermission("axplayerwarps.edit")
    public void setLocation(@NotNull Player sender, @Optional @SelectWarp @OwnWarps Warp warp) {
        DialogSelect.warp(sender, warp, selected -> WarpSettings.INSTANCE.setLocation(sender, selected));
    }

    @Subcommand({"rename", "name"})
    @CommandPermission("axplayerwarps.edit")
    public void rename(@NotNull Player sender, @Optional @SelectWarp @OwnWarps Warp warp, @Optional String newName) {
        DialogSelect.warp(sender, warp, selected -> {
            if (newName != null && !newName.isBlank()) {
                WarpSettings.INSTANCE.rename(sender, selected, newName);
                return;
            }
            InputManager.getInput(sender, "rename", result -> WarpSettings.INSTANCE.rename(sender, selected, result));
        });
    }

    @Subcommand({"desc", "description"})
    @CommandPermission("axplayerwarps.edit")
    public void description(@NotNull Player sender, @Optional DescriptionAction action, @Optional @SelectWarp @OwnWarps Warp warp, @Optional String text) {
        DialogSelect.warp(sender, warp, selected -> {
            if (action != null) {
                setDescription(sender, selected, action, text);
                return;
            }
            DialogSelect.option(sender, "&#33EEBB&lSelect Action", "Action", List.of(DescriptionAction.values()),
                    DialogSelect::prettify, selectedAction -> setDescription(sender, selected, selectedAction, text));
        });
    }

    @Subcommand({"access"})
    @CommandPermission("axplayerwarps.edit")
    public void access(@NotNull Player sender, @Optional @SelectWarp @OwnWarps Warp warp, @Optional Access access) {
        DialogSelect.warp(sender, warp, selected -> {
            if (access != null) {
                WarpSettings.INSTANCE.setAccess(sender, selected, access);
                return;
            }
            List<Access> values = List.of(Access.values());
            DialogSelect.option(sender, "&#33EEBB&lSelect Access", "Access", values,
                    value -> LANG.getString("access." + value.name().toLowerCase()),
                    value -> WarpSettings.INSTANCE.setAccess(sender, selected, value));
        });
    }

    @Subcommand({"price"})
    @CommandPermission("axplayerwarps.edit")
    public void price(@NotNull Player sender, @Optional @SelectWarp @OwnWarps Warp warp, @Optional String price, @Optional String currency) {
        DialogSelect.warp(sender, warp, selected -> {
            if (price == null || price.isBlank()) {
                InputManager.getInput(sender, "price", result -> setPrice(sender, selected, result, currency));
                return;
            }
            setPrice(sender, selected, price, currency);
        });
    }

    @Subcommand({"free"})
    @CommandPermission("axplayerwarps.edit")
    public void free(@NotNull Player sender, @Optional @SelectWarp @OwnWarps Warp warp) {
        DialogSelect.warp(sender, warp, selected -> WarpSettings.INSTANCE.setPrice(sender, selected, 0, null));
    }

    @Subcommand({"transfer"})
    @CommandPermission("axplayerwarps.edit")
    public void transfer(@NotNull Player sender, @Optional @SelectWarp @OwnWarps Warp warp, @Optional Player target) {
        DialogSelect.warp(sender, warp, selected -> {
            if (target != null) {
                WarpSettings.INSTANCE.transfer(sender, selected, target);
                return;
            }
            InputManager.getInput(sender, "transfer", result -> {
                Player player = Bukkit.getPlayerExact(result);
                if (player == null) {
                    MESSAGEUTILS.sendLang(sender, "errors.player-not-online");
                    return;
                }
                WarpSettings.INSTANCE.transfer(sender, selected, player);
            });
        });
    }

    @Subcommand({"whitelist", "wl"})
    @CommandPermission("axplayerwarps.edit")
    public void whitelist(@NotNull Player sender, @Optional ListAction action, @Optional @SelectWarp @OwnWarps Warp warp, @Optional OfflinePlayer target) {
        editList(sender, AccessList.WHITELIST, action, warp, target);
    }

    @Subcommand({"blacklist", "bl"})
    @CommandPermission("axplayerwarps.edit")
    public void blacklist(@NotNull Player sender, @Optional ListAction action, @Optional @SelectWarp @OwnWarps Warp warp, @Optional OfflinePlayer target) {
        editList(sender, AccessList.BLACKLIST, action, warp, target);
    }

    @Subcommand({"bank", "withdraw"})
    @CommandPermission("axplayerwarps.edit")
    public void bank(@NotNull Player sender, @Optional @SelectWarp @OwnWarps Warp warp) {
        DialogSelect.warp(sender, warp, selected -> WarpSettings.INSTANCE.withdraw(sender, selected));
    }

    @Subcommand({"delete"})
    @CommandPermission("axplayerwarps.delete")
    public void delete(@NotNull Player sender, @Optional @SelectWarp @OwnWarps Warp warp) {
        DialogSelect.warp(sender, warp, selected -> Delete.INSTANCE.execute(sender, selected));
    }

    @Subcommand({"edit", "settings"})
    @CommandPermission("axplayerwarps.edit")
    public void edit(@NotNull Player sender, @Optional @SelectWarp @OwnWarps Warp warp) {
        DialogSelect.warp(sender, warp, selected -> Edit.INSTANCE.execute(sender, selected));
    }

    @Subcommand({"info"})
    @CommandPermission("axplayerwarps.info")
    public void info(@NotNull CommandSender sender, @Optional @SelectWarp @AllWarps Warp warp) {
        if (warp != null) {
            Info.INSTANCE.execute(sender, warp);
            return;
        }
        if (!(sender instanceof Player player)) {
            MESSAGEUTILS.sendLang(sender, "errors.player-only");
            return;
        }
        DialogSelect.warp(player, null, selected -> Info.INSTANCE.execute(sender, selected));
    }

    @Subcommand({"icon"})
    @CommandPermission("axplayerwarps.icon")
    public void icon(@NotNull Player sender, @Optional IconAction action, @Optional @SelectWarp @OwnWarps Warp warp) {
        DialogSelect.warp(sender, warp, selected -> {
            if (action != null) {
                Icon.INSTANCE.execute(sender, action, selected);
                return;
            }
            List<IconAction> values = List.of(IconAction.values());
            DialogSelect.option(sender, "&#33EEBB&lSelect Action", "Action", values,
                    DialogSelect::prettify, selectedAction -> Icon.INSTANCE.execute(sender, selectedAction, selected));
        });
    }

    private void setDescription(Player sender, Warp warp, DescriptionAction action, @Nullable String text) {
        if (action == DescriptionAction.ADD && (text == null || text.isBlank())) {
            InputManager.getInput(sender, "add-line", result -> WarpSettings.INSTANCE.setDescription(sender, warp, DescriptionAction.ADD, result));
            return;
        }
        WarpSettings.INSTANCE.setDescription(sender, warp, action, text);
    }

    private void editList(Player sender, AccessList list, @Nullable ListAction action, @Nullable Warp warp, @Nullable OfflinePlayer target) {
        DialogSelect.warp(sender, warp, selected -> {
            if (action != null) {
                editList(sender, list, selected, action, target);
                return;
            }
            List<ListAction> values = List.of(ListAction.values());
            DialogSelect.option(sender, "&#33EEBB&lSelect Action", "Action", values,
                    DialogSelect::prettify, selectedAction -> editList(sender, list, selected, selectedAction, target));
        });
    }

    private void editList(Player sender, AccessList list, Warp warp, ListAction action, @Nullable OfflinePlayer target) {
        if (action == ListAction.CLEAR || target != null) {
            WarpSettings.INSTANCE.editList(sender, warp, list, action, target);
            return;
        }

        InputManager.getInput(sender, "add-player", result ->
                PlayerLookup.find(sender, result, player -> WarpSettings.INSTANCE.editList(sender, warp, list, action, player)));
    }

    private void setPrice(Player sender, Warp warp, String price, @Nullable String currency) {
        try {
            WarpSettings.INSTANCE.setPrice(sender, warp, Double.parseDouble(price.trim()), currency);
        } catch (NumberFormatException ignored) {
            MESSAGEUTILS.sendLang(sender, "errors.not-a-number");
        }
    }
}
