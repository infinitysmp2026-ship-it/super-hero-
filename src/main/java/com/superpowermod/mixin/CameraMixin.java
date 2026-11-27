package com.superpowermod.mixin;

import com.superpowermod.client.CameraShake;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private float xRot;

	@Shadow
	private float yRot;

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	// require = 0: if another mod rewrites this method, the shake is skipped instead of crashing the game
	@Inject(method = "setup", at = @At("TAIL"), require = 0)
	private void superpowermod$shake(Level level, Entity entity, boolean detached, boolean mirrored,
			float partialTick, CallbackInfo ci) {
		float yaw = CameraShake.yawOffset(partialTick);
		float pitch = CameraShake.pitchOffset(partialTick);

		if (yaw != 0 || pitch != 0) {
			setRotation(yRot + yaw, xRot + pitch);
		}
	}
}
