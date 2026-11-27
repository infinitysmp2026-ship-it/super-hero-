package com.superpowermod;

import com.superpowermod.ability.AbilityManager;
import com.superpowermod.config.ConfigManager;
import com.superpowermod.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SuperpowerMod implements ModInitializer {
	public static final String MOD_ID = "superpowermod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ConfigManager.reloadIfChanged();
		ModSounds.register();
		ModNetworking.register();
		AbilityManager.register();
		LOGGER.info("Superpower Mod loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
