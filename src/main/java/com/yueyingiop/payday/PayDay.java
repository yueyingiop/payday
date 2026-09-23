package com.yueyingiop.payday;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.yueyingiop.payday.event.server.ArmorCurioEvents;
import com.yueyingiop.payday.event.server.ArmorDamageEvents;
import com.yueyingiop.payday.event.server.SupplyEvents;
import com.yueyingiop.payday.event.server.ExposureRiskEvents;
import com.yueyingiop.payday.event.server.CommandEvents;
import com.yueyingiop.payday.init.RegistryItems;
import com.yueyingiop.payday.init.RegistryBlocks;
import com.yueyingiop.payday.init.RegistryBlockEntities;
import com.yueyingiop.payday.menu.ArmorMenu;
import com.yueyingiop.payday.network.PayDayNetwork;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(PayDay.MODID)
public class PayDay
{
    public static final String MODID = "payday";
    public static final Logger LOGGER = LogManager.getLogger(PayDay.MODID);
    
    // 创造物品栏
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    
    public static final RegistryObject<CreativeModeTab> PAYDAY_TAB = CREATIVE_MODE_TABS.register("payday_tab", () -> CreativeModeTab.builder()
        .withTabsBefore(CreativeModeTabs.COMBAT)
        .title(Component.translatable("itemGroup.payday"))
        .icon(() -> RegistryItems.SUIT.get().getDefaultInstance())
        .displayItems((parameters, output) -> {
            RegistryItems.ITEMS.getEntries().stream().map(RegistryObject::get).forEach(output::accept);
        })
        .build()
        
    );

    public PayDay(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        CREATIVE_MODE_TABS.register(modEventBus);
        RegistryItems.ITEMS.register(modEventBus);
        RegistryBlocks.BLOCKS.register(modEventBus);
        RegistryBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ArmorMenu.MENUS.register(modEventBus);
        PayDayNetwork.register();
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, Config.SPEC);
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, Config.CLIENT_SPEC,
                "payday-client.toml");

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ArmorDamageEvents.class);
        MinecraftForge.EVENT_BUS.register(ArmorCurioEvents.class);
        MinecraftForge.EVENT_BUS.register(SupplyEvents.class);
        MinecraftForge.EVENT_BUS.register(ExposureRiskEvents.class);
        MinecraftForge.EVENT_BUS.register(CommandEvents.class);
    }

}
