package com.yueyingiop.payday.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.entity.TurretEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 炮台生物代理不单独绘制，实际模型由炮台方块实体渲染。 */
public final class TurretEntityRenderer extends EntityRenderer<TurretEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PayDay.MODID, "textures/item/turret.png");

    public TurretEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TurretEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 保持空实现，避免模型与 TurretBlockEntity 重复渲染。
    }

    @Override
    public ResourceLocation getTextureLocation(TurretEntity entity) {
        return TEXTURE;
    }
}
