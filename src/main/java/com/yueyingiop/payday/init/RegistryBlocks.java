package com.yueyingiop.payday.init;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.block.SupplyBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RegistryBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PayDay.MODID);

    public static final RegistryObject<Block> ARMOR_PLATE = BLOCKS.register("armor_plate",
            () -> new SupplyBlock(SupplyBlock.SupplyType.ARMOR_PLATE));
    public static final RegistryObject<Block> FIRSTAID_KIT = BLOCKS.register("firstaid_kit",
            () -> new SupplyBlock(SupplyBlock.SupplyType.FIRST_AID));

    private RegistryBlocks() {
    }
}
