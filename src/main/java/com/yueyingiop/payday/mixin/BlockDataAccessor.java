package com.yueyingiop.payday.mixin;

import com.tacz.guns.resource.pojo.data.block.TabConfig;
import com.tacz.guns.resource.pojo.data.block.BlockData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * 提供对 TACZ 工作台标签列表的最小访问接口。
 */
@Mixin(value = BlockData.class, remap = false)
public interface BlockDataAccessor {
    /**
     * 替换工作台的标签列表。
     *
     * @param tabs 新的标签列表
     */
    @Accessor("tabs")
    void payday$setTabs(List<TabConfig> tabs);
}
