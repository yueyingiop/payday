package com.yueyingiop.payday.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Stops a living entity's movement and attack behavior while active. */
public class FearEffect extends MobEffect {
    public FearEffect() {
        super(MobEffectCategory.HARMFUL, 0x5B2C83);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.setDeltaMovement(0.0D, entity.getDeltaMovement().y, 0.0D);
    }
}
