package com.yueyingiop.payday.api;

import com.yueyingiop.payday.exposure.ExposureRisk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Public API for assigning exposure-risk modifiers to content. */
public final class ExposureRiskModifier {
    /** NBT key read from every inventory, armor, offhand, and Curios stack. */
    public static final String TEMPORARY_MODIFIER_KEY = "payday:exposure_modifier";

    private ExposureRiskModifier() {
    }

    /** Assigns a temporary modifier to this stack. It applies while the stack is carried or equipped. */
    /**
     * 执行 <code>setTemporary</code> 对应的操作。
     * @param stack 参数值。
     * @param value 参数值。
     * @return 方法执行结果。
     */
    public static ItemStack setTemporary(ItemStack stack, double value) {
        stack.getOrCreateTag().putDouble(TEMPORARY_MODIFIER_KEY, value);
        return stack;
    }

    /** Adds a persistent modifier to a player. Call on the logical server. */
    /**
     * 执行 <code>addPermanent</code> 对应的操作。
     * @param player 参数值。
     * @param value 参数值。
     */
    public static void addPermanent(Player player, double value) {
        ExposureRisk.addPermanentModifier(player, value);
    }

    /** Returns the player's persistent modifier total. */
    /**
     * 执行 <code>getPermanent</code> 对应的操作。
     * @param player 参数值。
     * @return 方法执行结果。
     */
    public static double getPermanent(Player player) {
        return ExposureRisk.getPermanentModifier(player);
    }

    /** Replaces the player's persistent modifier total. Call on the logical server. */
    /**
     * 执行 <code>setPermanent</code> 对应的操作。
     * @param player 参数值。
     * @param value 参数值。
     */
    public static void setPermanent(Player player, double value) {
        ExposureRisk.setPermanentModifier(player, value);
    }
}
