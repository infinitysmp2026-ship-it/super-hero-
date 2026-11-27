package com.superpowermod.common;

/** Visual effect bursts that clients draw when the server asks for them. */
public enum ImpactKind {
	LAUNCH,
	SLAM,
	GRAB,
	THROW,
	CRASH;

	public static ImpactKind byId(int id) {
		ImpactKind[] values = values();
		return id >= 0 && id < values.length ? values[id] : LAUNCH;
	}
}
