package com.example.aimassist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public class AimAssistClient implements ClientModInitializer {

    // === НАСТРОЙКИ ===
    public static boolean enabled = true;
    public static float fov = 90.0f;          // градусы
    public static float range = 4.5f;         // блоки
    public static float speed = 0.35f;        // 0..1, доля доворота за тик
    public static boolean teamCheck = true;

    // Цель, к которой сейчас ведём (null = нет цели)
    public static LivingEntity target = null;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!enabled || client.player == null || client.level == null) {
                target = null;
                return;
            }
            target = findBestTarget(client);
        });
    }

    private static LivingEntity findBestTarget(Minecraft client) {
        Player self = client.player;
        Vec3 eye = self.getEyePosition();
        Vec3 look = self.getLookAngle();

        return client.level.getEntities(self, self.getBoundingBox().inflate(range))
                .stream()
                .filter(e -> e instanceof LivingEntity && e.isAlive())
                .map(e -> (LivingEntity) e)
                .filter(e -> e != self)
                .filter(e -> !teamCheck || !isTeammate(self, e))
                .filter(e -> inFov(eye, look, e))
                .min(Comparator.comparingDouble(e -> angleTo(eye, look, e)))
                .orElse(null);
    }

    private static boolean inFov(Vec3 eye, Vec3 look, Entity e) {
        Vec3 to = e.getEyePosition().subtract(eye).normalize();
        double dot = look.dot(to);
        double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
        return angle <= fov / 2.0;
    }

    private static double angleTo(Vec3 eye, Vec3 look, Entity e) {
        Vec3 to = e.getEyePosition().subtract(eye).normalize();
        double dot = look.dot(to);
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
    }

    private static boolean isTeammate(Player self, LivingEntity other) {
        return self.getTeam() != null && self.getTeam().isAlliedTo(other.getTeam());
    }

    // Вызывается из миксина, возвращает целевой yaw (или null)
    public static Float getTargetYaw(Player player) {
        if (target == null) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 to = target.getEyePosition().subtract(eye);
        return (float) (Math.toDegrees(Math.atan2(-to.x, to.z)));
    }

    // Вызывается из миксина, возвращает целевой pitch (или null)
    public static Float getTargetPitch(Player player) {
        if (target == null) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 to = target.getEyePosition().subtract(eye);
        double horiz = Math.sqrt(to.x * to.x + to.z * to.z);
        return (float) (-Math.toDegrees(Math.atan2(to.y, horiz)));
    }
}
