package mekanism.common.tile;

import java.util.ArrayList;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.MekanismConfig.general;
import mekanism.api.MekanismConfig.usage;
import mekanism.api.Range4D;
import mekanism.api.infuse.InfuseObject;
import mekanism.api.infuse.InfuseRegistry;
import mekanism.common.InfuseStorage;
import mekanism.common.Mekanism;
import mekanism.common.MekanismItems;
import mekanism.common.PacketHandler;
import mekanism.common.SideData;
import mekanism.common.Upgrade;
import mekanism.common.base.IEjector;
import mekanism.common.base.IInvConfiguration;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.inputs.InfusionInput;
import mekanism.common.recipe.machines.MetallurgicInfuserRecipe;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.util.ChargeUtils;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.Method;

import io.netty.buffer.ByteBuf;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

@Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft")
public class TileEntityMetallurgicInfuser extends TileEntityNoisyElectricBlock implements IPeripheral, IInvConfiguration, IUpgradeTile, IRedstoneControl
{
	/** This machine's side configuration. */
	public byte[] sideConfig = new byte[] {2, 1, 0, 5, 3, 4};

	/** An arraylist of SideData for this machine. */
	public ArrayList<SideData> sideOutputs = new ArrayList<SideData>();

	/** The maxiumum amount of infuse this machine can store. */
	public int MAX_INFUSE = 1000;

	/** How much energy this machine consumes per-tick. */
	public double BASE_ENERGY_PER_TICK = usage.metallurgicInfuserUsage;

	public double energyPerTick = BASE_ENERGY_PER_TICK;

	/** How many ticks it takes to run an operation. */
	public int BASE_TICKS_REQUIRED = 200;

	public int ticksRequired = BASE_TICKS_REQUIRED;

	/** The amount of infuse this machine has stored. */
	public InfuseStorage infuseStored = new InfuseStorage();

	/** How many ticks this machine has been operating for. */
	public int operatingTicks;

	/** Whether or not this machine is in it's active state. */
	public boolean isActive;

	/** The client's current active state. */
	public boolean clientActive;

	/** How many ticks must pass until this block's active state can sync with the client. */
	public int updateDelay;

	/** This machine's previous amount of energy. */
	public double prevEnergy;

	/** This machine's current RedstoneControl type. */
	public RedstoneControl controlType = RedstoneControl.DISABLED;

	public TileComponentUpgrade upgradeComponent = new TileComponentUpgrade(this, 4);
	public TileComponentEjector ejectorComponent;

