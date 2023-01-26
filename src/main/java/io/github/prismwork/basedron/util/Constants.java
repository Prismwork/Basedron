package io.github.prismwork.basedron.util;

import io.github.prismwork.basedron.Basedron;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;

import java.util.List;

public final class Constants {
	public static final String MODID = "basedron";
	public static final List<Fluid> DYEABLE_FLUIDS = List.of(
			Fluids.WATER, Basedron.COLORED_WATER
	);
}
