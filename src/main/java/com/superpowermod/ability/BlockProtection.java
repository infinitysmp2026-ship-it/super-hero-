package com.superpowermod.ability;

import com.superpowermod.SuperpowerMod;
import com.superpowermod.config.ModConfig;
import com.superpowermod.config.ConfigManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The only place the mod removes blocks. Every block has to pass all of these checks. */
final class BlockProtection {
	private static final TagKey<Block> PROTECTED =
			TagKey.create(Registries.BLOCK, SuperpowerMod.id("protected"));

	private BlockProtection() {
	}

	static boolean tryBreak(ServerLevel level, ServerPlayer player, BlockPos pos, ModConfig.SkySlam config) {
		BlockState state = level.getBlockState(pos);

		if (state.isAir()) return false;

		// water and lava themselves are left alone
		if (state == state.getFluidState().createLegacyBlock()) return false;

		float hardness = state.getDestroySpeed(level, pos);

		// negative hardness means unbreakable: bedrock, barriers, portal frames and so on
		if (hardness < 0 || hardness > config.maxBlockHardness) return false;

		ModConfig.Protection protection = ConfigManager.get().protection;

		if (isProtected(level, pos, protection)) return false;

		if (protection.protectNeighbours) {
			for (Direction direction : Direction.values()) {
				if (isProtected(level, pos.relative(direction), protection)) return false;
			}
		}

		// Adventure mode, spawn protection and the world border
		if (!player.getAbilities().mayBuild || !level.mayInteract(player, pos)) return false;

		// Claim and region mods listen to this Fabric event.
		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, blockEntity)) {
			PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(level, player, pos, state, blockEntity);
			return false;
		}

		boolean removed = config.dropBrokenBlocks
				? level.destroyBlock(pos, true, player)
				: level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);

		if (removed) {
			PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, player, pos, state, blockEntity);
		}

		return removed;
	}

	private static boolean isProtected(ServerLevel level, BlockPos pos, ModConfig.Protection protection) {
		BlockState state = level.getBlockState(pos);

		if (state.isAir()) return false;
		if (protection.protectBlockEntities && state.hasBlockEntity()) return true;
		if (state.is(PROTECTED)) return true;

		return protection.protectedBlocks.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
	}
}
