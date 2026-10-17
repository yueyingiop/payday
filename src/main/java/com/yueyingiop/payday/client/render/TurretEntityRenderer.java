package com.yueyingiop.payday.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yueyingiop.payday.client.model.TurretGeoModel;
import com.yueyingiop.payday.entity.TurretEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.math.Axis;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 炮台实体的 GeckoLib 渲染器。 */
public final class TurretEntityRenderer extends GeoEntityRenderer<TurretEntity> {
    public TurretEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new TurretGeoModel());
    }

    /** 按实体 yaw 旋转模型；炮台模型的枪口朝向局部 +Z。 */
    @Override
    protected void applyRotations(TurretEntity entity, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotationYaw));
    }
}
