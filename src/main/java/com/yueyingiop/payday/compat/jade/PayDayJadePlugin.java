package com.yueyingiop.payday.compat.jade;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.api.TurretTemperature;
import com.yueyingiop.payday.api.TurretFluid;
import com.yueyingiop.payday.api.TurretFluidManager;
import com.yueyingiop.payday.block.TurretBlock;
import com.yueyingiop.payday.block.TurretBlockEntity;
import com.yueyingiop.payday.entity.TurretEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Jade 集成：显示炮台当前温度和温度上限。 */
@WailaPlugin
public final class PayDayJadePlugin implements IWailaPlugin {
    private static final TurretTemperatureProvider TEMPERATURE_PROVIDER =
            new TurretTemperatureProvider();
    private static final TurretBlockTemperatureProvider BLOCK_TEMPERATURE_PROVIDER =
            new TurretBlockTemperatureProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(TEMPERATURE_PROVIDER, TurretEntity.class);
        registration.registerBlockDataProvider(BLOCK_TEMPERATURE_PROVIDER, TurretBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(TEMPERATURE_PROVIDER, TurretEntity.class);
        registration.registerBlockComponent(BLOCK_TEMPERATURE_PROVIDER, TurretBlock.class);
    }

    private static final class TurretTemperatureProvider
            implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
        private static final String TEMPERATURE = "Temperature";
        private static final String MAXIMUM = "Maximum";
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                PayDay.MODID, "turret_temperature");

        @Override
        public void appendServerData(CompoundTag data, EntityAccessor accessor) {
            if (!(accessor.getEntity() instanceof TurretEntity turret)
                    || turret.getTurret() == null) {
                return;
            }
            data.putDouble(TEMPERATURE, TurretTemperature.get(turret.getTurret()));
            data.putDouble(MAXIMUM, turret.getMaximumTemperature());
            putFluidData(data, "Coolant", turret.getTurret().getCoolantFluid());
            putFluidData(data, "Overload", turret.getTurret().getOverloadFluid());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor,
                                  IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(TEMPERATURE) || !data.contains(MAXIMUM)) {
                return;
            }
            tooltip.add(Component.translatable("jade.payday.turret_temperature",
                    data.getDouble(TEMPERATURE), data.getDouble(MAXIMUM)));
            appendFluidTooltip(tooltip, data, "Coolant", "jade.payday.turret_coolant");
            appendFluidTooltip(tooltip, data, "Overload", "jade.payday.turret_overload_fluid");
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private static final class TurretBlockTemperatureProvider
            implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                PayDay.MODID, "turret_temperature_block");

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof TurretBlockEntity turret)) {
                return;
            }
            data.putDouble("Temperature", TurretTemperature.get(turret));
            data.putDouble("Maximum", turret.getMaximumTemperature());
            putFluidData(data, "Coolant", turret.getCoolantFluid());
            putFluidData(data, "Overload", turret.getOverloadFluid());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor,
                                  IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("Temperature") || !data.contains("Maximum")) {
                return;
            }
            tooltip.add(Component.translatable("jade.payday.turret_temperature",
                    data.getDouble("Temperature"), data.getDouble("Maximum")));
            appendFluidTooltip(tooltip, data, "Coolant", "jade.payday.turret_coolant");
            appendFluidTooltip(tooltip, data, "Overload", "jade.payday.turret_overload_fluid");
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private static void putFluidData(CompoundTag data, String prefix, FluidStack stack) {
        data.putBoolean(prefix + "Present", !stack.isEmpty());
        data.putInt(prefix + "Amount", stack.getAmount());
        if (!stack.isEmpty()) {
            ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(stack.getFluid());
            TurretFluid definition = TurretFluidManager.get(stack.getFluid());
            if (id != null) {
                data.putString(prefix + "Name", stack.getDisplayName().getString());
            }
            if (definition != null) {
                data.putString(prefix + "Type", definition.type().translationKey());
            }
        }
    }

    private static void appendFluidTooltip(ITooltip tooltip, CompoundTag data,
                                           String prefix, String translationKey) {
        if (!data.getBoolean(prefix + "Present")) {
            tooltip.add(Component.translatable(translationKey,
                    Component.translatable("gui.payday.turret_fluid_empty"),
                    Component.translatable("turret_fluid.payday.empty"), 0, TurretBlockEntity.FLUID_CAPACITY));
            return;
        }
        String fluidName = data.getString(prefix + "Name");
        String typeKey = data.getString(prefix + "Type");
        tooltip.add(Component.translatable(translationKey, fluidName,
                Component.translatable(typeKey), data.getInt(prefix + "Amount"),
                TurretBlockEntity.FLUID_CAPACITY));
    }
}
