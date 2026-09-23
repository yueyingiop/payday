package com.yueyingiop.payday.api.helper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Shared access helpers for Curios inventories. */
public final class CurioHelper {
    public static final String ARMOR_SLOT = "armor";
    public static final String EQUIPMENT_BOX_SLOT = "equipment_box";

    private CurioHelper() {
    }

    /** Returns non-empty stacks in one Curios slot group. */
    public static List<ItemStack> stacks(LivingEntity entity, String identifier) {
        List<ItemStack> result = new ArrayList<>();
        for (SlotStack slot : slots(entity, identifier)) {
            if (!slot.stack().isEmpty()) {
                result.add(slot.stack());
            }
        }
        return result;
    }

    /** Returns every slot, including empty slots, with its inventory index. */
    public static List<SlotStack> slots(LivingEntity entity, String identifier) {
        List<SlotStack> result = new ArrayList<>();
        CuriosApi.getCuriosInventory(entity).ifPresent(handler ->
                handler.getStacksHandler(identifier).ifPresent(stacks -> {
                    for (int i = 0; i < stacks.getStacks().getSlots(); i++) {
                        result.add(new SlotStack(i, stacks.getStacks().getStackInSlot(i)));
                    }
                }));
        return result;
    }

    /** Returns stacks from all registered Curios slot groups. */
    public static List<ItemStack> allStacks(LivingEntity entity) {
        List<ItemStack> result = new ArrayList<>();
        CuriosApi.getCuriosInventory(entity).ifPresent(handler ->
                handler.getCurios().values().forEach(stacks -> {
                    for (int i = 0; i < stacks.getStacks().getSlots(); i++) {
                        result.add(stacks.getStacks().getStackInSlot(i));
                    }
                }));
        return result;
    }

    /**
     * 判断槽位中是否存在匹配物品。
     * @param entity 实体。
     * @param identifier 槽位标识。
     * @param predicate 筛选条件。
     * @return 对应的执行结果。
     */
    public static boolean hasStack(LivingEntity entity, String identifier, Predicate<ItemStack> predicate) {
        return stacks(entity, identifier).stream().anyMatch(predicate);
    }

    /**
     * 查找第一个匹配物品。
     * @param entity 实体。
     * @param identifier 槽位标识。
     * @param predicate 筛选条件。
     * @return 对应的执行结果。
     */
    public static Optional<SlotResult> findFirst(LivingEntity entity, String identifier,
                                                  Predicate<ItemStack> predicate) {
        return slots(entity, identifier).stream()
                .filter(slot -> !slot.stack().isEmpty() && predicate.test(slot.stack()))
                .findFirst()
                .map(slot -> new SlotResult(
                        new SlotContext(identifier, entity, slot.index(), false, true), slot.stack()));
    }

    /**
     * 获取指定槽位物品。
     * @param entity 实体。
     * @param identifier 槽位标识。
     * @param index 索引。
     * @return 对应的执行结果。
     */
    public static ItemStack stackAt(LivingEntity entity, String identifier, int index) {
        final ItemStack[] result = {ItemStack.EMPTY};
        CuriosApi.getCuriosInventory(entity).ifPresent(handler ->
                handler.getStacksHandler(identifier).ifPresent(stacks -> {
                    if (index >= 0 && index < stacks.getStacks().getSlots()) {
                        result[0] = stacks.getStacks().getStackInSlot(index);
                    }
                }));
        return result[0];
    }

    /**
     * 设置指定槽位物品。
     * @param entity 实体。
     * @param identifier 槽位标识。
     * @param index 索引。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    public static boolean setStack(LivingEntity entity, String identifier, int index, ItemStack stack) {
        var handler = CuriosApi.getCuriosInventory(entity).orElse(null);
        if (handler == null) {
            return false;
        }
        handler.setEquippedCurio(identifier, index, stack);
        return true;
    }

    public record SlotStack(int index, ItemStack stack) {
    }
}
