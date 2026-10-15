package com.yueyingiop.payday.item;

import com.yueyingiop.payday.block.SupplyBlock;
import com.yueyingiop.payday.item.base.SupplyItem;

/** Supplies two magazines' worth of ammunition for the currently held gun. */
public final class AmmoBagItem extends SupplyItem {
    @Override
    protected SupplyBlock.SupplyType supplyType() {
        return SupplyBlock.SupplyType.AMMO_BAG;
    }
}
