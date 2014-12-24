package mekanism.common.tile;

import java.util.ArrayList;

import mekanism.api.Coord4D;
import mekanism.api.IConfigurable;
import mekanism.api.Range4D;
import mekanism.api.util.StackUtils;
import mekanism.common.Mekanism;
import mekanism.common.PacketHandler;
import mekanism.common.base.IActiveState;
import mekanism.common.base.ILogisticalTransporter;
import mekanism.common.block.states.BlockStateBasic.BasicBlockType;
import mekanism.common.content.transporter.TransporterManager;
import mekanism.common.item.ItemBlockBasic;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.TransporterUtils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.common.Optional.Interface;

import io.netty.buffer.ByteBuf;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;
import scala.tools.nsc.backend.icode.BasicBlocks.BasicBlock;

@Interface(iface = "powercrystals.minefactoryreloaded.api.IDeepStorageUnit", modid = "MineFactoryReloaded")
public class TileEntityBin extends TileEntityBasicBlock implements ISidedInventory, IActiveState, IDeepStorageUnit, IConfigurable
{
	public boolean isActive;

	public boolean clientActive;

	public final int MAX_DELAY = 10;

	public int addTicks = 0;

	public int delayTicks;

	public int cacheCount;

	public final int MAX_STORAGE = 4096;

	public ItemStack itemType;

	public ItemStack topStack;
	public ItemStack bottomStack;

	public int prevCount;

	public int clientAmount;

	public void sortStacks()
	{
		if(getItemCount() == 0 || itemType == null)
		{
			itemType = null;
			topStack = null;
			bottomStack = null;
			cacheCount = 0;

			return;
		}

		int count = getItemCount();
		int remain = MAX_STORAGE-count;

		if(remain >= itemType.getMaxStackSize())
		{
			topStack = null;
		}
		else {
			topStack = itemType.copy();
			topStack.stackSize = itemType.getMaxStackSize()-remain;
		}

		count -= StackUtils.getSize(topStack);

		bottomStack = itemType.copy();
		bottomStack.stackSize = Math.min(itemType.getMaxStackSize(), count);

		count -= StackUtils.getSize(bottomStack);

		cacheCount = count;
	}

	public boolean isValid(ItemStack stack)
	{
		if(stack == null || stack.stackSize <= 0)
		{
			return false;
		}

		if(stack.getItem() instanceof ItemBlockBasic && stack.getItemDamage() == 6)
		{
			return false;
		}

		if(itemType == null)
		{
			return true;
		}

		if(!stack.isItemEqual(itemType) || !ItemStack.areItemStackTagsEqual(stack, itemType))
		{
			return false;
		}

		return true;
	}

	public ItemStack add(ItemStack stack)
	{
		if(isValid(stack) && getItemCount() != MAX_STORAGE)
		{
			if(itemType == null)
			{
				setItemType(stack);
			}

			if(getItemCount() + stack.stackSize <= MAX_STORAGE)
			{
				setItemCount(getItemCount() + stack.stackSize);
				return null;
			}
			else {
				ItemStack rejects = itemType.copy();
				rejects.stackSize = (getItemCount()+stack.stackSize) - MAX_STORAGE;

				setItemCount(MAX_STORAGE);

				return rejects;
			}
		}

		return stack;
	}

	public ItemStack removeStack()
	{
		if(getItemCount() == 0)
		{
			return null;
		}

		return remove(bottomStack.stackSize);
	}

	public ItemStack remove(int amount)
	{
		if(getItemCount() == 0)
		{
			return null;
		}

		ItemStack ret = itemType.copy();
		ret.stackSize = Math.min(Math.min(amount, itemType.getMaxStackSize()), getItemCount());

		setItemCount(getItemCount() - ret.stackSize);

		return ret;
	}

	public int getItemCount()
	{
		return StackUtils.getSize(bottomStack) + cacheCount + StackUtils.getSize(topStack);
	}

	@Override
	public void onUpdate()
	{
		if(!worldObj.isRemote)
		{
			addTicks = Math.max(0, addTicks-1);
			delayTicks = Math.max(0, delayTicks-1);

			sortStacks();

			if(getItemCount() != prevCount)
			{
				markDirty();
				MekanismUtils.saveChunk(this);
			}

			if(delayTicks == 0)
			{
				if(bottomStack != null && isActive)
				{
					TileEntity tile = Coord4D.get(this).offset(EnumFacing.getFront(0)).getTileEntity(worldObj);

					if(tile instanceof ILogisticalTransporter)
					{
						ILogisticalTransporter transporter = (ILogisticalTransporter)tile;

						ItemStack rejects = TransporterUtils.insert(this, transporter, bottomStack, null, true, 0);

						if(TransporterManager.didEmit(bottomStack, rejects))
						{
							setInventorySlotContents(0, rejects);
						}
					}
					else if(tile instanceof IInventory)
					{
						setInventorySlotContents(0, InventoryUtils.putStackInInventory((IInventory)tile, bottomStack, EnumFacing.DOWN, false));
					}

					delayTicks = 10;
				}
			}
			else {
				delayTicks--;
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);

		nbtTags.setBoolean("isActive", isActive);
		nbtTags.setInteger("itemCount", cacheCount);

		if(bottomStack != null)
		{
			nbtTags.setTag("bottomStack", bottomStack.writeToNBT(new NBTTagCompound()));
		}

		if(topStack != null)
		{
			nbtTags.setTag("topStack", topStack.writeToNBT(new NBTTagCompound()));
		}

		if(getItemCount() > 0)
		{
			nbtTags.setTag("itemType", itemType.writeToNBT(new NBTTagCompound()));
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);

		isActive = nbtTags.getBoolean("isActive");
		cacheCount = nbtTags.getInteger("itemCount");

		bottomStack = ItemStack.loadItemStackFromNBT(nbtTags.getCompoundTag("bottomStack"));
		topStack = ItemStack.loadItemStackFromNBT(nbtTags.getCompoundTag("topStack"));

		if(getItemCount() > 0)
		{
			itemType = ItemStack.loadItemStackFromNBT(nbtTags.getCompoundTag("itemType"));
		}
	}

	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		super.getNetworkedData(data);

		data.add(isActive);
		data.add(getItemCount());

		if(getItemCount() > 0)
		{
			data.add(itemType);
		}

		return data;
	}

	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		super.handlePacketData(dataStream);

