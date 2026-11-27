package com.superpowermod.client;

import com.superpowermod.SuperpowerMod;
import com.superpowermod.common.Cue;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;

/** The bridge to Player Animation Library: one animation layer per player. */
final class ClientAnimations {
	private static final Identifier LAYER = SuperpowerMod.id("abilities");
	/** High enough to override emote and idle animation mods while an ability plays. */
	private static final int PRIORITY = 1500;

	private ClientAnimations() {
	}

	static void register() {
		PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, PRIORITY, avatar -> {
			// STOP means "play nothing unless an animation has been triggered"
			PlayerAnimationController controller = new PlayerAnimationController(avatar,
					(self, data, setter) -> PlayState.STOP);

			controller.setFirstPersonModeHandler(self -> usesFirstPersonArms(avatar)
					? FirstPersonMode.THIRD_PERSON_MODEL
					: FirstPersonMode.NONE);
			controller.setFirstPersonConfiguration(new FirstPersonConfiguration(true, true, true, true));
			return controller;
		});
	}

	private static boolean usesFirstPersonArms(Avatar avatar) {
		if (!ClientConfig.get().firstPersonArms) return false;

		Cue cue = ClientCues.cueOf(avatar.getId());
		return cue == Cue.TITAN_GRAB || cue == Cue.TITAN_THROW;
	}

	static void play(AbstractClientPlayer player, Cue cue, int age) {
		PlayerAnimationController controller = controller(player);

		if (controller == null) return;

		controller.removeAllModifiers();

		if (cue == Cue.STOP) {
			controller.stopTriggeredAnimation();
			return;
		}

		Identifier animation = SuperpowerMod.id(cue.animation());
		boolean found;

		if (age == 0 && cue.fadeTicks() > 0) {
			found = controller.replaceAnimationWithFade(
					AbstractFadeModifier.standardFadeIn(cue.fadeTicks(), EasingType.EASE_IN_OUT_SINE), animation);
		} else {
			found = controller.triggerAnimation(animation, age);
		}

		if (!found) {
			SuperpowerMod.LOGGER.warn("Animation {} is missing", animation);
		}
	}

	private static PlayerAnimationController controller(AbstractClientPlayer player) {
		IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER);
		return layer instanceof PlayerAnimationController controller ? controller : null;
	}
}
