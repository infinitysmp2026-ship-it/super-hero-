package com.superpowermod.network;

import com.superpowermod.SuperpowerMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: a player changed animation state.
 *
 * @param entityId  the animated player
 * @param cue       ordinal of {@link com.superpowermod.common.Cue}
 * @param partnerId the thrower, for the grabbed player; otherwise -1
 * @param age       ticks already elapsed, for viewers who arrive part way through
 */
public record CuePayload(int entityId, int cue, int partnerId, int age) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CuePayload> TYPE =
			new CustomPacketPayload.Type<>(SuperpowerMod.id("cue"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CuePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CuePayload::entityId,
			ByteBufCodecs.VAR_INT, CuePayload::cue,
			ByteBufCodecs.INT, CuePayload::partnerId,
			ByteBufCodecs.VAR_INT, CuePayload::age,
			CuePayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
