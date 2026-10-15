package com.yueyingiop.payday.item;

import com.yueyingiop.payday.block.TurretBlock;
import net.minecraft.world.item.BlockItem;

/** 无状态炮台物品，炮台运行数据只存在于世界中的方块实体内。 */
public final class TurretItem extends BlockItem {
    public TurretItem(TurretBlock block, Properties properties) {
        super(block, properties);
    }
}
