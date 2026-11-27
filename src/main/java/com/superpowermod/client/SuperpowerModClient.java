package com.superpowermod.client;

import com.superpowermod.common.Ability;
import com.superpowermod.network.ActivatePayload;
import com.superpowermod.network.CuePayload;
import com.superpowermod.network.ImpactPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class SuperpowerModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientConfig.load();
		ModKeys.register();
		ClientAnimations.register();

		ClientPlayNetworking.registerGlobalReceiver(CuePayload.TYPE,
				(payload, context) -> ClientCues.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(ImpactPayload.TYPE,
				(payload, context) -> ClientEffects.receive(context.client(), payload));

		ClientTickEvents.START_CLIENT_TICK.register(ClientCues::startTick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			handleKeys(client);

			if (client.level != null && !client.isPaused()) {
				ClientCues.endTick(client);
				ClientEffects.tick(client);
				CameraShake.tick();
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientCues.clear();
			ClientEffects.clear();
			CameraShake.clear();
		});
	}

	private static void handleKeys(Minecraft client) {
		while (ModKeys.skySlam().consumeClick()) {
			activate(client, Ability.SKY_SLAM);
		}

		while (ModKeys.titanThrow().consumeClick()) {
			activate(client, Ability.TITAN_THROW);
		}
	}

	private static void activate(Minecraft client, Ability ability) {
		if (client.player != null && ClientPlayNetworking.canSend(ActivatePayload.TYPE)) {
			ClientPlayNetworking.send(new ActivatePayload(ability.ordinal()));
		}
	}
}
