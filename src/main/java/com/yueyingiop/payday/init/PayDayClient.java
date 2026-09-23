package com.yueyingiop.payday.init;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.client.ClientArmorSlotTooltip;
import com.yueyingiop.payday.client.render.ArmorCurioRenderer;
import com.yueyingiop.payday.client.render.EquipmentBoxRenderer;
import com.yueyingiop.payday.client.render.SupplyBlockRenderer;
import com.yueyingiop.payday.client.screen.ArmorScreen;
import com.yueyingiop.payday.client.screen.PayDayConfigScreen;
import com.yueyingiop.payday.tooltip.ArmorSlotTooltip;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import com.yueyingiop.payday.menu.ArmorMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.base.EquipmentBoxItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings({"removal"})
@Mod.EventBusSubscriber(modid = PayDay.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PayDayClient {
    public static final KeyMapping OPEN_ARMOR = new KeyMapping("key.payday.open_armor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories.payday");
    public static final KeyMapping USE_EQUIPMENT_BOX = new KeyMapping("key.payday.use_equipment_box", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.payday");
    public static final KeyMapping MODIFIER = new KeyMapping("key.payday.modifier", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, "key.categories.payday");


    /** 注册客户端菜单界面和护甲饰品渲染器。 */
    /**
     * 执行 <code>clientSetup</code> 对应的操作。
     * @param event 参数值。
     */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(PayDayConfigScreen::new));
        event.enqueueWork(() -> {
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                if (item instanceof ArmorCurioItem armorCurio) {
                    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(armorCurio);
                    if (itemId != null) {
                        CuriosRendererRegistry.register(armorCurio,
                                () -> new ArmorCurioRenderer(itemId));
                    }
                } else if (item instanceof EquipmentBoxItem equipmentBox) {
                    CuriosRendererRegistry.register(equipmentBox, EquipmentBoxRenderer::new);
                }
            }
            MenuScreens.register(ArmorMenu.TYPE.get(), ArmorScreen::new);
        });
    }

    /** 注册 PayDay 的客户端按键。 */
    /**
     * 执行 <code>registerKeys</code> 对应的操作。
     * @param event 参数值。
     */
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ARMOR);
        event.register(USE_EQUIPMENT_BOX);
        event.register(MODIFIER);
    }

    /** 注册补给方块实体的客户端渲染器。 */
    /**
     * 执行 <code>registerBlockRenderers</code> 对应的操作。
     * @param event 参数值。
     */
    @SubscribeEvent
    public static void registerBlockRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RegistryBlockEntities.SUPPLY.get(), SupplyBlockRenderer::new);
    }

    /** 注册护甲饰品 Tooltip 图标的客户端绘制工厂。 */
    /**
     * 执行 <code>registerTooltipComponents</code> 对应的操作。
     * @param event 参数值。
     */
    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ArmorSlotTooltip.class, ClientArmorSlotTooltip::new);
    }

    /** 注册仅供物品栏显示的装备箱平面图标模型。 */
    /**
     * 执行 <code>registerEquipmentBoxGuiModels</code> 对应的操作。
     * @param event 参数值。
     */
    @SubscribeEvent
    public static void registerEquipmentBoxGuiModels(ModelEvent.RegisterAdditional event) {
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (!(item instanceof EquipmentBoxItem)) {
                continue;
            }
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId != null) {
                event.register(new ModelResourceLocation(
                        ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), itemId.getPath() + "_gui"),
                        "inventory"));
            }
        }
    }

}
