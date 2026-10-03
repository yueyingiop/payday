package com.yueyingiop.payday.event.server;

import com.yueyingiop.payday.api.helper.CurioHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 服务端事件共用的护甲饰品查询与计时工具。 */
final class ServerEventUtil {
    static final String LAST_DAMAGE = "payday_last_damage_tick";
    static final String BROKEN_SEQUENCE = "payday_broken_sequence";
    static final String ARMOR_CHANGE_COOLDOWN = "payday_armor_change_cooldown";
    static final String EQUIPMENT_BOX_CHANGE_COOLDOWN = "payday_equipment_box_change_cooldown";
    static final String CLOTHES_BOX_COOLDOWN = "payday_clothes_box_cooldown";
    static final String EQUIPMENT_BOX_USE_COOLDOWN = "payday_equipment_box_use_cooldown";
    static final String ARMOR = CurioHelper.ARMOR_SLOT;
    static final String EQUIPMENT_BOX = CurioHelper.EQUIPMENT_BOX_SLOT;

    private ServerEventUtil() {
    }

    /** 将秒数转换为至少一 tick 的恢复间隔。 */
    /**
     * 执行 <code>recoveryTicks</code> 对应的操作。
     * @param armor 参数值。
     * @return 方法执行结果。
     */
    static long recoveryTicks(com.yueyingiop.payday.item.ArmorSlotItem armor) {
        return Math.max(1L, Math.round(armor.recoverySeconds() * 20.0D));
    }

    /** 将指定护甲定义的恢复秒数转换为至少一 tick。 */
    /**
     * 执行 <code>recoveryTicks</code> 对应的操作。
     * @param armor 参数值。
     * @param definitionIndex 参数值。
     * @return 方法执行结果。
     */
    static long recoveryTicks(com.yueyingiop.payday.item.ArmorSlotItem armor, int definitionIndex) {
        return Math.max(1L, Math.round(armor.recoverySeconds(definitionIndex) * 20.0D));
    }

    /** 将护甲升级提供的恢复间隔修正应用到指定实体。 */
    static long recoveryTicks(net.minecraft.world.entity.LivingEntity entity,
                              ItemStack armorStack,
                              com.yueyingiop.payday.item.ArmorSlotItem armor, int definitionIndex) {
        return Math.max(1L, Math.round(recoveryTicks(armor, definitionIndex)
                * com.yueyingiop.payday.api.ArmorModifier.recoveryMultiplier(entity, armorStack)));
    }

    /** 将秒数转换为 tick。 */
    /**
     * 执行 <code>cooldownTicks</code> 对应的操作。
     * @param seconds 参数值。
     * @return 方法执行结果。
     */
    static long cooldownTicks(double seconds) {
        return Math.max(0L, Math.round(seconds * 20.0D));
    }

    /** 判断玩家是否处于装备箱 V 键使用冷却中。 */
    /**
     * 执行 <code>hasEquipmentBoxUseCooldown</code> 对应的操作。
     * @param player 参数值。
     * @return 方法执行结果。
     */
    static boolean hasEquipmentBoxUseCooldown(Player player) {
        return equipmentBoxUseCooldownRemainingTicks(player) > 0L;
    }

    /** 获取玩家剩余装备箱 V 键使用冷却 tick。 */
    /**
     * 执行 <code>equipmentBoxUseCooldownRemainingTicks</code> 对应的操作。
     * @param player 参数值。
     * @return 方法执行结果。
     */
    static long equipmentBoxUseCooldownRemainingTicks(Player player) {
        return Math.max(0L, player.getPersistentData().getLong(EQUIPMENT_BOX_USE_COOLDOWN)
                - player.level().getGameTime());
    }

    /** 设置装备箱 V 键使用冷却。 */
    /**
     * 执行 <code>setEquipmentBoxUseCooldown</code> 对应的操作。
     * @param player 参数值。
     */
    static void setEquipmentBoxUseCooldown(Player player) {
        player.getPersistentData().putLong(EQUIPMENT_BOX_USE_COOLDOWN,
                player.level().getGameTime() + cooldownTicks(
                        com.yueyingiop.payday.Config.EQUIPMENT_BOX_USE_COOLDOWN_SECONDS.get()));
    }

    /** 向玩家显示装备箱 V 键使用冷却剩余时间。 */
    /**
     * 执行 <code>sendEquipmentBoxUseCooldownMessage</code> 对应的操作。
     * @param player 参数值。
     */
    static void sendEquipmentBoxUseCooldownMessage(Player player) {
        long seconds = Math.max(1L, (equipmentBoxUseCooldownRemainingTicks(player) + 19L) / 20L);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.payday.equipment_box_use_cooldown", seconds)
                .withStyle(net.minecraft.ChatFormatting.RED), true);
    }

    /** 消耗一个物品，创造模式下不减少物品数量。 */
    /**
     * 执行 <code>consumeOne</code> 对应的操作。
     * @param player 参数值。
     * @param stack 参数值。
     */
    static void consumeOne(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