	public TileEntityMetallurgicInfuser()
	{
		super("metalinfuser", "MetallurgicInfuser", MachineBlockType.METALLURGIC_INFUSER.baseEnergy);

		sideOutputs.add(new SideData(EnumColor.GREY, InventoryUtils.EMPTY));
		sideOutputs.add(new SideData(EnumColor.ORANGE, new int[] {0}));
		sideOutputs.add(new SideData(EnumColor.PURPLE, new int[] {1}));
		sideOutputs.add(new SideData(EnumColor.DARK_RED, new int[] {2}));
		sideOutputs.add(new SideData(EnumColor.DARK_BLUE, new int[] {3}));
		sideOutputs.add(new SideData(EnumColor.DARK_GREEN, new int[] {4}));

		inventory = new ItemStack[5];
		ejectorComponent = new TileComponentEjector(this, sideOutputs.get(4));
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();

		if(worldObj.isRemote && updateDelay > 0)
		{
			updateDelay--;

			if(updateDelay == 0 && clientActive != isActive)
			{
				isActive = clientActive;
				MekanismUtils.updateBlock(worldObj, xCoord, yCoord, zCoord);
			}
		}

		if(!worldObj.isRemote)
		{
			if(updateDelay > 0)
			{
				updateDelay--;

				if(updateDelay == 0 && clientActive != isActive)
				{
					Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList())), new Range4D(Coord4D.get(this)));
				}
			}

			ChargeUtils.discharge(0, this);

			if(inventory[1] != null)
			{
				if(InfuseRegistry.getObject(inventory[1]) != null)
				{
					InfuseObject infuse = InfuseRegistry.getObject(inventory[1]);

					if(infuseStored.type == null || infuseStored.type == infuse.type)
					{
						if(infuseStored.amount + infuse.stored <= MAX_INFUSE)
						{
							infuseStored.amount += infuse.stored;
							infuseStored.type = infuse.type;
							inventory[1].stackSize--;

							if(inventory[1].stackSize <= 0)
							{
								inventory[1] = null;
							}
						}
					}
				}
			}

			MetallurgicInfuserRecipe recipe = RecipeHandler.getMetallurgicInfuserRecipe(getInput());

			if(MekanismUtils.canFunction(this) && getEnergy() >= energyPerTick)
			{
				if(canOperate(recipe))
				{
					setActive(true);
					setEnergy(getEnergy() - energyPerTick);

					if((operatingTicks + 1) < ticksRequired)
					{
						operatingTicks++;
					} else
					{
						operate(recipe);
						operatingTicks = 0;
					}
				}
			}
			else {
				if(prevEnergy >= getEnergy())
				{
					setActive(false);
				}
			}

			if(!canOperate(recipe))
			{
				operatingTicks = 0;
			}

			if(infuseStored.amount <= 0)
			{
				infuseStored.amount = 0;
				infuseStored.type = null;
			}

			prevEnergy = getEnergy();
		}
	}

	@Override
	public boolean canExtractItem(int slotID, ItemStack itemstack, int side)
	{
		if(slotID == 4)
		{
			return ChargeUtils.canBeOutputted(itemstack, false);
		}
		else if(slotID == 3)
		{
			return true;
		}

		return false;
	}

	@Override
	public boolean isItemValidForSlot(int slotID, ItemStack itemstack)
	{
		if(slotID == 3)
		{
			return false;
		}
		else if(slotID == 1)
		{
			return InfuseRegistry.getObject(itemstack) != null && (infuseStored.type == null || infuseStored.type == InfuseRegistry.getObject(itemstack).type);
		}
		else if(slotID == 0)
		{
			return itemstack.getItem() == MekanismItems.SpeedUpgrade || itemstack.getItem() == MekanismItems.EnergyUpgrade;
		}
		else if(slotID == 2)
		{
			if(infuseStored.type != null)
			{
				if(RecipeHandler.getMetallurgicInfuserRecipe(new InfusionInput(infuseStored, itemstack)) != null)
				{
					return true;
				}
			}
			else {
				for(Object obj : Recipe.METALLURGIC_INFUSER.get().keySet())
				{
					InfusionInput input = (InfusionInput)obj;
					if(input.inputStack.isItemEqual(itemstack))
					{
						return true;
					}
				}
			}
		}
		else if(slotID == 4)
		{
			return ChargeUtils.canBeDischarged(itemstack);
		}

		return false;
	}

	public InfusionInput getInput()
	{
		return new InfusionInput(infuseStored, inventory[2]);
	}

	public void operate(MetallurgicInfuserRecipe recipe)
	{
		recipe.output(inventory, infuseStored);

		markDirty();
		ejectorComponent.onOutput();
	}

	public boolean canOperate(MetallurgicInfuserRecipe recipe)
	{
		return recipe != null && recipe.canOperate(inventory, infuseStored);
	}

	public int getScaledInfuseLevel(int i)
	{
		return infuseStored.amount * i / MAX_INFUSE;
	}

	public double getScaledProgress()
	{
		return ((double)operatingTicks) / ((double)ticksRequired);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);

		clientActive = isActive = nbtTags.getBoolean("isActive");
		operatingTicks = nbtTags.getInteger("operatingTicks");
		infuseStored.amount = nbtTags.getInteger("infuseStored");
		controlType = RedstoneControl.values()[nbtTags.getInteger("controlType")];
		infuseStored.type = InfuseRegistry.get(nbtTags.getString("type"));

		if(nbtTags.hasKey("sideDataStored"))
		{
			for(int i = 0; i < 6; i++)
			{
				sideConfig[i] = nbtTags.getByte("config"+i);
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);

		nbtTags.setBoolean("isActive", isActive);
		nbtTags.setInteger("operatingTicks", operatingTicks);
		nbtTags.setInteger("infuseStored", infuseStored.amount);
		nbtTags.setInteger("controlType", controlType.ordinal());

		if(infuseStored.type != null)
		{
			nbtTags.setString("type", infuseStored.type.name);
		}
		else {
			nbtTags.setString("type", "null");
		}

		nbtTags.setBoolean("sideDataStored", true);

		for(int i = 0; i < 6; i++)
		{
			nbtTags.setByte("config"+i, sideConfig[i]);
		}
	}

	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		if(!worldObj.isRemote)
		{
			infuseStored.amount = dataStream.readInt();
			return;
		}

		super.handlePacketData(dataStream);

		clientActive = dataStream.readBoolean();
		operatingTicks = dataStream.readInt();
		infuseStored.amount = dataStream.readInt();
		controlType = RedstoneControl.values()[dataStream.readInt()];
		infuseStored.type = InfuseRegistry.get(PacketHandler.readString(dataStream));

		for(int i = 0; i < 6; i++)
		{
			sideConfig[i] = dataStream.readByte();
		}

		if(updateDelay == 0 && clientActive != isActive)
		{
			updateDelay = general.UPDATE_DELAY;
			isActive = clientActive;
			MekanismUtils.updateBlock(worldObj, xCoord, yCoord, zCoord);
		}
	}

	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		super.getNetworkedData(data);

		data.add(isActive);
		data.add(operatingTicks);
		data.add(infuseStored);
		data.add(controlType.ordinal());

		if(infuseStored.type != null)
		{
			data.add(infuseStored.type.name);
		}
		else {
			data.add("null");
		}

		data.add(sideConfig);
		return data;
	}

	@Override
	@Method(modid = "ComputerCraft")
	public String getType()
	{
		return getInventoryName();
	}

	@Override
	@Method(modid = "ComputerCraft")
	public String[] getMethodNames()
	{
		return new String[] {"getStored", "getProgress", "facing", "canOperate", "getMaxEnergy", "getEnergyNeeded", "getInfuse", "getInfuseNeeded"};
	}

	@Override
	@Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws LuaException, InterruptedException
	{
		switch(method)
		{
			case 0:
				return new Object[] {getEnergy()};
			case 1:
				return new Object[] {operatingTicks};
			case 2:
				return new Object[] {facing};
			case 3:
				return new Object[] {canOperate(RecipeHandler.getMetallurgicInfuserRecipe(getInput()))};
			case 4:
				return new Object[] {getMaxEnergy()};
			case 5:
				return new Object[] {getMaxEnergy()-getEnergy()};
			case 6:
				return new Object[] {infuseStored};
			case 7:
				return new Object[] {MAX_INFUSE-infuseStored.amount};
			default:
				Mekanism.logger.error("Attempted to call unknown method with computer ID " + computer.getID());
				return new Object[] {"Unknown command."};
		}
	}

	@Override
	@Method(modid = "ComputerCraft")
	public boolean equals(IPeripheral other)
	{
		return this == other;
	}

	@Override
	@Method(modid = "ComputerCraft")
	public void attach(IComputerAccess computer) {}

	@Override
	@Method(modid = "ComputerCraft")
	public void detach(IComputerAccess computer) {}

	@Override
	public int[] getAccessibleSlotsFromSide(int side)
	{
		return sideOutputs.get(sideConfig[MekanismUtils.getBaseOrientation(side, facing)]).availableSlots;
	}

	@Override
	public boolean canSetFacing(int side)
	{
		return side != 0 && side != 1;
	}

	@Override
	public void setActive(boolean active)
	{
		isActive = active;

		if(clientActive != active && updateDelay == 0)
		{
			Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList())), new Range4D(Coord4D.get(this)));

			updateDelay = 10;
			clientActive = active;
		}
	}

	@Override
	public boolean getActive()
	{
		return isActive;
	}

	@Override
	public ArrayList<SideData> getSideData()
	{
		return sideOutputs;
	}

	@Override
	public byte[] getConfiguration()
	{
		return sideConfig;
	}

	@Override
	public int getFacing()
	{
		return facing;
	}

	@Override
	public boolean renderUpdate()
	{
		return true;
	}

	@Override
	public boolean lightUpdate()
	{
		return true;
	}

	@Override
	public RedstoneControl getControlType()
	{
		return controlType;
	}

	@Override
	public void setControlType(RedstoneControl type)
	{
		controlType = type;
		MekanismUtils.saveChunk(this);
	}

	@Override
	public boolean canPulse()
	{
		return false;
	}

	@Override
	public TileComponentUpgrade getComponent()
	{
		return upgradeComponent;
	}

	@Override
	public IEjector getEjector()
	{
		return ejectorComponent;
	}

	@Override
	public void recalculateUpgradables(Upgrade upgrade)
	{
		super.recalculateUpgradables(upgrade);

		switch(upgrade)
		{
			case SPEED:
				ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
			case ENERGY:
				energyPerTick = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK);
		}
	}
}