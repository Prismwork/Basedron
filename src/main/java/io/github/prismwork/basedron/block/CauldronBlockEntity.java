package io.github.prismwork.basedron.block;

import io.github.prismwork.basedron.Basedron;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ColorUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CauldronBlockEntity extends BlockEntity
		implements ExtendedScreenHandlerFactory {
	private final SingleVariantStorage<FluidVariant> fluidStorage = new SingleVariantStorage<>() {
		@Override
		protected FluidVariant getBlankVariant() {
			return FluidVariant.blank();
		}

		@Override
		protected long getCapacity(FluidVariant variant) {
			return FluidConstants.BUCKET;
		}

		@Override
		protected void onFinalCommit() {
			markDirty();
		}
	};
	private int fluidTint;

	public CauldronBlockEntity(BlockPos pos, BlockState state) {
		super(Basedron.CAULDRON, pos, state);
		this.fluidTint = FluidVariantRendering.getColor(getFluidStorage().variant, world, pos);
	}

	@Override
	public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
	}

	@Override
	public Text getDisplayName() {
		return null;
	}

	@Nullable
	@Override
	public ScreenHandler createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
		return null;
	}

	@Override
	public void writeNbt(NbtCompound tag) {
		tag.put("fluidVariant", getFluidStorage().variant.toNbt());
		tag.putLong("amount", getFluidStorage().amount);
		tag.putInt("fluidTint", fluidTint);
		super.writeNbt(tag);
	}

	@Override
	public void readNbt(NbtCompound tag) {
		super.readNbt(tag);
		getFluidStorage().variant = FluidVariant.fromNbt(tag.getCompound("fluidVariant"));
		getFluidStorage().amount = tag.getLong("amount");
		fluidTint = tag.getInt("fluidTint");
	}

	@Nullable
	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.of(this);
	}

	@Override
	public NbtCompound toSyncedNbt() {
		return toNbt();
	}

	@Override
	public void markDirty() {
		if (world != null) {
			if (world.isClient()) {
				MinecraftClient.getInstance().worldRenderer.scheduleBlockRenders(
						pos.getX(), pos.getY(), pos.getZ(),
						pos.getX(), pos.getY(), pos.getZ()
				);
			} else if (world instanceof ServerWorld serverWorld) {
				serverWorld.getChunkManager().markForUpdate(pos);
			}
			super.markDirty();
		}
	}

	public int getFluidTint() {
		return fluidTint;
	}

	public void setFluidTint(int fluidTint) {
		this.fluidTint = fluidTint;
	}

	public int mixColor(int inputColor) {
		if (fluidTint == -1) return inputColor;
		return ColorUtil.ARGB32.mixColor(fluidTint, inputColor);
	}

	@SuppressWarnings("unused")
	public static void tick(World world, BlockPos pos, BlockState state, CauldronBlockEntity be) {
		be.markDirty();
	}

	/* The Public API */

	/**
	 * Gets the fluid storage of this cauldron.
	 *
	 * @return the fluid storage
	 */
	public SingleVariantStorage<FluidVariant> getFluidStorage() {
		return fluidStorage;
	}

	/**
	 * Get the height of the contained fluid.<p>
	 * The fluid height is always within a specific range ([0,1]).
	 *
	 * @return
	 */
	public double getFluidHeight() {
		return ((double) getFluidStorage().getAmount()) / ((double) getFluidStorage().getCapacity());
	}
}
