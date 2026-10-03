package com.yueyingiop.payday.api.phone;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 手机升级的 Curios tick 回调。
 *
 * <p>该回调由 {@code PhoneItem#curioTick} 调用，适合实现需要持续刷新或定时处理的效果。</p>
 */
@FunctionalInterface
public interface CurioTick {
    /**
     * 处理一次手机升级 tick。
     *
     * @param identifier Curios 槽位标识
     * @param index 手机在该槽位中的索引
     * @param livingEntity 当前佩戴手机的实体
     * @param stack 当前手机升级物品
     */
    void accept(String identifier, int index, LivingEntity livingEntity, ItemStack stack);
}
