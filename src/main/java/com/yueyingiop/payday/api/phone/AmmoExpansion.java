package com.yueyingiop.payday.api.phone;

import com.yueyingiop.payday.api.PhoneUpgradeAction;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * TACZ 枪械弹匣容量扩展定义。
 *
 * <p>空的枪械类型集合表示支持全部枪械；非空集合表示只支持集合中列出的 TACZ 枪械类型。
 * 该定义只计算最终容量，不会直接补充当前弹药。</p>
 *
 * @param amount 扩容数值；乘法模式下表示比例，加法模式下表示直接增加的弹药数量
 * @param operation 扩容运算方式
 * @param gunTypes 支持的枪械类型
 */
public record AmmoExpansion(double amount, PhoneUpgradeAction.Operation operation, Set<String> gunTypes) {
    /** 使用可变参数枪械类型创建扩容定义。 */
    public AmmoExpansion(double amount, PhoneUpgradeAction.Operation operation,
                         String... gunTypes) {
        this(amount, operation, normalizeGunTypes(gunTypes));
        if (!Double.isFinite(amount) || amount < 0.0D) {
            throw new IllegalArgumentException("ammo expansion amount must be non-negative");
        }
        if (operation == null) {
            throw new IllegalArgumentException("ammo expansion operation must not be null");
        }
    }

    /** 判断该扩容定义是否支持指定枪械类型。 */
    public boolean supports(String gunType) {
        return gunTypes.isEmpty() || gunType != null && gunTypes.contains(gunType);
    }

    /** 根据扩容运算方式计算新的弹匣容量。 */
    public int apply(int capacity) {
        return switch (operation) {
            case MULTIPLY -> (int) Math.ceil(capacity * (1.0D + amount));
            case ADDITION -> (int) Math.ceil(capacity + amount);
        };
    }

    /** 将枪械类型参数规范化为不可变集合。 */
    private static Set<String> normalizeGunTypes(String[] gunTypes) {
        if (gunTypes == null || gunTypes.length == 0) {
            return Collections.emptySet();
        }
        return Set.copyOf(Arrays.asList(gunTypes));
    }
}
