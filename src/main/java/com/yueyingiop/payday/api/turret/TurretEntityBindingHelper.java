package com.yueyingiop.payday.api.turret;

import com.yueyingiop.payday.block.TurretBlockEntity;
import com.yueyingiop.payday.entity.TurretEntity;
import com.yueyingiop.payday.init.RegistryEntities;
import net.minecraft.world.phys.AABB;

/** 管理炮台方块实体与其可渲染、可受击实体之间的绑定关系。 */
public final class TurretEntityBindingHelper {
    private TurretEntityBindingHelper() {
    }

    /** 查找当前方块对应的炮台实体。 */
    public static TurretEntity find(TurretBlockEntity turret) {
        if (turret.getLevel() == null) {
            return null;
        }
        if (turret.getTurretEntityId() >= 0
                && turret.getLevel().getEntity(turret.getTurretEntityId()) instanceof TurretEntity entity
                && turret.getBlockPos().equals(entity.getTurretPos())) {
            return entity;
        }
        TurretEntity entity = turret.getLevel().getEntitiesOfClass(TurretEntity.class,
                        new AABB(turret.getBlockPos()))
                .stream()
                .filter(candidate -> turret.getBlockPos().equals(candidate.getTurretPos()))
                .findFirst()
                .orElse(null);
        if (entity != null) {
            turret.setTurretEntityId(entity.getId());
        }
        return entity;
    }

    /** 确保炮台实体已经生成并绑定到方块位置。 */
    public static void ensure(TurretBlockEntity turret) {
        if (find(turret) != null || turret.getLevel() == null) {
            return;
        }
        TurretEntity entity = RegistryEntities.TURRET.get().create(turret.getLevel());
        if (entity == null) {
            return;
        }
        entity.bindTo(turret.getBlockPos(), turret.getBaseYaw());
        turret.getLevel().addFreshEntity(entity);
        turret.setTurretEntityId(entity.getId());
        turret.setChanged();
    }

    /** 在方块移除时销毁绑定的炮台实体。 */
    public static void remove(TurretBlockEntity turret) {
        TurretEntity entity = find(turret);
        if (entity != null) {
            entity.discard();
        }
        turret.setTurretEntityId(-1);
    }
}
