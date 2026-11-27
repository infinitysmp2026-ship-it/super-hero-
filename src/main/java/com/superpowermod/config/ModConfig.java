package com.superpowermod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Server settings, stored as config/superpowermod.json. */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public SkySlam skySlam = new SkySlam();
	public TitanThrow titanThrow = new TitanThrow();
	public Protection protection = new Protection();

	public static final class SkySlam {
		public double cooldownSeconds = 8;
		/** Blocks the hero rises before diving. */
		public double jumpHeight = 22;
		/** Blocks per tick while diving. */
		public double diveSpeed = 2.4;
		/** How far away you can aim the dive by looking at a spot. 0 always dives straight down. */
		public double aimRange = 40;
		public double shockwaveRadius = 9;
		/** Sideways launch speed given to nearby players. */
		public double knockbackStrength = 1.8;
		public double knockbackUpward = 0.9;
		/** Health points (2 = one heart) dealt to players hit by the shockwave. */
		public double shockwaveDamage = 0;
		public boolean breakBlocks = true;
		public int blockRadius = 4;
		/** Crater depth at the centre. */
		public int blockDepth = 2;
		public int maxBlocks = 250;
		/** Blocks harder than this survive. Stone is 1.5, obsidian is 50. */
		public double maxBlockHardness = 25;
		public boolean dropBrokenBlocks = false;
	}

	public static final class TitanThrow {
		public double cooldownSeconds = 10;
		public double grabRange = 4.5;
		/** Ticks the victim is held before the throw begins (20 = 1 second). */
		public int holdTicks = 30;
		/** Forward launch speed. 2.8 sends a player about 30 blocks on level ground. */
		public double throwStrength = 2.8;
		public double throwUpward = 0.9;
		/** Health points dealt when the victim lands. */
		public double landingDamage = 0;
	}

	public static final class Protection {
		/** Never break chests, furnaces, signs, beds, spawners and other blocks that store data. */
		public boolean protectBlockEntities = true;
		/** Extra protected block ids, for example "minecraft:diamond_block". */
		public List<String> protectedBlocks = new ArrayList<>();
		/** Also protect the six blocks touching a protected block, so it keeps its support. */
		public boolean protectNeighbours = true;
	}

	/**
	 * Reads the file, or creates it with defaults when it does not exist. A broken file is reported by
	 * throwing, and is left untouched so the user's edits are not lost.
	 */
	public static ModConfig load(Path file) throws IOException {
		if (Files.notExists(file)) {
			ModConfig defaults = new ModConfig();
			defaults.save(file);
			return defaults;
		}

		ModConfig config;

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			config = GSON.fromJson(reader, ModConfig.class);
		} catch (JsonParseException e) {
			throw new IOException("Invalid JSON in " + file + ": " + e.getMessage(), e);
		}

		if (config == null) {
			config = new ModConfig();
		}

		config.sanitize();
		return config;
	}

	public void save(Path file) throws IOException {
		Path parent = file.getParent();

		if (parent != null) {
			Files.createDirectories(parent);
		}

		try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			GSON.toJson(this, writer);
		}
	}

	/** Keeps every value in a range that cannot crash the server or trip vanilla anti-flying checks. */
	public void sanitize() {
		if (skySlam == null) skySlam = new SkySlam();
		if (titanThrow == null) titanThrow = new TitanThrow();
		if (protection == null) protection = new Protection();
		if (protection.protectedBlocks == null) protection.protectedBlocks = new ArrayList<>();

		skySlam.cooldownSeconds = clamp(skySlam.cooldownSeconds, 0, 3600);
		skySlam.jumpHeight = clamp(skySlam.jumpHeight, 4, 60);
		skySlam.diveSpeed = clamp(skySlam.diveSpeed, 0.8, 3.8);
		skySlam.aimRange = clamp(skySlam.aimRange, 0, 96);
		skySlam.shockwaveRadius = clamp(skySlam.shockwaveRadius, 1, 32);
		skySlam.knockbackStrength = clamp(skySlam.knockbackStrength, 0, 3.9);
		skySlam.knockbackUpward = clamp(skySlam.knockbackUpward, 0, 3);
		skySlam.shockwaveDamage = clamp(skySlam.shockwaveDamage, 0, 1000);
		skySlam.blockRadius = (int) clamp(skySlam.blockRadius, 0, 12);
		skySlam.blockDepth = (int) clamp(skySlam.blockDepth, 0, 6);
		skySlam.maxBlocks = (int) clamp(skySlam.maxBlocks, 0, 3000);
		skySlam.maxBlockHardness = clamp(skySlam.maxBlockHardness, 0, 10000);

		titanThrow.cooldownSeconds = clamp(titanThrow.cooldownSeconds, 0, 3600);
		titanThrow.grabRange = clamp(titanThrow.grabRange, 1, 16);
		titanThrow.holdTicks = (int) clamp(titanThrow.holdTicks, 10, 50);
		titanThrow.throwStrength = clamp(titanThrow.throwStrength, 0.5, 3.9);
		titanThrow.throwUpward = clamp(titanThrow.throwUpward, 0, 3);
		titanThrow.landingDamage = clamp(titanThrow.landingDamage, 0, 1000);
	}

	private static double clamp(double value, double min, double max) {
		if (Double.isNaN(value)) return min;
		return Math.max(min, Math.min(max, value));
	}
}
