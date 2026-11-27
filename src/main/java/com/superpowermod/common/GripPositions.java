package com.superpowermod.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Turns a {@link GripPath.Offset} into a world position that does not push the victim into a wall. */
public final class GripPositions {
	private GripPositions() {
	}

	public static Vec3 resolve(Level level, Entity target, Vec3 holderPos, float holderYaw, GripPath.Offset offset) {
		double yaw = Math.toRadians(holderYaw);
		double forwardX = -Math.sin(yaw);
		double forwardZ = Math.cos(yaw);
		double rightX = -Math.cos(yaw);
		double rightZ = -Math.sin(yaw);

		Vec3 wanted = holderPos.add(
				forwardX * offset.forward() + rightX * offset.right(),
				offset.up(),
				forwardZ * offset.forward() + rightZ * offset.right());

		// Walk from the wanted spot back towards the holder until the victim's hitbox fits.
		for (int step = 0; step <= 4; step++) {
			double t = step / 4.0;
			Vec3 candidate = wanted.add(holderPos.subtract(wanted).scale(t));
			AABB box = target.getBoundingBox().move(candidate.subtract(target.position()));

			if (level.noCollision(target, box.deflate(0.02))) {
				return candidate;
			}
		}

		// The holder's own position is always free enough to stand in.
		return holderPos;
	}
}
