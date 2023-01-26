package io.github.prismwork.basedron.client.util;

import io.github.prismwork.basedron.block.CauldronBlockEntity;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.minecraft.ClientOnly;

@ClientOnly
public class ColoredWaterFluidRenderHandler extends SimpleFluidRenderHandler {
	public ColoredWaterFluidRenderHandler() {
		super(SimpleFluidRenderHandler.WATER_STILL, SimpleFluidRenderHandler.WATER_STILL);
	}

	@Override
	public int getFluidColor(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state) {
		if (view != null && pos != null) {
			BlockEntity be = view.getBlockEntity(pos);
			if (be instanceof CauldronBlockEntity cauldron && cauldron.getFluidTint() != -1) {
				return cauldron.getFluidTint();
			}
		}
		return 0x3f76e4;
	}
}
