package com.yueyingiop.payday.mixin;

import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import com.yueyingiop.payday.api.PhoneUpgradeAction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 将 PayDay 计算出的弹匣容量接入 TACZ 的统一容量计算入口。
 *
 * <p>扩容值存储在枪械 ItemStack 上，TACZ 的换弹逻辑读取该入口后会按新的容量
 * 计算需求弹药并正常消耗玩家库存，不会由 PayDay 直接生成当前弹药。</p>
 */
@Mixin(value = AttachmentDataUtils.class, remap = false)
public abstract class AttachmentDataUtilsMixin {
    @Inject(method = "getAmmoCountWithAttachment", at = @At("RETURN"), cancellable = true,
            remap = false)
    private static void payday$applyAmmoCapacity(ItemStack gunStack, GunData gunData,
                                                   CallbackInfoReturnable<Integer> cir) {
        CompoundTag tag = gunStack.getTag();
        if (tag == null || !tag.contains(PhoneUpgradeAction.AMMO_CAPACITY, Tag.TAG_INT)) {
            return;
        }

        cir.setReturnValue(Math.max(0, tag.getInt(PhoneUpgradeAction.AMMO_CAPACITY)));
    }
}
