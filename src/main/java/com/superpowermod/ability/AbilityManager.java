package com.superpowermod.ability;

import com.superpowermod.common.Ability;
import com.superpowermod.config.ConfigManager;
import com.superpowermod.config.ModConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Validates activations, runs the active abilities and keeps cooldowns. Server thread only. */
public final class AbilityManager {
	private static final List<Session> SESSIONS = new ArrayList<>();
	private static final List<Session> PENDING = new ArrayList<>();
	private static final List<ShockwaveEffect> SHOCKWAVES = new ArrayList<>();
	/** Server tick on which each ability becomes ready again. */
	private static final Map<UUID, EnumMap<Ability, Long>> COOLDOWNS = new HashMap<>();

	private static long ticks;

	private AbilityManager() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(AbilityManager::tick);

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(entity instanceof ServerPlayer player
						&& source.is(DamageTypeTags.IS_FALL)
						&& FallProtection.isProtected(player.getUUID())));

		// Someone who walks into view part way through an ability still sees the right animation.
		EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
			if (tracked instanceof ServerPlayer player) {
				Sync.resend(player, viewer, ticks);
			}
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> reset());
	}

	public static void tryActivate(ServerPlayer player, Ability ability) {
		if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isFallFlying()
				|| isBusy(player.getUUID())) {
			tell(player, Component.translatable("message.superpowermod.busy"));
			return;
		}

		ConfigManager.reloadIfChanged();
		ModConfig config = ConfigManager.get();

		long readyAt = COOLDOWNS.getOrDefault(player.getUUID(), new EnumMap<>(Ability.class))
				.getOrDefault(ability, 0L);

		if (ticks < readyAt) {
			long seconds = (readyAt - ticks + 19) / 20;
			tell(player, Component.translatable("message.superpowermod.cooldown",
					Component.translatable(ability.translationKey()), seconds));
			return;
		}

		double cooldownSeconds;

		switch (ability) {
			case SKY_SLAM -> {
				if (player.isInWater() || player.isInLava()) {
					tell(player, Component.translatable("message.superpowermod.not_here"));
					return;
				}

				SESSIONS.add(new SkySlamSession(player, config.skySlam, ticks));
				cooldownSeconds = config.skySlam.cooldownSeconds;
			}
			case TITAN_THROW -> {
				ServerPlayer target = Targeting.findPlayer(player, config.titanThrow.grabRange);

				if (target == null) {
					tell(player, Component.translatable("message.superpowermod.no_target"));
					return;
				}

				if (target.isFallFlying() || isBusy(target.getUUID())) {
					tell(player, Component.translatable("message.superpowermod.target_busy"));
					return;
				}

				SESSIONS.add(new TitanThrowSession(player, target, config.titanThrow, ticks));
				cooldownSeconds = config.titanThrow.cooldownSeconds;
			}
			default -> throw new IllegalStateException("Unknown ability " + ability);
		}

		COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new EnumMap<>(Ability.class))
				.put(ability, ticks + Math.round(cooldownSeconds * 20));
	}

	private static void tick(MinecraftServer server) {
		ticks++;

		// Sessions may start follow-up sessions while ticking; those are queued in PENDING.
		SESSIONS.removeIf(session -> !session.tick(server));
		SESSIONS.addAll(PENDING);
		PENDING.clear();

		SHOCKWAVES.removeIf(wave -> !wave.tick(server));
		FallProtection.tick(server);
		Sync.tick(ticks);
	}

	static void startFollowUp(Session session) {
		PENDING.add(session);
	}

	static void addShockwave(ShockwaveEffect wave) {
		SHOCKWAVES.add(wave);
	}

	static boolean isBusy(UUID playerId) {
		for (Session session : SESSIONS) {
			if (session.involves(playerId)) return true;
		}

		for (Session session : PENDING) {
			if (session.involves(playerId)) return true;
		}

		return false;
	}

	static long currentTick() {
		return ticks;
	}

	private static void tell(ServerPlayer player, Component message) {
		player.displayClientMessage(message, true);
	}

	private static void reset() {
		SESSIONS.clear();
		PENDING.clear();
		SHOCKWAVES.clear();
		COOLDOWNS.clear();
		FallProtection.clear();
		Sync.clear();
		ticks = 0;
	}
}
