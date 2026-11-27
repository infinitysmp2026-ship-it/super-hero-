package com.superpowermod.common;

/**
 * Where a grabbed player's feet are, relative to the thrower, during the grab and throw. The numbers
 * follow the thrower's right hand in titan_grab and titan_throw. Server and clients share this class
 * so both agree on the position.
 */
public final class GripPath {
	private GripPath() {
	}

	/** Offset in blocks: forward along the thrower's view, to their right, and up from their feet. */
	public record Offset(double forward, double right, double up) {
		public Offset lerp(Offset to, double t) {
			return new Offset(
					forward + (to.forward - forward) * t,
					right + (to.right - right) * t,
					up + (to.up - up) * t);
		}
	}

	/** Where the grip first closes: at arm's length, feet still near the floor. */
	private static final Offset CAUGHT = new Offset(0.95, 0.30, 0.02);
	/** Held out in front, feet off the ground. */
	private static final Offset HOLD = new Offset(0.55, 0.38, 0.32);

	// Throw keyframes: {tick, forward, right, up}
	private static final double[][] THROW_KEYS = {
			{0, 0.55, 0.38, 0.32},
			{2, 0.10, 0.85, 0.40},
			{4, -0.65, 0.55, 0.38},
			{6, -0.95, 0.00, 0.30},
			{8, -0.65, 0.55, 0.55},
			{9, 0.20, 0.95, 0.75},
			{Timing.THROW_RELEASE, 1.15, 0.30, 0.65},
	};

	/** Position while holding. {@code age} is ticks since the grab started. */
	public static Offset holding(float age) {
		// a slow bob so the victim does not look frozen in place
		double bob = Math.sin(age * 0.35) * 0.04;
		double lift = smooth(Math.max(0, Math.min(1, (age - Timing.GRAB_REACH) / (double) Timing.GRAB_REACH)));
		Offset offset = CAUGHT.lerp(HOLD, lift);
		return new Offset(offset.forward, offset.right, offset.up + bob * lift);
	}

	/** Position during the throw. {@code age} is ticks since the throw animation started. */
	public static Offset throwing(float age) {
		for (int i = 1; i < THROW_KEYS.length; i++) {
			double[] next = THROW_KEYS[i];

			if (age <= next[0]) {
				double[] previous = THROW_KEYS[i - 1];
				double t = Math.max(0, (age - previous[0]) / (next[0] - previous[0]));
				return toOffset(previous).lerp(toOffset(next), smooth(t));
			}
		}

		return toOffset(THROW_KEYS[THROW_KEYS.length - 1]);
	}

	private static Offset toOffset(double[] key) {
		return new Offset(key[1], key[2], key[3]);
	}

	/** Smoothstep easing for t in 0..1. */
	public static double smooth(double t) {
		return t * t * (3 - 2 * t);
	}
}
