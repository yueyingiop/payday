package com.yueyingiop.payday.client.screen;

import com.yueyingiop.payday.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/** Paginated in-game editor matching Config's ArmorCurio, ArmorSolt, Other and HUD groups. */
public final class PayDayConfigScreen extends Screen {
    private static final int FIELD_WIDTH = 260;
    private static final int ROW_HEIGHT = 42;
    private static final int CONTENT_TOP = 42;
    private static final int CONTENT_BOTTOM_MARGIN = 36;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int BUTTON_Y_OFFSET = 28;

    private final Screen parent;
    private final List<Field> fields = new ArrayList<>();
    private final List<Toggle> toggles = new ArrayList<>();
    private Page page = Page.ARMOR_CURIO;
    private Button previousButton;
    private Button nextButton;
    private int scroll;
    private int contentHeight;

    public PayDayConfigScreen(Screen parent) {
        super(Component.translatable("config.payday.title"));
        this.parent = parent;
    }

    /**
     * 初始化界面或插件。
     */
    @Override
    protected void init() {
        previousButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(width / 2 - 108, 12, 20, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(width / 2 + 88, 12, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
                .bounds(width / 2 - 105, height - BUTTON_Y_OFFSET, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> closeWithoutSaving())
                .bounds(width / 2 + 5, height - BUTTON_Y_OFFSET, 100, 20).build());
        buildPage();
    }

    /**
     * 切换配置分页并重建控件。
     * @param direction 方向。
     */
    private void changePage(int direction) {
        applyCurrentPage();
        page = Page.values()[Math.floorMod(page.ordinal() + direction, Page.values().length)];
        scroll = 0;
        buildPage();
    }

    /**
     * 创建当前分页的配置控件。
     */
    private void buildPage() {
        fields.forEach(field -> removeWidget(field.box()));
        toggles.forEach(toggle -> removeWidget(toggle.checkbox()));
        fields.clear();
        toggles.clear();
        int x = Math.max(8, (width - Math.min(FIELD_WIDTH, width - 32)) / 2);
        int[] y = {CONTENT_TOP};
        switch (page) {
            case ARMOR_CURIO -> {
                addInt("西装升级槽", x, y, Config.SUIT_UPGRADES::set, Config.SUIT_UPGRADES.get());
                addInt("轻型背心升级槽", x, y, Config.LIGHT_UPGRADES::set, Config.LIGHT_UPGRADES.get());
                addInt("重型背心升级槽", x, y, Config.HEAVY_UPGRADES::set, Config.HEAVY_UPGRADES.get());
                addInt("防爆弹衣升级槽", x, y, Config.BLAST_UPGRADES::set, Config.BLAST_UPGRADES.get());
                addInt("复合战术背心升级槽", x, y, Config.TACTICAL_UPGRADES::set, Config.TACTICAL_UPGRADES.get());
                addInt("铁人升级槽", x, y, Config.IRON_MAN_UPGRADES::set, Config.IRON_MAN_UPGRADES.get());
            }
            case ARMOR_SLOT -> {
                addDouble("冲击护甲值", x, y, Config.IMPACT_ARMOR::set, Config.IMPACT_ARMOR.get());
                addDouble("自适应护甲值", x, y, Config.ADAPTIVE_ARMOR::set, Config.ADAPTIVE_ARMOR.get());
                addDouble("抗性护甲值", x, y, Config.RESISTANCE_ARMOR::set, Config.RESISTANCE_ARMOR.get());
                addDouble("冲击恢复秒数", x, y, Config.IMPACT_RECOVERY::set, Config.IMPACT_RECOVERY.get());
                addDouble("自适应恢复秒数", x, y, Config.ADAPTIVE_RECOVERY::set, Config.ADAPTIVE_RECOVERY.get());
                addDouble("抗性恢复秒数", x, y, Config.RESISTANCE_RECOVERY::set, Config.RESISTANCE_RECOVERY.get());
            }
            case OTHER -> {
                addDouble("护甲饰品更换冷却秒数", x, y, Config.ARMOR_CHANGE_COOLDOWN_SECONDS::set, Config.ARMOR_CHANGE_COOLDOWN_SECONDS.get());
                addDouble("装备箱更换冷却秒数", x, y, Config.EQUIPMENT_BOX_CHANGE_COOLDOWN_SECONDS::set, Config.EQUIPMENT_BOX_CHANGE_COOLDOWN_SECONDS.get());
                addDouble("衣物箱更换冷却秒数", x, y, Config.CLOTHES_BOX_CHANGE_COOLDOWN_SECONDS::set, Config.CLOTHES_BOX_CHANGE_COOLDOWN_SECONDS.get());
                addDouble("装备箱使用冷却秒数", x, y, Config.EQUIPMENT_BOX_USE_COOLDOWN_SECONDS::set, Config.EQUIPMENT_BOX_USE_COOLDOWN_SECONDS.get());
                addInt("暴露风险最小值", x, y, Config.EXPOSURE_RISK_MIN::set, Config.EXPOSURE_RISK_MIN.get());
                addInt("暴露风险最大值", x, y, Config.EXPOSURE_RISK_MAX::set, Config.EXPOSURE_RISK_MAX.get());
                addInt("暴露风险默认值", x, y, Config.EXPOSURE_RISK_DEFAULT::set, Config.EXPOSURE_RISK_DEFAULT.get());
                addText("特殊物品风险 (ID或#标签=值,逗号分隔)", x, y, this::setSpecialItemRisks, String.join(", ", Config.EXPOSURE_SPECIAL_ITEM_RISKS.get()));
            }
            case HUD -> {
                addToggle("开启精简 HUD", x, y, Config.COMPACT_HUD::set, Config.COMPACT_HUD.get());
                addToggle("开启装备箱 HUD", x, y, Config.EQUIPMENT_BOX_HUD_ENABLED::set, Config.EQUIPMENT_BOX_HUD_ENABLED.get());
                addToggle("开启暴露风险 HUD", x, y, Config.EXPOSURE_RISK_HUD_ENABLED::set, Config.EXPOSURE_RISK_HUD_ENABLED.get());
            }
        }
        previousButton.active = page.ordinal() > 0;
        nextButton.active = page.ordinal() < Page.values().length - 1;
        contentHeight = y[0];
        repositionPageContents();
    }

    /**
     * 添加整数配置输入框。
     * @param label 标签。
     * @param x 横坐标。
     * @param y 纵坐标。
     * @param setter 配置写入器。
     * @param value 数值。
     */
    private void addInt(String label, int x, int[] y, Consumer<Integer> setter, int value) {
        addField(label, x, y, setter, Integer.toString(value), FieldType.INTEGER);
    }

    /**
     * 添加小数配置输入框。
     * @param label 标签。
     * @param x 横坐标。
     * @param y 纵坐标。
     * @param setter 配置写入器。
     * @param value 数值。
     */
    private void addDouble(String label, int x, int[] y, Consumer<Double> setter, double value) {
        addField(label, x, y, setter, Double.toString(value), FieldType.DOUBLE);
    }

    /**
     * 添加文本配置输入框。
     * @param label 标签。
     * @param x 横坐标。
     * @param y 纵坐标。
     * @param setter 配置写入器。
     * @param value 数值。
     */
    private void addText(String label, int x, int[] y, Consumer<String> setter, String value) {
        addField(label, x, y, setter, value, FieldType.TEXT);
    }

    /**
     * 创建并登记配置输入框。
     * @param label 标签。
     * @param x 横坐标。
     * @param y 纵坐标。
     * @param setter 配置写入器。
     * @param value 数值。
     * @param type 类型。
     */
    private <T> void addField(String label, int x, int[] y, Consumer<T> setter, String value, FieldType type) {
        int fieldWidth = Math.min(FIELD_WIDTH, Math.max(120, width - 32));
        EditBox box = new EditBox(font, x, y[0] + 14, fieldWidth, 20, Component.literal(label));
        box.setValue(value);
        if (type == FieldType.INTEGER) box.setFilter(text -> text.matches("-?\\d*"));
        if (type == FieldType.DOUBLE) box.setFilter(text -> text.matches("-?\\d*(\\.\\d*)?"));
        fields.add(new Field(box, label, setter, type, y[0]));
        addRenderableWidget(box);
        y[0] += ROW_HEIGHT;
    }

    /**
     * 添加布尔值配置开关。
     * @param label 标签。
     * @param x 横坐标。
     * @param y 纵坐标。
     * @param setter 配置写入器。
     * @param value 数值。
     */
    private void addToggle(String label, int x, int[] y, Consumer<Boolean> setter, boolean value) {
        Checkbox checkbox = new Checkbox(x, y[0], 20, 20, Component.literal(label), value);
        toggles.add(new Toggle(checkbox, setter, y[0]));
        addRenderableWidget(checkbox);
        y[0] += ROW_HEIGHT;
    }

    /**
     * 将当前分页的控件值写入配置。
     */
    private void applyCurrentPage() {
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

    /**
     * 解析并保存特殊物品风险列表。
     * @param value 数值。
     */
    private void setSpecialItemRisks(String value) {
        List<String> entries = Arrays.stream(value.split(",")).map(String::trim)
                .filter(entry -> !entry.isEmpty()).toList();
        Config.EXPOSURE_SPECIAL_ITEM_RISKS.set(entries);
    }

    /**
     * 保存配置并关闭界面。
     */
    private void saveAndClose() {
        applyCurrentPage();
        Config.SPEC.save();
        Config.CLIENT_SPEC.save();
        closeWithoutSaving();
    }

    /**
     * 计算当前分页可滚动的最大距离。
     * @return 对应的执行结果。
     */
    private int maxScroll() {
        return Math.max(0, contentHeight - (height - CONTENT_BOTTOM_MARGIN));
    }

    /**
     * 根据滚动偏移重新定位分页控件。
     */
    private void repositionPageContents() {
        int bottom = height - CONTENT_BOTTOM_MARGIN;
        for (Field field : fields) {
            int top = field.baseY() - scroll;
            field.box().setY(top + 14);
            field.box().visible = top + 14 + field.box().getHeight() >= CONTENT_TOP && top < bottom;
        }
        for (Toggle toggle : toggles) {
            int top = toggle.baseY() - scroll;
            toggle.checkbox().setY(top);
            toggle.checkbox().visible = top + toggle.checkbox().getHeight() >= CONTENT_TOP && top < bottom;
        }
    }

    /**
     * 根据滚轮输入更新分页滚动位置。
     * @param mouseX 鼠标横坐标。
     * @param mouseY 鼠标纵坐标。
     * @param delta 滚动增量。
     * @return 对应的执行结果。
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= CONTENT_TOP && mouseY < height - CONTENT_BOTTOM_MARGIN) {
            scroll = Mth.clamp(scroll - (int) Math.round(delta * 18.0D), 0, maxScroll());
            repositionPageContents();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /**
     * 绘制分页滚动条。
     * @param graphics 图形渲染上下文。
     */
    private void renderScrollbar(GuiGraphics graphics) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return;
        }
        int top = CONTENT_TOP;
        int bottom = height - CONTENT_BOTTOM_MARGIN;
        int trackHeight = bottom - top;
        int thumbHeight = Math.max(12, trackHeight * trackHeight / contentHeight);
        int thumbTop = top + (int) ((long) (trackHeight - thumbHeight) * scroll / maxScroll);
        int x = width - SCROLLBAR_WIDTH - 4;
        graphics.fill(x, top, x + SCROLLBAR_WIDTH, bottom, 0x553F3F3F);
        graphics.fill(x, thumbTop, x + SCROLLBAR_WIDTH, thumbTop + thumbHeight, 0xFFBFBFBF);
    }

