package com.superpowermod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** A short decaying shake, applied to the camera only. It never changes where the player is aiming. */
public final class CameraShake {
	private static float strength;
	private static int duration;
	private static int remaining;
	private static int clock;

	private CameraShake() {
	}

	/**
	 * @param power degrees of shake at the source
	 * @param ticks how long it lasts
	 * @param range distance at which it fades out completely
	 */
	static void add(Minecraft client, Vec3 source, float power, int ticks, double range) {
		if (!ClientConfig.get().cameraShake || client.player == null) return;

		double distance = client.gameRenderer.getMainCamera().position().distanceTo(source);

		if (distance >= range) return;

		float felt = (float) (power * (1 - distance / range) * ClientConfig.get().cameraShakeStrength);

		// a weaker shake does not cut off a stronger one that is still running
		if (felt > current(0)) {
			strength = felt;
			duration = ticks;
			remaining = ticks;
		}
	}

	static void tick() {
		clock++;

		if (remaining > 0) remaining--;
	}

	private static float current(float partialTick) {
		if (remaining <= 0 || duration <= 0) return 0;

		float left = Math.max(0, (remaining - partialTick) / duration);
		return strength * left * left;
	}

	public static float yawOffset(float partialTick) {
		return current(partialTick) * (float) Math.sin((clock + partialTick) * 2.9) * 0.6F;
	}

	public static float pitchOffset(float partialTick) {
		return current(partialTick) * (float) Math.sin((clock + partialTick) * 4.3 + 1.3);
	}

	static void clear() {
		remaining = 0;
	}
}
