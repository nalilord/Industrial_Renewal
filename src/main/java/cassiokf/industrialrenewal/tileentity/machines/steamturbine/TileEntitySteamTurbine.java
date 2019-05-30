package cassiokf.industrialrenewal.tileentity.machines.steamturbine;

import cassiokf.industrialrenewal.IRSoundHandler;
import cassiokf.industrialrenewal.config.IRConfig;
import cassiokf.industrialrenewal.init.FluidInit;
import cassiokf.industrialrenewal.tileentity.Fluid.TileFluidHandlerBase;
import cassiokf.industrialrenewal.util.Utils;
import cassiokf.industrialrenewal.util.VoltsEnergyContainer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.List;

public class TileEntitySteamTurbine extends TileFluidHandlerBase implements ICapabilityProvider, ITickable
{
    private final VoltsEnergyContainer energyContainer;
    public FluidTank waterTank = new FluidTank(32000)
    {
        @Override
        public boolean canFill()
        {
            return false;
        }

        @Override
        public void onContentsChanged()
        {
            TileEntitySteamTurbine.this.Sync();
        }
    };
    public FluidTank steamTank = new FluidTank(320000)
    {
        @Override
        public boolean canFillFluidType(FluidStack fluid)
        {
            return fluid.getFluid().equals(FluidInit.STEAM);
        }

        @Override
        public void onContentsChanged()
        {
            TileEntitySteamTurbine.this.Sync();
        }
    };

    private boolean master;
    private int maxRotation = 16000;
    private int rotation;
    private int energyPerTick = 240;
    private int oldRotation;
    private int steamPtick = 100;

    private int timeSincePlayed;

    public TileEntitySteamTurbine()
    {
        this.energyContainer = new VoltsEnergyContainer(100000, 0, 10240)
        {
            @Override
            public void onEnergyChange()
            {
                if (!world.isRemote) TileEntitySteamTurbine.this.Sync();
            }
        };
    }

