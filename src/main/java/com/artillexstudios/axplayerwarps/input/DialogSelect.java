package com.artillexstudios.axplayerwarps.input;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.INPUT;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.LANG;

/**
 * Small Paper Dialog based selectors, used to pick a warp or a command option
 * without having to type it in the command.
 */
public final class DialogSelect {
    private static final String KEY = "value";

    private DialogSelect() {
    }

    public static void warp(Player player, @Nullable Warp selected, Consumer<Warp> callback) {
        warp(player, selected, WarpManager.getWarps(player), callback);
    }

    public static void warp(Player player, @Nullable Warp selected, Collection<Warp> warps, Consumer<Warp> callback) {
        if (selected != null) {
            callback.accept(selected);
            return;
        }

        List<Warp> options = new ArrayList<>(warps);
        if (options.isEmpty()) {
            player.sendMessage(StringUtils.formatToString(LANG.getString("errors.no-warps")));
            return;
        }

        options.sort(Comparator.comparing(Warp::getName, String.CASE_INSENSITIVE_ORDER));
        Section section = INPUT.getSection("select.warp");
        String entryFormat = section == null ? "%warp%" : section.getString("entry", "%warp%");
        List<String> labels = options.stream()
                .map(warp -> entryFormat.replace("%warp%", warp.getName()))
                .toList();

        select(player, section, labels, index -> callback.accept(options.get(index)));
    }

    public static <T> void option(Player player, String route, List<T> values, Function<T, String> displayName, Consumer<T> callback) {
        if (values.isEmpty()) return;

        Section section = INPUT.getSection("select." + route);
        if (section == null) section = INPUT.getSection("select.action");

        List<String> labels = values.stream().map(displayName).toList();
        select(player, section, labels, index -> callback.accept(values.get(index)));
    }

    public static String prettify(Enum<?> value) {
        String name = value.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        return "&f" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static void select(Player player, @Nullable Section section, List<String> labels, Consumer<Integer> callback) {
        List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>(labels.size());
        for (int i = 0; i < labels.size(); i++) {
            entries.add(SingleOptionDialogInput.OptionEntry.create(
                    String.valueOf(i),
                    StringUtils.format(labels.get(i)),
                    i == 0
            ));
        }

        String title = section == null ? "" : section.getString("title", "");
        String label = section == null ? "" : section.getString("label", "");
        List<DialogBody> body = new ArrayList<>();
        if (section != null) {
            for (String line : section.getStringList("body")) {
                body.add(DialogBody.plainMessage(StringUtils.format(line)));
            }
        }

        DialogInput input = DialogInput.singleOption(KEY, StringUtils.format(label), entries)
                .width(250)
                .build();

        Section buttons = INPUT.getSection("select.buttons");
        String confirmLabel = buttons == null ? "" : buttons.getString("confirm.label", "");
        String confirmTooltip = buttons == null ? "" : buttons.getString("confirm.tooltip", "");
        ActionButton confirm = ActionButton.create(
                StringUtils.format(confirmLabel),
                confirmTooltip.isBlank() ? Component.empty() : StringUtils.format(confirmTooltip),
                100,
                DialogAction.customClick((view, audience) -> {
                    if (!(audience instanceof Player player1)) return;
                    Integer index = readIndex(view);
                    if (index == null || index < 0 || index >= labels.size()) return;
                    Scheduler.get().run(player1, task -> callback.accept(index), () -> {});
                }, ClickCallback.Options.builder().uses(1).build())
        );

        String cancelLabel = buttons == null ? "" : buttons.getString("cancel.label", "");
        String cancelTooltip = buttons == null ? "" : buttons.getString("cancel.tooltip", "");
        ActionButton cancel = ActionButton.create(
                StringUtils.format(cancelLabel),
                cancelTooltip.isBlank() ? Component.empty() : StringUtils.format(cancelTooltip),
                100,
                DialogAction.customClick((view, audience) -> {}, ClickCallback.Options.builder().uses(1).build())
        );

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(StringUtils.format(title))
                        .body(body)
                        .inputs(List.of(input))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(confirm, cancel)));

        player.showDialog(dialog);
    }

    @Nullable
    private static Integer readIndex(DialogResponseView view) {
        String value = view.getText(KEY);
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
