package cassiokf.industrialrenewal.tileentity;

import cassiokf.industrialrenewal.IRSoundHandler;
import cassiokf.industrialrenewal.util.enumproperty.EnumFaceRotation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import static cassiokf.industrialrenewal.blocks.BlockValvePipeLarge.ACTIVE;

public class TileEntityValvePipeLarge extends TileFluidHandlerBase implements ITickable
{

    private final Set<EnumFacing> enabledFacings = EnumSet.allOf(EnumFacing.class);
    private EnumFacing facing = EnumFacing.SOUTH;
    private EnumFaceRotation faceRotation = EnumFaceRotation.UP;
    private Boolean active = false;

    public FluidTank tank = new FluidTank(2000);

    public TileEntityValvePipeLarge()
    {
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return (oldState.getBlock() != newState.getBlock());
    }

    @Override
    public void update() {

        if (this.hasWorld() && !world.isRemote)
        {
            boolean vActive = this.getWorld().getBlockState(pos).getValue(ACTIVE);
            if (vActive)
            {
                EnumFacing faceToFill = getOutPutFace();
                TileEntity teOut = this.getWorld().getTileEntity(this.getPos().offset(faceToFill));
                TileEntity teIn = this.getWorld().getTileEntity(this.getPos().offset(faceToFill.getOpposite()));

                if (teOut != null && (tank.getFluidAmount() > 0 || (teIn != null
                        && teIn.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, faceToFill)))
                        && teOut.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, faceToFill.getOpposite()))
                {
                    IFluidHandler inTank = tank.getFluidAmount() > 0 ? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(tank) : teIn.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, faceToFill);
                    IFluidHandler outTank = teOut.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, faceToFill.getOpposite());
                    if (inTank != null && outTank != null)
                    {
                        int amount = 1000;
                        inTank.drain(outTank.fill(inTank.drain(amount, false), true), true);
                    }
                }
            }
        }
    }

    public void setActive(boolean value)
    {
        active = value;
    }

    public void playSwitchSound() {
        Random r = new Random();
        float pitch = r.nextFloat() * (1.2f - 0.8f) + 0.8f;
        this.getWorld().playSound(null, this.getPos(), IRSoundHandler.TILEENTITY_VALVE_CHANGE, SoundCategory.BLOCKS, 1F, pitch);
    }

    public EnumFacing getOutPutFace()
    {
        EnumFacing vFace = this.getFacing();
        EnumFaceRotation rFace = getFaceRotation();

        if ((vFace == EnumFacing.NORTH && rFace == EnumFaceRotation.UP) || (vFace == EnumFacing.SOUTH && rFace == EnumFaceRotation.DOWN) || (vFace == EnumFacing.UP && rFace == EnumFaceRotation.UP) || (vFace == EnumFacing.DOWN && rFace == EnumFaceRotation.UP)) {
            return EnumFacing.EAST;
        }
        if ((vFace == EnumFacing.NORTH && rFace == EnumFaceRotation.DOWN) || (vFace == EnumFacing.SOUTH && rFace == EnumFaceRotation.UP) || (vFace == EnumFacing.UP && rFace == EnumFaceRotation.DOWN) || (vFace == EnumFacing.DOWN && rFace == EnumFaceRotation.DOWN)) {
            return EnumFacing.WEST;
        }
        if ((vFace == EnumFacing.NORTH && rFace == EnumFaceRotation.LEFT) || (vFace == EnumFacing.SOUTH && rFace == EnumFaceRotation.LEFT) || (vFace == EnumFacing.WEST && rFace == EnumFaceRotation.LEFT) || (vFace == EnumFacing.EAST && rFace == EnumFaceRotation.LEFT)) {
            return EnumFacing.DOWN;
        }
        if ((vFace == EnumFacing.NORTH && rFace == EnumFaceRotation.RIGHT) || (vFace == EnumFacing.SOUTH && rFace == EnumFaceRotation.RIGHT) || (vFace == EnumFacing.WEST && rFace == EnumFaceRotation.RIGHT) || (vFace == EnumFacing.EAST && rFace == EnumFaceRotation.RIGHT)) {
            return EnumFacing.UP;
        }
        if ((vFace == EnumFacing.EAST && rFace == EnumFaceRotation.UP) || (vFace == EnumFacing.WEST && rFace == EnumFaceRotation.DOWN) || (vFace == EnumFacing.UP && rFace == EnumFaceRotation.RIGHT) || (vFace == EnumFacing.DOWN && rFace == EnumFaceRotation.LEFT)) {
            return EnumFacing.SOUTH;
        }
        if ((vFace == EnumFacing.WEST && rFace == EnumFaceRotation.UP) || (vFace == EnumFacing.EAST && rFace == EnumFaceRotation.DOWN) || (vFace == EnumFacing.UP && rFace == EnumFaceRotation.LEFT) || (vFace == EnumFacing.DOWN && rFace == EnumFaceRotation.RIGHT)) {
            return EnumFacing.NORTH;
        }
        return EnumFacing.NORTH;
    }

    public EnumFacing getFacing() {
        return facing;
    }

    public void setFacing(final EnumFacing facing) {
        this.facing = facing;
        markDirty();
    }

    public boolean toggleFacing(final EnumFacing facing) {
        if (enabledFacings.contains(facing)) {
            enabledFacings.remove(facing);
            return false;
        } else {
            enabledFacings.add(facing);
            return true;
        }
    }

    public boolean disableFacing(final EnumFacing facing) {
        if (enabledFacings.contains(facing)) {
            enabledFacings.remove(facing);
            return false;
        } else {

            return true;
        }
    }

    public boolean activeFacing(final EnumFacing facing) {
        if (enabledFacings.contains(facing)) {
            return false;
        } else {
            enabledFacings.add(facing);
            return true;
        }
    }

    public boolean isFacingEnabled(final @Nullable EnumFacing facing) {
        return enabledFacings.contains(facing) || facing == null;
    }

    public Set<EnumFacing> getEnabledFacings() {
        return enabledFacings;
    }

    private void notifyBlockUpdate() {
        final IBlockState state = getWorld().getBlockState(getPos());
        getWorld().notifyBlockUpdate(getPos(), state, state, 3);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        notifyBlockUpdate();
    }

    @Override
    public void readFromNBT(final NBTTagCompound tag) {
        facing = EnumFacing.getFront(tag.getInteger("facing"));
        faceRotation = EnumFaceRotation.values()[tag.getInteger("faceRotation")];
        active = tag.getBoolean("active");

        enabledFacings.clear();

        final int[] enabledFacingIndices = tag.getIntArray("EnabledFacings");
        for (final int index : enabledFacingIndices) {
            enabledFacings.add(EnumFacing.getFront(index));
        }
        super.readFromNBT(tag);
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound tag) {
        final int[] enabledFacingIndices = enabledFacings.stream()
                .mapToInt(EnumFacing::getIndex)
                .toArray();
        tag.setInteger("facing", facing.getIndex());
        tag.setInteger("faceRotation", faceRotation.ordinal());
        tag.setIntArray("EnabledFacings", enabledFacingIndices);
        tag.setBoolean("active", this.active);

        return super.writeToNBT(tag);
    }

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return isFacingEnabled(facing);
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && isFacingEnabled(facing))
        {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(tank);
        }
        return super.getCapability(capability, facing);
    }

    public EnumFaceRotation getFaceRotation() {
        return faceRotation;
    }

    public void setFaceRotation(final EnumFaceRotation faceRotation) {
        this.faceRotation = faceRotation;
        markDirty();
    }
}