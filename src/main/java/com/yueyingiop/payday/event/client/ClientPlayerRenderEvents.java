package com.yueyingiop.payday.event.client;

import com.yueyingiop.payday.PayDay;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.IdentityHashMap;
import java.util.Map;

/** 处理佩戴护甲饰品时隐藏原版玩家护甲模型。 */
@Mod.EventBusSubscriber(modid = PayDay.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientPlayerRenderEvents {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET,
            EquipmentSlot.LEGS,
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD
    };
    private static final Map<Player, ItemStack[]> HIDDEN_ARMOR = new IdentityHashMap<>();

    private ClientPlayerRenderEvents() {
    }

    /**
     * 在玩家模型渲染前暂时移除原版护甲，避免与护甲饰品模型重叠。
     *
     * @param event 玩家模型渲染前事件
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforePlayerRender(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!ClientEventUtil.hasArmorCurio(player) || HIDDEN_ARMOR.containsKey(player)) {
            return;
        }

        ItemStack[] armor = new ItemStack[ARMOR_SLOTS.length];
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            armor[i] = player.getInventory().armor.get(i);
            player.getInventory().armor.set(i, ItemStack.EMPTY);
        }
        HIDDEN_ARMOR.put(player, armor);
    }

    /**
     * 在玩家模型渲染结束后恢复原版护甲物品，避免改变玩家真实背包数据。
     *
     * @param event 玩家模型渲染结束事件
     */
    @SubscribeEvent
    public static void afterPlayerRender(RenderPlayerEvent.Post event) {
        ItemStack[] armor = HIDDEN_ARMOR.remove(event.getEntity());
        if (armor == null) {
            return;
        }

        for (int i = 0; i < armor.length; i++) {
            event.getEntity().getInventory().armor.set(i, armor[i]);
        }
    }
}
