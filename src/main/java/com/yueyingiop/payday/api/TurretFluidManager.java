package com.yueyingiop.payday.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 管理炮台流体行为定义，并提供默认水和岩浆配置。 */
public final class TurretFluidManager {
    private static final Map<ResourceLocation, TurretFluid> DEFINITIONS = new ConcurrentHashMap<>();
    private static boolean defaultsRegistered;

    private TurretFluidManager() {
    }

    /** 注册或覆盖一种炮台流体行为。 */
    public static void register(TurretFluid fluid) {
        if (fluid == null) {
            throw new IllegalArgumentException("Turret fluid must not be null");
        }
        DEFINITIONS.put(fluid.fluidId(), fluid);
    }

    /** 使用流体标识注册一种炮台流体行为，方便 KubeJS 调用。 */
    public static TurretFluid register(String fluidId, TurretFluid fluid) {
        ResourceLocation id = ResourceLocation.tryParse(fluidId == null ? "" : fluidId.trim());
        if (id == null) {
            throw new IllegalArgumentException("Invalid turret fluid id: " + fluidId);
        }
        if (fluid == null || !id.equals(fluid.fluidId())) {
            throw new IllegalArgumentException("The registered fluid id must match the fluid definition");
        }
        register(fluid);
        return fluid;
    }

    /** 获取流体的行为定义；未注册时返回 null。 */
    public static TurretFluid get(ResourceLocation fluidId) {
        return fluidId == null ? null : DEFINITIONS.get(fluidId);
    }

    /** 根据 Forge 流体对象获取行为定义。 */
    public static TurretFluid get(net.minecraft.world.level.material.Fluid fluid) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        return get(id);
    }

    /**
     * 获取所有已注册的炮台流体定义。
     *
     * <p>返回值是按流体 ID 排序的独立快照，调用方不能修改管理器内部注册表。</p>
     *
     * @return 所有炮台流体定义
     */
    public static List<TurretFluid> all() {
        List<TurretFluid> result = new ArrayList<>(DEFINITIONS.values());
        result.sort(Comparator.comparing(fluid -> fluid.fluidId().toString()));
        return List.copyOf(result);
    }

    /** 注册默认流体。水每 50 mB 抵消 1 点温度，岩浆提供 40 点温度上限。 */
    public static void registerDefaults() {
        if (defaultsRegistered) {
            return;
        }
        defaultsRegistered = true;
        register(TurretFluid.builder()
                .fluid(ResourceLocation.fromNamespaceAndPath("minecraft", "water"))
                .type(TurretFluid.Type.CONSUMABLE_COOLANT)
                .mbPerTemperature(50.0D)
                .build());
        register(TurretFluid.builder()
                .fluid(ResourceLocation.fromNamespaceAndPath("minecraft", "lava"))
                .type(TurretFluid.Type.OVERLOAD)
                .maximumTemperatureBonus(40.0D)
                .consumptionInterval(20)
                .consumptionAmount(1)
                .build());
    }
}
