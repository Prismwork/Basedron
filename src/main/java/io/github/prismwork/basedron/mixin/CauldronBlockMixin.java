package io.github.prismwork.basedron.mixin;

import io.github.prismwork.basedron.Basedron;
import io.github.prismwork.basedron.block.CauldronBlockEntity;
import io.github.prismwork.basedron.util.BlockEntityHelper;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.*;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(CauldronBlock.class)
public abstract class CauldronBlockMixin extends AbstractCauldronBlock
		implements BlockEntityProvider {
	@Shadow
	protected static boolean canFillWithPrecipitation(World world, Biome.Precipitation precipitation) {
		return false;
	}

	public CauldronBlockMixin(Settings settings, Map<Item, CauldronBehavior> behaviorMap) {
		super(settings, behaviorMap);
	}

	@Override
	public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new CauldronBlockEntity(pos, state);
	}

	@Override
	public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return BlockEntityHelper.checkType(type, Basedron.CAULDRON, CauldronBlockEntity::tick);
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof CauldronBlockEntity cauldron) {
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
									cauldron.fluidStorage,
									fluidVariant -> view.getResource().equals(cauldron.fluidStorage.variant) ||
											cauldron.fluidStorage.isResourceBlank(),
									FluidConstants.BUCKET,
									transaction
							);
							if (actualExtract > 0) {
								world.playSound(
										null,
										pos,
										SoundEvents.ITEM_BUCKET_EMPTY,
										SoundCategory.BLOCKS
								);
								transaction.commit();
							}
						}
					} else {
						if (!cauldron.fluidStorage.variant.isOf(Basedron.POWDER_SNOW)) {
							try (Transaction transaction = Transaction.openOuter()) {
								long actualExtract = StorageUtil.move(
										cauldron.fluidStorage,
										another,
										fluidVariant -> !cauldron.fluidStorage.isResourceBlank(),
										FluidConstants.BUCKET,
										transaction
								);
								if (actualExtract > 0) {
									world.playSound(
											null,
											pos,
											SoundEvents.ITEM_BUCKET_EMPTY,
											SoundCategory.BLOCKS
									);
									transaction.commit();
								}
							}
						} else {
							if (cauldron.fluidStorage.getAmount() >= FluidConstants.BUCKET) {
								cauldron.fluidStorage.amount = 0;
								cauldron.fluidStorage.variant = FluidVariant.blank();
								ItemStack newStack = new ItemStack(Items.POWDER_SNOW_BUCKET);
								if (stack.getNbt() != null) {
									newStack.writeNbt(stack.getNbt());
								}
								ItemUsage.exchangeStack(stack, player, newStack);
							}
						}
					}
				});
				world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
				return ActionResult.CONSUME;
			}
		}
		return ActionResult.PASS;
	}

	@Inject(
			method = "fillFromDripstone",
			at = @At("HEAD"),
			cancellable = true
	)
	public void basedron$onFillFromDripstone(BlockState state, World world, BlockPos pos, Fluid fluid, CallbackInfo ci) {
		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof CauldronBlockEntity cauldron) {
			try (Transaction transaction = Transaction.openOuter()) {
				long actualInsert = cauldron.fluidStorage.insert(FluidVariant.of(fluid), FluidConstants.DROPLET, transaction);
				if (actualInsert > 0) transaction.commit();
			}
			world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.create(state));
			world.syncWorldEvent(1047, pos, 0);
			world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
		}
		ci.cancel();
	}

	@Inject(
			method = "precipitationTick",
			at = @At("HEAD"),
			cancellable = true
	)
	public void basedron$onPrecipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation, CallbackInfo ci) {
		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof CauldronBlockEntity cauldron) {
			if (canFillWithPrecipitation(world, precipitation)) {
				if (precipitation == Biome.Precipitation.RAIN) {
					try (Transaction transaction = Transaction.openOuter()) {
						long actualInsert = cauldron.fluidStorage.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BOTTLE, transaction);
						if (actualInsert > 0) transaction.commit();
					}
					world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
					world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
				} else if (precipitation == Biome.Precipitation.SNOW) {
					try (Transaction transaction = Transaction.openOuter()) {
						long actualInsert = cauldron.fluidStorage.insert(FluidVariant.of(Basedron.POWDER_SNOW), FluidConstants.BOTTLE, transaction);
						if (actualInsert > 0) transaction.commit();
					}
					world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
					world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
				}
			}
		}
		ci.cancel();
	}

	@Override
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (!world.isClient()) {
			if (this.isEntityTouchingFluid(state, pos, entity)){
				BlockEntity be = world.getBlockEntity(pos);
				if (be instanceof CauldronBlockEntity cauldron) {
					if (cauldron.fluidStorage.variant.getFluid().isIn(FluidTags.WATER) && entity.isOnFire()) {
						basedron$extinguishEntity(world, pos, entity, cauldron);
					} else if (cauldron.fluidStorage.variant.getFluid().isIn(FluidTags.LAVA)) {
						entity.setOnFireFromLava();
					} else if (cauldron.fluidStorage.variant.isOf(Basedron.POWDER_SNOW)) {
						if (entity.isOnFire()) {
							basedron$extinguishEntity(world, pos, entity, cauldron);
						}

					}
					world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
				}
			}
		}
	}

	private void basedron$extinguishEntity(World world, BlockPos pos, Entity entity, CauldronBlockEntity cauldron) {
		try (Transaction transaction = Transaction.openOuter()) {
			long consumed = cauldron.fluidStorage.extract(
					cauldron.fluidStorage.getResource(),
					FluidConstants.BOTTLE,
					transaction
			);
			if (consumed == FluidConstants.BOTTLE) {
				entity.extinguish();
				if (entity.canModifyAt(world, pos)) {
					transaction.commit();
				}
			}
		}
	}
}
