package com.yueyingiop.payday.client.model;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.block.SupplyBlock;
import com.yueyingiop.payday.block.SupplyBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SupplyBlockGeoModel extends GeoModel<SupplyBlockEntity> {
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            PayDay.MODID, "animations/supply.animation.json");

    /**
     * 获取模型资源路径。
     * @param animatable 动画对象。
     * @return 对应的执行结果。
     */
    @Override
    public ResourceLocation getModelResource(SupplyBlockEntity animatable) {
        String folder = animatable.supplyType() == SupplyBlock.SupplyType.ARMOR_PLATE ? "armor_plate" : "firstaid_kit";
        String name = animatable.supplyType() == SupplyBlock.SupplyType.ARMOR_PLATE ? "armor_plate" : "firstaid_kit";
        return ResourceLocation.fromNamespaceAndPath(PayDay.MODID,
                "geo/" + folder + "/" + name + animatable.amount() + "x.geo.json");
    }

    /**
     * 获取模型纹理资源路径。
     * @param animatable 动画对象。
     * @return 对应的执行结果。
     */
    @Override
    public ResourceLocation getTextureResource(SupplyBlockEntity animatable) {
        String name = animatable.supplyType() == SupplyBlock.SupplyType.ARMOR_PLATE ? "armor_plate" : "firstaid_kit";
        return ResourceLocation.fromNamespaceAndPath(PayDay.MODID, "textures/block/" + name + ".png");
    }

    /**
     * 获取模型动画资源路径。
     * @param animatable 动画对象。
     * @return 对应的执行结果。
     */
    @Override
    public ResourceLocation getAnimationResource(SupplyBlockEntity animatable) {
        return ANIMATIONS;
    }
}
