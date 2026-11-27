package com.superpowermod.network;

import com.superpowermod.SuperpowerMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: draw particles and shake the camera at a position.
 *
 * @param kind   ordinal of {@link com.superpowermod.common.ImpactKind}
 * @param radius size of the effect in blocks
 */
public record ImpactPayload(double x, double y, double z, int kind, float radius) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ImpactPayload> TYPE =
			new CustomPacketPayload.Type<>(SuperpowerMod.id("impact"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ImpactPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, ImpactPayload::x,
			ByteBufCodecs.DOUBLE, ImpactPayload::y,
			ByteBufCodecs.DOUBLE, ImpactPayload::z,
			ByteBufCodecs.VAR_INT, ImpactPayload::kind,
			ByteBufCodecs.FLOAT, ImpactPayload::radius,
			ImpactPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
