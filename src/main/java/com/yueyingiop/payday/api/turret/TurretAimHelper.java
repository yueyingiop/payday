package com.yueyingiop.payday.api.turret;

import com.yueyingiop.payday.block.TurretBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/** 炮台的目标筛选、瞄准计算和枪口坐标辅助类。 */
public final class TurretAimHelper {
    private static final double MODEL_UNIT = 1.0D / 16.0D;
    private static final Vec3 LASER_MUZZLE_LOCAL = new Vec3(-MODEL_UNIT, 6.5D * MODEL_UNIT,
            9.0D * MODEL_UNIT);
    private static final Vec3 IN_SUPPORT_PIVOT = new Vec3(-2.0D * MODEL_UNIT,
            MODEL_UNIT, 7.0D * MODEL_UNIT);
    private static final Vec3 OUT_SUPPORT_PIVOT = new Vec3(0.0D, 3.0D * MODEL_UNIT,
            -7.0D * MODEL_UNIT);
    private static final Vec3 HEAD_PIVOT = new Vec3(0.0D, 3.0D * MODEL_UNIT,
            7.0D * MODEL_UNIT);

    private TurretAimHelper() {
    }

    /** 根据方块状态获取旧存档炮台的默认水平朝向。 */
    public static float baseYaw(BlockState state) {
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return Mth.wrapDegrees(direction.toYRot() + 180.0F);
    }

