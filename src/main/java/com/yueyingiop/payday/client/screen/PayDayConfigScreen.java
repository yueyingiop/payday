package com.yueyingiop.payday.client.screen;

import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.api.helper.ConfigScreenHelper;
import com.yueyingiop.payday.api.helper.ConfigScreenHelper.ActionButton;
import com.yueyingiop.payday.api.helper.ConfigScreenHelper.Field;
import com.yueyingiop.payday.api.helper.ConfigScreenHelper.FieldType;
import com.yueyingiop.payday.api.helper.ConfigScreenHelper.Toggle;
import com.yueyingiop.payday.client.base.StringListEditScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
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
    private final List<ActionButton> actionButtons = new ArrayList<>();
    private Page page = Page.ARMOR_CURIO;
    private Button previousButton;
    private Button nextButton;
    private Button specialItemRisksButton;
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
        actionButtons.forEach(action -> removeWidget(action.button()));
        fields.clear();
        toggles.clear();
        actionButtons.clear();
        specialItemRisksButton = null;
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
                specialItemRisksButton = addActionButton(
                        "编辑特殊物品风险 (" + Config.EXPOSURE_SPECIAL_ITEM_RISKS.get().size() + "项)",
                        x, y, () -> minecraft.setScreen(new StringListEditScreen(
                                this,
                                Component.literal("特殊物品风险"),
                                Component.literal("每行填写一个物品 ID 或 #标签=风险值"),
                                Config.EXPOSURE_SPECIAL_ITEM_RISKS.get(),
                                values -> {
                                    Config.EXPOSURE_SPECIAL_ITEM_RISKS.set(values);
                                    refreshSpecialItemRisksButton();
                                })));
                addActionButton(
                        "编辑属性 HUD 图标白名单 (" + Config.PROBABILITY_HUD_ATTRIBUTE_ICONS.get().size() + "项)",
                        x, y, () -> minecraft.setScreen(new StringListEditScreen(
                                this,
                                Component.literal("属性 HUD 图标白名单"),
                                Component.literal("格式：属性ID.-图标资源路径；内置图标表：payday:gui/cd_attribute"),
                                Config.PROBABILITY_HUD_ATTRIBUTE_ICONS.get(),
                                Config.PROBABILITY_HUD_ATTRIBUTE_ICONS::set)));
            }
            case HUD -> {
                addToggle("开启精简 HUD", x, y, Config.COMPACT_HUD::set, Config.COMPACT_HUD.get());
                addToggle("开启装备箱 HUD", x, y, Config.EQUIPMENT_BOX_HUD_ENABLED::set, Config.EQUIPMENT_BOX_HUD_ENABLED.get());
                addToggle("开启暴露风险 HUD", x, y, Config.EXPOSURE_RISK_HUD_ENABLED::set, Config.EXPOSURE_RISK_HUD_ENABLED.get());
                addDouble("装备箱 HUD 左侧位置比例", x, y,
                        Config.EQUIPMENT_BOX_HUD_X_RATIO::set, Config.EQUIPMENT_BOX_HUD_X_RATIO.get());
                addDouble("装备箱 HUD 顶部位置比例", x, y,
                        Config.EQUIPMENT_BOX_HUD_Y_RATIO::set, Config.EQUIPMENT_BOX_HUD_Y_RATIO.get());
                addDouble("暴露风险 HUD 左侧位置比例", x, y,
                        Config.EXPOSURE_RISK_HUD_X_RATIO::set, Config.EXPOSURE_RISK_HUD_X_RATIO.get());
                addDouble("暴露风险 HUD 顶部位置比例", x, y,
                        Config.EXPOSURE_RISK_HUD_Y_RATIO::set, Config.EXPOSURE_RISK_HUD_Y_RATIO.get());
            }
            case TURRET -> {
                addText("炮台瞄准显示物品 ID", x, y,
                        Config.TURRET_AIM_DISPLAY_ITEM::set, Config.TURRET_AIM_DISPLAY_ITEM.get());
                addDouble("炮台基础攻击伤害", x, y,
                        Config.TURRET_ATTACK_DAMAGE::set, Config.TURRET_ATTACK_DAMAGE.get());
                addDouble("炮台攻击范围", x, y,
                        Config.TURRET_ATTACK_RANGE::set, Config.TURRET_ATTACK_RANGE.get());
                addInt("炮台左右偏航角", x, y,
                        Config.TURRET_YAW_ANGLE::set, Config.TURRET_YAW_ANGLE.get());
                addInt("炮台最大俯角", x, y,
                        Config.TURRET_PITCH_DOWN_ANGLE::set, Config.TURRET_PITCH_DOWN_ANGLE.get());
                addInt("炮台最大仰角", x, y,
                        Config.TURRET_PITCH_UP_ANGLE::set, Config.TURRET_PITCH_UP_ANGLE.get());
                addDouble("炮台最大温度", x, y,
                        Config.TURRET_MAX_TEMPERATURE::set, Config.TURRET_MAX_TEMPERATURE.get());
                addInt("炮台过载销毁时间秒数", x, y,
                        Config.TURRET_OVERLOAD_DESTROY_TIME_SECONDS::set,
                        Config.TURRET_OVERLOAD_DESTROY_TIME_SECONDS.get());
                addInt("炮台攻击间隔游戏刻", x, y,
                        Config.TURRET_ATTACK_INTERVAL_TICKS::set,
                        Config.TURRET_ATTACK_INTERVAL_TICKS.get());
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

    /** 添加一个打开独立编辑器的按钮。 */
    private Button addActionButton(String label, int x, int[] y, Runnable action) {
        int fieldWidth = ConfigScreenHelper.fieldWidth(width, FIELD_WIDTH);
        Button button = addRenderableWidget(Button.builder(Component.literal(label), ignored -> action.run())
                .bounds(x, y[0] + 8, fieldWidth, 24).build());
        actionButtons.add(new ActionButton(button, y[0]));
        y[0] += ROW_HEIGHT;
        return button;
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
        int fieldWidth = ConfigScreenHelper.fieldWidth(width, FIELD_WIDTH);
        EditBox box = new EditBox(font, x, y[0] + 14, fieldWidth, 20, Component.literal(label));
        box.setValue(value);
        ConfigScreenHelper.applyFilter(box, type);
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
        ConfigScreenHelper.applyFields(fields, toggles);
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
        return ConfigScreenHelper.maxScroll(contentHeight, height, CONTENT_BOTTOM_MARGIN);
    }

    /**
     * 根据滚动偏移重新定位分页控件。
     */
    private void repositionPageContents() {
        int bottom = height - CONTENT_BOTTOM_MARGIN;
        for (Field field : fields) {
            int top = field.baseY() - scroll;
            field.box().setY(top + 14);
            field.box().visible = ConfigScreenHelper.isVisible(
                    top + 14, field.box().getHeight(), CONTENT_TOP, bottom);
        }
        for (Toggle toggle : toggles) {
            int top = toggle.baseY() - scroll;
            toggle.checkbox().setY(top);
            toggle.checkbox().visible = ConfigScreenHelper.isVisible(
                    top, toggle.checkbox().getHeight(), CONTENT_TOP, bottom);
        }
        for (ActionButton action : actionButtons) {
            int top = action.baseY() - scroll;
            action.button().setY(top + 8);
            action.button().visible = ConfigScreenHelper.isVisible(
                    top + 8, action.button().getHeight(), CONTENT_TOP, bottom);
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
            scroll = ConfigScreenHelper.clampScroll(
                    scroll, contentHeight, height, CONTENT_BOTTOM_MARGIN, delta);
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

    private void refreshSpecialItemRisksButton() {
        if (specialItemRisksButton != null) {
            specialItemRisksButton.setMessage(Component.literal(
                    "编辑特殊物品风险 (" + Config.EXPOSURE_SPECIAL_ITEM_RISKS.get().size() + "项)"));
        }
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

    private enum Page {
        ARMOR_CURIO("ArmorCurio"), ARMOR_SLOT("ArmorSolt"), OTHER("Other"), HUD("HUD"), TURRET("Turret");
        private final String title;
        Page(String title) { this.title = title; }
    }

}
