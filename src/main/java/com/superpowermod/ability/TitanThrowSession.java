package com.superpowermod.ability;

import com.superpowermod.ModSounds;
import com.superpowermod.common.Cue;
import com.superpowermod.common.GripPath;
import com.superpowermod.common.GripPositions;
import com.superpowermod.common.ImpactKind;
import com.superpowermod.common.Timing;
import com.superpowermod.config.ModConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Grab, hold, wind up and release. The flight afterwards is a {@link ThrownSession}. */
final class TitanThrowSession implements Session {
	/** A held player further than this from the hand is pulled back by the server. */
	private static final double MAX_DRIFT = 3.5;

	private final UUID casterId;
	private final UUID targetId;
	private final ServerLevel level;
	private final ModConfig.TitanThrow config;

	private boolean throwing;
	private boolean released;
	private int phaseTicks;

	TitanThrowSession(ServerPlayer caster, ServerPlayer target, ModConfig.TitanThrow config, long now) {
		this.casterId = caster.getUUID();
		this.targetId = target.getUUID();
		this.level = (ServerLevel) caster.level();
		this.config = config;

		if (target.isPassenger()) target.stopRiding();

		if (target.getAbilities().flying) {
			target.getAbilities().flying = false;
			target.onUpdateAbilities();
		}

		FallProtection.protect(target);
		Sync.velocity(target, Vec3.ZERO);
		Sync.cue(caster, Cue.TITAN_GRAB, null);
		Sync.cue(target, Cue.TARGET_HELD, caster);
		Sync.sound(level, target.position(), ModSounds.TITAN_GRAB, 2.0F);
		Sync.impact(level, target.position().add(0, 1.2, 0), ImpactKind.GRAB, 1);
	}

	@Override
	public boolean involves(UUID playerId) {
		return casterId.equals(playerId) || (!released && targetId.equals(playerId));
	}

	@Override
	public boolean tick(MinecraftServer server) {
		ServerPlayer caster = server.getPlayerList().getPlayer(casterId);
		ServerPlayer target = server.getPlayerList().getPlayer(targetId);

		phaseTicks++;

		if (released) {
			// only the thrower's follow-through is left
			if (!Session.isUsable(caster, level)) {
				if (caster != null) Sync.cue(caster, Cue.STOP, null);
				return false;
			}

			return phaseTicks < Timing.THROW_TOTAL;
		}

		if (!Session.isUsable(caster, level) || !Session.isUsable(target, level) || target.isPassenger()) {
			if (caster != null) Sync.cue(caster, Cue.STOP, null);
			if (target != null) Sync.cue(target, Cue.STOP, null);
			return false;
		}

		target.resetFallDistance();

		if (!throwing && phaseTicks >= config.holdTicks) {
			throwing = true;
			phaseTicks = 0;
			Sync.cue(caster, Cue.TITAN_THROW, null);
		}

		if (throwing && phaseTicks >= Timing.THROW_RELEASE) {
			release(caster, target);
			return true;
		}

		keepInHand(caster, target);
		return true;
	}

	/**
	 * The clients place the victim smoothly every tick. The server only steps in when the victim has
	 * drifted far from the hand, for example through lag.
	 */
	private void keepInHand(ServerPlayer caster, ServerPlayer target) {
		// give the victim's client time to be pulled into the grip
		if (!throwing && phaseTicks < Timing.GRAB_REACH + 6) return;

		GripPath.Offset offset = throwing ? GripPath.throwing(phaseTicks) : GripPath.holding(phaseTicks);
		Vec3 hand = GripPositions.resolve(level, target, caster.position(), caster.getYRot(), offset);

		if (target.position().distanceToSqr(hand) > MAX_DRIFT * MAX_DRIFT) {
			target.teleportTo(hand.x, hand.y, hand.z);
		}
	}

	private void release(ServerPlayer caster, ServerPlayer target) {
		released = true;

		Vec3 look = caster.getLookAngle();
		double yaw = Math.toRadians(caster.getYRot());
		Vec3 flat = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		// looking up throws higher and shorter
		double upLook = Math.max(0, look.y);
		double forward = config.throwStrength * (1 - 0.5 * upLook);
		double upward = config.throwUpward + upLook * config.throwStrength * 0.6;
		Vec3 velocity = new Vec3(flat.x * forward, upward, flat.z * forward);

		FallProtection.protect(target);
		// the cue must arrive first so the victim's client lets go before the velocity is applied
		Sync.cue(target, Cue.TARGET_FLYING, caster);
		Sync.velocity(target, velocity);
		Sync.sound(level, caster.position(), ModSounds.TITAN_THROW, 3.0F);
		Sync.impact(level, target.position().add(0, 1, 0), ImpactKind.THROW, 2);
		AbilityManager.startFollowUp(new ThrownSession(caster, target, config));
	}
}
