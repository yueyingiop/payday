package com.yueyingiop.payday.client.model;

import com.yueyingiop.payday.item.ArmorCurioItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** GeckoLib model mapping for the armor curio assets. */
public final class ArmorGeoModel extends GeoModel<ArmorCurioItem> {
    private final ResourceLocation itemId;

    public ArmorGeoModel(ResourceLocation itemId) {
        this.itemId = itemId;
    }

    /**
     * 获取模型资源路径。
     * @param animatable 动画对象。
     * @return 对应的执行结果。
     */
    @Override
    public ResourceLocation getModelResource(ArmorCurioItem animatable) {
        return resource("geo/" + itemId.getPath() + ".geo.json");
    }

    /**
     * 获取模型纹理资源路径。
     * @param animatable 动画对象。
     * @return 对应的执行结果。
     */
    @Override
    public ResourceLocation getTextureResource(ArmorCurioItem animatable) {
        return resource("textures/armor/" + itemId.getPath() + ".png");
    }

    /**
     * 获取模型动画资源路径。
     * @param animatable 动画对象。
     * @return 对应的执行结果。
     */
    @Override
    public ResourceLocation getAnimationResource(ArmorCurioItem animatable) {
        return resource("animations/armor/" + itemId.getPath() + ".animation.json");
    }

    /**
     * 创建资源标识。
     * @param path 对应参数。
     * @return 对应的执行结果。
     */
    private ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), path);
    }
}
