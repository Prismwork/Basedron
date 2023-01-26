package io.github.prismwork.basedron;

import io.github.prismwork.basedron.client.render.block.CauldronBlockEntityRenderer;
import io.github.prismwork.basedron.client.util.ColoredWaterFluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

public class BasedronClient implements ClientModInitializer {
	@Override
	public void onInitializeClient(ModContainer mod) {
		FluidRenderHandlerRegistry.INSTANCE.register(Basedron.POWDER_SNOW, new SimpleFluidRenderHandler(
				new Identifier("minecraft:block/powder_snow"),
				new Identifier("minecraft:block/powder_snow"),
				0xFFFFFF
		));
		FluidRenderHandlerRegistry.INSTANCE.register(Basedron.COLORED_WATER, new ColoredWaterFluidRenderHandler());
		BlockEntityRendererFactories.register(Basedron.CAULDRON, CauldronBlockEntityRenderer::new);
	}
}
