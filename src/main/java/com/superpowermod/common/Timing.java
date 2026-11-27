package com.superpowermod.common;

/**
 * All timings in game ticks (20 ticks = 1 second). They match the keyframes in
 * assets/superpowermod/player_animations. If you retime an animation, update the value here too.
 */
public final class Timing {
	private Timing() {
	}

	/** Crouch before the jump leaves the ground. */
	public static final int SLAM_WINDUP = 5;
	/** Pause at the top of the jump, while the hero tucks and turns into the dive. */
	public static final int SLAM_HANG = 8;
	/** Full length of the landing animation. */
	public static final int SLAM_LAND = 34;
	/** How long the hero stays planted after landing. */
	public static final int SLAM_LAND_ROOT = 24;

	/** Ticks for the target to be pulled into the grip. */
	public static final int GRAB_REACH = 6;
	/** Tick of the throw animation on which the target is released. */
	public static final int THROW_RELEASE = 10;
	/** Full length of the throw animation. */
	public static final int THROW_TOTAL = 24;

	/** Full length of the target's crash and get-up animation. */
	public static final int TARGET_LAND = 40;
	/** How long the target cannot walk after crashing. */
	public static final int TARGET_LAND_ROOT = 32;

	/** Blocks per tick at which the slam shockwave ring expands (server knockback and client particles). */
	public static final double SHOCKWAVE_SPEED = 1.25;
}
