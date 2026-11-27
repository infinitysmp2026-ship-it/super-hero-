package com.superpowermod.ability;

import com.superpowermod.common.Cue;
import com.superpowermod.common.ImpactKind;
import com.superpowermod.network.CuePayload;
import com.superpowermod.network.ImpactPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Everything the server sends to clients. */
final class Sync {
	private static final double EFFECT_RANGE = 128;

	private record Active(Cue cue, int partnerId, long startTick) {
	}

	private static final Map<UUID, Active> ACTIVE = new HashMap<>();

	private Sync() {
	}

	/** Puts a player into an animation state and tells everyone who can see them, themselves included. */
	static void cue(ServerPlayer player, Cue cue, ServerPlayer partner) {
		int partnerId = partner == null ? -1 : partner.getId();

		if (cue == Cue.STOP) {
			ACTIVE.remove(player.getUUID());
		} else {
			ACTIVE.put(player.getUUID(), new Active(cue, partnerId, AbilityManager.currentTick()));
		}

		CuePayload payload = new CuePayload(player.getId(), cue.ordinal(), partnerId, 0);
		Set<ServerPlayer> viewers = new HashSet<>(PlayerLookup.tracking(player));
		viewers.add(player);

		if (partner != null) {
			viewers.add(partner);
		}

		for (ServerPlayer viewer : viewers) {
			send(viewer, payload);
		}
	}

	static void resend(ServerPlayer subject, ServerPlayer viewer, long now) {
		Active active = ACTIVE.get(subject.getUUID());

		if (active != null) {
			send(viewer, new CuePayload(subject.getId(), active.cue().ordinal(), active.partnerId(),
					(int) (now - active.startTick())));
		}
	}

	static void impact(ServerLevel level, Vec3 pos, ImpactKind kind, double radius) {
		ImpactPayload payload = new ImpactPayload(pos.x, pos.y, pos.z, kind.ordinal(), (float) radius);

		for (ServerPlayer viewer : level.players()) {
			if (viewer.position().distanceToSqr(pos) < EFFECT_RANGE * EFFECT_RANGE) {
				send(viewer, payload);
			}
		}
	}

	static void sound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume) {
		float pitch = 0.95F + level.getRandom().nextFloat() * 0.1F;
		level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
	}

	/** Player movement runs on the owning client, so velocity has to be sent to that client. */
	static void velocity(ServerPlayer player, Vec3 velocity) {
		player.setDeltaMovement(velocity);
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}

	private static void send(ServerPlayer viewer, CustomPacketPayload payload) {
		if (ServerPlayNetworking.canSend(viewer, payload.type())) {
			ServerPlayNetworking.send(viewer, payload);
		}
	}

	/** Forgets cues that have run their course. */
	static void tick(long now) {
		ACTIVE.values().removeIf(active -> now - active.startTick() > active.cue().maxTicks());
	}

	static void clear() {
		ACTIVE.clear();
	}
}
