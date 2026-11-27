package com.superpowermod.client;

import com.superpowermod.common.Cue;
import com.superpowermod.common.ImpactKind;
import com.superpowermod.common.Timing;
import com.superpowermod.network.ImpactPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Dust, shockwave rings and trails. Drawn on each client, so the server sends only one packet. */
final class ClientEffects {
	/** A ring of dust that grows over several ticks. */
	private static final class Ring {
		final Vec3 center;
		final double radius;
		final double speed;
		double current = 0.8;

		Ring(Vec3 center, double radius, double speed) {
			this.center = center;
			this.radius = radius;
			this.speed = speed;
		}
	}

	private static final List<Ring> RINGS = new ArrayList<>();

	private ClientEffects() {
	}

	static void receive(Minecraft client, ImpactPayload payload) {
		ClientLevel level = client.level;

		if (level == null) return;

		Vec3 pos = new Vec3(payload.x(), payload.y(), payload.z());
		double radius = payload.radius();
		RandomSource random = level.getRandom();

		switch (ImpactKind.byId(payload.kind())) {
			case LAUNCH -> {
				RINGS.add(new Ring(pos, radius, 0.9));
				burst(level, ParticleTypes.CLOUD, pos.add(0, 0.2, 0), 30, 0.6, 0.18);
				debris(level, pos, 40, 0.8, 0.4);
				CameraShake.add(client, pos, 0.8F, 8, 16);
			}
			case SLAM -> {
				RINGS.add(new Ring(pos, radius, Timing.SHOCKWAVE_SPEED));
				add(level, ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 0.5, pos.z, 0, 0, 0);
				debris(level, pos, 180, radius * 0.35, 0.9);
				burst(level, ParticleTypes.POOF, pos.add(0, 0.3, 0), 60, 1.2, 0.35);

				// a tall column of lingering dust at the point of impact
				for (int i = 0; i < count(45); i++) {
					add(level, ParticleTypes.CAMPFIRE_COSY_SMOKE,
							pos.x + random.nextGaussian() * 0.9, pos.y + random.nextDouble() * 1.2,
							pos.z + random.nextGaussian() * 0.9,
							random.nextGaussian() * 0.03, 0.02 + random.nextDouble() * 0.07,
							random.nextGaussian() * 0.03);
				}

				CameraShake.add(client, pos, 4.0F, 22, radius * 4);
			}
			case GRAB -> {
				burst(level, ParticleTypes.CRIT, pos, 25, 0.3, 0.4);
				CameraShake.add(client, pos, 0.6F, 6, 10);
			}
			case THROW -> {
				add(level, ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 0, 0, 0);
				burst(level, ParticleTypes.CLOUD, pos, 35, 0.5, 0.3);
				burst(level, ParticleTypes.CRIT, pos, 25, 0.5, 0.6);
				CameraShake.add(client, pos, 1.2F, 8, 20);
			}
			case CRASH -> {
				RINGS.add(new Ring(pos, radius, 0.8));
				add(level, ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z, 0, 0, 0);
				debris(level, pos, 80, 1.2, 0.6);
				burst(level, ParticleTypes.POOF, pos.add(0, 0.3, 0), 35, 0.8, 0.2);
				CameraShake.add(client, pos, 2.2F, 14, 24);
			}
		}
	}

	static void tick(Minecraft client) {
		ClientLevel level = client.level;

		RINGS.removeIf(ring -> {
			drawRing(level, ring);
			ring.current += ring.speed;
			return ring.current > ring.radius + 0.01;
		});
	}

