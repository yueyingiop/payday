package com.yueyingiop.payday.client.render;

import com.yueyingiop.payday.block.SupplyBlockEntity;
import com.yueyingiop.payday.client.model.SupplyBlockGeoModel;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class SupplyBlockRenderer extends GeoBlockRenderer<SupplyBlockEntity> {
    public SupplyBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new SupplyBlockGeoModel());
    }
}
