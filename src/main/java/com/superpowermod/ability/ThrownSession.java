package com.superpowermod.ability;

import com.superpowermod.ModSounds;
import com.superpowermod.common.Cue;
import com.superpowermod.common.ImpactKind;
import com.superpowermod.common.Timing;
import com.superpowermod.config.ModConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Follows a thrown player until they hit something, then plays the crash and get-up. */
final class ThrownSession implements Session {
	private static final int MAX_FLIGHT_TICKS = 160;
	/** Time allowed for the launch to reach the victim's client before testing for a landing. */
	private static final int MIN_FLIGHT_TICKS = 6;

	private final UUID throwerId;
	private final UUID targetId;
	private final ServerLevel level;
	private final ModConfig.TitanThrow config;

	private boolean crashed;
	private int ticks;
	private Vec3 lastPos;
	private double topSpeed;
	private boolean hitWall;

	ThrownSession(ServerPlayer thrower, ServerPlayer target, ModConfig.TitanThrow config) {
		this.throwerId = thrower.getUUID();
		this.targetId = target.getUUID();
		this.level = (ServerLevel) target.level();
		this.config = config;
		this.lastPos = target.position();
	}

	@Override
	public boolean involves(UUID playerId) {
		return targetId.equals(playerId);
	}

	@Override
	public boolean tick(MinecraftServer server) {
		ServerPlayer target = server.getPlayerList().getPlayer(targetId);

		if (!Session.isUsable(target, level)) {
			if (target != null) Sync.cue(target, Cue.STOP, null);
			return false;
		}

		ticks++;

		if (crashed) {
			return ticks < Timing.TARGET_LAND;
		}

		if (target.isPassenger() || target.isFallFlying() || ticks > MAX_FLIGHT_TICKS) {
			Sync.cue(target, Cue.STOP, null);
			return false;
		}

		target.resetFallDistance();

		Vec3 position = target.position();
		double speed = Math.hypot(position.x - lastPos.x, position.z - lastPos.z);
		lastPos = position;
		topSpeed = Math.max(topSpeed, speed);

		// Flying into a wall: burst of impact effects, then the victim drops and crashes on the ground.
		// The nearby-block test keeps a late packet from counting as a wall.
		if (!hitWall && topSpeed > 0.8 && speed < 0.12
				&& !level.noCollision(target, target.getBoundingBox().inflate(0.35, 0, 0.35))) {
			hitWall = true;
			Sync.sound(level, position, ModSounds.TITAN_LAND, 2.0F);
			Sync.impact(level, position.add(0, 1, 0), ImpactKind.THROW, 2);
		}

		if (target.isInWater() && ticks > MIN_FLIGHT_TICKS) {
			Sync.impact(level, target.position(), ImpactKind.THROW, 2);
			Sync.cue(target, Cue.STOP, null);
			return false;
		}

		if (ticks > MIN_FLIGHT_TICKS && target.onGround()) {
			crash(server.getPlayerList().getPlayer(throwerId), target);
		}

		return true;
	}

	private void crash(ServerPlayer thrower, ServerPlayer target) {
		crashed = true;
		ticks = 0;

		FallProtection.protect(target);
		Sync.velocity(target, Vec3.ZERO);
		Sync.cue(target, Cue.TARGET_LAND, null);
		Sync.sound(level, target.position(), ModSounds.TITAN_LAND, 3.0F);
		Sync.impact(level, target.position(), ImpactKind.CRASH, 4);

		if (config.landingDamage > 0 && thrower != null) {
			target.hurtServer(level, level.damageSources().playerAttack(thrower), (float) config.landingDamage);
		}
	}
}
