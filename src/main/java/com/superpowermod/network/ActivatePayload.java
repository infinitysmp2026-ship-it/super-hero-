package com.superpowermod.network;

import com.superpowermod.SuperpowerMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: "I pressed an ability key". The server does all checks and targeting. */
public record ActivatePayload(int ability) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ActivatePayload> TYPE =
			new CustomPacketPayload.Type<>(SuperpowerMod.id("activate"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ActivatePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ActivatePayload::ability,
			ActivatePayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
