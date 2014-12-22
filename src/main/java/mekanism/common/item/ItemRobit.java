package mekanism.common.item;

import java.util.List;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.common.base.ISustainedInventory;
import mekanism.common.entity.EntityRobit;
import mekanism.common.tile.TileEntityChargepad;
import mekanism.common.util.MekanismUtils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemRobit extends ItemEnergized implements ISustainedInventory
{
	public ItemRobit()
	{
		super(100000);
	}

/*
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(TextureMap register) {}
*/

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack itemstack, EntityPlayer entityplayer, List list, boolean flag)
	{
		super.addInformation(itemstack, entityplayer, list, flag);

		list.add(EnumColor.INDIGO + MekanismUtils.localize("tooltip.name") + ": " + EnumColor.GREY + getName(itemstack));
		list.add(EnumColor.AQUA + MekanismUtils.localize("tooltip.inventory") + ": " + EnumColor.GREY + (getInventory(itemstack) != null && getInventory(itemstack).tagCount() != 0));
	}

	@Override
	public boolean onItemUse(ItemStack itemstack, EntityPlayer entityplayer, World world, BlockPos pos, EnumFacing side, float posX, float posY, float posZ)
	{
		TileEntity tileEntity = world.getTileEntity(pos);

		if(tileEntity instanceof TileEntityChargepad)
		{
			TileEntityChargepad chargepad = (TileEntityChargepad)tileEntity;
			if(!chargepad.isActive)
			{
				if(!world.isRemote)
				{
					EntityRobit robit = new EntityRobit(world, pos.getX()+0.5, pos.getY()+0.1, pos.getZ()+0.5);

					robit.setHome(Coord4D.get(chargepad));
					robit.setEnergy(getEnergy(itemstack));
					robit.setOwner(entityplayer.getName());
					robit.setInventory(getInventory(itemstack));
					robit.setName(getName(itemstack));

					world.spawnEntityInWorld(robit);
				}

				entityplayer.setCurrentItemOrArmor(0, null);

				return true;
			}
		}

		return false;
	}

	@Override
	public boolean canSend(ItemStack itemStack)
	{
		return false;
	}

	public void setName(ItemStack itemstack, String name)
	{
		if(itemstack.getTagCompound() == null)
		{
			itemstack.setTagCompound(new NBTTagCompound());
		}

		itemstack.getTagCompound().setString("name", name);
	}

	public String getName(ItemStack itemstack)
	{
		if(itemstack.getTagCompound() == null)
		{
			return "Robit";
		}

		String name = itemstack.getTagCompound().getString("name");

		return name.equals("") ? "Robit" : name;
	}

	@Override
	public void setInventory(NBTTagList nbtTags, Object... data)
	{
		if(data[0] instanceof ItemStack)
		{
			ItemStack itemStack = (ItemStack)data[0];

			if(itemStack.getTagCompound() == null)
			{
				itemStack.setTagCompound(new NBTTagCompound());
			}

			itemStack.getTagCompound().setTag("Items", nbtTags);
		}
	}

	@Override
	public NBTTagList getInventory(Object... data)
	{
		if(data[0] instanceof ItemStack)
		{
			ItemStack itemStack = (ItemStack)data[0];

			if(itemStack.getTagCompound() == null)
			{
				return null;
			}

			return itemStack.getTagCompound().getTagList("Items", 10);
		}

		return null;
	}
}
