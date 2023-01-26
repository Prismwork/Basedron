package io.github.prismwork.basedron.event;

import io.github.prismwork.basedron.Basedron;
import io.github.prismwork.basedron.api.CauldronInteractionEvent;
import io.github.prismwork.basedron.block.CauldronBlockEntity;
import io.github.prismwork.basedron.util.Constants;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ColorUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public final class CauldronInteractionImpl implements CauldronInteractionEvent.CauldronInteraction {
	@Override
	public @NotNull ActionResult interact(@NotNull CauldronBlockEntity cauldron,
										  PlayerEntity player,
										  Hand hand,
										  BlockHitResult hit) {
		World world = cauldron.getWorld();
		BlockPos pos = cauldron.getPos();
		BlockState state = world.getBlockState(pos);
		ItemStack stack = player.getStackInHand(hand);
		Storage<FluidVariant> another = FluidStorage.ITEM.find(
				stack,
				ContainerItemContext.forPlayerInteraction(player, hand)
		);
		if (another != null) {
			another.iterator().forEachRemaining(view -> {
				if (!view.isResourceBlank()) {
					try (Transaction transaction = Transaction.openOuter()) {
						long actualExtract = StorageUtil.move(
								another,
								cauldron.getFluidStorage(),
								fluidVariant -> view.getResource().equals(cauldron.getFluidStorage().variant) ||
										cauldron.getFluidStorage().isResourceBlank(),
								FluidConstants.BUCKET,
								transaction
						);
						if (actualExtract > 0) {
							world.playSound(
									null,
									pos,
									getBucketSound(view.getResource(), true),
									SoundCategory.BLOCKS
							);
							transaction.commit();
						}
					}
				} else {
					if (!cauldron.getFluidStorage().variant.isOf(Basedron.POWDER_SNOW)) {
						try (Transaction transaction = Transaction.openOuter()) {
							long actualExtract = StorageUtil.move(
									cauldron.getFluidStorage(),
									another,
									fluidVariant -> !cauldron.getFluidStorage().isResourceBlank(),
									FluidConstants.BUCKET,
									transaction
							);
							if (actualExtract > 0) {
								world.playSound(
										null,
										pos,
										getBucketSound(cauldron.getFluidStorage().variant, false),
										SoundCategory.BLOCKS
								);
								transaction.commit();
							}
						}
					} else {
						try (Transaction transaction = Transaction.openOuter()) {
							long actualExtract = cauldron.getFluidStorage().extract(
									cauldron.getFluidStorage().variant,
									FluidConstants.BUCKET,
									transaction
							);
							if (actualExtract >= FluidConstants.BUCKET) {
								world.playSound(
										null,
										pos,
										getBucketSound(FluidVariant.of(Basedron.POWDER_SNOW), false),
										SoundCategory.BLOCKS
								);
								ItemUsage.exchangeStack(
										stack,
										player,
										Items.POWDER_SNOW_BUCKET.getDefaultStack()
								);
								transaction.commit();
							}
						}
					}
				}
			});
			world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
			return ActionResult.CONSUME;
		} else if (stack.getItem() instanceof DyeItem dye) {
			DyeColor color = dye.getColor();
			int dyeColor = ColorUtil.ARGB32.getArgb(
					256,
					(int) (color.getColorComponents()[0] * 255.0f),
					(int) (color.getColorComponents()[1] * 255.0f),
					(int) (color.getColorComponents()[2] * 255.0f)
			);
			if (Constants.DYEABLE_FLUIDS.contains(cauldron.getFluidStorage().variant.getFluid())) {
				if (!cauldron.getFluidStorage().variant.isOf(Basedron.COLORED_WATER)) {
					cauldron.getFluidStorage().variant = FluidVariant.of(Basedron.COLORED_WATER);
				}
				cauldron.setFluidTint(cauldron.mixColor(dyeColor));
				world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
				return ActionResult.SUCCESS;
			}
		} else if (stack.getItem() instanceof DyeableItem dyeable) {
			if (cauldron.getFluidStorage().variant.isOf(Fluids.WATER) && dyeable.hasColor(stack)) {
				cauldron.getFluidStorage().variant = FluidVariant.of(Basedron.COLORED_WATER);
				cauldron.setFluidTint(cauldron.mixColor(dyeable.getColor(stack)));
				world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
				dyeable.removeColor(stack);
				return ActionResult.SUCCESS;
			} else if (cauldron.getFluidStorage().variant.isOf(Basedron.COLORED_WATER)) {
				try (Transaction transaction = Transaction.openOuter()) {
					long actualUsed = cauldron.getFluidStorage().extract(
							cauldron.getFluidStorage().variant,
							FluidConstants.BOTTLE,
							transaction
					);
					if (actualUsed >= FluidConstants.BOTTLE) {
						dyeable.setColor(stack, cauldron.getFluidTint());
						world.playSound(
								null,
								pos,
								getBucketSound(cauldron.getFluidStorage().variant, true),
								SoundCategory.BLOCKS
						);
						transaction.commit();
					}
				}
				world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
				return ActionResult.SUCCESS;
			}
		}
		return ActionResult.PASS;
	}

	private static SoundEvent getBucketSound(FluidVariant fluid, boolean empty) {
		if (fluid.getFluid().isIn(FluidTags.LAVA)) return empty ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_FILL_LAVA;
		else if (fluid.isOf(Basedron.POWDER_SNOW)) return empty ? SoundEvents.ITEM_BUCKET_EMPTY_POWDER_SNOW : SoundEvents.ITEM_BUCKET_FILL_POWDER_SNOW;
		else return empty ? SoundEvents.ITEM_BUCKET_EMPTY : SoundEvents.ITEM_BUCKET_FILL;
	}
}
