package com.yueyingiop.payday.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yueyingiop.payday.client.model.ArmorGeoModel;
import com.yueyingiop.payday.item.ArmorCurioItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.RenderUtils;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** GeckoLib renderer shared by the in-world curio render and the armor GUI preview. */
@OnlyIn(Dist.CLIENT)
public final class ArmorCurioRenderer extends GeoArmorRenderer<ArmorCurioItem> implements ICurioRenderer {
    private final ResourceLocation modelResource;
    private final ResourceLocation textureResource;

    public ArmorCurioRenderer(ResourceLocation itemId) {
        super(new ArmorGeoModel(itemId));
        this.modelResource = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(),
                "geo/" + itemId.getPath() + ".geo.json");
        this.textureResource = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(),
                "textures/armor/" + itemId.getPath() + ".png");
    }

    /**
     * 检查该护甲饰品的 GeckoLib 模型和纹理是否存在。
     * KubeJS 可以创建没有客户端资源的护甲饰品，这类物品应当跳过渲染。
     */
    public boolean hasRenderResources() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getResourceManager().getResource(modelResource).isPresent()
                && minecraft.getResourceManager().getResource(textureResource).isPresent();
    }

    /**
     * A curio armor item is a complete outfit, so every armor bone in the
     * single geo model must remain visible even though Curios has one slot.
     */
    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        setAllVisible(true);
    }

    /**
     * Some exports put the visible head geometry under armorHead, whose local
     * pivot is not the vanilla head pivot. Use the biped root so the complete
     * head subtree follows the player's head rotation as one unit.
     */
    @Override
    public GeoBone getHeadBone() {
        return getBone("bipedHead", "armorHead");
    }

    /**
     * The supplied models use biped leg bones as animation roots and put the
     * visible leg geometry below them. Using armorLeftLeg directly would make
     * the leg rotate around its local calf/foot pivot.
     */
    @Override
    public GeoBone getLeftLegBone() {
        return getBone("bipedLeftLeg", "armorLeftLeg");
    }

    /**
     * 获取右腿骨骼。
     * @return 对应的执行结果。
     */
    @Override
    public GeoBone getRightLegBone() {
        return getBone("bipedRightLeg", "armorRightLeg");
    }

    /** The leg root rotates the boot children together, so do not rotate them a second time. */
    @Override
    public GeoBone getLeftBootBone() {
        return getBone("bipedLeftLeg", null) == null ? super.getLeftBootBone() : null;
    }

    /**
     * 获取右靴骨骼。
     * @return 对应的执行结果。
     */
    @Override
    public GeoBone getRightBootBone() {
        return getBone("bipedRightLeg", null) == null ? super.getRightBootBone() : null;
    }

    /**
     * 根据玩家模型应用护甲骨骼变换。
     * @param baseModel 对应参数。
     */
    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        super.applyBaseTransformations(baseModel);

        applyHeadTransform(head, baseModel.head);

        // 将异常或导出错误的腿部骨骼枢轴修正为原版髋部枢轴。
        applyLegTransform(leftLeg, baseModel.leftLeg, true);
        applyLegTransform(rightLeg, baseModel.rightLeg, false);
    }

    /**
     * 将头部骨骼变换同步到玩家模型。
     * @param bone 骨骼。
     * @param modelPart 对应参数。
     */
    private static void applyHeadTransform(GeoBone bone, ModelPart modelPart) {
        if (bone == null) {
            return;
        }

        RenderUtils.matchModelPartRot(modelPart, bone);
        bone.updatePivot(0.0F, 24.0F, 0.0F);
        bone.updatePosition(modelPart.x, -modelPart.y, modelPart.z);
    }

    /**
     * 将腿部骨骼变换同步到玩家模型。
     * @param bone 骨骼。
     * @param modelPart 对应参数。
     * @param left 对应参数。
     */
    private static void applyLegTransform(GeoBone bone, ModelPart modelPart, boolean left) {
        if (bone == null) {
            return;
        }

        RenderUtils.matchModelPartRot(modelPart, bone);
        bone.updatePivot(left ? 2.0F : -2.0F, 12.0F, 0.0F);
        bone.updatePosition(modelPart.x + (left ? -2.0F : 2.0F), 12.0F - modelPart.y, modelPart.z);
    }

    /**
     * 按主名称或备用名称查找模型骨骼。
     * @param primary 对应参数。
     * @param fallback 对应参数。
     * @return 对应的执行结果。
     */
    private GeoBone getBone(String primary, String fallback) {
        return getGeoModel().getBone(primary).orElseGet(() -> fallback == null
                ? null
                : getGeoModel().getBone(fallback).orElse(null));
    }

    /**
     * 绘制对应内容。
     * @param stack 物品堆。
     * @param slotContext 槽位上下文。
     * @param poseStack 对应参数。
     * @param parent 对应参数。
     * @param buffer 网络缓冲区。
     * @param light 对应参数。
     * @param limbSwing 对应参数。
     * @param limbSwingAmount 对应参数。
     * @param partialTick 帧间插值。
     * @param ageInTicks 对应参数。
     * @param netHeadYaw 对应参数。
     * @param headPitch 对应参数。
     */
    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
        ItemStack stack,
        SlotContext slotContext,
        PoseStack poseStack,
        RenderLayerParent<T, M> parent,
        MultiBufferSource buffer,
        int light,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        if (!hasRenderResources()) {
            return;
        }

        if (!(parent.getModel() instanceof HumanoidModel<?> humanoid)) {
            return;
        }

        prepForRender(slotContext.entity(), stack, EquipmentSlot.CHEST, humanoid);

        renderPrepared(poseStack, buffer, light, partialTick);
    }

    /** Prepare the same GeoArmorRenderer for a static GUI preview. */
    public void preparePreview(ItemStack stack) {
        if (!hasRenderResources()) {
            return;
        }

        LivingEntity player = Minecraft.getInstance().player;
        if (player != null) {
            prepForRender(player, stack, EquipmentSlot.CHEST, this);
        }
    }

    /**
     * 绘制准备好的护甲模型。
     * @param poseStack 对应参数。
     * @param buffer 网络缓冲区。
     * @param light 对应参数。
     */
    public void renderModel(PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (!hasRenderResources()) {
            return;
        }

        renderPrepared(poseStack, buffer, light, Minecraft.getInstance().getFrameTime());
    }

    /**
     * 应用渲染变换并绘制护甲模型。
     * @param poseStack 对应参数。
     * @param buffer 网络缓冲区。
     * @param light 对应参数。
     * @param partialTick 帧间插值。
     */
    private void renderPrepared(PoseStack poseStack, MultiBufferSource buffer, int light, float partialTick) {
        if (!hasRenderResources()) {
            return;
        }

        ArmorCurioItem item = getAnimatable();
        if (item == null || getCurrentStack() == null) {
            return;
        }

        try {
            RenderType renderType = getRenderType(item, getTextureLocation(item), buffer, partialTick);
            VertexConsumer consumer = buffer.getBuffer(renderType);
            defaultRender(poseStack, item, buffer, renderType, consumer, 0.0F, partialTick, light);
        } catch (RuntimeException ignored) {
            // 资源在运行时被资源包/KubeJS 移除或替换时，跳过本帧渲染，避免客户端崩溃。
        }
    }
}
