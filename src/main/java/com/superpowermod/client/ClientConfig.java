package com.superpowermod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.superpowermod.SuperpowerMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Per-player visual settings, stored as config/superpowermod-client.json. Read when the game starts. */
public final class ClientConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static ClientConfig current = new ClientConfig();

	public boolean cameraShake = true;
	/** 1 is the default strength, 2 is twice as violent. */
	public double cameraShakeStrength = 1;
	/** Show your animated arms in first person while grabbing and throwing. */
	public boolean firstPersonArms = true;
	/** Scales the number of effect particles, from 0.25 to 2. */
	public double particleDensity = 1;

	static ClientConfig get() {
		return current;
	}

	static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("superpowermod-client.json");

		try {
			if (Files.exists(file)) {
				try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
					ClientConfig loaded = GSON.fromJson(reader, ClientConfig.class);

					if (loaded != null) current = loaded;
				}
			} else {
				Files.createDirectories(file.getParent());

				try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
					GSON.toJson(current, writer);
				}
			}
		} catch (IOException | RuntimeException e) {
			SuperpowerMod.LOGGER.error("Could not load {}. Using default client settings.", file, e);
		}

		current.cameraShakeStrength = Math.max(0, Math.min(4, current.cameraShakeStrength));
		current.particleDensity = Math.max(0.25, Math.min(2, current.particleDensity));
	}
}
