package com.superpowermod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.superpowermod.SuperpowerMod;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Rebindable under Options > Controls > Key Binds > Superpowers. */
final class ModKeys {
	private static KeyMapping skySlam;
	private static KeyMapping titanThrow;

	private ModKeys() {
	}

	static void register() {
		KeyMapping.Category category = KeyMapping.Category.register(SuperpowerMod.id("abilities"));

		skySlam = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.superpowermod.sky_slam", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, category));
		titanThrow = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.superpowermod.titan_throw", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, category));
	}

	static KeyMapping skySlam() {
		return skySlam;
	}

	static KeyMapping titanThrow() {
		return titanThrow;
	}
}
