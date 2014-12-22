package mekanism.common.item;

import java.util.List;

import mekanism.api.EnumColor;
import mekanism.api.MekanismAPI;
import mekanism.common.MekanismBlocks;
import mekanism.common.block.BlockCardboardBox.BlockData;
import mekanism.common.tile.TileEntityCardboardBox;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemBlockCardboardBox extends ItemBlock
{
	private static boolean isMonitoring;

	public Block metaBlock;

	public ItemBlockCardboardBox(Block block)
	{
		super(block);
		setMaxStackSize(1);
		metaBlock = block;

		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack itemstack, EntityPlayer entityplayer, List list, boolean flag)
	{
		list.add(EnumColor.INDIGO + MekanismUtils.localize("tooltip.blockData") + ": " + LangUtils.transYesNo(getBlockData(itemstack) != null));

		if(getBlockData(itemstack) != null)
		{
			list.add(MekanismUtils.localize("tooltip.block") + ": " + new ItemStack(getBlockData(itemstack).block, getBlockData(itemstack).meta).getDisplayName());
			list.add(MekanismUtils.localize("tooltip.meta") + ": " + getBlockData(itemstack).meta);

			if(getBlockData(itemstack).tileTag != null)
			{
				list.add(MekanismUtils.localize("tooltip.tile") + ": " + getBlockData(itemstack).tileTag.getString("id"));
			}
		}
	}

	@Override
	public int getMetadata(int i)
	{
		return i;
	}

	@Override
	public TextureAtlasSprite getIconFromDamage(int i)
	{
		return metaBlock.getIcon(2, i);
	}

	@Override
	public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if(!player.isSneaking() && !world.isAirBlock(x, y, z) && stack.getItemDamage() == 0)
		{
			Block block = world.getBlockState(new BlockPos(x, y, z)).getBlock();
			int meta = world.getBlockMetadata(x, y, z);

			if(!world.isRemote && MekanismAPI.isBlockCompatible(Item.getItemFromBlock(block), meta))
			{
				BlockData data = new BlockData();
				data.block = block;
				data.meta = meta;

				isMonitoring = true;

				if(world.getTileEntity(new BlockPos(x, y, z)) != null)
				{
					TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
					NBTTagCompound tag = new NBTTagCompound();

					tile.writeToNBT(tag);
					data.tileTag = tag;
				}

				if(!player.capabilities.isCreativeMode)
				{
					stack.stackSize--;
				}

				world.setBlock(x, y, z, MekanismBlocks.CardboardBox, 1, 3);

				isMonitoring = false;

				TileEntityCardboardBox tileEntity = (TileEntityCardboardBox)world.getTileEntity(new BlockPos(x, y, z));

				if(tileEntity != null)
				{
					tileEntity.storedData = data;
				}

				return true;
			}
		}

		return false;
	}

	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int metadata)
	{
		if(world.isRemote)
		{
			return true;
		}

		boolean place = super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata);

		if(place)
		{
			TileEntityCardboardBox tileEntity = (TileEntityCardboardBox)world.getTileEntity(new BlockPos(x, y, z));

			if(tileEntity != null)
			{
				tileEntity.storedData = getBlockData(stack);
			}
		}

		return place;
	}

	public void setBlockData(ItemStack itemstack, BlockData data)
	{
		if(itemstack.getTagCompound() == null)
		{
			itemstack.setTagCompound(new NBTTagCompound());
		}

		itemstack.getTagCompound().setTag("blockData", data.write(new NBTTagCompound()));
	}

	public BlockData getBlockData(ItemStack itemstack)
	{
		if(itemstack.getTagCompound() == null || !itemstack.getTagCompound().hasKey("blockData"))
		{
			return null;
		}

		return BlockData.read(itemstack.getTagCompound().getCompoundTag("blockData"));
	}

	@SubscribeEvent
	public void onEntitySpawn(EntityJoinWorldEvent event)
	{
		if(event.entity instanceof EntityItem && isMonitoring)
		{
			event.setCanceled(true);
		}
	}
}
