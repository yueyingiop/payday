package com.yueyingiop.payday.api.phone;

import net.minecraftforge.eventbus.api.Event;

/**
 * 一条手机升级事件代理定义。
 *
 * @param <E> 被代理的 Forge 事件类型
 * @param eventType 事件类型
 * @param callback 事件回调
 */
public record ProxyEventDefinition<E extends Event>(Class<E> eventType, ProxyEvent<E> callback) {
}