    /** 查找当前炮台攻击范围内最近的可攻击敌对生物。 */
    public static LivingEntity findTarget(TurretBlockEntity turret) {
        if (turret.getLevel() == null) {
            return null;
        }
        AABB searchArea = new AABB(turret.getBlockPos()).inflate(turret.getAttackRange());
        LivingEntity currentTarget = turret.getTargetEntity();
        if (currentTarget != null && currentTarget.isAlive() && currentTarget instanceof Enemy
                && !currentTarget.isInvulnerable() && isInRange(turret, currentTarget)
                && canTrack(turret, currentTarget)) {
            return currentTarget;
        }

        return turret.getLevel().getEntitiesOfClass(LivingEntity.class, searchArea,
                        entity -> entity.isAlive() && entity instanceof Enemy
                                && !entity.isInvulnerable() && canTrack(turret, entity))
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(
                        turret.getBlockPos().getX() + 0.5D,
                        turret.getBlockPos().getY() + 0.5D,
                        turret.getBlockPos().getZ() + 0.5D)))
                .orElse(null);
    }

    /** 判断目标是否仍然位于炮台攻击范围内。 */
    private static boolean isInRange(TurretBlockEntity turret, LivingEntity target) {
        double range = turret.getAttackRange();
        return target.distanceToSqr(turret.getBlockPos().getX() + 0.5D,
                turret.getBlockPos().getY() + 0.5D,
                turret.getBlockPos().getZ() + 0.5D) <= range * range;
    }

    /** 让炮台朝目标逐 tick 平滑转动。 */
    public static void updateAim(TurretBlockEntity turret, LivingEntity target) {
        float targetYaw = targetYaw(turret, target);
        float targetPitch = targetPitch(turret, target, targetYaw);
        float yaw = approachAngle(turret.getAimYaw(), targetYaw, 12.0F);
        float pitch = Mth.clamp(approach(turret.getAimPitch(), targetPitch, 8.0F),
                turret.getMinAimPitch(), turret.getMaxAimPitch());
        turret.setAimInternal(yaw, pitch);
    }

    /** 判断炮台当前枪口是否已经对准目标。 */
    public static boolean isAimedAt(TurretBlockEntity turret, LivingEntity target) {
        return Math.abs(Mth.wrapDegrees(targetYaw(turret, target) - turret.getAimYaw())) <= 3.0F
                && Math.abs(targetPitch(turret, target, targetYaw(turret, target))
                - turret.getAimPitch()) <= 3.0F;
    }

    /** 根据炮台位置生成固定攻击错位，避免多个炮台同 tick 触发目标无敌帧。 */
    public static int attackStagger(TurretBlockEntity turret) {
        int interval = Math.max(1, com.yueyingiop.payday.Config.TURRET_ATTACK_INTERVAL_TICKS.get());
        return Math.floorMod(Long.hashCode(turret.getBlockPos().asLong()), interval);
    }

    /** 返回当前枪口在世界中的位置。 */
    public static Vec3 muzzlePosition(TurretBlockEntity turret) {
        return muzzlePosition(turret, turret.getAimYaw(), turret.getAimPitch());
    }

    /** 返回指定瞄准角度下的枪口位置，用于计算弹道和目标俯仰角。 */
    public static Vec3 muzzlePosition(TurretBlockEntity turret, float yawDegrees, float pitchDegrees) {
        Vec3 local = rotateX(LASER_MUZZLE_LOCAL, HEAD_PIVOT, 60.0D - pitchDegrees)
                .add(0.0D, -5.0D * MODEL_UNIT, 0.0D);
        local = rotateX(local, OUT_SUPPORT_PIVOT, -120.0D);
        local = rotateX(local, IN_SUPPORT_PIVOT, 60.0D)
                .add(0.0D, 0.0D, -6.0D * MODEL_UNIT);

        double yaw = Math.toRadians(yawDegrees);
        double worldX = Math.cos(yaw) * local.x - Math.sin(yaw) * local.z;
        double worldZ = Math.sin(yaw) * local.x + Math.cos(yaw) * local.z;
        return new Vec3(turret.getBlockPos().getX() + 0.5D + worldX,
                turret.getBlockPos().getY() + local.y,
                turret.getBlockPos().getZ() + 0.5D + worldZ);
    }

    /** 返回当前枪口的世界方向。 */
    public static Vec3 muzzleDirection(TurretBlockEntity turret) {
        double yaw = Math.toRadians(turret.getAimYaw());
        double pitch = Math.toRadians(turret.getAimPitch());
        double horizontal = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * horizontal, Math.sin(pitch),
                Math.cos(yaw) * horizontal).normalize();
    }

    /** 判断目标是否位于炮台的水平和垂直可追踪范围内。 */
    private static boolean canTrack(TurretBlockEntity turret, LivingEntity target) {
        float yawDifference = Mth.wrapDegrees(targetYaw(turret, target) - turret.getBaseYaw());
        float pitch = targetPitch(turret, target, targetYaw(turret, target));
        return Math.abs(yawDifference) <= turret.getYawAngle()
                && pitch >= turret.getMinAimPitch() && pitch <= turret.getMaxAimPitch();
    }

    /** 计算目标相对于炮台的水平角度。 */
    private static float targetYaw(TurretBlockEntity turret, LivingEntity target) {
        Vec3 muzzle = muzzlePosition(turret);
        return (float) (Mth.atan2(target.getZ() - muzzle.z, target.getX() - muzzle.x)
                * Mth.RAD_TO_DEG) - 90.0F;
    }

    /** 迭代计算目标相对于枪口的垂直角度。 */
    private static float targetPitch(TurretBlockEntity turret, LivingEntity target, float yaw) {
        float pitch = turret.getAimPitch();
        for (int index = 0; index < 4; index++) {
            Vec3 muzzle = muzzlePosition(turret, yaw, pitch);
            double dx = target.getX() - muzzle.x;
            double dz = target.getZ() - muzzle.z;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            double targetCenterY = target.getY() + target.getBbHeight() * 0.5D;
            pitch = (float) (Mth.atan2(targetCenterY - muzzle.y, horizontalDistance)
                    * Mth.RAD_TO_DEG);
        }
        return pitch;
    }

    /** 按最大步长调整角度，处理跨越 -180/180 度的情况。 */
    private static float approachAngle(float current, float target, float maxStep) {
        return current + Mth.clamp(Mth.wrapDegrees(target - current), -maxStep, maxStep);
    }

    /** 按最大步长调整普通浮点数。 */
    private static float approach(float current, float target, float maxStep) {
        return current + Mth.clamp(target - current, -maxStep, maxStep);
    }

    /** 绕指定支点旋转一个局部坐标。 */
    private static Vec3 rotateX(Vec3 point, Vec3 pivot, double degrees) {
        double angle = Math.toRadians(degrees);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double y = point.y - pivot.y;
        double z = point.z - pivot.z;
        return new Vec3(point.x, pivot.y + y * cos - z * sin,
                pivot.z + y * sin + z * cos);
    }
}
