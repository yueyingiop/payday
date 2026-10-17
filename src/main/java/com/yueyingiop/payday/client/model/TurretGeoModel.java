package com.yueyingiop.payday.client.model;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.entity.TurretEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** 炮台的 GeckoLib 模型、纹理和动画资源。 */
public final class TurretGeoModel extends GeoModel<TurretEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            PayDay.MODID, "geo/turret.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PayDay.MODID, "textures/block/turret.png");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            PayDay.MODID, "animations/turret.animation.json");

    @Override
    public ResourceLocation getModelResource(TurretEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TurretEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TurretEntity animatable) {
        return ANIMATIONS;
    }

    @Override
    public RenderType getRenderType(TurretEntity animatable, ResourceLocation texture) {
        // turret.png 的激光像素包含渐变 alpha，使用半透明实体渲染保留渐变效果。
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void setCustomAnimations(TurretEntity animatable, long instanceId,
                                    AnimationState<TurretEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        if (!animatable.isActivated() || animatable.isActivating()) {
            return;
        }

        GeoBone shaft = getBone("shaft").orElse(null);
        GeoBone gun = getBone("gun").orElse(null);
        if (shaft == null || gun == null) {
            return;
        }

        float yawOffset = Mth.wrapDegrees(animatable.getAimYaw() - animatable.getBaseYaw());
        gun.setRotY(-yawOffset * Mth.DEG_TO_RAD);
        shaft.setRotX(-animatable.getAimPitch() * Mth.DEG_TO_RAD);
    }
}
