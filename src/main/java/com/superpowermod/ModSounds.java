package com.superpowermod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	private ModSounds() {
	}

	public static final SoundEvent SLAM_LAUNCH = create("slam_launch");
	public static final SoundEvent SLAM_DIVE = create("slam_dive");
	public static final SoundEvent SLAM_IMPACT = create("slam_impact");
	public static final SoundEvent TITAN_GRAB = create("titan_grab");
	public static final SoundEvent TITAN_THROW = create("titan_throw");
	public static final SoundEvent TITAN_LAND = create("titan_land");

	private static SoundEvent create(String name) {
		Identifier id = SuperpowerMod.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/** Calling this loads the class, which registers the sounds above. */
	public static void register() {
	}
}