    /**
     * 关闭界面且不保存修改。
     */
    private void closeWithoutSaving() {
        minecraft.setScreen(parent);
    }

    /**
     * 绘制对应内容。
     * @param graphics 图形渲染上下文。
     * @param mouseX 鼠标横坐标。
     * @param mouseY 鼠标纵坐标。
     * @param partialTick 帧间插值。
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal(page.title), width / 2, 18, 0xD0D0D0);
        graphics.enableScissor(0, CONTENT_TOP, width, height - CONTENT_BOTTOM_MARGIN);
        for (Field field : fields) {
            if (field.box().visible) {
                graphics.drawString(font, field.label(), field.box().getX(), field.box().getY() - 14, 0xFFFFFF, false);
            }
        }
        renderScrollbar(graphics);
        graphics.disableScissor();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private record Field(EditBox box, String label, Consumer<?> setter, FieldType type, int baseY) { }
    private record Toggle(Checkbox checkbox, Consumer<Boolean> setter, int baseY) { }
    private enum FieldType { INTEGER, DOUBLE, TEXT }
    private enum Page {
        ARMOR_CURIO("ArmorCurio"), ARMOR_SLOT("ArmorSolt"), OTHER("Other"), HUD("HUD");
        private final String title;
        Page(String title) { this.title = title; }
    }
}
