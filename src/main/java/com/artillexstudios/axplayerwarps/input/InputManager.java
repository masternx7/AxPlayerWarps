package com.artillexstudios.axplayerwarps.input;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axplayerwarps.utils.PlaceholderUtils;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.INPUT;

public class InputManager {

    public static void getInput(Player player, String route, Consumer<String> consumer) {
        Section section = INPUT.getSection(route);
        if (section == null) {
            consumer.accept("");
            return;
        }

        String inputType = section.getString("input.type", "text").toLowerCase(Locale.ENGLISH);
        String key = section.getString("input.key", "value");
        String title = format(player, section.getString("title", route));
        List<DialogBody> body = new ArrayList<>();
        for (String line : section.getStringList("body")) {
            body.add(DialogBody.plainMessage(formatComponent(player, line)));
        }

        DialogInput input = createInput(player, section.getSection("input"), inputType, key);
        ActionButton confirm = createButton(player, section.getSection("confirm"), "Confirm", (view, audience) -> {
            if (!(audience instanceof Player player1)) return;
            String result = readValue(view, inputType, key);
            Scheduler.get().run(player1, task -> consumer.accept(result), () -> {});
        });
        ActionButton cancel = createButton(player, section.getSection("cancel"), "Cancel", (view, audience) -> {
            if (!(audience instanceof Player player1)) return;
            Scheduler.get().run(player1, task -> consumer.accept(""), () -> {});
        });

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(StringUtils.format(title))
                        .body(body)
                        .inputs(List.of(input))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(confirm, cancel)));

        player.showDialog(dialog);
    }

    private static DialogInput createInput(Player player, @Nullable Section section, String type, String key) {
        if (section == null) {
            return DialogInput.text(key, StringUtils.format("Value")).build();
        }

        Component label = formatComponent(player, section.getString("label", "Value"));
        switch (type) {
            case "number", "number-range" -> {
                float min = section.getDouble("min", 0.0).floatValue();
                float max = section.getDouble("max", 100.0).floatValue();
                var builder = DialogInput.numberRange(key, label, min, max);
                if (section.contains("initial")) builder.initial(section.getDouble("initial").floatValue());
                if (section.contains("step")) builder.step(section.getDouble("step", 1.0).floatValue());
                if (section.contains("label-format")) builder.labelFormat(section.getString("label-format"));
                if (section.contains("width")) builder.width(section.getInt("width", 200));
                return builder.build();
            }
            case "bool", "boolean" -> {
                var builder = DialogInput.bool(key, label);
                if (section.contains("initial")) builder.initial(section.getBoolean("initial", false));
                if (section.contains("on-true")) builder.onTrue(section.getString("on-true"));
                if (section.contains("on-false")) builder.onFalse(section.getString("on-false"));
                return builder.build();
            }
            case "single", "single-option" -> {
                List<SingleOptionDialogInput.OptionEntry> options = new ArrayList<>();
                String initial = section.getString("initial", "");
                Section optionsSection = section.getSection("options");
                if (optionsSection != null) {
                    for (String id : optionsSection.getRoutesAsStrings(false)) {
                        options.add(SingleOptionDialogInput.OptionEntry.create(
                                id,
                                formatComponent(player, optionsSection.getString(id)),
                                id.equalsIgnoreCase(initial)
                        ));
                    }
                }
                var builder = DialogInput.singleOption(key, label, options);
                if (section.contains("width")) builder.width(section.getInt("width", 200));
                return builder.build();
            }
            default -> {
                var builder = DialogInput.text(key, label);
                builder.maxLength(section.getInt("max-length", 256));
                if (section.contains("initial")) builder.initial(section.getString("initial"));
                if (section.contains("width")) builder.width(section.getInt("width", 200));
                return builder.build();
            }
        }
    }

    private static ActionButton createButton(Player player, @Nullable Section section, String defaultLabel, DialogActionCallback callback) {
        String label = section == null ? defaultLabel : section.getString("label", defaultLabel);
        String tooltip = section == null ? "" : section.getString("tooltip", "");
        return ActionButton.create(
                formatComponent(player, label),
                tooltip.isBlank() ? Component.empty() : formatComponent(player, tooltip),
                100,
                DialogAction.customClick(callback, ClickCallback.Options.builder().uses(1).build())
        );
    }

    private static String readValue(DialogResponseView view, String type, String key) {
        return switch (type) {
            case "number", "number-range" -> {
                Float value = view.getFloat(key);
                if (value == null) yield "";
                yield value == Math.floor(value) ? String.valueOf(value.intValue()) : String.valueOf(value);
            }
            case "bool", "boolean" -> {
                Boolean value = view.getBoolean(key);
                yield value == null ? "" : value.toString();
            }
            default -> {
                String value = view.getText(key);
                yield value == null ? "" : value;
            }
        };
    }

    private static String format(Player player, String text) {
        return PlaceholderUtils.parse(player, text);
    }

    private static Component formatComponent(Player player, String text) {
        return StringUtils.format(format(player, text));
    }
}
