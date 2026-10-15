package com.yueyingiop.payday.client.render;

import com.yueyingiop.payday.block.TurretBlockEntity;
import com.yueyingiop.payday.client.model.TurretGeoModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** 炮台的 GeckoLib 方块实体渲染器。 */
public final class TurretBlockRenderer extends GeoBlockRenderer<TurretBlockEntity> {
    public TurretBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new TurretGeoModel());
    }
}
