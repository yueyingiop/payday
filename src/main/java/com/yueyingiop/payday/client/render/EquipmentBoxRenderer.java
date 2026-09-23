package com.yueyingiop.payday.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.blaze3d.platform.Lighting;
import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.client.model.EquipmentBoxGeoModel;
import com.yueyingiop.payday.item.base.EquipmentBoxItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** 装备箱的 GeckoLib 物品渲染器，同时负责 Curios 跨包渲染。 */
@OnlyIn(Dist.CLIENT)
public final class EquipmentBoxRenderer extends GeoItemRenderer<EquipmentBoxItem> implements ICurioRenderer {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(PayDay.MODID,
            "geo/equipment_box.geo.json");
    private boolean curioRender;

    public EquipmentBoxRenderer() {
        super(new EquipmentBoxGeoModel());
    }

    /** 检查共用模型和当前装备箱对应的纹理是否存在。 */
    public boolean hasRenderResources(ItemStack stack) {
        if (!(stack.getItem() instanceof EquipmentBoxItem box)) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation texture = getGeoModel().getTextureResource(box);
        return minecraft.getResourceManager().getResource(MODEL).isPresent()
                && minecraft.getResourceManager().getResource(texture).isPresent();
    }

    /**
     * 按物品显示上下文绘制装备箱模型。
     * 
     * @param stack          物品堆。
     * @param displayContext 对应参数。
     * @param poseStack      对应参数。
     * @param buffer         网络缓冲区。
     * @param packedLight    对应参数。
     * @param packedOverlay  对应参数。
     */
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (displayContext == ItemDisplayContext.GUI) {
            renderGuiIcon(stack, poseStack, buffer, packedLight, packedOverlay);
            return;
        }

        if (!hasRenderResources(stack)) {
            return;
        }

        try {
            super.renderByItem(stack, displayContext, poseStack, buffer, packedLight, packedOverlay);
        } catch (RuntimeException ignored) {
            // 资源包重载或 KubeJS 资源缺失时，默认跳过这一帧的模型渲染。
        }
    }

    /** 在物品栏上下文使用普通 item 图标，其他上下文仍然使用 GeckoLib 模型。 */
    private void renderGuiIcon(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return;
        }

        ResourceLocation guiModelId = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "models/item/" + itemId.getPath() + "_gui.json");
        if (!Minecraft.getInstance().getResourceManager().getResource(guiModelId).isPresent()) {
            return;
        }

        ModelResourceLocation modelId = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), itemId.getPath() + "_gui"),
            "inventory"
        );
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelId);
        poseStack.pushPose();
        try {
            // BEWLR 收到的矩阵已被 ItemRenderer 平移到模型原点，抵消后再使用 GUI 图标自身的变换。
            poseStack.translate(0.5F, 0.5F, 0.5F);
            Lighting.setupForFlatItems();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            Minecraft.getInstance().getItemRenderer().render(stack, ItemDisplayContext.GUI, false,
                    poseStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            Lighting.setupFor3DItems();
            poseStack.popPose();
        }
    }

    /**
     * 绘制对应内容。
     * 
     * @param stack           物品堆。
     * @param slotContext     槽位上下文。
     * @param poseStack       对应参数。
     * @param parent          对应参数。
     * @param buffer          网络缓冲区。
     * @param light           对应参数。
     * @param limbSwing       对应参数。
     * @param limbSwingAmount 对应参数。
     * @param partialTick     帧间插值。
     * @param ageInTicks      对应参数。
     * @param netHeadYaw      对应参数。
     * @param headPitch       对应参数。
     */
    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack poseStack,
            RenderLayerParent<T, M> parent, MultiBufferSource buffer, int light,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
            float netHeadYaw, float headPitch) {
        if (!hasRenderResources(stack)
                || !(parent.getModel() instanceof HumanoidModel<?> humanoid)
                || !(stack.getItem() instanceof EquipmentBoxItem box)) {
            return;
        }

        this.animatable = box;
        this.currentItemStack = stack;
        this.curioRender = true;
        poseStack.pushPose();
        try {
            ICurioRenderer.translateIfSneaking(poseStack, slotContext.entity());
            humanoid.body.translateAndRotate(poseStack);

            // 模型以玩家身体为基准，放到右侧并略微向前，作为挎包佩戴。
            poseStack.translate(0.38F, -0.5F, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));

            RenderType renderType = getRenderType(box, getTextureLocation(box), buffer, partialTick);
            VertexConsumer consumer = buffer.getBuffer(renderType);
            defaultRender(poseStack, box, buffer, renderType, consumer, 0.0F, partialTick, light);
        } catch (RuntimeException ignored) {
            // 没有可用的 GeckoLib 资源时保持游戏运行，仅跳过装备箱模型。
        } finally {
            poseStack.popPose();
            this.curioRender = false;
        }
    }

    /** 饰品渲染不使用 GeoItemRenderer 面向物品栏时的中心偏移。 */
    @Override
    public void preRender(
        PoseStack poseStack, 
        EquipmentBoxItem animatable, 
        BakedGeoModel model,
        MultiBufferSource buffer, 
        VertexConsumer consumer, 
        boolean isReRender,
        float partialTick, 
        int packedLight, 
        int packedOverlay,
        float red, 
        float green, 
        float blue, 
        float alpha
    ) {
        super.preRender(poseStack, animatable, model, buffer, consumer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        if (curioRender && !isReRender) {
            poseStack.translate(-0.5F, -0.51F, -0.5F);

            // GeckoLib 的装备模型使用与 GeoArmorRenderer 相同的 Y 轴向下坐标系。
            // 装备箱作为 Curios 挎包渲染时必须同步护甲渲染器的翻转变换，否则模型会倒置。
            poseStack.translate(0.0F, 24.0F / 16.0F, 0.0F);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
        }
    }

}
