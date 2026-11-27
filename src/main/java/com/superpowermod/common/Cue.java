package com.superpowermod.common;

/**
 * One synchronized animation state of a player. The server decides which cue a player is in and
 * tells every client that can see them; clients play the matching animation.
 */
public enum Cue {
	/** Ends whatever is playing. */
	STOP(null, 0, 0),

	SLAM_JUMP("slam_jump", 200, 2),
	SLAM_FALL("slam_fall", 200, 3),
	SLAM_LAND("slam_land", Timing.SLAM_LAND, 0),

	TITAN_GRAB("titan_grab", 120, 2),
	TITAN_THROW("titan_throw", Timing.THROW_TOTAL, 2),

	TARGET_HELD("target_held", 140, 3),
	TARGET_FLYING("target_flying", 200, 2),
	TARGET_LAND("target_land", Timing.TARGET_LAND, 0);

	private final String animation;
	private final int maxTicks;
	private final int fadeTicks;

	Cue(String animation, int maxTicks, int fadeTicks) {
		this.animation = animation;
		this.maxTicks = maxTicks;
		this.fadeTicks = fadeTicks;
	}

	/** Animation name inside the mod's animation files, or null for {@link #STOP}. */
	public String animation() {
		return animation;
	}

	/** Safety limit: a cue older than this is dropped even if no further packet arrives. */
	public int maxTicks() {
		return maxTicks;
	}

	/** Ticks used to blend from the previous pose. 0 means a hard cut, used for impacts. */
	public int fadeTicks() {
		return fadeTicks;
	}

	/** Whether the player's own movement keys are ignored while in this cue. */
	public boolean locksMovement(int age) {
		return switch (this) {
			case STOP -> false;
			case SLAM_JUMP, SLAM_FALL, TITAN_GRAB, TARGET_HELD, TARGET_FLYING -> true;
			case SLAM_LAND -> age < Timing.SLAM_LAND_ROOT;
			case TITAN_THROW -> age < Timing.THROW_TOTAL - 6;
			case TARGET_LAND -> age < Timing.TARGET_LAND_ROOT;
		};
	}

	/** Whether the player is standing their ground, so horizontal velocity is cancelled too. */
	public boolean plantsFeet(int age) {
		return switch (this) {
			case SLAM_JUMP -> age < Timing.SLAM_WINDUP;
			case SLAM_LAND, TITAN_GRAB, TITAN_THROW, TARGET_LAND -> locksMovement(age);
			case STOP, SLAM_FALL, TARGET_HELD, TARGET_FLYING -> false;
		};
	}

	public static Cue byId(int id) {
		Cue[] values = values();
		return id >= 0 && id < values.length ? values[id] : STOP;
	}
}
