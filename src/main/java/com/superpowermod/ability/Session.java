package com.superpowermod.ability;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** A running ability, advanced once per server tick until it reports that it is done. */
interface Session {
	/** @return false once the session is finished */
	boolean tick(MinecraftServer server);

	/** Whether this player is currently taking part, and so cannot start or receive another ability. */
	boolean involves(UUID playerId);

	/** A participant is usable while online, alive, in the same dimension and not spectating. */
	static boolean isUsable(ServerPlayer player, ServerLevel level) {
		return player != null
				&& !player.isRemoved()
				&& player.isAlive()
				&& !player.isSpectator()
				&& player.level() == level;
	}
}
