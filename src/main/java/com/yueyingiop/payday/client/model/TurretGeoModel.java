package com.yueyingiop.payday.client.model;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.block.TurretBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** 炮台的 GeckoLib 模型、纹理和动画资源。 */
public final class TurretGeoModel extends GeoModel<TurretBlockEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            PayDay.MODID, "geo/turret.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PayDay.MODID, "textures/block/turret.png");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            PayDay.MODID, "animations/turret.animation.json");

    @Override
    public ResourceLocation getModelResource(TurretBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TurretBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TurretBlockEntity animatable) {
        return ANIMATIONS;
    }

    @Override
    public RenderType getRenderType(TurretBlockEntity animatable, ResourceLocation texture) {
        // turret.png 的激光像素包含渐变 alpha，使用半透明实体渲染保留渐变效果。
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void setCustomAnimations(TurretBlockEntity animatable, long instanceId,
                                    AnimationState<TurretBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        if (!animatable.isActivated() || animatable.isActivating()) {
            return;
        }

        GeoBone base = getBone("base").orElse(null);
        GeoBone head = getBone("head").orElse(null);
        if (base == null || head == null) {
            return;
        }

        float yawOffset = Mth.wrapDegrees(animatable.getAimYaw() - animatable.getBaseYaw());
        base.setRotY(-yawOffset * Mth.DEG_TO_RAD);
        // GeckoLib 的 X 轴正旋转向下；游戏语义中的正俯仰角表示抬高炮管。
        head.setRotX(head.getRotX() - animatable.getAimPitch() * Mth.DEG_TO_RAD);
    }
}
