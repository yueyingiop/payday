package com.yueyingiop.payday.client.model;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.item.base.EquipmentBoxItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.model.GeoModel;

/** 装备箱共用的 GeckoLib 模型和按物品 ID 选择的纹理。 */
public final class EquipmentBoxGeoModel extends GeoModel<EquipmentBoxItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            PayDay.MODID, "geo/equipment_box.geo.json");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            PayDay.MODID, "animations/equipment_box.animation.json");

    /**
     * 获取模型资源路径。
     * @param animatable 动画对象。
     * @return 对应的执行结果。
     */
    @Override
    public ResourceLocation getModelResource(EquipmentBoxItem animatable) {
        return MODEL;
    }

    /**
     * 获取模型纹理资源路径。
     * @param animatable 动画对象。
     * @return 对应的执行结果。
     */
    @Override
    public ResourceLocation getTextureResource(EquipmentBoxItem animatable) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(animatable);
        String path = itemId == null ? "equipment_box" : itemId.getPath();
        return ResourceLocation.fromNamespaceAndPath(
                itemId == null ? PayDay.MODID : itemId.getNamespace(),
                "textures/box/" + path + ".png");
    }

    /**
     * 获取模型动画资源路径。
     * @param animatable 动画对象。
     * @return 对应的执行结果。
     */
    @Override
    public ResourceLocation getAnimationResource(EquipmentBoxItem animatable) {
        return ANIMATIONS;
    }
}
