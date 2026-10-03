package com.yueyingiop.payday.init;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.effect.FearEffect;
import com.yueyingiop.payday.effect.HowlEffect;
import com.yueyingiop.payday.effect.InfiltrationEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Custom effects used by phone upgrades. */
public final class RegistryEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, PayDay.MODID);

    public static final RegistryObject<MobEffect> FEAR = EFFECTS.register("fear", FearEffect::new);
    /** 啸叫：持有者周期性使附近非玩家生物陷入恐惧。 */
    public static final RegistryObject<MobEffect> HOWL = EFFECTS.register("howl", HowlEffect::new);
    public static final RegistryObject<MobEffect> INFILTRATION = EFFECTS.register("infiltration", InfiltrationEffect::new);
}
