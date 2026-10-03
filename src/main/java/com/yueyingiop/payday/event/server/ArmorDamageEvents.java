package com.yueyingiop.payday.event.server;

import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.api.ArmorModifier;
import com.yueyingiop.payday.api.PhoneUpgradeAction;
import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.ArmorSlotItem;
import com.yueyingiop.payday.item.PhoneItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collections;
import java.util.List;

/** 处理护甲饰品的伤害吸收、永久损伤和自动恢复。 */
public final class ArmorDamageEvents {
    private ArmorDamageEvents() {
    }

    /**
     * 让护甲插槽按照加载顺序的逆序吸收最终伤害，并记录连续受击造成的永久损伤。
     *
     * @param event 生物受到伤害事件
     */
    @SubscribeEvent
    public static void damage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || event.getAmount() <= 0) {
            return;
        }

        PhoneItem.refreshArmorSlotModifiers(entity);

        // 杀手本能触发时跳过本模组的护甲吸收，保留原版伤害流程。
        if (entity.getPersistentData().getBoolean(PhoneUpgradeAction.PIERCE_REDUCTION)) {
            return;
        }

        List<ItemStack> armorStacks = CurioHelper.stacks(entity, CurioHelper.ARMOR_SLOT);
        if (armorStacks.isEmpty()) {
            return;
        }

        long now = entity.level().getGameTime();
        boolean hasPreviousDamage = entity.getPersistentData().contains(ServerEventUtil.LAST_DAMAGE);
        long previousDamage = entity.getPersistentData().getLong(ServerEventUtil.LAST_DAMAGE);
        float remaining = event.getAmount();

        Collections.reverse(armorStacks);
        boolean repeatedResistancePenaltyApplied = false;
        for (ItemStack armorStack : armorStacks) {
            if (!(armorStack.getItem() instanceof ArmorCurioItem armorCurio)) {
                continue;
            }

            List<ItemStack> upgrades = armorCurio.getUpgradeStacks(armorStack);
            for (int i = upgrades.size() - 1; i >= 0 && remaining > 0; i--) {
                ItemStack stack = upgrades.get(i);
                if (!(stack.getItem() instanceof ArmorSlotItem armor)) {
                    continue;
                }

                for (int definitionIndex = armor.armorDefinitions().size() - 1;
                     definitionIndex >= 0 && remaining > 0; definitionIndex--) {
                    double current = ArmorModifier.current(entity, armorStack, armor, stack,
                            definitionIndex);
                    if (current <= 0) {
                        continue;
                    }

                    double absorbed = Math.min(current, remaining);
                    boolean wasBroken = armor.isBroken(stack, definitionIndex);
                    ArmorModifier.setCurrent(entity, armorStack, armor, stack, definitionIndex,
                            current - absorbed);

                    boolean repeatedHit = hasPreviousDamage
                            && now - previousDamage < ServerEventUtil.recoveryTicks(
                            entity, armorStack, armor, definitionIndex);
                    double permanentBefore = armor.permanent(stack, definitionIndex);
                    if (armor.kind(definitionIndex) == ArmorSlotItem.Kind.RESISTANCE
                            && repeatedHit && !repeatedResistancePenaltyApplied) {
                        // 永久损伤只记在本次实际吸收伤害的抗性护甲定义上。
                        ArmorModifier.addPermanent(entity, armorStack, armor, stack,
                                definitionIndex, 2.0D);
                        repeatedResistancePenaltyApplied = true;
                    }

                    if (armor.permanent(stack, definitionIndex) > permanentBefore
                            || (!wasBroken && armor.isBroken(stack, definitionIndex))) {
                        armor.markBroken(stack, definitionIndex, nextBrokenSequence(entity));
                        if (!wasBroken && armor.isBroken(stack, definitionIndex)) {
                            ArmorModifier.grantBreakImmunity(entity);
                        }
                    }
                    remaining -= (float) absorbed;
                }
            }
            armorCurio.setUpgradeStacks(armorStack, upgrades);
        }

        entity.getPersistentData().putLong(ServerEventUtil.LAST_DAMAGE, now);
        event.setAmount(Math.max(0, remaining));
    }

    /**
     * 在各个护甲插槽的恢复间隔结束后逐步恢复其当前护甲值。
     *
     * @param event 玩家 tick 事件
     */
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        PhoneItem.refreshArmorSlotModifiers(event.player);

        List<ItemStack> armorStacks = CurioHelper.stacks(event.player, CurioHelper.ARMOR_SLOT);
        if (armorStacks.isEmpty()) {
            event.player.getPersistentData().remove(ServerEventUtil.LAST_DAMAGE);
            return;
        }

        long last = event.player.getPersistentData().getLong(ServerEventUtil.LAST_DAMAGE);
        long elapsed = event.player.level().getGameTime() - last;
        if (elapsed < 1) {
            return;
        }

        for (ItemStack armorStack : armorStacks) {
            if (!(armorStack.getItem() instanceof ArmorCurioItem armorCurio)) {
                continue;
            }

            List<ItemStack> upgrades = armorCurio.getUpgradeStacks(armorStack);
            for (ItemStack stack : upgrades) {
                if (!(stack.getItem() instanceof ArmorSlotItem armor)) {
                    continue;
                }

                for (int definitionIndex = 0;
                     definitionIndex < armor.armorDefinitions().size(); definitionIndex++) {
                    long wait = ServerEventUtil.recoveryTicks(event.player, armorStack, armor,
                            definitionIndex);
                    boolean fixedRecovery = ArmorModifier.hasFixedRecovery(event.player, armorStack);
                    if (elapsed < wait || fixedRecovery && elapsed % wait != 0L
                            || armor.isBroken(stack, definitionIndex)) {
                        continue;
                    }

                    double current = ArmorModifier.current(event.player, armorStack, armor, stack,
                            definitionIndex);
                    double maximum = ArmorModifier.effectiveMax(event.player, armorStack,
                            armor, stack, definitionIndex);
                    if (current < maximum) {
                        // 必须通过 ArmorModifier 写回，否则 ArmorSlotItem 会按原始上限截断，
                        // 手机升级提供的额外护甲无法通过自然恢复补满。
                        ArmorModifier.setCurrent(event.player, armorStack, armor, stack,
                                definitionIndex, Math.min(maximum,
                                        current + ArmorModifier.recoveryAmount(event.player,
                                                armorStack, armor, stack, definitionIndex)));
                    }
                }
            }
            armorCurio.setUpgradeStacks(armorStack, upgrades);
        }
    }

    /** 获取下一个损坏顺序编号，用于确定护甲板优先修复目标。 */
    /**
     * 执行 <code>nextBrokenSequence</code> 对应的操作。
     * @param entity 参数值。
     * @return 方法执行结果。
     */
    private static long nextBrokenSequence(LivingEntity entity) {
        long next = entity.getPersistentData().getLong(ServerEventUtil.BROKEN_SEQUENCE) + 1L;
        entity.getPersistentData().putLong(ServerEventUtil.BROKEN_SEQUENCE, next);
        return next;
    }
}
