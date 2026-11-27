package com.superpowermod.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

final class Targeting {
	/** Extra margin around hitboxes, so grabbing a moving player is not frustrating. */
	private static final double AIM_ASSIST = 0.45;

	private Targeting() {
	}

	/** The nearest player under the crosshair within range and not behind a wall, or null. */
	static ServerPlayer findPlayer(ServerPlayer caster, double range) {
		ServerLevel level = (ServerLevel) caster.level();
		Vec3 eye = caster.getEyePosition();
		Vec3 end = eye.add(caster.getLookAngle().scale(range));

		ServerPlayer best = null;
		double bestDistance = Double.MAX_VALUE;

		for (ServerPlayer candidate : level.players()) {
			if (candidate == caster || !candidate.isAlive() || candidate.isSpectator()) continue;

			AABB box = candidate.getBoundingBox().inflate(AIM_ASSIST);
			Optional<Vec3> hit = box.contains(eye) ? Optional.of(eye) : box.clip(eye, end);

			if (hit.isEmpty()) continue;

			double distance = eye.distanceToSqr(hit.get());

			if (distance >= bestDistance) continue;

			HitResult wall = level.clip(new ClipContext(eye, candidate.getBoundingBox().getCenter(),
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));

			if (wall.getType() != HitResult.Type.MISS) continue;

			best = candidate;
			bestDistance = distance;
		}

		return best;
	}
}
