package com.superpowermod.ability;

import com.superpowermod.ModSounds;
import com.superpowermod.common.Cue;
import com.superpowermod.common.ImpactKind;
import com.superpowermod.common.JumpPhysics;
import com.superpowermod.common.Timing;
import com.superpowermod.config.ModConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Crouch, launch, hang, dive, smash. */
final class SkySlamSession implements Session {
	private static final int MAX_DIVE_TICKS = 120;

	private enum Phase {
		WINDUP, ASCEND, HANG, DIVE, LANDING
	}

	private final UUID casterId;
	private final ServerLevel level;
	private final ModConfig.SkySlam config;
	private final double launchVelocity;
	private final int ascentTicks;

	private Phase phase = Phase.WINDUP;
	private int phaseTicks;
	private double startY;
	private Vec3 lastPos;
	private int stalledTicks;
	private Vec3 diveDirection = new Vec3(0, -1, 0);

	SkySlamSession(ServerPlayer caster, ModConfig.SkySlam config, long now) {
		this.casterId = caster.getUUID();
		this.level = (ServerLevel) caster.level();
		this.config = config;
		this.launchVelocity = JumpPhysics.velocityForHeight(config.jumpHeight);
		this.ascentTicks = JumpPhysics.ascentTicks(launchVelocity);
		this.lastPos = caster.position();

		FallProtection.protect(caster);
		Sync.cue(caster, Cue.SLAM_JUMP, null);
	}

	@Override
	public boolean involves(UUID playerId) {
		return casterId.equals(playerId);
	}

	@Override
	public boolean tick(MinecraftServer server) {
		ServerPlayer caster = server.getPlayerList().getPlayer(casterId);

		if (!Session.isUsable(caster, level)) {
			if (caster != null) Sync.cue(caster, Cue.STOP, null);
			return false;
		}

		phaseTicks++;

		if (phase != Phase.LANDING) {
			if (caster.isPassenger() || caster.isFallFlying()) {
				return cancel(caster);
			}

			caster.resetFallDistance();
		}

		return switch (phase) {
			case WINDUP -> windup(caster);
			case ASCEND -> ascend(caster);
			case HANG -> hang(caster);
			case DIVE -> dive(caster);
			case LANDING -> phaseTicks < Timing.SLAM_LAND;
		};
	}

	private boolean windup(ServerPlayer caster) {
		if (phaseTicks < Timing.SLAM_WINDUP) return true;

		// creative flight would cancel the physics of the jump
		if (caster.getAbilities().flying) {
			caster.getAbilities().flying = false;
			caster.onUpdateAbilities();
		}

		FallProtection.protect(caster);
		startY = caster.getY();
		lastPos = caster.position();

		Sync.velocity(caster, new Vec3(0, launchVelocity, 0));
		Sync.sound(level, caster.position(), ModSounds.SLAM_LAUNCH, 2.5F);
		Sync.impact(level, caster.position(), ImpactKind.LAUNCH, 3);
		return next(Phase.ASCEND);
	}

	private boolean ascend(ServerPlayer caster) {
		boolean hasRisen = caster.getY() > startY + 1;
		stalledTicks = hasRisen && caster.getY() - lastPos.y < 0.15 ? stalledTicks + 1 : 0;
		lastPos = caster.position();

		// The top is reached on schedule, or early when a ceiling stops the climb.
		if (phaseTicks >= ascentTicks || stalledTicks >= 3) {
			Sync.cue(caster, Cue.SLAM_FALL, null);
			return next(Phase.HANG);
		}

		return true;
	}

	private boolean hang(ServerPlayer caster) {
		if (phaseTicks <= Timing.SLAM_HANG) {
			Sync.velocity(caster, new Vec3(0, 0.03, 0));
			return true;
		}

		diveDirection = aim(caster);
		lastPos = caster.position();
		stalledTicks = 0;

		Sync.velocity(caster, diveDirection.scale(config.diveSpeed));
		Sync.sound(level, caster.position(), ModSounds.SLAM_DIVE, 2.5F);
		return next(Phase.DIVE);
	}

	private boolean dive(ServerPlayer caster) {
		if (caster.isInWater() || caster.isInLava()) {
			return cancel(caster);
		}

		boolean stalled = phaseTicks > 8 && caster.position().distanceToSqr(lastPos) < 0.0025;
		stalledTicks = stalled ? stalledTicks + 1 : 0;
		lastPos = caster.position();

		if (caster.onGround() || stalledTicks >= 6) {
			return smash(caster);
		}

		if (phaseTicks > MAX_DIVE_TICKS) {
			// for example diving into the void
			return cancel(caster);
		}

		Sync.velocity(caster, diveDirection.scale(config.diveSpeed));
		return true;
	}

	private boolean smash(ServerPlayer caster) {
		Vec3 pos = caster.position();

		FallProtection.protect(caster);
		Sync.velocity(caster, Vec3.ZERO);
		Sync.cue(caster, Cue.SLAM_LAND, null);
		Sync.sound(level, pos, ModSounds.SLAM_IMPACT, 4.0F);
		Sync.impact(level, pos, ImpactKind.SLAM, config.shockwaveRadius);
		AbilityManager.addShockwave(new ShockwaveEffect(level, pos, casterId, config));
		return next(Phase.LANDING);
	}

	/** Dive towards the spot the player is looking at, never flatter than about 35 degrees. */
	private Vec3 aim(ServerPlayer caster) {
		Vec3 straightDown = new Vec3(0, -1, 0);

		if (config.aimRange <= 0) return straightDown;

		Vec3 eye = caster.getEyePosition();
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(caster.getLookAngle().scale(config.aimRange)),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));

		if (hit.getType() == HitResult.Type.MISS) return straightDown;

		Vec3 direction = hit.getLocation().subtract(caster.position()).normalize();

		if (direction.y > -0.58) {
			Vec3 flat = new Vec3(direction.x, 0, direction.z).normalize();
			direction = new Vec3(flat.x * 0.815, -0.58, flat.z * 0.815).normalize();
		}

		return direction;
	}

	private boolean next(Phase newPhase) {
		phase = newPhase;
		phaseTicks = 0;
		return true;
	}

	private boolean cancel(ServerPlayer caster) {
		Sync.cue(caster, Cue.STOP, null);
		return false;
	}
}
