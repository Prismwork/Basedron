package io.github.prismwork.basedron.api;

import io.github.prismwork.basedron.block.CauldronBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.qsl.base.api.event.Event;
import org.quiltmc.qsl.base.api.event.EventAwareListener;

/**
 * The event that handles unique cauldron interactions.
 *
 * @see CauldronBlockEntity
 */
@SuppressWarnings("unused")
public final class CauldronInteractionEvent {
	/**
	 * The instance of the event.
	 */
	public static final Event<CauldronInteraction> INSTANCE = Event.create(CauldronInteraction.class, callbacks ->
			(cauldron, player, hand, hit) -> {
		for (CauldronInteraction interaction : callbacks) {
			ActionResult ret = interaction.interact(cauldron, player, hand, hit);
			if (ret != ActionResult.PASS) {
				return ret;
			}
		}
		return ActionResult.PASS;
	});

	@FunctionalInterface
	public interface CauldronInteraction extends EventAwareListener {
		/**
		 * Interacts with a cauldron and returns the result.
		 *
		 * @param cauldron the cauldron block entity. Will never be null.
		 * @param player the player interacting
		 * @param hand the hand the player uses while interacting
		 * @param hit the hit result while interacting
		 * @return the result of this interaction
		 */
		@NotNull ActionResult interact(@NotNull CauldronBlockEntity cauldron, PlayerEntity player, Hand hand, BlockHitResult hit);
	}
}
