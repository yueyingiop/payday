package com.yueyingiop.payday.init;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.entity.TurretEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** PayDay 生物实体注册表。 */
public final class RegistryEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PayDay.MODID);

    public static final RegistryObject<EntityType<TurretEntity>> TURRET = ENTITIES.register("turret",
            () -> EntityType.Builder.<TurretEntity>of(TurretEntity::new, MobCategory.MISC)
                    .sized(1.0F, 2.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(PayDay.MODID + ":turret"));

    private RegistryEntities() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TURRET.get(), TurretEntity.createAttributes().build());
    }
}
