package com.yueyingiop.payday.item.base;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.yueyingiop.payday.client.render.EquipmentBoxRenderer;
import com.yueyingiop.payday.event.server.ArmorCurioEvents;
import com.yueyingiop.payday.item.ClothesBoxItem;
import com.yueyingiop.payday.item.PhoneItem;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;
/** Base item for the single-slot equipment_box curios. */
public abstract class EquipmentBoxItem extends Item implements ICurioItem, GeoItem {
    public static final String CONTENTS = "payday_box_contents";
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    protected EquipmentBoxItem() {
        super(new Item.Properties().stacksTo(1));
        GeoItem.registerSyncedAnimatable(this);
    }

    /**
     * 获取容量。
     * @return 对应的执行结果。
     */
    public abstract int capacity();

    /**
     * 判断是否接收该物品。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    public abstract boolean accepts(ItemStack stack);

    /**
     * 统计装备箱内保存的物品数量。
     * @param box 对应参数。
     * @return 对应的执行结果。
     */
    public int storedCount(ItemStack box) {
        int total = 0;
        ListTag list = contents(box);
        for (int i = 0; i < list.size(); i++) {
            total += ItemStack.of(list.getCompound(i)).getCount();
        }
        return total;
    }

    /**
     * 将一个符合类型的物品存入装备箱。
     * @param box 对应参数。
     * @param source 来源。
     * @return 对应的执行结果。
     */
    public boolean insertOne(ItemStack box, ItemStack source) {
        if (!accepts(source) || storedCount(box) >= capacity()) {
            return false;
        }

        ListTag list = contents(box);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stored = ItemStack.of(list.getCompound(i));
            if (ItemStack.isSameItemSameTags(stored, source) && stored.getCount() < stored.getMaxStackSize()) {
                stored.grow(1);
                list.set(i, stored.save(new CompoundTag()));
                box.getOrCreateTag().put(CONTENTS, list);
                return true;
            }
        }

        ItemStack copy = source.copyWithCount(1);
        list.add(copy.save(new CompoundTag()));
        box.getOrCreateTag().put(CONTENTS, list);
        return true;
    }

    /**
     * 从装备箱中取出一个物品。
     * @param box 对应参数。
     * @return 对应的执行结果。
     */
    public ItemStack takeOne(ItemStack box) {
        ListTag list = contents(box);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stored = ItemStack.of(list.getCompound(i));
            if (stored.isEmpty()) {
                continue;
            }

            ItemStack result = stored.copyWithCount(1);
            stored.shrink(1);
            if (stored.isEmpty()) {
                list.remove(i);
            } else {
                list.set(i, stored.save(new CompoundTag()));
            }
            box.getOrCreateTag().put(CONTENTS, list);
            return result;
        }
        return ItemStack.EMPTY;
    }

    /**
     * 读取装备箱内物品但不取出。
     * @param box 对应参数。
     * @return 对应的执行结果。
     */
    public ItemStack peek(ItemStack box) {
        ListTag list = contents(box);
        return list.isEmpty() ? ItemStack.EMPTY : ItemStack.of(list.getCompound(0));
    }

    /**
     * 处理装备箱被拖放到其他物品槽位。
     * @param box 对应参数。
     * @param slot 槽位。
     * @param action 交互操作。
     * @param player 玩家。
     * @return 对应的执行结果。
     */
    @Override
    public boolean overrideStackedOnOther(ItemStack box, Slot slot, ClickAction action, Player player) {
        if (box.getCount() != 1 || action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false;
        }

        // 鼠标当前拿着装备箱，将点击槽位中的一个物品存入箱内。
        ItemStack clicked = slot.getItem();
        if (clicked.isEmpty() || !accepts(clicked) || storedCount(box) >= capacity()) {
            return true;
        }

        ItemStack taken = slot.safeTake(1, 1, player);
        if (!taken.isEmpty()) {
            if (!insertOne(box, taken)) {
                slot.safeInsert(taken);
            }
        }
        return true;
    }

    /**
     * 处理其他物品被拖放到装备箱。
     * @param box 对应参数。
     * @param carried 鼠标携带物。
     * @param slot 槽位。
     * @param action 交互操作。
     * @param player 玩家。
     * @param carriedSlotAccess 对应参数。
     * @return 对应的执行结果。
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack box, ItemStack carried, Slot slot,
                                            ClickAction action, Player player,
                                            net.minecraft.world.entity.SlotAccess carriedSlotAccess) {
        if (box.getCount() != 1 || action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false;
        }

        // 装备箱位于点击的背包槽位中，鼠标当前拿着待存入物品。
        if (carried.isEmpty()) {
            ItemStack stored = takeOne(box);
            if (!stored.isEmpty()) {
                carriedSlotAccess.set(stored);
            }
            return true;
        }

        if (!accepts(carried) || storedCount(box) >= capacity()) {
            return true;
        }

        if (insertOne(box, carried)) {
            carried.shrink(1);
        }
        return true;
    }

    /**
     * 读取装备箱 NBT 中的物品列表。
     * @param box 对应参数。
     * @return 对应的执行结果。
     */
    protected static ListTag contents(ItemStack box) {
        return box.hasTag() ? box.getTag().getList(CONTENTS, Tag.TAG_COMPOUND) : new ListTag();
    }

    /**
     * 检查物品是否可放入指定饰品槽。
     * @param slotContext 槽位上下文。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        // 衣帽箱只有在玩家安装“特殊升级-衣帽间”时才允许装备。
        return "equipment_box".equals(slotContext.identifier())
                && (!(stack.getItem() instanceof ClothesBoxItem)
                || PhoneItem.hasInstalledUpgrade(slotContext.entity(), "special_wardrobe"));
    }

    /**
     * 检查物品是否可通过使用快捷装备。
     * @param slotContext 槽位上下文。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        // 右键快捷装备同样受“特殊升级-衣帽间”限制。
        return "equipment_box".equals(slotContext.identifier())
                && (!(stack.getItem() instanceof ClothesBoxItem)
                || PhoneItem.hasInstalledUpgrade(slotContext.entity(), "special_wardrobe"))
                && !ArmorCurioEvents.isEquipmentBoxChangeLocked(slotContext.entity(),
                        slotContext.identifier(), slotContext.index());
    }

    /**
     * 注册装备箱的客户端渲染扩展。
     * @param consumer 对应参数。
     */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private EquipmentBoxRenderer renderer;

            /**
             * 获取装备箱自定义物品渲染器。
             * @return 对应的执行结果。
             */
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new EquipmentBoxRenderer();
                }
                return renderer;
            }
        });
    }

    /**
     * 注册装备箱的动画控制器；当前模型不包含动画。
     * @param controllers 动画控制器。
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 装备箱模型没有动画控制器。
    }

    /**
     * 获取装备箱的动画实例缓存。
     * @return 对应的执行结果。
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    /**
     * 向物品提示添加护甲信息。
     * @param stack 物品堆。
     * @param level 世界。
     * @param tooltip 对应参数。
     * @param flag 对应参数。
     */
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.payday.box_contents", storedCount(stack), capacity())
                .withStyle(ChatFormatting.GRAY));
    }
}
