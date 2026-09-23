package com.yueyingiop.payday.mixin;

import com.tacz.guns.resource.index.CommonBlockIndex;
import com.tacz.guns.resource.pojo.BlockIndexPOJO;
import com.tacz.guns.resource.pojo.data.block.BlockData;
import com.tacz.guns.resource.pojo.data.block.TabConfig;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * 为 TACZ 附件工作台的数据索引追加 PayDay 配方标签页。
 */
@Pseudo
@Mixin(targets = "com.tacz.guns.resource.index.CommonBlockIndex", remap = false)
public abstract class CommonBlockIndexMixin {
    private static final ResourceLocation ATTACHMENT_WORKBENCH_DATA =
            ResourceLocation.fromNamespaceAndPath("tacz", "attachment_workbench");
    private static final ResourceLocation PAYDAY_MISC_TAB =
            ResourceLocation.fromNamespaceAndPath("payday", "misc");

    /**
     * 在 TACZ 完成工作台数据索引创建后，补充 PayDay 标签页。
     */
    @Inject(method = "getInstance", at = @At("RETURN"))
    private static void payday$addAttachmentTab(
        BlockIndexPOJO pojo,
        CallbackInfoReturnable<CommonBlockIndex> cir
    ) {
        if (pojo == null || !ATTACHMENT_WORKBENCH_DATA.equals(pojo.getData())) return;

        CommonBlockIndex blockIndex = cir.getReturnValue();
        if (blockIndex == null) return;

        BlockData blockData = blockIndex.getData();
        List<TabConfig> tabs = new ArrayList<>(blockData.getTabs());
        boolean alreadyPresent = tabs.stream().anyMatch(tab -> PAYDAY_MISC_TAB.equals(tab.id()));
        if (!alreadyPresent) {
            tabs.add(
                new TabConfig(
                    PAYDAY_MISC_TAB,
                    "payday.type.misc.name",
                    com.yueyingiop.payday.init.RegistryItems.RESISTANCE_SLOT.get().getDefaultInstance()
                )
            );
            ((BlockDataAccessor) (Object) blockData).payday$setTabs(tabs);
        }
    }
}
