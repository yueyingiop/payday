package com.yueyingiop.payday.api.helper;

import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.Consumer;

/** 配置界面中的输入控件、数值解析和布局辅助方法。 */
public final class ConfigScreenHelper {
    private ConfigScreenHelper() {
    }

    public enum FieldType {
        INTEGER,
        DOUBLE,
        TEXT
    }

    public record Field(EditBox box, String label, Consumer<?> setter, FieldType type, int baseY) {
    }

    public record Toggle(Checkbox checkbox, Consumer<Boolean> setter, int baseY) {
    }

    public record ActionButton(net.minecraft.client.gui.components.Button button, int baseY) {
    }

    public static int fieldWidth(int screenWidth, int preferredWidth) {
        return Math.min(preferredWidth, Math.max(120, screenWidth - 32));
    }

    public static void applyFilter(EditBox box, FieldType type) {
        if (type == FieldType.INTEGER) {
            box.setFilter(text -> text.matches("-?\\d*"));
        } else if (type == FieldType.DOUBLE) {
            box.setFilter(text -> text.matches("-?\\d*(\\.\\d*)?"));
        }
    }

    public static void applyFields(List<Field> fields, List<Toggle> toggles) {
        for (Field field : fields) {
            try {
                if (field.type() == FieldType.INTEGER) {
                    @SuppressWarnings("unchecked") Consumer<Integer> setter = (Consumer<Integer>) field.setter();
                    setter.accept(Integer.parseInt(field.box().getValue()));
                } else if (field.type() == FieldType.DOUBLE) {
                    @SuppressWarnings("unchecked") Consumer<Double> setter = (Consumer<Double>) field.setter();
                    setter.accept(Double.parseDouble(field.box().getValue()));
                } else {
                    @SuppressWarnings("unchecked") Consumer<String> setter = (Consumer<String>) field.setter();
                    setter.accept(field.box().getValue());
                }
            } catch (RuntimeException ignored) {
            }
        }
        toggles.forEach(toggle -> toggle.setter().accept(toggle.checkbox().selected()));
    }

    public static int maxScroll(int contentHeight, int screenHeight, int bottomMargin) {
        return Math.max(0, contentHeight - (screenHeight - bottomMargin));
    }

    public static int clampScroll(int scroll, int contentHeight, int screenHeight,
                                   int bottomMargin, double delta) {
        return Mth.clamp(scroll - (int) Math.round(delta * 18.0D),
                0, maxScroll(contentHeight, screenHeight, bottomMargin));
    }

    public static boolean isVisible(int top, int widgetHeight, int contentTop, int contentBottom) {
        return top + widgetHeight >= contentTop && top < contentBottom;
    }
}
