package com.yueyingiop.payday.api.phone;

import com.yueyingiop.payday.api.ArmorModifier;

/**
 * 手机升级对护甲插槽的修改回调。
 *
 * <p>回调接收已经绑定手机、护甲饰品和代理实体的 {@link ArmorModifier}，
 * 可以直接复用统一的护甲遍历、恢复间隔、护甲上限和损坏免疫逻辑。</p>
 */
@FunctionalInterface
public interface ArmorSoltModifier {
    /**
     * 修改一个护甲饰品上下文。
     *
     * @param modifier 当前护甲饰品对应的护甲修改器
     */
    void accept(ArmorModifier modifier);
}
