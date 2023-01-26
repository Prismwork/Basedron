package io.github.prismwork.basedron.client.render.block;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.prismwork.basedron.block.CauldronBlockEntity;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.quiltmc.loader.api.minecraft.ClientOnly;

@ClientOnly
public class CauldronBlockEntityRenderer implements BlockEntityRenderer<CauldronBlockEntity> {
	@SuppressWarnings("unused")
	public CauldronBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

	/**
	 * Modified from Modern Industrialization
	 */
	@Override
	public void render(CauldronBlockEntity entity,
					   float tickDelta,
					   MatrixStack matrices,
					   VertexConsumerProvider vertexConsumers,
					   int light,
					   int overlay) {
		Sprite sprite = FluidVariantRendering.getSprite(entity.getFluidStorage().variant);
		int color = FluidVariantRendering.getColor(entity.getFluidStorage().variant, entity.getWorld(), entity.getPos());
		float r = ((color >> 16) & 255) / 256f;
		float g = ((color >> 8) & 255) / 256f;
		float b = (color & 255) / 256f;
		if (sprite == null) return;
		RenderSystem.enableDepthTest();
		QuadEmitter emitter = RendererAccess.INSTANCE.getRenderer().meshBuilder().getEmitter();
		emitter.square(
				Direction.UP,
				normalize(2f),
				normalize(2f),
				normalize(2f, true),
				normalize(2f, true),
				(float) (1 - MathHelper.lerp(
						entity.getFluidHeight(),
						normalize(3f),
						normalize(1f, true)
				))
		);
		emitter.spriteBake(0, sprite, MutableQuadView.BAKE_LOCK_UV);
		vertexConsumers.getBuffer(
				RenderLayer.getTranslucent()
		).bakedQuad(
				matrices.peek(),
				emitter.toBakedQuad(0, sprite, false),
				r, g, b,
				light, overlay
		);
	}

	private static float normalize(float input) {
		return normalize(input, false);
	}

	private static float normalize(float input, boolean reversed) {
		if (reversed) return 1 - (input / 16f);
		else return input / 16f;
	}
}
