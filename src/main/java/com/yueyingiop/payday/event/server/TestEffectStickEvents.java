package com.yueyingiop.payday.event.server;

import com.yueyingiop.payday.item.TestEffectStickItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Applies NBT-configured effects from the temporary test stick. */
public final class TestEffectStickEvents {
    private TestEffectStickEvents() {
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof TestEffectStickItem)
                || !(event.getTarget() instanceof LivingEntity target)
                || player.level().isClientSide) {
            return;
        }
        TestEffectStickItem.applyEffects(held, target);
    }
}