    @Override
    public void update()
    {
        if (this.getIsMaster())
        {
            if (!this.world.isRemote)
            {
                if (this.steamTank.getFluidAmount() > 0)
                {
                    FluidStack stack = this.steamTank.drain(steamPtick, true);
                    int amount = stack != null ? stack.amount : 0;
                    FluidStack waterStack = new FluidStack(FluidRegistry.WATER, amount / IRConfig.steamBoilerConvertionFactor);
                    this.waterTank.fillInternal(waterStack, true);
                    rotation += 10;
                } else rotation -= 5;

                if (rotation >= 5000 && this.energyContainer.getEnergyStored() < this.energyContainer.getMaxEnergyStored())
                {
                    this.energyContainer.setEnergyStored((int) Math.min(this.energyContainer.getMaxEnergyStored(), this.energyContainer.getEnergyStored() + energyPerTick * getRotation()));
                    rotation -= 4;
                }

                rotation -= 2;
                rotation = MathHelper.clamp(rotation, 0, maxRotation);


                EnumFacing facing = getBlockFacing();
                TileEntity eTE = this.world.getTileEntity(pos.offset(facing.getOpposite(), 2).down());
                if (eTE != null && this.steamTank.getFluidAmount() > 0 && eTE != null && eTE.hasCapability(CapabilityEnergy.ENERGY, facing))
                {
                    IEnergyStorage upTank = eTE.getCapability(CapabilityEnergy.ENERGY, facing);
                    this.energyContainer.extractEnergy(upTank.receiveEnergy(this.energyContainer.extractEnergy(10240, true), false), false);
                }
                TileEntity wTE = this.world.getTileEntity(pos.offset(facing, 2).down());
                if (wTE != null && this.waterTank.getFluidAmount() > 0 && wTE.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite()))
                {
                    IFluidHandler wTank = wTE.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite());
                    this.waterTank.drain(wTank.fill(this.waterTank.drain(2000, false), true), true);
                }

                if (oldRotation != rotation)
                {
                    this.Sync();
                    oldRotation = rotation;
                }
            }
            if (this.rotation > 0)
            {
                if (timeSincePlayed == 9)
                {
                    timeSincePlayed = 0;
                    this.world.playSound(null, this.pos, IRSoundHandler.MOTOR_ROTAION, SoundCategory.BLOCKS, 0.8F, Math.min(getRotation(), 0.9F));
                }
                timeSincePlayed++;
            }
        }
    }

    @Override
    public void onLoad()
    {
        this.getIsMaster();
    }

    public TileEntitySteamTurbine getMaster()
    {
        List<BlockPos> list = Utils.getBlocksIn3x3x3Centered(this.pos);
        for (BlockPos currentPos : list)
        {
            Block block = world.getBlockState(currentPos).getBlock();
            if (block instanceof BlockSteamTurbine && ((TileEntitySteamTurbine) world.getTileEntity(currentPos)).getIsMaster())
            {
                return ((TileEntitySteamTurbine) world.getTileEntity(currentPos));
            }
        }
        return null;
    }

    public void breakMultiBlocks()
    {
        if (!this.getIsMaster())
        {
            if (getMaster() != null)
            {
                getMaster().breakMultiBlocks();
            }
            return;
        }
        List<BlockPos> list = Utils.getBlocksIn3x3x3Centered(this.pos);
        for (BlockPos currentPos : list)
        {
            Block block = world.getBlockState(currentPos).getBlock();
            if (block instanceof BlockSteamTurbine) world.setBlockToAir(currentPos);
        }
    }

    public boolean isMaster()
    {
        return this.master;
    }

    private boolean getIsMaster()
    {
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockSteamTurbine)) return false;
        return state.getValue(BlockSteamTurbine.MASTER);
    }

    public EnumFacing getBlockFacing()
    {
        IBlockState state = this.world.getBlockState(this.pos);
        return state.getValue(BlockSteamTurbine.FACING);
    }

    public String getWaterText()
    {
        return FluidRegistry.WATER.getName();
    }

    public String getSteamText()
    {
        return FluidInit.STEAM.getName();
    }

    public String getGenerationText()
    {
        int energy = (rotation >= 5000 && this.energyContainer.getEnergyStored() < this.energyContainer.getMaxEnergyStored()) ? (int) (energyPerTick * getRotation()) : 0;
        String text = energy + " FE/t";
        if (energy >= 1000 && energy < 1000000)
            text = energy / 1000 + "K FE/t";
        if (energy >= 1000000)
            text = energy / 1000000 + "M FE/t";
        return text;
    }

    public String getEnergyText()
    {
        int energy = this.energyContainer.getEnergyStored();
        String text = energy + " FE";
        if (energy >= 1000 && energy < 1000000)
            text = energy / 1000 + "K FE";
        if (energy >= 1000000)
            text = energy / 1000000 + "M FE";
        return text;
    }

    public String getRotationText()
    {
        return rotation + " rpm";
    }

    public float GetEnergyFill() //0 ~ 180
    {
        float currentAmount = this.energyContainer.getEnergyStored() / 1000;
        float totalCapacity = this.energyContainer.getMaxEnergyStored() / 1000;
        currentAmount = currentAmount / totalCapacity;
        return currentAmount * 180f;
    }

    private float getRotation()
    {
        float currentAmount = this.rotation / 100f;
        float totalCapacity = this.maxRotation / 100f;
        currentAmount = currentAmount / totalCapacity;
        return currentAmount;
    }

    public float GetGenerationFill() //0 ~ 180
    {
        float currentAmount = ((rotation >= 5000 && this.energyContainer.getEnergyStored() < this.energyContainer.getMaxEnergyStored()) ? energyPerTick * getRotation() : 0) / 100f;
        float totalCapacity = energyPerTick / 100f;
        currentAmount = currentAmount / totalCapacity;
        return currentAmount * 180f;
    }

    public float GetWaterFill() //0 ~ 180
    {
        float currentAmount = this.waterTank.getFluidAmount() / 1000f;
        float totalCapacity = this.waterTank.getCapacity() / 1000f;
        currentAmount = currentAmount / totalCapacity;
        return currentAmount * 180f;
    }

    public float GetSteamFill() //0 ~ 180
    {
        float currentAmount = this.steamTank.getFluidAmount() / 1000f;
        float totalCapacity = this.steamTank.getCapacity() / 1000f;
        currentAmount = currentAmount / totalCapacity;
        return currentAmount * 180f;
    }

    public float getRotationFill() //0 ~ 180
    {
        return getRotation() * 140f;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        NBTTagCompound waterTag = new NBTTagCompound();
        NBTTagCompound steamTag = new NBTTagCompound();
        this.waterTank.writeToNBT(waterTag);
        this.steamTank.writeToNBT(steamTag);
        compound.setTag("water", waterTag);
        compound.setTag("steam", steamTag);
        compound.setTag("StoredIR", this.energyContainer.serializeNBT());
        compound.setBoolean("master", this.getIsMaster());
        compound.setInteger("heat", rotation);
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        NBTTagCompound waterTag = compound.getCompoundTag("water");
        NBTTagCompound steamTag = compound.getCompoundTag("steam");
        this.waterTank.readFromNBT(waterTag);
        this.steamTank.readFromNBT(steamTag);
        this.energyContainer.deserializeNBT(compound.getCompoundTag("StoredIR"));
        this.master = compound.getBoolean("master");
        this.rotation = compound.getInteger("heat");
        super.readFromNBT(compound);
    }

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing)
    {
        EnumFacing face = this.getBlockFacing();
        TileEntitySteamTurbine masterTE = this.getMaster();
        if (masterTE == null) return false;

        return (facing == EnumFacing.UP && this.pos.equals(masterTE.getPos().up()) && capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
                || (facing == face && this.pos.equals(masterTE.getPos().down().offset(this.getBlockFacing())) && capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
                || (facing == face.getOpposite() && this.pos.equals(masterTE.getPos().down().offset(this.getBlockFacing().getOpposite())) && capability == CapabilityEnergy.ENERGY);
    }

    @Nullable
    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing)
    {
        EnumFacing face = this.getBlockFacing();
        TileEntitySteamTurbine masterTE = this.getMaster();
        if (masterTE == null) return super.getCapability(capability, facing);

        if (facing == EnumFacing.UP && this.pos.equals(masterTE.getPos().up()) && capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(masterTE.steamTank);
        if (facing == face && this.pos.equals(masterTE.getPos().down().offset(this.getBlockFacing())) && capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(masterTE.waterTank);
        if (facing == face.getOpposite() && this.pos.equals(masterTE.getPos().down().offset(this.getBlockFacing().getOpposite())) && capability == CapabilityEnergy.ENERGY)
            return (T) masterTE.energyContainer;

        return super.getCapability(capability, facing);
    }
}