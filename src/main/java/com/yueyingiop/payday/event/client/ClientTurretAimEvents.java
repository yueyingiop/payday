package com.yueyingiop.payday.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.block.TurretBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** 玩家手持配置物品时绘制炮台的俯仰判断区域。 */
@Mod.EventBusSubscriber(modid = PayDay.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientTurretAimEvents {
    private static final double DISPLAY_DISTANCE = 48.0D;
    private static final int SEGMENTS = 24;
    private static final int PITCH_STEP = 15;
    private static final int CONE_RADIAL_STEPS = 4;
    private static final double AIM_ORIGIN_HEIGHT = 1.0D;
    private static final int BOUNDARY_COLOR = 0xFFFFB52E;
    private static final int MERIDIAN_COLOR = 0xFF55D6FF;
    private static final int AIM_COLOR = 0xFF65E572;

    private ClientTurretAimEvents() {
    }

    @SubscribeEvent
    public static void renderAimAreas(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null || !isHoldingDisplayItem(player)) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        try {
            renderLoadedTurrets(level, player, poseStack, lines);
        } finally {
            poseStack.popPose();
            bufferSource.endBatch(RenderType.lines());
        }
    }

    private static void renderLoadedTurrets(ClientLevel level, Player player,
                                            PoseStack poseStack, VertexConsumer lines) {
        BlockPos playerPos = player.blockPosition();
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        int chunkRadius = (int) Math.ceil(DISPLAY_DISTANCE / 16.0D);

        for (int chunkX = playerChunkX - chunkRadius; chunkX <= playerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = playerChunkZ - chunkRadius; chunkZ <= playerChunkZ + chunkRadius; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof TurretBlockEntity turret)
                            || turret.getBlockPos().distSqr(playerPos) > DISPLAY_DISTANCE * DISPLAY_DISTANCE) {
                        continue;
                    }
                    renderTurretArea(turret, poseStack, lines);
                }
            }
        }
    }

    private static void renderTurretArea(TurretBlockEntity turret, PoseStack poseStack,
                                         VertexConsumer lines) {
        BlockPos pos = turret.getBlockPos();
        Vec3 origin = new Vec3(pos.getX() + 0.5D,
                pos.getY() + AIM_ORIGIN_HEIGHT,
                pos.getZ() + 0.5D);

        double areaRadius = TurretBlockEntity.getAttackRange();
        double minPitch = TurretBlockEntity.getMinAimPitch();
        double maxPitch = TurretBlockEntity.getMaxAimPitch();
        double yawStart = turret.getBaseYaw() - TurretBlockEntity.getYawAngle();
        double yawEnd = turret.getBaseYaw() + TurretBlockEntity.getYawAngle();

        // 上下边界是以炮台为顶点的两个圆锥面，并限制在左右偏航角形成的扇区内。
        drawPitchBoundary(origin, yawStart, yawEnd, minPitch, areaRadius,
                poseStack, lines, BOUNDARY_COLOR);
        drawPitchBoundary(origin, yawStart, yawEnd, maxPitch, areaRadius,
                poseStack, lines, BOUNDARY_COLOR);

        // 在最大检测距离处绘制俯仰范围内的辅助弧线，帮助判断目标所在高度。
        for (int pitch = (int) Math.ceil(minPitch) + PITCH_STEP;
             pitch < maxPitch; pitch += PITCH_STEP) {
            drawArc(origin, pitch, areaRadius, yawStart, yawEnd, poseStack, lines,
                    pitch == 0 ? MERIDIAN_COLOR : BOUNDARY_COLOR);
        }

        // 左右偏航边界形成扇区的两侧，并显示完整的俯仰范围。
        drawYawBoundary(origin, yawStart, minPitch, maxPitch, areaRadius,
                poseStack, lines, MERIDIAN_COLOR);
        drawYawBoundary(origin, yawEnd, minPitch, maxPitch, areaRadius,
                poseStack, lines, MERIDIAN_COLOR);

        // 绿色射线显示炮台当前保存的瞄准方向。
        drawLine(poseStack, lines, origin,
                directionPoint(origin, turret.getAimYaw(), turret.getAimPitch(), areaRadius), AIM_COLOR);
    }

    private static void drawPitchBoundary(Vec3 origin, double yawStart, double yawEnd,
                                          double pitch, double areaRadius,
                                          PoseStack poseStack, VertexConsumer lines, int color) {
        for (int step = 1; step <= CONE_RADIAL_STEPS; step++) {
            double distance = areaRadius * step / CONE_RADIAL_STEPS;
            drawArc(origin, pitch, distance, yawStart, yawEnd, poseStack, lines, color);
        }

        int segments = horizontalSegments(yawStart, yawEnd);
        for (int i = 0; i <= segments; i++) {
            double yaw = yawStart + (yawEnd - yawStart) * i / segments;
            drawLine(poseStack, lines, origin,
                    directionPoint(origin, yaw, pitch, areaRadius), color);
        }
    }

    private static void drawYawBoundary(Vec3 origin, double yaw, double minPitch, double maxPitch,
                                        double areaRadius, PoseStack poseStack,
                                        VertexConsumer lines, int color) {
        for (int step = 1; step <= CONE_RADIAL_STEPS; step++) {
            double distance = areaRadius * step / CONE_RADIAL_STEPS;
            drawPitchArc(origin, yaw, minPitch, maxPitch, distance, poseStack, lines, color);
        }

        int segments = pitchSegments(minPitch, maxPitch);
        for (int i = 0; i <= segments; i++) {
            double pitch = minPitch + (maxPitch - minPitch) * i / segments;
            drawLine(poseStack, lines, origin,
                    directionPoint(origin, yaw, pitch, areaRadius), color);
        }
    }

    private static void drawArc(Vec3 origin, double pitch, double distance,
                                double yawStart, double yawEnd, PoseStack poseStack,
                                VertexConsumer lines, int color) {
        int segments = horizontalSegments(yawStart, yawEnd);
        Vec3 previous = directionPoint(origin, yawStart, pitch, distance);
        for (int i = 1; i <= segments; i++) {
            double yaw = yawStart + (yawEnd - yawStart) * i / segments;
            Vec3 current = directionPoint(origin, yaw, pitch, distance);
            drawLine(poseStack, lines, previous, current, color);
            previous = current;
        }
    }

    private static void drawPitchArc(Vec3 origin, double yaw, double minPitch, double maxPitch,
                                     double distance, PoseStack poseStack,
                                     VertexConsumer lines, int color) {
        int segments = pitchSegments(minPitch, maxPitch);
        Vec3 previous = directionPoint(origin, yaw, minPitch, distance);
        for (int i = 1; i <= segments; i++) {
            double pitch = minPitch + (maxPitch - minPitch) * i / segments;
            Vec3 current = directionPoint(origin, yaw, pitch, distance);
            drawLine(poseStack, lines, previous, current, color);
            previous = current;
        }
    }

    private static int horizontalSegments(double yawStart, double yawEnd) {
        double span = Math.min(360.0D, Math.abs(yawEnd - yawStart));
        return Math.max(2, (int) Math.ceil(SEGMENTS * span / 360.0D));
    }

    private static int pitchSegments(double minPitch, double maxPitch) {
        return Math.max(2, (int) Math.ceil(Math.abs(maxPitch - minPitch) / PITCH_STEP));
    }

    private static Vec3 directionPoint(Vec3 origin, double yawDegrees, double pitchDegrees, double distance) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double horizontal = Math.cos(pitch) * distance;
        return origin.add(-Math.sin(yaw) * horizontal,
                Math.sin(pitch) * distance,
                Math.cos(yaw) * horizontal);
    }

    private static void drawLine(PoseStack poseStack, VertexConsumer lines,
                                 Vec3 start, Vec3 end, int color) {
        PoseStack.Pose pose = poseStack.last();
        addVertex(lines, pose, start, color);
        addVertex(lines, pose, end, color);
    }

    private static void addVertex(VertexConsumer lines, PoseStack.Pose pose, Vec3 point, int color) {
        lines.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static boolean isHoldingDisplayItem(Player player) {
        String configuredId = Config.TURRET_AIM_DISPLAY_ITEM.get();
        ResourceLocation itemId = ResourceLocation.tryParse(configuredId);
        if (itemId == null) {
            return false;
        }

        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            return false;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return mainHand.is(item) || offHand.is(item);
    }
}
