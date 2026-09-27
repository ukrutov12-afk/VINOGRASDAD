package dev.fashion.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import dev.fashion.core.Category;
import dev.fashion.core.Module;
import dev.fashion.core.Targets;
import dev.fashion.core.setting.BoolSetting;
import dev.fashion.core.setting.ModeSetting;
import dev.fashion.core.setting.MultiSetting;
import dev.fashion.core.setting.NumberSetting;

public final class AttackAura extends Module {
    private final ModeSetting aim = mode("Наведение", "Как взгляд доходит до цели", 1, "Резкое", "Плавное", "Тихое");
    private final NumberSetting smoothness = number("Скорость наведения", "Доля угла, проходимая за тик", 0.45, 0.1, 1.0, 0.05, "")
            .visibleWhen(() -> aim.is(1));
    private final NumberSetting attackRange = number("Дистанция атаки", "Дальше удар не наносится", 3.0, 2.0, 6.0, 0.1, " б");
    private final NumberSetting aimRange = number("Дистанция наведения", "С какой дистанции цель берётся на прицел", 4.5, 2.0, 8.0, 0.1, " б");
    private final MultiSetting targets = add(Targets.setting("Кого атаковать: игроков, мобов или всех вместе"));
    private final BoolSetting invisible = bool("Невидимые", "Атаковать невидимых", false);
    private final ModeSetting sprintReset = mode("Сброс спринта", "Что делать со спринтом перед ударом", 1, "Нет", "Легитный", "Быстрый");

    private LivingEntity target;
    private boolean pendingHit;

    public AttackAura() {
        super("AttackAura", "Наводится на ближайшую цель и бьёт по готовности удара", Category.COMBAT, '\uE030');
    }

    public LivingEntity target() {
        return enabled() ? target : null;
    }

    @Override
    public String suffix() {
        return aim.mode();
    }

    @Override
    protected void onDisable() {
        target = null;
        pendingHit = false;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) {
            target = null;
            return;
        }
        target = find();
        if (target == null) {
            pendingHit = false;
            return;
        }
        float[] angles = angles(mc.player.getEyePos(), aimPoint(target));
        if (aim.is(1)) {
            float k = smoothness.asFloat();
            float yawDiff = MathHelper.wrapDegrees(angles[0] - mc.player.getYaw());
            mc.player.setYaw(mc.player.getYaw() + yawDiff * k);
            mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + (angles[1] - mc.player.getPitch()) * k, -90f, 90f));
        }
        if (mc.player.distanceTo(target) > attackRange.asFloat()) {
            pendingHit = false;
            return;
        }
        if (mc.player.getAttackCooldownProgress(0.5f) < 1f) {
            return;
        }
        if (sprintReset.is(1) && mc.player.isSprinting() && !pendingHit) {
            mc.player.setSprinting(false);
            pendingHit = true;
            return;
        }
        pendingHit = false;
        if (aim.is(0)) {
            mc.player.setYaw(angles[0]);
            mc.player.setPitch(angles[1]);
        } else if (aim.is(2)) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(angles[0], angles[1],
                    mc.player.isOnGround(), mc.player.horizontalCollision));
        }
        boolean fast = sprintReset.is(2) && mc.player.isSprinting();
        if (fast) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (fast) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
    }

    private LivingEntity find() {
        LivingEntity best = null;
        double bestDist = aimRange.asDouble();
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity living) || !living.isAlive() || living.isDead()) {
                continue;
            }
            if (!accepts(living)) {
                continue;
            }
            double d = mc.player.distanceTo(living);
            if (d <= bestDist) {
                bestDist = d;
                best = living;
            }
        }
        return best;
    }

    private boolean accepts(LivingEntity e) {
        if (e.isInvisible() && !invisible.on()) {
            return false;
        }
        return Targets.accepts(targets, e);
    }

    private static Vec3d aimPoint(LivingEntity e) {
        return e.getEntityPos().add(0, e.getHeight() * 0.62, 0);
    }

    private static float[] angles(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
    }
}
