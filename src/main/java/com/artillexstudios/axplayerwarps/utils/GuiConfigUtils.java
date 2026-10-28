package com.artillexstudios.axplayerwarps.utils;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.placeholders.PlaceholderHandler;
import com.artillexstudios.axapi.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class GuiConfigUtils {
    private GuiConfigUtils() {
    }

    /**
     * AxAPI's item builder only accepts the "item-model" key, but users often write
     * "item_model" in the gui files. This applies the alias on every section of the
     * config, so both spellings can be used.
     */
    public static void applyItemModelAlias(Config config) {
        if (config == null) return;
        applyItemModelAlias(config.getBackingDocument());
    }

    private static void applyItemModelAlias(Section section) {
        if (section == null) return;

        for (Object key : new ArrayList<>(section.getKeys())) {
            String name = String.valueOf(key);
            Section child = section.getSection(name);
            if (child != null) {
                applyItemModelAlias(child);
                continue;
            }

            if (!name.equals("item_model")) continue;
            if (!section.contains("item-model")) {
                section.set("item-model", section.get(name));
            }
            section.remove(name);
        }
    }

    /**
     * Applies an item model to a dynamically created item, for example the warp items.
     * Invalid item model keys are ignored to keep the gui working.
     */
    public static void applyItemModel(ItemBuilder builder, @Nullable String itemModel) {
        if (itemModel == null || itemModel.isBlank()) return;

        try {
            builder.itemModel(itemModel);
        } catch (IllegalArgumentException ignored) {
        }
    }

    /**
     * Creates an item builder from a gui section while resolving player head materials
     * such as "PLAYER_HEAD-%player%". The player name is taken from the given headName,
     * so member lists can show the member's head and warp items the owner's head.
     *
     * @return null when the section has no material or the material is invalid
     */
    @Nullable
    public static ConfiguredItem createConfiguredItem(@Nullable Section section, @Nullable String headName, @Nullable Player viewer) {
        if (section == null) return null;

        String configured = section.getString("material");
        if (configured == null) configured = section.getString("type");
        if (configured == null) return null;

        ResolvedMaterial resolved = resolveMaterial(configured, headName, viewer);
        if (resolved == null) return null;

        Map<String, Object> map = new HashMap<>();
        for (Object key : section.getKeys()) {
            String name = String.valueOf(key);
            map.put(name, section.get(name));
        }
        map.put("material", resolved.material().name());
        map.remove("type");

        return new ConfiguredItem(ItemBuilder.create(map), resolved.headName());
    }

    /**
     * Sets the skull owner of a player head, both for online and offline players.
     */
    public static void applyHeadOwner(@Nullable ItemStack item, @Nullable String headName) {
        if (item == null || headName == null || headName.isBlank()) return;
        if (item.getType() != Material.PLAYER_HEAD) return;

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta)) return;

        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(headName));
        item.setItemMeta(skullMeta);
    }

    @Nullable
    private static ResolvedMaterial resolveMaterial(String configured, @Nullable String headName, @Nullable Player viewer) {
        String value = configured.trim();

        if (headName != null && !headName.isBlank()) {
            value = value.replace("%player%", headName).replace("%owner%", headName);
        }

        if (viewer != null) {
            value = PlaceholderHandler.parse(value, viewer);
            value = PlaceholderUtils.parse(viewer, value);
        }

        int separator = value.indexOf('-');
        if (separator > 0) {
            String materialName = value.substring(0, separator).trim();
            String playerName = value.substring(separator + 1).trim();

            if (materialName.equalsIgnoreCase("PLAYER_HEAD") && !playerName.isEmpty() && !playerName.contains("%")) {
                return new ResolvedMaterial(Material.PLAYER_HEAD, playerName);
            }
        }

        Material material = Material.matchMaterial(value);
        return material == null ? null : new ResolvedMaterial(material, null);
    }

    public record ConfiguredItem(ItemBuilder builder, @Nullable String headName) {
    }

    private record ResolvedMaterial(Material material, @Nullable String headName) {
    }
}