	private static void drawRing(ClientLevel level, Ring ring) {
		RandomSource random = level.getRandom();
		int points = Math.max(8, count((int) (ring.current * 9)));

		for (int i = 0; i < points; i++) {
			double angle = Math.PI * 2 * i / points + random.nextDouble() * 0.1;
			double outX = Math.cos(angle);
			double outZ = Math.sin(angle);
			double x = ring.center.x + outX * ring.current;
			double z = ring.center.z + outZ * ring.current;
			double y = ring.center.y + 0.15;
			BlockState ground = groundBelow(level, x, ring.center.y, z);

			add(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), x, y, z, outX * 2, 3, outZ * 2);
			add(level, ParticleTypes.POOF, x, y, z, outX * 0.25, 0.03, outZ * 0.25);

			if (i % 3 == 0) {
				add(level, new BlockParticleOption(ParticleTypes.DUST_PILLAR, ground), x, y, z, 0, 0.15, 0);
			}

			if (i % 5 == 0) {
				add(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, outX * 0.06, 0.015, outZ * 0.06);
			}
		}
	}

	/** Chunks of the ground flying out of an impact. */
	private static void debris(ClientLevel level, Vec3 pos, int amount, double spread, double lift) {
		RandomSource random = level.getRandom();

		for (int i = 0; i < count(amount); i++) {
			double x = pos.x + random.nextGaussian() * spread * 0.5;
			double z = pos.z + random.nextGaussian() * spread * 0.5;
			BlockState ground = groundBelow(level, x, pos.y, z);

			add(level, new BlockParticleOption(ParticleTypes.BLOCK, ground),
					x, pos.y + 0.2 + random.nextDouble() * 0.5, z,
					random.nextGaussian() * 2, lift * (2 + random.nextDouble() * 6), random.nextGaussian() * 2);
		}
	}

	private static void burst(ClientLevel level, ParticleOptions particle, Vec3 pos, int amount, double spread,
			double speed) {
		RandomSource random = level.getRandom();

		for (int i = 0; i < count(amount); i++) {
			add(level, particle,
					pos.x + random.nextGaussian() * spread * 0.4,
					pos.y + random.nextGaussian() * spread * 0.25,
					pos.z + random.nextGaussian() * spread * 0.4,
					random.nextGaussian() * speed, Math.abs(random.nextGaussian()) * speed * 0.5,
					random.nextGaussian() * speed);
		}
	}

	/** Streaks behind a hero who is rising or diving, and behind a thrown player. */
	static void trail(Minecraft client, AbstractClientPlayer player, Cue cue, int age) {
		boolean rising = cue == Cue.SLAM_JUMP && age > Timing.SLAM_WINDUP;
		boolean diving = cue == Cue.SLAM_FALL && age > Timing.SLAM_HANG;
		boolean flying = cue == Cue.TARGET_FLYING;

		if (!rising && !diving && !flying) return;

		ClientLevel level = client.level;
		RandomSource random = level.getRandom();
		Vec3 from = new Vec3(player.xo, player.yo, player.zo);
		Vec3 step = player.position().subtract(from);
		// do not draw over the player's own view while they are the one moving
		if (player == client.player && client.options.getCameraType().isFirstPerson()) return;

		for (int i = 0; i < count(8); i++) {
			Vec3 point = from.add(step.scale(i / 8.0));
			add(level, ParticleTypes.CLOUD,
					point.x + random.nextGaussian() * 0.15, point.y + 0.8 + random.nextGaussian() * 0.3,
					point.z + random.nextGaussian() * 0.15, 0, 0, 0);

			if (diving || flying) {
				add(level, ParticleTypes.CRIT,
						point.x + random.nextGaussian() * 0.4, point.y + 0.9 + random.nextGaussian() * 0.5,
						point.z + random.nextGaussian() * 0.4, 0, 0, 0);
			}
		}
	}

	/** The block under a point, so dust matches the terrain. */
	private static BlockState groundBelow(ClientLevel level, double x, double y, double z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int depth = 0; depth < 6; depth++) {
			pos.set(Math.floor(x), Math.floor(y - 0.2) - depth, Math.floor(z));
			BlockState state = level.getBlockState(pos);

			if (!state.isAir() && state.getFluidState().isEmpty()) return state;
		}

		return Blocks.DIRT.defaultBlockState();
	}

	private static int count(int base) {
		return Math.max(1, (int) Math.round(base * ClientConfig.get().particleDensity));
	}

	private static void add(ClientLevel level, ParticleOptions particle, double x, double y, double z,
			double xSpeed, double ySpeed, double zSpeed) {
		// "always visible" keeps effects in frame for cameras further than 32 blocks away
		level.addAlwaysVisibleParticle(particle, true, x, y, z, xSpeed, ySpeed, zSpeed);
	}

	static void clear() {
		RINGS.clear();
	}
}
