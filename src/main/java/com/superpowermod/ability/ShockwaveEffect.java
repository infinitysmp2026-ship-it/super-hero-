package com.superpowermod.ability;

import com.superpowermod.common.Timing;
import com.superpowermod.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** The expanding ring after a slam: knocks players away and breaks the crater from the centre outwards. */
final class ShockwaveEffect {
	private static final double VERTICAL_REACH = 5;

	private record Candidate(BlockPos pos, double distance, boolean aboveGround) {
	}

	private final ServerLevel level;
	private final Vec3 center;
	private final UUID casterId;
	private final ModConfig.SkySlam config;
	private final Set<UUID> alreadyHit = new HashSet<>();
	private final List<Candidate> crater = new ArrayList<>();

	private int nextBlock;
	private int broken;
	private int age;

	ShockwaveEffect(ServerLevel level, Vec3 center, UUID casterId, ModConfig.SkySlam config) {
		this.level = level;
		this.center = center;
		this.casterId = casterId;
		this.config = config;

		if (config.breakBlocks && config.blockRadius > 0) {
			planCrater();
		}
	}

	/** A shallow bowl with a ragged edge, plus the plants growing on it. */
	private void planCrater() {
		BlockPos ground = BlockPos.containing(center.x, center.y - 0.2, center.z);
		int radius = config.blockRadius;

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double distance = Math.sqrt(dx * dx + dz * dz);

				if (distance > radius + 0.3) continue;

				// leave random gaps on the rim so the crater does not look like a perfect circle
				if (distance > radius - 1 && level.getRandom().nextFloat() < 0.4F) continue;

				double falloff = 1 - Math.pow(distance / (radius + 0.5), 2);
				int depth = (int) Math.round(config.blockDepth * falloff);

				for (int dy = 2; dy > -depth; dy--) {
					crater.add(new Candidate(ground.offset(dx, dy, dz), distance, dy > 0));
				}
			}
		}

		// closest first; within a column from the top down
		crater.sort(Comparator.<Candidate>comparingDouble(Candidate::distance)
				.thenComparing(candidate -> -candidate.pos().getY()));
	}

	/** Above ground level only loose things such as grass and flowers go, so walls stay standing. */
	private boolean breakCandidate(ServerPlayer caster, Candidate candidate) {
		BlockPos pos = candidate.pos();

		if (candidate.aboveGround() && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
			return false;
		}

		return BlockProtection.tryBreak(level, caster, pos, config);
	}

	/** @return false once the ring has reached its full size */
	boolean tick(MinecraftServer server) {
		age++;
		double ring = age * Timing.SHOCKWAVE_SPEED + 1;
		ServerPlayer caster = server.getPlayerList().getPlayer(casterId);

		// Without the caster there is nobody to check permissions against, so nothing more is broken.
		if (caster != null && caster.level() == level) {
			while (nextBlock < crater.size() && crater.get(nextBlock).distance() <= ring) {
				if (broken >= config.maxBlocks) {
					nextBlock = crater.size();
				} else if (breakCandidate(caster, crater.get(nextBlock++))) {
					broken++;
				}
			}
		}

		knockBack(Math.min(ring, config.shockwaveRadius), caster);

		return ring < config.shockwaveRadius || (nextBlock < crater.size() && caster != null && age < 40);
	}

	private void knockBack(double ring, ServerPlayer caster) {
		for (ServerPlayer player : level.players()) {
			UUID id = player.getUUID();

			if (id.equals(casterId) || alreadyHit.contains(id) || !player.isAlive() || player.isSpectator()) {
				continue;
			}

			double dx = player.getX() - center.x;
			double dz = player.getZ() - center.z;
			double distance = Math.sqrt(dx * dx + dz * dz);

			if (distance > ring || Math.abs(player.getY() - center.y) > VERTICAL_REACH) continue;

			// players being grabbed or thrown are left to that ability
			if (AbilityManager.isBusy(id)) continue;

			alreadyHit.add(id);

			if (config.shockwaveDamage > 0 && caster != null) {
				player.hurtServer(level, level.damageSources().playerAttack(caster), (float) config.shockwaveDamage);
			}

			double outX = distance > 0.01 ? dx / distance : 1;
			double outZ = distance > 0.01 ? dz / distance : 0;
			// full strength at the centre, half at the rim
			double power = 1 - 0.5 * Math.min(1, distance / config.shockwaveRadius);

			if (player.isPassenger()) {
				player.stopRiding();
			}

			FallProtection.protect(player);
			Sync.velocity(player, new Vec3(
					outX * config.knockbackStrength * power,
					config.knockbackUpward * (0.5 + 0.5 * power),
					outZ * config.knockbackStrength * power));
		}
	}
}
