package io.github.prismwork.basedron.block.fluid;

import io.github.prismwork.basedron.Basedron;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class PowderSnowFluid extends FlowableFluid {
	@Override
	public Item getBucketItem() {
		return Items.POWDER_SNOW_BUCKET;
	}

	@Override
	protected boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluid, Direction direction) {
		return false;
	}

	@Override
	public Vec3d getVelocity(BlockView world, BlockPos pos, FluidState state) {
		return Vec3d.ZERO;
	}

	@Override
	public Fluid getFlowing() {
		return Basedron.POWDER_SNOW;
	}

	@Override
	public Fluid getStill() {
		return Basedron.POWDER_SNOW;
	}

	@Override
	protected boolean isInfinite(World world) {
		return false;
	}

	@Override
	protected void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state) {
	}

	@Override
	protected int getFlowSpeed(WorldView world) {
		return 0;
	}

	@Override
	protected int getLevelDecreasePerBlock(WorldView world) {
		return 0;
	}

	@Override
	public int getTickRate(WorldView world) {
		return 0;
	}

	@Override
	protected float getBlastResistance() {
		return 100.0F;
	}

	@Override
	public float getHeight(FluidState state, BlockView world, BlockPos pos) {
		return 1;
	}

	@Override
	public float getHeight(FluidState state) {
		return 1;
	}

	@Override
	protected BlockState toBlockState(FluidState state) {
		return Blocks.POWDER_SNOW.getDefaultState();
	}

	@Override
	public boolean isSource(FluidState state) {
		return false;
	}

	@Override
	public int getLevel(FluidState state) {
		return 0;
	}

	@Override
	public VoxelShape getShape(FluidState state, BlockView world, BlockPos pos) {
		return VoxelShapes.fullCube();
	}
}
