package com.yueyingiop.payday.api.phone;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * 手机升级的 Forge 事件代理回调。
 *
 * @param <E> 被代理的 Forge 事件类型
 */
@FunctionalInterface
public interface ProxyEvent<E extends Event> {
    /**
     * 处理一次代理事件。
     *
     * @param event 当前 Forge 事件
     * @param owner 持有该手机升级的实体
     */
    void accept(E event, LivingEntity owner);
}
