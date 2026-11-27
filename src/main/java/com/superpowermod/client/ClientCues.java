package com.superpowermod.client;

import com.superpowermod.common.Cue;
import com.superpowermod.common.GripPath;
import com.superpowermod.common.GripPositions;
import com.superpowermod.common.Timing;
import com.superpowermod.network.CuePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** What each visible player is doing, and everything that has to follow from it every tick. */
final class ClientCues {
	private static final class State {
		final Cue cue;
		final int partnerId;
		int age;
		/** False until the player entity exists on this client and the animation has started. */
		boolean started;
		float flightYaw = Float.NaN;

		State(Cue cue, int partnerId, int age) {
			this.cue = cue;
			this.partnerId = partnerId;
			this.age = age;
		}
	}

	private static final Map<Integer, State> STATES = new HashMap<>();

	private ClientCues() {
	}

	static Cue cueOf(int entityId) {
		State state = STATES.get(entityId);
		return state == null ? Cue.STOP : state.cue;
	}

	static void receive(CuePayload payload) {
		Cue cue = Cue.byId(payload.cue());
		State previous = STATES.get(payload.entityId());
		State state = new State(cue, payload.partnerId(), payload.age());

		if (previous != null) {
			state.flightYaw = previous.flightYaw;
		}

		STATES.put(payload.entityId(), state);
		start(Minecraft.getInstance(), payload.entityId(), state);
	}

	private static void start(Minecraft client, int entityId, State state) {
		if (client.level != null && client.level.getEntity(entityId) instanceof AbstractClientPlayer player) {
			ClientAnimations.play(player, state.cue, state.age);
			state.started = true;

			if (state.cue == Cue.STOP) {
				STATES.remove(entityId);
			}
		}
	}

	/** Runs before the player moves: releases movement keys while an ability has control. */
	static void startTick(Minecraft client) {
		if (client.player == null) return;

		State state = STATES.get(client.player.getId());

		if (state == null || !state.cue.locksMovement(state.age)) return;

		if (state.cue.plantsFeet(state.age)) {
			// removes leftover sliding and pushes from other hitboxes
			Vec3 velocity = client.player.getDeltaMovement();
			client.player.setDeltaMovement(0, velocity.y, 0);
		}

		Options options = client.options;

		for (KeyMapping key : new KeyMapping[] {
				options.keyUp, options.keyDown, options.keyLeft, options.keyRight, options.keyJump, options.keyShift,
				options.keySprint
		}) {
			key.setDown(false);
		}
	}

	/** Runs after all entities have moved, so these positions are the ones that get drawn. */
	static void endTick(Minecraft client) {
		Iterator<Map.Entry<Integer, State>> iterator = STATES.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<Integer, State> entry = iterator.next();
			State state = entry.getValue();
			state.age++;

			if (state.age > state.cue.maxTicks()) {
				if (client.level.getEntity(entry.getKey()) instanceof AbstractClientPlayer player) {
					ClientAnimations.play(player, Cue.STOP, 0);
				}

				iterator.remove();
				continue;
			}

			if (!(client.level.getEntity(entry.getKey()) instanceof AbstractClientPlayer player)) continue;

			if (!state.started) {
				ClientAnimations.play(player, state.cue, state.age);
				state.started = true;
			}

			apply(client, player, state);
		}
	}

	private static void apply(Minecraft client, AbstractClientPlayer player, State state) {
		switch (state.cue) {
			case SLAM_JUMP, SLAM_FALL, SLAM_LAND, TITAN_GRAB, TITAN_THROW ->
					// the body faces where the hero is looking, so poses point at the target
					faceBody(player, player.getYRot());
			case TARGET_HELD -> holdInHand(client, player, state);
			case TARGET_FLYING -> {
				Vec3 moved = new Vec3(player.getX() - player.xo, 0, player.getZ() - player.zo);

				if (moved.lengthSqr() > 0.04) {
					// turn the back towards the direction of travel
					state.flightYaw = (float) Math.toDegrees(Math.atan2(-moved.x, moved.z)) + 180;
				}

				if (!Float.isNaN(state.flightYaw)) faceBody(player, state.flightYaw);
			}
			case TARGET_LAND -> {
				if (!Float.isNaN(state.flightYaw)) faceBody(player, state.flightYaw);
			}
			default -> {
			}
		}

		ClientEffects.trail(client, player, state.cue, state.age);
	}

	private static void holdInHand(Minecraft client, AbstractClientPlayer target, State state) {
		Entity holder = client.level.getEntity(state.partnerId);

		if (holder == null) return;

		State holderState = STATES.get(state.partnerId);
		GripPath.Offset offset = holderState != null && holderState.cue == Cue.TITAN_THROW
				? GripPath.throwing(holderState.age)
				: GripPath.holding(state.age);

		Vec3 hand = GripPositions.resolve(client.level, target, holder.position(), holder.getYRot(), offset);

		if (state.age < Timing.GRAB_REACH && holderState != null && holderState.cue == Cue.TITAN_GRAB) {
			// pulled into the grip over the first few ticks instead of jumping there
			Vec3 from = new Vec3(target.xo, target.yo, target.zo);
			hand = from.lerp(hand, 1.0 / (Timing.GRAB_REACH - state.age + 1));
		}

		target.setPos(hand.x, hand.y, hand.z);
		target.setDeltaMovement(Vec3.ZERO);
		state.flightYaw = holder.getYRot() + 180;
		faceBody(target, state.flightYaw);
	}

	private static void faceBody(AbstractClientPlayer player, float yaw) {
		player.yBodyRot = yaw;
		player.yBodyRotO = yaw;
	}

	static void clear() {
		STATES.clear();
	}
}
