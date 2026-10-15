package com.yueyingiop.payday.client.render;

import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.api.helper.RenderHelper;
import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.item.AmmoBoxItem;
import com.yueyingiop.payday.item.ClothesBoxItem;
import com.yueyingiop.payday.item.MedicalBoxItem;
import com.yueyingiop.payday.item.base.EquipmentBoxItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 绘制佩戴中的装备箱内容 HUD，并提供 HUD 配置界面使用的定位工具。 */
public final class EquipmentBoxHudRenderer {
    public static final int HUD_WIDTH = 40;
    public static final int HUD_HEIGHT = 32;
    private static final ResourceLocation HUD_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "payday", "textures/gui/equipment_box_hud.png");
    private static final int HUD_TEXTURE_WIDTH = 64;
    private static final int HUD_TEXTURE_HEIGHT = 256;
    private static final int HUD_U = 1;
    private static final int HUD_V = 1;
    private static final int HUD_V_STEP = 35;
    private static Contents lastContents = Contents.EMPTY;
    private static int lastContentsTick = Integer.MIN_VALUE;

    private EquipmentBoxHudRenderer() {
    }

    /** 查找玩家装备箱中的第一个物品和当前总数量。 */
    public static Contents findContents(Player player) {
        Contents result = Contents.EMPTY;
        for (ItemStack boxStack : CurioHelper.stacks(player, CurioHelper.EQUIPMENT_BOX_SLOT)) {
            if (!(boxStack.getItem() instanceof EquipmentBoxItem box)) {
                continue;
            }

            HudType type = HudType.from(boxStack.getItem());
            int count = box.storedCount(boxStack);
            ItemStack stored = box.peek(boxStack);
            result = new Contents(stored.isEmpty() ? ItemStack.EMPTY : stored.copyWithCount(1),
                    Math.max(0, count), type, true);
            break;
        }
        if (!result.isEmpty()) {
            lastContents = result;
            lastContentsTick = player.tickCount;
        }
        return result;
    }

    /** 绘制装备箱内容 HUD。 */
    public static void render(GuiGraphics graphics, Player player) {
        Contents contents = findContents(player);
        if (!contents.equipped() && player.tickCount - lastContentsTick <= 20) {
            contents = lastContents;
        }
        if (!contents.equipped()) {
            return;
        }

        int x = RenderHelper.xFromRatio(Config.EQUIPMENT_BOX_HUD_X_RATIO.get(),
                graphics.guiWidth(), HUD_WIDTH);
        int y = RenderHelper.yFromRatio(Config.EQUIPMENT_BOX_HUD_Y_RATIO.get(),
                graphics.guiHeight(), HUD_HEIGHT);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        try {
            renderAt(graphics, contents, x, y);
        } finally {
            graphics.pose().popPose();
            RenderSystem.enableDepthTest();
        }
    }

    /** 在指定位置绘制一个 HUD 预览。 */
    public static void renderAt(GuiGraphics graphics, Contents contents, int x, int y) {
        renderAt(graphics, contents, x, y, false);
    }

    /** 绘制饰品 HUD 背景、物品图标和配置模式下的绿色蒙板。 */
    public static void renderAt(GuiGraphics graphics, Contents contents, int x, int y, boolean configurationMode) {
        graphics.blit(HUD_TEXTURE, x, y, HUD_U, HUD_V + contents.type().row() * HUD_V_STEP,
                HUD_WIDTH, HUD_HEIGHT, HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        if (configurationMode) {
            graphics.fill(x, y, x + HUD_WIDTH, y + HUD_HEIGHT, 0x6600FF00);
            graphics.renderOutline(x, y, HUD_WIDTH, HUD_HEIGHT, 0xFF55FF55);
        }
        if (contents.isEmpty()) {
            return;
        }

        int itemX = x + 12;
        int itemY = y + 4;
        graphics.renderItem(contents.stack(), itemX, itemY);
        graphics.renderItemDecorations(Minecraft.getInstance().font, contents.stack(), itemX, itemY,
                Integer.toString(contents.count()));
    }

    /** HUD 中显示的物品和数量。 */
    public record Contents(ItemStack stack, int count, HudType type, boolean equipped) {
        private static final Contents EMPTY = new Contents(ItemStack.EMPTY, 0, HudType.ARMOR_BOX, false);

        public Contents(ItemStack stack, int count) {
            this(stack, count, HudType.ARMOR_BOX, false);
        }

        /**
         * 判断装备箱 HUD 内容是否为空。
         * @return 对应的执行结果。
         */
        public boolean isEmpty() {
            return stack.isEmpty() || count <= 0;
        }
    }

    /** 饰品 HUD 背景在纹理图集中的类型。 */
    public enum HudType {
        CLOTHES_BOX(0),
        ARMOR_BOX(1),
        MEDICAL_BOX(2),
        AMMO_BOX(3);

        private final int row;

        HudType(int row) {
            this.row = row;
        }

        /**
         * 获取纹理行索引。
         * @return 对应的执行结果。
         */
        public int row() {
            return row;
        }

        /** 根据装备箱物品类型选择对应 HUD 背景。 */
        public static HudType from(Item item) {
            if (item instanceof ClothesBoxItem) {
                return CLOTHES_BOX;
            }
            if (item instanceof MedicalBoxItem) {
                return MEDICAL_BOX;
            }
            if (item instanceof AmmoBoxItem) {
                return AMMO_BOX;
            }
            return ARMOR_BOX;
        }
    }
}
