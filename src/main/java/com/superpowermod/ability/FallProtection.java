package com.superpowermod.ability;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Stops ability launches from causing fall damage until the player is safely back on the ground. */
final class FallProtection {
	private static final int MAX_TICKS = 20 * 20;
	private static final int TICKS_AFTER_LANDING = 10;
	/** A launch needs a few ticks to leave the ground before "on ground" means "landed". */
	private static final int TAKEOFF_TICKS = 10;

	private static final class Grace {
		int age;
		int grounded;
	}

	private static final Map<UUID, Grace> PROTECTED = new HashMap<>();

	private FallProtection() {
	}

	static void protect(ServerPlayer player) {
		PROTECTED.put(player.getUUID(), new Grace());
		player.resetFallDistance();
	}

	static boolean isProtected(UUID playerId) {
		return PROTECTED.containsKey(playerId);
	}

	static void tick(MinecraftServer server) {
		PROTECTED.entrySet().removeIf(entry -> {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Grace grace = entry.getValue();

			if (player == null || !player.isAlive() || ++grace.age > MAX_TICKS) {
				return true;
			}

			// A player still in an ability keeps protection, for example while held in the air.
			boolean landed = (player.onGround() || player.isInWater())
					&& grace.age > TAKEOFF_TICKS
					&& !AbilityManager.isBusy(entry.getKey());
			grace.grounded = landed ? grace.grounded + 1 : 0;

			if (grace.grounded > TICKS_AFTER_LANDING) {
				player.resetFallDistance();
				return true;
			}

			return false;
		});
	}

	static void clear() {
		PROTECTED.clear();
	}
}
