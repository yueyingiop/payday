package com.yueyingiop.payday.init;

import com.yueyingiop.payday.PayDay;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Custom attributes used by PayDay upgrades. */
public final class RegistryAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, PayDay.MODID);

    public static final RegistryObject<Attribute> RANGED_DAMAGE = ATTRIBUTES.register("ranged_damage",
            () -> new RangedAttribute("attribute.name.payday.ranged_damage", 0.0D, 0.0D, 1.0E9D)
                    .setSyncable(true));

    public static final RegistryObject<Attribute> HEADSHOT_DAMAGE = ATTRIBUTES.register("headshot_damage",
            () -> new RangedAttribute("attribute.name.payday.headshot_damage", 0.0D, 0.0D, 1.0E9D)
                    .setSyncable(true));

    public static final RegistryObject<Attribute> DODGE_CHANCE = ATTRIBUTES.register("dodge_chance",
            () -> new RangedAttribute("attribute.name.payday.dodge_chance", 0.0D, 0.0D, 10000.0D)
                    .setSyncable(true));

    public static final RegistryObject<Attribute> EXPOSURE_RISK = ATTRIBUTES.register("exposure_risk",
            () -> new RangedAttribute("attribute.name.payday.exposure_risk", 0.0D, -1.0E9D, 1.0E9D)
                    .setSyncable(true));

    private RegistryAttributes() {
    }
}
