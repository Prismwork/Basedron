package io.github.prismwork.basedron;

import io.github.prismwork.basedron.block.CauldronBlockEntity;
import io.github.prismwork.basedron.block.fluid.PowderSnowFluid;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.block.entity.api.QuiltBlockEntityTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Basedron implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Basedron");
	public static final Fluid POWDER_SNOW = new PowderSnowFluid();
	public static final BlockEntityType<CauldronBlockEntity> CAULDRON = QuiltBlockEntityTypeBuilder.create(CauldronBlockEntity::new, Blocks.CAULDRON).build();

	@Override
	public void onInitialize(ModContainer mod) {
		Registry.register(
				Registries.FLUID,
				new Identifier("minecraft", "powder_snow"),
				POWDER_SNOW
		);
		Registry.register(
				Registries.BLOCK_ENTITY_TYPE,
				new Identifier("basedron", "cauldron"),
				CAULDRON
		);
		FluidStorage.SIDED.registerForBlockEntity((cauldron, direction) -> {
			if (!direction.equals(Direction.UP) && !direction.equals(Direction.DOWN)) {
				return cauldron.fluidStorage;
			} else return null;
		}, CAULDRON);
		FluidStorage.combinedItemApiProvider(Items.POWDER_SNOW_BUCKET).register(ctx ->
				new FullItemFluidStorage(
					ctx,
					Items.BUCKET,
					FluidVariant.of(POWDER_SNOW),
					FluidConstants.BUCKET
				)
		);
		LOGGER.info("Cauldron but *based* from {}\u2122", mod.metadata().name());
	}
}
