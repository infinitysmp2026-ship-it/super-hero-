package com.superpowermod.network;

import com.superpowermod.ability.AbilityManager;
import com.superpowermod.common.Ability;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(ActivatePayload.TYPE, ActivatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CuePayload.TYPE, CuePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ImpactPayload.TYPE, ImpactPayload.CODEC);

		// Fabric runs play payload handlers on the server thread.
		ServerPlayNetworking.registerGlobalReceiver(ActivatePayload.TYPE, (payload, context) -> {
			Ability ability = Ability.byId(payload.ability());

			if (ability != null) {
				AbilityManager.tryActivate(context.player(), ability);
			}
		});
	}
}
