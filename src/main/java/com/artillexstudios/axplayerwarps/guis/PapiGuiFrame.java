package com.artillexstudios.axplayerwarps.guis;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axguiframework.GuiFrame;
import com.artillexstudios.axguiframework.libs.gui.guis.BaseGui;
import com.artillexstudios.axplayerwarps.utils.PlaceholderUtils;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class PapiGuiFrame<T extends BaseGui> extends GuiFrame<T> {

    protected PapiGuiFrame(Config config, Player player) {
        super(config, player);
    }

    protected PapiGuiFrame(int updateTicks, Config config, Player player) {
        super(updateTicks, config, player);
    }

    protected PapiGuiFrame(Section section, Player player) {
        super(section, player);
    }

    protected PapiGuiFrame(int updateTicks, Section section, Player player) {
        super(updateTicks, section, player);
    }

    @Override
    public String parseText(String text) {
        return PlaceholderUtils.parse(player, super.parseText(text));
    }

    @Override
    public List<String> parseText(List<String> text) {
        return PlaceholderUtils.parse(player, super.parseText(text));
    }

    @Override
    public String[] parseText(String... text) {
        String[] parsed = super.parseText(text);
        for (int i = 0; i < parsed.length; i++) {
            parsed[i] = PlaceholderUtils.parse(player, parsed[i]);
        }
        return parsed;
    }

    @Override
    public String parseText(String text, Object... parameters) {
        return PlaceholderUtils.parse(player, super.parseText(text, parameters));
    }

    @Override
    public List<String> parseText(List<String> text, Object... parameters) {
        return PlaceholderUtils.parse(player, super.parseText(text, parameters));
    }
}