		isActive = dataStream.readBoolean();
		clientAmount = dataStream.readInt();

		if(clientAmount > 0)
		{
			itemType = PacketHandler.readStack(dataStream);
		}
		else {
			itemType = null;
		}

		MekanismUtils.updateBlock(worldObj, getPos());
	}

	@Override
	public ItemStack getStackInSlot(int slotID)
	{
		if(slotID == 1)
		{
			return topStack;
		}
		else {
			return bottomStack;
		}
	}

	@Override
	public ItemStack decrStackSize(int slotID, int amount)
	{
		if(slotID == 1)
		{
			return null;
		}
		else if(slotID == 0)
		{
			int toRemove = Math.min(getItemCount(), amount);

			if(toRemove > 0)
			{
				ItemStack ret = itemType.copy();
				ret.stackSize = toRemove;

				setItemCount(getItemCount()-toRemove);

				return ret;
			}
		}

		return null;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slotID)
	{
		return getStackInSlot(slotID);
	}

	@Override
	public int getSizeInventory()
	{
		return 2;
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack)
	{
		if(i == 0)
		{
			if(getItemCount() == 0)
			{
				return;
			}

			if(itemstack == null)
			{
				setItemCount(getItemCount() - bottomStack.stackSize);
			}
			else {
				setItemCount(getItemCount() - (bottomStack.stackSize-itemstack.stackSize));
			}
		}
		else if(i == 1)
		{
			if(isValid(itemstack))
			{
				add(itemstack);
			}
		}
	}

	@Override
	public void markDirty()
	{
		super.markDirty();

		if(!worldObj.isRemote)
		{
			MekanismUtils.saveChunk(this);
			Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList())), new Range4D(Coord4D.get(this)));
			prevCount = getItemCount();
			sortStacks();
		}
	}

	public void setItemType(ItemStack stack)
	{
		if(stack == null)
		{
			itemType = null;
			cacheCount = 0;
			topStack = null;
			bottomStack = null;
			return;
		}

		ItemStack ret = stack.copy();
		ret.stackSize = 1;
		itemType = ret;
	}

	public void setItemCount(int count)
	{
		cacheCount = Math.max(0, count);
		topStack = null;
		bottomStack = null;

		if(count == 0)
		{
			setItemType(null);
		}

		markDirty();
	}

	@Override
	public String getName()
	{
		return MekanismUtils.localize("tile.BasicBlock.Bin.name");
	}

	@Override
	public boolean hasCustomName()
	{
		return true;
	}

	@Override
	public IChatComponent getDisplayName()
	{
		return new ChatComponentText(getName());
	}

	@Override
	public int getInventoryStackLimit()
	{
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer)
	{
		return true;
	}

	@Override
	public void openInventory(EntityPlayer entityPlayer) {}

	@Override
	public void closeInventory(EntityPlayer entityPlayer) {}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack)
	{
		return i == 1 && isValid(itemstack);
	}

	@Override
	public int getField(int id)
	{
		return 0;
	}

	@Override
	public void setField(int id, int value)
	{

	}

	@Override
	public int getFieldCount()
	{
		return 0;
	}

	@Override
	public void clear()
	{
		//TODO
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side)
	{
		if(side == EnumFacing.UP)
		{
			return new int[] {1};
		}
		else if(side == EnumFacing.DOWN)
		{
			return new int[] {0};
		}

		return InventoryUtils.EMPTY;
	}

	@Override
	public boolean canInsertItem(int index, ItemStack itemstack, EnumFacing side)
	{
		return isItemValidForSlot(index, itemstack);
	}

	@Override
	public boolean canExtractItem(int index, ItemStack itemstack, EnumFacing side)
	{
		return index == 0 && isValid(itemstack);
	}

	@Override
	public Predicate<EnumFacing> getFacePredicate()
	{
		return BasicBlockType.BIN.facingPredicate;
	}

	@Override
	public void setActive(boolean active)
	{
		isActive = active;

		if(clientActive != active)
		{
			Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList())), new Range4D(Coord4D.get(this)));

			clientActive = active;
		}
	}

	@Override
	public boolean getActive()
	{
		return isActive;
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
	public ItemStack getStoredItemType()
	{
		if(itemType == null)
		{
			return null;
		}

		return MekanismUtils.size(itemType, getItemCount());
	}

	@Override
	public void setStoredItemCount(int amount)
	{
		if(amount == 0)
		{
			setStoredItemType(null, 0);
		}

		setItemCount(amount);
	}

	@Override
	public void setStoredItemType(ItemStack type, int amount)
	{
		itemType = type;
		cacheCount = amount;

		topStack = null;
		bottomStack = null;

		markDirty();
	}

	@Override
	public int getMaxStoredCount()
	{
		return MAX_STORAGE;
	}

	@Override
	public boolean onSneakRightClick(EntityPlayer player, EnumFacing side)
	{
		setActive(!getActive());
		worldObj.playSoundEffect(getPos().getX(), getPos().getY(), getPos().getZ(), "random.click", 0.3F, 1);
		return true;
	}

	@Override
	public boolean onRightClick(EntityPlayer player, EnumFacing side)
	{
		return false;
	}
}