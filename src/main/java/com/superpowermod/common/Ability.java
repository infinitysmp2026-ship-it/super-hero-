package com.superpowermod.common;

/** The two abilities. The ordinal is what travels over the network. */
public enum Ability {
	SKY_SLAM("sky_slam"),
	TITAN_THROW("titan_throw");

	private final String id;

	Ability(String id) {
		this.id = id;
	}

	public String translationKey() {
		return "ability.superpowermod." + id;
	}

	/** Returns null for an unknown id, so a bad packet is simply ignored. */
	public static Ability byId(int id) {
		Ability[] values = values();
		return id >= 0 && id < values.length ? values[id] : null;
	}
}
