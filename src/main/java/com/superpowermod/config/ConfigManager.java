package com.superpowermod.config;

import com.superpowermod.SuperpowerMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * Holds the current server config. The file is re-read when it changes, so you can tune values between
 * takes without restarting: save the file, then use an ability.
 */
public final class ConfigManager {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("superpowermod.json");

	private static ModConfig current = new ModConfig();
	private static FileTime loadedAt;

	private ConfigManager() {
	}

	public static ModConfig get() {
		return current;
	}

	public static void reloadIfChanged() {
		try {
			if (Files.exists(FILE) && Files.getLastModifiedTime(FILE).equals(loadedAt)) {
				return;
			}

			boolean first = loadedAt == null;
			// remember the time before parsing, so a broken file is only reported once per save
			loadedAt = Files.exists(FILE) ? Files.getLastModifiedTime(FILE) : null;
			current = ModConfig.load(FILE);
			loadedAt = Files.getLastModifiedTime(FILE);

			if (!first) {
				SuperpowerMod.LOGGER.info("Reloaded {}", FILE);
			}
		} catch (IOException e) {
			SuperpowerMod.LOGGER.error("Could not load {}. Keeping the previous settings.", FILE, e);
		}
	}
}
