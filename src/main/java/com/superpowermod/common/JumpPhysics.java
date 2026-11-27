package com.superpowermod.common;

/** Reproduces vanilla vertical player physics so a jump height in blocks can be turned into a velocity. */
public final class JumpPhysics {
	private static final double GRAVITY = 0.08;
	private static final double DRAG = 0.98;

	private JumpPhysics() {
	}

	/** Height in blocks reached by a player launched straight up with this velocity. */
	public static double apexHeight(double velocity) {
		double height = 0;
		double v = velocity;

		while (v > 0) {
			height += v;
			v = (v - GRAVITY) * DRAG;
		}

		return height;
	}

	/** Number of ticks the player keeps rising. */
	public static int ascentTicks(double velocity) {
		int ticks = 0;
		double v = velocity;

		while (v > 0) {
			v = (v - GRAVITY) * DRAG;
			ticks++;
		}

		return ticks;
	}

	/** Launch velocity (blocks per tick) needed to rise the given number of blocks. */
	public static double velocityForHeight(double height) {
		double low = 0;
		double high = 10;

		for (int i = 0; i < 50; i++) {
			double mid = (low + high) / 2;

			if (apexHeight(mid) < height) {
				low = mid;
			} else {
				high = mid;
			}
		}

		return high;
	}
}
