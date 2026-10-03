package com.yueyingiop.payday.api.phone;

import net.minecraft.world.damagesource.DamageSource;

import java.util.function.Predicate;

/**
 * 按伤害来源判定的概率免疫规则。
 *
 * @param damageType 伤害来源判定器
 * @param probability 免疫概率，范围为 {@code 0.0~1.0}
 */
public record DamageImmunity(Predicate<DamageSource> damageType, double probability) {
    /** 创建伤害免疫规则并校验参数。 */
    public DamageImmunity {
        if (damageType == null) {
            throw new IllegalArgumentException("damageType must not be null");
        }
        if (!Double.isFinite(probability) || probability < 0.0D || probability > 1.0D) {
            throw new IllegalArgumentException("probability must be between 0 and 1");
        }
    }

    /** 判断该规则是否匹配当前伤害来源。 */
    public boolean matches(DamageSource source) {
        return damageType.test(source);
    }
}
