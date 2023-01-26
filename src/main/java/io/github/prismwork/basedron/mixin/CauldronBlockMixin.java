package io.github.prismwork.basedron.mixin;

import io.github.prismwork.basedron.Basedron;
import io.github.prismwork.basedron.api.CauldronInteractionEvent;
import io.github.prismwork.basedron.block.CauldronBlockEntity;
import io.github.prismwork.basedron.util.BlockEntityHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
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
import net.minecraft.registry.tag.FluidTags;
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
			return CauldronInteractionEvent.INSTANCE.invoker().interact(cauldron, player, hand, hit);
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
				long actualInsert = cauldron.getFluidStorage().insert(FluidVariant.of(fluid), FluidConstants.DROPLET, transaction);
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
						long actualInsert = cauldron.getFluidStorage().insert(
								FluidVariant.of(Fluids.WATER),
								FluidConstants.BOTTLE,
								transaction
						);
						if (actualInsert > 0) transaction.commit();
					}
					world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
					world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
				} else if (precipitation == Biome.Precipitation.SNOW) {
					try (Transaction transaction = Transaction.openOuter()) {
						long actualInsert = cauldron.getFluidStorage().insert(
								FluidVariant.of(Basedron.POWDER_SNOW),
								FluidConstants.BOTTLE,
								transaction
						);
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
					if (cauldron.getFluidStorage().variant.getFluid().isIn(FluidTags.WATER) && entity.isOnFire()) {
						basedron$extinguishEntity(world, pos, entity, cauldron);
					} else if (cauldron.getFluidStorage().variant.getFluid().isIn(FluidTags.LAVA)) {
						entity.setOnFireFromLava();
					} else if (cauldron.getFluidStorage().variant.isOf(Basedron.POWDER_SNOW)) {
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
			long consumed = cauldron.getFluidStorage().extract(
					cauldron.getFluidStorage().getResource(),
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
