package com.yueyingiop.payday.item;

import com.yueyingiop.payday.block.SupplyBlock;
import com.yueyingiop.payday.item.base.SupplyItem;

public final class FirstAidKitItem extends SupplyItem {
    /**
     * 获取补给类型。
     * @return 对应的执行结果。
     */
    @Override
    protected SupplyBlock.SupplyType supplyType() {
        return SupplyBlock.SupplyType.FIRST_AID;
    }
}
