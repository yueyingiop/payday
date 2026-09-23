package com.yueyingiop.payday.init;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.block.SupplyBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RegistryBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PayDay.MODID);

    public static final RegistryObject<BlockEntityType<SupplyBlockEntity>> SUPPLY = BLOCK_ENTITIES.register("supply",
            () -> BlockEntityType.Builder.of(SupplyBlockEntity::new,
                    RegistryBlocks.ARMOR_PLATE.get(), RegistryBlocks.FIRSTAID_KIT.get()).build(null));

    private RegistryBlockEntities() {
    }
}
