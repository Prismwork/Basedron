package io.github.prismwork.basedron.block;

import io.github.prismwork.basedron.Basedron;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CauldronBlockEntity extends BlockEntity
		implements ExtendedScreenHandlerFactory {
	public final SingleVariantStorage<FluidVariant> fluidStorage = new SingleVariantStorage<>() {
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

	public CauldronBlockEntity(BlockPos pos, BlockState state) {
		super(Basedron.CAULDRON, pos, state);
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
		tag.put("fluidVariant", fluidStorage.variant.toNbt());
		tag.putLong("amount", fluidStorage.amount);
		super.writeNbt(tag);
	}

	@Override
	public void readNbt(NbtCompound tag) {
		super.readNbt(tag);
		fluidStorage.variant = FluidVariant.fromNbt(tag.getCompound("fluidVariant"));
		fluidStorage.amount = tag.getLong("amount");
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

	public double getFluidHeight() {
		return ((double) fluidStorage.getAmount()) / ((double) fluidStorage.getCapacity());
	}

	@SuppressWarnings("unused")
	public static void tick(World world, BlockPos pos, BlockState state, CauldronBlockEntity be) {
		be.markDirty();
	}
}
