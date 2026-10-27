package com.artillexstudios.axplayerwarps.commands.subcommands;

import com.artillexstudios.axintegrations.types.CurrencyIntegration;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.enums.Access;
import com.artillexstudios.axplayerwarps.enums.AccessList;
import com.artillexstudios.axplayerwarps.enums.DescriptionAction;
import com.artillexstudios.axplayerwarps.enums.ListAction;
import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.user.WarpUser;
import com.artillexstudios.axplayerwarps.utils.FormatUtils;
import com.artillexstudios.axplayerwarps.utils.WarpNameUtils;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.LANG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public enum WarpSettings {
    INSTANCE;

    public void setLocation(Player sender, Warp warp) {
        if (!canEdit(sender, warp)) return;
        warp.setLocation(sender.getLocation());
        update(sender, warp, "editor.update-location");
    }

    public void rename(Player sender, Warp warp, String newName) {
        if (!canEdit(sender, warp)) return;
        if (newName.isBlank()) {
            MESSAGEUTILS.sendLang(sender, "errors.invalid-name");
            return;
        }

        switch (WarpNameUtils.isAllowed(newName)) {
            case DISALLOWED -> {
                MESSAGEUTILS.sendLang(sender, "errors.disallowed-name-blacklisted");
                return;
            }
            case CONTAINS_SPACES -> {
                MESSAGEUTILS.sendLang(sender, "errors.disallowed-name-space");
                return;
            }
            case INVALID_LENGTH -> {
                MESSAGEUTILS.sendLang(sender, "errors.disallowed-name-length");
                return;
            }
        }

        if (warp.getName().equalsIgnoreCase(newName)) {
            MESSAGEUTILS.sendLang(sender, "editor.update-name");
            return;
        }

        String name = newName.replace(" ", "_");
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            if (!warp.setName(name)) {
                MESSAGEUTILS.sendLang(sender, "errors.name-exists");
            } else {
                AxPlayerWarps.getDatabase().updateWarp(warp);
                MESSAGEUTILS.sendLang(sender, "editor.update-name");
            }
        });
    }

    public void setDescription(Player sender, Warp warp, DescriptionAction action, @Nullable String text) {
        if (!canEdit(sender, warp)) return;

        List<String> description = new ArrayList<>();
        String realDescription = warp.getRealDescription();
        if (realDescription != null && !realDescription.isBlank()) {
            description.addAll(Arrays.asList(realDescription.split("\n")));
        }

        switch (action) {
            case ADD -> {
                if (text == null || text.isBlank()) {
                    MESSAGEUTILS.sendLang(sender, "errors.missing-text");
                    return;
                }
                int maxLines = CONFIG.getInt("warp-description.max-lines", 3);
                if (description.size() >= maxLines) {
                    MESSAGEUTILS.sendLang(sender, "errors.max-lines");
                    return;
                }
                int maxLength = CONFIG.getInt("warp-description.max-line-length", 50);
                if (text.length() > maxLength) {
                    MESSAGEUTILS.sendLang(sender, "errors.max-length", Map.of("%length%", String.valueOf(maxLength)));
                    return;
                }
                description.add(text);
            }
            case REMOVE -> {
                if (description.isEmpty()) {
                    MESSAGEUTILS.sendLang(sender, "errors.no-description");
                    return;
                }
                description.removeLast();
            }
            case CLEAR -> description.clear();
        }

        warp.setDescription(description);
        update(sender, warp, "editor.update-description");
    }

    public void setAccess(Player sender, Warp warp, Access access) {
        if (!canEdit(sender, warp)) return;
        warp.setAccess(access);
        update(sender, warp, "editor.update-access", Map.of(
                "%access%", LANG.getString("access." + access.name().toLowerCase())
        ));
    }

    public void setPrice(Player sender, Warp warp, double price, @Nullable String currency) {
        if (!canEdit(sender, warp)) return;
        if (price < 0) {
            MESSAGEUTILS.sendLang(sender, "errors.must-be-positive");
            return;
        }

        CurrencyIntegration integration = null;
        if (price > 0) {
            if (currency != null) {
                integration = CurrencyIntegration.one(currency);
                if (integration == null) {
                    MESSAGEUTILS.sendLang(sender, "errors.currency-not-found");
                    return;
                }
            } else {
                integration = warp.getCurrencyIntegration();
                if (integration == null) {
                    integration = CurrencyIntegration.list().stream().findFirst().orElse(null);
                }
                if (integration == null) {
                    MESSAGEUTILS.sendLang(sender, "errors.currency-not-found");
                    return;
                }
            }
        }

        if (warp.getEarnedMoney() > 0) {
            warp.withdrawMoney();
        }

        warp.setTeleportPrice(price);
        warp.setCurrency(integration);

        String displayPrice = price <= 0
                ? LANG.getString("placeholders.free")
                : FormatUtils.formatCurrency(integration, price);
        update(sender, warp, "editor.update-price", Map.of("%price%", displayPrice));
    }

    public void transfer(Player sender, Warp warp, Player target) {
        if (!canEdit(sender, warp)) return;
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            MESSAGEUTILS.sendLang(sender, "errors.transfer-self");
            return;
        }

        WarpUser targetUser = Users.get(target);
        long limit = targetUser.getWarpLimit();
        long warps = WarpManager.getWarps(target).size();
        if (limit <= warps) {
            MESSAGEUTILS.sendLang(sender, "errors.transfer-failed", Map.of(
                    "%player%", target.getName(),
                    "%warp%", warp.getName()
            ));
            return;
        }

        warp.setOwner(target.getUniqueId());
        MESSAGEUTILS.sendLang(target, "editor.new-owner", Map.of(
                "%player%", sender.getName(),
                "%warp%", warp.getName()
        ));
        MESSAGEUTILS.sendLang(sender, "editor.transferred", Map.of(
                "%player%", target.getName(),
                "%warp%", warp.getName()
        ));

        AxPlayerWarps.getThreadedQueue().submit(() -> {
            AxPlayerWarps.getDatabase().updateWarp(warp);
            AxPlayerWarps.getDatabase().removeFromList(warp, AccessList.WHITELIST, target);
            AxPlayerWarps.getDatabase().removeFromList(warp, AccessList.BLACKLIST, target);
        });
    }

    public void editList(Player sender, Warp warp, AccessList list, ListAction action, @Nullable OfflinePlayer target) {
        if (!canEdit(sender, warp)) return;

        String route = list.name().toLowerCase();
        if (action == ListAction.CLEAR) {
            AxPlayerWarps.getThreadedQueue().submit(() -> {
                AxPlayerWarps.getDatabase().clearList(warp, list);
                MESSAGEUTILS.sendLang(sender, route + ".clear");
            });
            return;
        }

        if (target == null) {
            MESSAGEUTILS.sendLang(sender, "errors.player-not-found");
            return;
        }

        if (target.getUniqueId().equals(sender.getUniqueId())) {
            MESSAGEUTILS.sendLang(sender, "errors." + route + "-self");
            return;
        }

        String targetName = target.getName() == null ? target.getUniqueId().toString() : target.getName();
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            if (action == ListAction.ADD) {
                AxPlayerWarps.getDatabase().addToList(warp, list, target);
                MESSAGEUTILS.sendLang(sender, route + ".add", Map.of("%player%", targetName));
            } else {
                AxPlayerWarps.getDatabase().removeFromList(warp, list, target);
                MESSAGEUTILS.sendLang(sender, route + ".remove", Map.of("%player%", targetName));
            }
        });
    }

    public void withdraw(Player sender, Warp warp) {
        if (!canEdit(sender, warp)) return;
        warp.withdrawMoney();
    }

    private boolean canEdit(Player sender, Warp warp) {
        if (warp.getOwner().equals(sender.getUniqueId())) return true;
        if (sender.hasPermission("axplayerwarps.admin.bypass")) return true;
        MESSAGEUTILS.sendLang(sender, "errors.not-your-warp");
        return false;
    }

    private void update(Player sender, Warp warp, String message) {
        update(sender, warp, message, Map.of());
    }

    private void update(Player sender, Warp warp, String message, Map<String, String> placeholders) {
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            AxPlayerWarps.getDatabase().updateWarp(warp);
            MESSAGEUTILS.sendLang(sender, message, placeholders);
        });
    }
}
