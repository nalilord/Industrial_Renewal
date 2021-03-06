package cassiokf.industrialrenewal.tileentity;

import cassiokf.industrialrenewal.blocks.BlockBatteryBank;
import cassiokf.industrialrenewal.config.IRConfig;
import cassiokf.industrialrenewal.item.ItemWindBlade;
import cassiokf.industrialrenewal.util.Utils;
import cassiokf.industrialrenewal.util.VoltsEnergyContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public class TileEntitySmallWindTurbine extends TileEntitySyncable implements ICapabilityProvider, ITickable
{
    private final VoltsEnergyContainer energyContainer;
    public ItemStackHandler bladeInv = new ItemStackHandler(1)
    {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack)
        {
            if (stack.isEmpty()) return false;
            return stack.getItem() instanceof ItemWindBlade;
        }

        @Override
        protected void onContentsChanged(int slot)
        {
            TileEntitySmallWindTurbine.this.Sync();
        }
    };
    private float rotation;
    private int energyGenerated;
    private int tickToDamage;

    public TileEntitySmallWindTurbine()
    {
        this.energyContainer = new VoltsEnergyContainer(32000, 1024, 1024)
        {
            @Override
            public void onEnergyChange()
            {
                TileEntitySmallWindTurbine.this.Sync();
            }
        };
    }

    public void dropAllItems()
    {
        Utils.dropInventoryItems(world, pos, bladeInv);
    }

    public static int getMaxGeneration()
    {
        return IRConfig.MainConfig.Main.maxEnergySWindTurbine;
    }

    @Override
    public void update()
    {
        if (!world.isRemote)
        {
            //Generate Energy
            if (hasBlade())
            {
                int energyGen = Math.round(getMaxGeneration() * getEfficiency());
                energyGenerated = this.energyContainer.receiveEnergy(energyGen, false);
                if (tickToDamage >= 1200 && energyGen > 0)
                {
                    tickToDamage = 0;
                    bladeInv.getStackInSlot(0).attemptDamageItem(1, new Random(), null);
                    if (bladeInv.getStackInSlot(0).getItemDamage() < 0) bladeInv.setStackInSlot(0, ItemStack.EMPTY);
                }
                if (tickToDamage < 1201) tickToDamage++;
            } else
            {
                energyGenerated = 0;
            }
            //OutPut Energy
            if (this.energyContainer.getEnergyStored() > 0)
            {
                TileEntity te = world.getTileEntity(pos.down());
                if (te != null && te.hasCapability(CapabilityEnergy.ENERGY, EnumFacing.UP))
                {
                    IEnergyStorage downE = te.getCapability(CapabilityEnergy.ENERGY, EnumFacing.UP);
                    if (downE != null && downE.canReceive())
                    {
                        this.energyContainer.extractEnergy(downE.receiveEnergy(this.energyContainer.extractEnergy(this.energyContainer.getMaxOutput(), true), false), false);
                        this.markDirty();
                    }
                }
            }
        } else
        {
            rotation += 4.5f * getEfficiency();
            if (rotation > 360) rotation = 0;
        }
    }

    public int getEnergyGenerated()
    {
        return energyGenerated;
    }

    public IItemHandler getBladeHandler()
    {
        return this.bladeInv;
    }

    public float getRotation()
    {
        return -rotation;
    }

    public boolean hasBlade()
    {
        return !this.bladeInv.getStackInSlot(0).isEmpty();
    }

    private float getEfficiency()
    {
        float weatherModifier;
        if (world.isThundering())
        {
            weatherModifier = 1f;
        } else if (world.isRaining())
        {
            weatherModifier = 0.9f;
        } else
        {
            weatherModifier = 0.8f;
        }

        float heightModifier;
        float posMin = -2040f;
        if (pos.getY() - 62 <= 0) heightModifier = 0;
        else heightModifier = (pos.getY() - posMin) / (255 - posMin);
        heightModifier = MathHelper.clamp(heightModifier, 0, 1);
        //System.out.println(weatherModifier + " H " + heightModifier + " " + (pos.getY()- posMin)/(255-posMin));
        return weatherModifier * heightModifier;
    }

    public EnumFacing getBlockFacing()
    {
        return this.world.getBlockState(this.pos).getValue(BlockBatteryBank.FACING);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        return (capability == CapabilityEnergy.ENERGY) || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
        if (capability == CapabilityEnergy.ENERGY)
            return CapabilityEnergy.ENERGY.cast(this.energyContainer);
        return super.getCapability(capability, facing);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        this.energyContainer.deserializeNBT(compound.getCompoundTag("StoredIR"));
        this.bladeInv.deserializeNBT(compound.getCompoundTag("bladeInv"));
        this.energyGenerated = compound.getInteger("generation");
        this.tickToDamage = compound.getInteger("damageTick");
        //this.rotation = compound.getFloat("rotation");
        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        compound.setTag("StoredIR", this.energyContainer.serializeNBT());
        compound.setTag("bladeInv", this.bladeInv.serializeNBT());
        compound.setInteger("generation", this.energyGenerated);
        compound.setInteger("damageTick", tickToDamage);
        //compound.setFloat("rotation", this.rotation);
        return super.writeToNBT(compound);
    }
}
