package mekanism.common.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import mekanism.api.Coord4D;
import mekanism.api.Range4D;
import mekanism.client.ClientProxy;
import mekanism.common.CTMData;
import mekanism.common.ItemAttacher;
import mekanism.common.Mekanism;
import mekanism.common.MekanismBlocks;
import mekanism.common.base.IActiveState;
import mekanism.common.base.IBlockCTM;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.content.tank.TankUpdateProtocol;
import mekanism.common.inventory.InventoryBin;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.tile.TileEntityBasicBlock;
import mekanism.common.tile.TileEntityBin;
import mekanism.common.tile.TileEntityDynamicTank;
import mekanism.common.tile.TileEntityDynamicValve;
import mekanism.common.tile.TileEntitySalinationBlock;
import mekanism.common.tile.TileEntitySalinationController;
import mekanism.common.tile.TileEntitySalinationValve;
import mekanism.common.util.MekanismUtils;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Block class for handling multiple metal block IDs.
 * 0:0: Osmium Block
 * 0:1: Bronze Block
 * 0:2: Refined Obsidian
 * 0:3: Charcoal Block
 * 0:4: Refined Glowstone
 * 0:5: Steel Block
 * 0:6: Bin
 * 0:7: Teleporter Frame
 * 0:8: Steel Casing
 * 0:9: Dynamic Tank
 * 0:10: Dynamic Glass
 * 0:11: Dynamic Valve
 * 0:12: Copper Block
 * 0:13: Tin Block
 * 0:14: Salination Controller
 * 0:15: Salination Valve
 * 1:0: Salination Block
 * @author AidanBrady
 *
 */
public class BlockBasic extends Block implements IBlockCTM
{
	public IIcon[][] icons = new IIcon[16][6];

	public CTMData[][] ctms = new CTMData[16][2];

	public BasicBlock blockType;

	public BlockBasic(BasicBlock type)
	{
		super(Material.iron);
		setHardness(5F);
		setResistance(10F);
		setCreativeTab(Mekanism.tabMekanism);
		blockType = type;
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block block)
	{
		if(!world.isRemote)
		{
			TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));

			if(block == this && tileEntity instanceof TileEntityDynamicTank)
			{
				((TileEntityDynamicTank)tileEntity).update();
			}

			if(tileEntity instanceof TileEntityBasicBlock)
			{
				((TileEntityBasicBlock)tileEntity).onNeighborChange(block);
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register)
	{
		switch(blockType)
		{
			case BASIC_BLOCK_1:
				ctms[9][0] = new CTMData("ctm/DynamicTank", this, Arrays.asList(9, 11)).registerIcons(register);
				ctms[10][0] = new CTMData("ctm/DynamicGlass", this, Arrays.asList(10)).registerIcons(register);
				ctms[11][0] = new CTMData("ctm/DynamicValve", this, Arrays.asList(11, 9)).registerIcons(register);

				ctms[14][0] = new CTMData("ctm/SalinationBlock", this, Arrays.asList(14, 15)).addOtherBlockConnectivities(MekanismBlocks.BasicBlock2, Arrays.asList(0)).addFacingOverride("ctm/SalinationController").registerIcons(register);
				ctms[14][1] = new CTMData("ctm/SalinationBlock", this, Arrays.asList(14, 15)).addOtherBlockConnectivities(MekanismBlocks.BasicBlock2, Arrays.asList(0)).addFacingOverride("ctm/SalinationControllerOn").registerIcons(register);
				ctms[15][0] = new CTMData("ctm/SalinationValve", this, Arrays.asList(15, 14)).addOtherBlockConnectivities(MekanismBlocks.BasicBlock2, Arrays.asList(0)).registerIcons(register);

				icons[0][0] = register.registerIcon("mekanism:OsmiumBlock");
				icons[1][0] = register.registerIcon("mekanism:BronzeBlock");
				icons[2][0] = register.registerIcon("mekanism:RefinedObsidian");
				icons[3][0] = register.registerIcon("mekanism:CoalBlock");
				icons[4][0] = register.registerIcon("mekanism:RefinedGlowstone");
				icons[5][0] = register.registerIcon("mekanism:SteelBlock");
				icons[6][0] = register.registerIcon("mekanism:BinSide");
				icons[6][1] = register.registerIcon("mekanism:BinTop");
				icons[6][2] = register.registerIcon("mekanism:BinFront");
				icons[6][3] = register.registerIcon("mekanism:BinTopOn");
				icons[6][4] = register.registerIcon("mekanism:BinFrontOn");
				icons[7][0] = register.registerIcon("mekanism:TeleporterFrame");
				icons[8][0] = register.registerIcon("mekanism:SteelCasing");
				icons[9][0] = ctms[9][0].mainTextureData.icon;
				icons[10][0] = ctms[10][0].mainTextureData.icon;
				icons[11][0] = ctms[11][0].mainTextureData.icon;
				icons[12][0] = register.registerIcon("mekanism:CopperBlock");
				icons[13][0] = register.registerIcon("mekanism:TinBlock");
				icons[14][0] = ctms[14][0].facingOverride.icon;
				icons[14][1] = ctms[14][1].facingOverride.icon;
				icons[14][2] = ctms[14][0].mainTextureData.icon;
				icons[15][0] = ctms[15][0].mainTextureData.icon;
				break;
			case BASIC_BLOCK_2:
				ctms[0][0] = new CTMData("ctm/SalinationBlock", this, Arrays.asList(0)).addOtherBlockConnectivities(MekanismBlocks.BasicBlock, Arrays.asList(14, 15)).registerIcons(register);

				icons[0][0] = ctms[0][0].mainTextureData.icon;
				break;
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side)
	{
		int meta = world.getBlockMetadata(x, y, z);

		switch(blockType)
		{
			case BASIC_BLOCK_1:
				switch(meta)
				{
					case 6:
						TileEntityBasicBlock tileEntity6 = (TileEntityBasicBlock)world.getTileEntity(new BlockPos(x, y, z));

						if(side == 0 || side == 1)
						{
							return MekanismUtils.isActive(world, x, y, z) ? icons[meta][3] : icons[meta][1];
						} else if(side == tileEntity6.facing)
						{
							return MekanismUtils.isActive(world, x, y, z) ? icons[meta][4] : icons[meta][2];
						} else
						{
							return icons[meta][0];
						}
					case 9:
					case 10:
					case 11:
						return ctms[meta][0].getIcon(side);
					case 14:
						TileEntitySalinationController tileEntity14 = (TileEntitySalinationController)world.getTileEntity(new BlockPos(x, y, z));

						if(side == tileEntity14.facing)
						{
							return MekanismUtils.isActive(world, x, y, z) ? icons[meta][1] : icons[meta][0];
						} else
						{
							return icons[meta][2];
						}
					default:
						return getIcon(side, meta);
				}
			case BASIC_BLOCK_2:
				switch(meta)
				{
					default:
						return getIcon(side, meta);
				}
		}

		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta)
	{
		switch(blockType)
		{
			case BASIC_BLOCK_1:
				switch(meta)
				{
					case 6:
						if(side == 0 || side == 1)
						{
							return icons[meta][1];
						} else if(side == 3)
						{
							return icons[meta][2];
						} else
						{
							return icons[meta][0];
						}
					case 14:
						if(side == 3)
						{
							return icons[meta][0];
						} else
						{
							return icons[meta][2];
						}
					default:
						return icons[meta][0];
				}
			case BASIC_BLOCK_2:
				return icons[meta][0];
			default:
				return icons[meta][0];
		}
	}

	@Override
	public int damageDropped(int i)
	{
		return i;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(Item item, CreativeTabs creativetabs, List list)
	{
		switch(blockType)
		{
			case BASIC_BLOCK_1:
				for(int i = 0; i < 16; i++)
				{
					list.add(new ItemStack(item, 1, i));
				}
				break;
			case BASIC_BLOCK_2:
				for(int i = 0; i < 1; i++)
				{
					list.add(new ItemStack(item, 1, i));
				}
				break;
		}
	}

	@Override
	public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z)
	{
		int meta = world.getBlockMetadata(x, y, z);

		switch(blockType)
		{
			case BASIC_BLOCK_1:
				switch(meta)
				{
					case 9:
					case 10:
					case 11:
						TileEntityDynamicTank tileEntity = (TileEntityDynamicTank)world.getTileEntity(new BlockPos(x, y, z));

						if(tileEntity != null)
						{
							if(FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER)
							{
								if(tileEntity.structure != null)
								{
									return false;
								}
							} else
							{
								if(tileEntity.clientHasStructure)
								{
									return false;
								}
							}
						}
				}
			case BASIC_BLOCK_2:
				return super.canCreatureSpawn(type, world, x, y, z);
			default:
				return super.canCreatureSpawn(type, world, x, y, z);
		}
	}

	@Override
	public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player)
	{
		int meta = world.getBlockMetadata(x, y, z);

		if(blockType == BasicBlock.BASIC_BLOCK_1)
		{
			if(!world.isRemote && meta == 6)
			{
				TileEntityBin bin = (TileEntityBin)world.getTileEntity(new BlockPos(x, y, z));
				MovingObjectPosition pos = MekanismUtils.rayTrace(world, player);

				if(pos != null && pos.sideHit == bin.facing)
				{
					if(bin.bottomStack != null)
					{
						if(!player.isSneaking())
						{
							world.spawnEntityInWorld(new EntityItem(world, player.posX, player.posY, player.posZ, bin.removeStack().copy()));
						}
						else {
							world.spawnEntityInWorld(new EntityItem(world, player.posX, player.posY, player.posZ, bin.remove(1).copy()));
						}
					}
				}
			}
		}
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityplayer, int i1, float f1, float f2, float f3)
	{
		int metadata = world.getBlockMetadata(x, y, z);

		if(blockType == BasicBlock.BASIC_BLOCK_1)
		{
			if(metadata != 6)
			{
				if(ItemAttacher.canAttach(entityplayer.getCurrentEquippedItem()))
				{
					return false;
				}
			}

			if(metadata == 2)
			{
				if(entityplayer.isSneaking())
				{
					entityplayer.openGui(Mekanism.instance, 1, world, x, y, z);
					return true;
				}
			}

			if(metadata == 14)
			{
				if(!entityplayer.isSneaking())
				{
					entityplayer.openGui(Mekanism.instance, 33, world, x, y, z);
					return true;
				}
			}

			if(world.isRemote)
			{
				return true;
			}

			if(metadata == 6)
			{
				TileEntityBin bin = (TileEntityBin)world.getTileEntity(new BlockPos(x, y, z));

				if(bin.getItemCount() < bin.MAX_STORAGE)
				{
					if(bin.addTicks == 0 && entityplayer.getCurrentEquippedItem() != null)
					{
						if(entityplayer.getCurrentEquippedItem() != null)
						{
							ItemStack remain = bin.add(entityplayer.getCurrentEquippedItem());
							entityplayer.setCurrentItemOrArmor(0, remain);
							bin.addTicks = 5;
						}
					}
					else if(bin.addTicks > 0 && bin.getItemCount() > 0)
					{
						ItemStack[] inv = entityplayer.inventory.mainInventory;

						for(int i = 0; i < inv.length; i++)
						{
							if(bin.getItemCount() == bin.MAX_STORAGE)
							{
								break;
							}

							if(inv[i] != null)
							{
								ItemStack remain = bin.add(inv[i]);
								inv[i] = remain;
								bin.addTicks = 5;
							}

							((EntityPlayerMP)entityplayer).sendContainerAndContentsToPlayer(entityplayer.openContainer, entityplayer.openContainer.getInventory());
						}
					}
				}

				return true;
			}
			else if(metadata == 9 || metadata == 10 || metadata == 11)
			{
				if(!entityplayer.isSneaking() && ((TileEntityDynamicTank)world.getTileEntity(new BlockPos(x, y, z))).structure != null)
				{
					TileEntityDynamicTank tileEntity = (TileEntityDynamicTank)world.getTileEntity(new BlockPos(x, y, z));

					if(!manageInventory(entityplayer, tileEntity))
					{
						Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(tileEntity), tileEntity.getNetworkedData(new ArrayList())), new Range4D(Coord4D.get(tileEntity)));
						entityplayer.openGui(Mekanism.instance, 18, world, x, y, z);
					}
					else {
						entityplayer.inventory.markDirty();
						tileEntity.sendPacketToRenderer();
					}

					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean isSideSolid(IBlockAccess world, int x, int y, int z, EnumFacing side)
	{
		return !(blockType == BasicBlock.BASIC_BLOCK_1 && world.getBlockMetadata(x, y, z) == 10);
	}

	private boolean manageInventory(EntityPlayer player, TileEntityDynamicTank tileEntity)
	{
		ItemStack itemStack = player.getCurrentEquippedItem();

		if(itemStack != null && tileEntity.structure != null)
		{
			if(FluidContainerRegistry.isEmptyContainer(itemStack))
			{
				if(tileEntity.structure.fluidStored != null && tileEntity.structure.fluidStored.amount >= FluidContainerRegistry.BUCKET_VOLUME)
				{
					ItemStack filled = FluidContainerRegistry.fillFluidContainer(tileEntity.structure.fluidStored, itemStack);

					if(filled != null)
					{
						if(player.capabilities.isCreativeMode)
						{
							tileEntity.structure.fluidStored.amount -= FluidContainerRegistry.getFluidForFilledItem(filled).amount;

							if(tileEntity.structure.fluidStored.amount == 0)
							{
								tileEntity.structure.fluidStored = null;
							}

							return true;
						}

						if(itemStack.stackSize > 1)
						{
							if(player.inventory.addItemStackToInventory(filled))
							{
								itemStack.stackSize--;

								tileEntity.structure.fluidStored.amount -= FluidContainerRegistry.getFluidForFilledItem(filled).amount;

								if(tileEntity.structure.fluidStored.amount == 0)
								{
									tileEntity.structure.fluidStored = null;
								}

								return true;
							}
						}
						else if(itemStack.stackSize == 1)
						{
							player.setCurrentItemOrArmor(0, filled);

							tileEntity.structure.fluidStored.amount -= FluidContainerRegistry.getFluidForFilledItem(filled).amount;

							if(tileEntity.structure.fluidStored.amount == 0)
							{
								tileEntity.structure.fluidStored = null;
							}

							return true;
						}
					}
				}
			}
			else if(FluidContainerRegistry.isFilledContainer(itemStack))
			{
				FluidStack itemFluid = FluidContainerRegistry.getFluidForFilledItem(itemStack);
				int max = tileEntity.structure.volume*TankUpdateProtocol.FLUID_PER_TANK;

				if(tileEntity.structure.fluidStored == null || (tileEntity.structure.fluidStored.isFluidEqual(itemFluid) && (tileEntity.structure.fluidStored.amount+itemFluid.amount <= max)))
				{
					boolean filled = false;
					
					if(player.capabilities.isCreativeMode)
					{
						filled = true;
					}
					else {
						ItemStack containerItem = itemStack.getItem().getContainerItem(itemStack);
	
						if(containerItem != null)
						{
							if(itemStack.stackSize == 1)
							{
								player.setCurrentItemOrArmor(0, containerItem);
								filled = true;
							}
							else {
								if(player.inventory.addItemStackToInventory(containerItem))
								{
									itemStack.stackSize--;
	
									filled = true;
								}
							}
						}
						else {
							itemStack.stackSize--;
	
							if(itemStack.stackSize == 0)
							{
								player.setCurrentItemOrArmor(0, null);
							}
	
							filled = true;
						}
					}

					if(filled)
					{
						if(tileEntity.structure.fluidStored == null)
						{
							tileEntity.structure.fluidStored = itemFluid;
						}
						else {
							tileEntity.structure.fluidStored.amount += itemFluid.amount;
						}
						
						return true;
					}
				}
			}
		}

		return false;
	}

	@Override
	public boolean renderAsNormalBlock()
	{
		return false;
	}

	@Override
	public boolean isOpaqueCube()
	{
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getRenderType()
	{
		return ClientProxy.CTM_RENDER_ID;
	}

	@Override
	public int getLightValue(IBlockAccess world, int x, int y, int z)
	{
		TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));
		int metadata = world.getBlockMetadata(x, y, z);

		if(tileEntity instanceof IActiveState)
		{
			if(((IActiveState)tileEntity).getActive() && ((IActiveState)tileEntity).lightUpdate())
			{
				return 15;
			}
		}

		if(blockType == BasicBlock.BASIC_BLOCK_1)
		{
			switch(metadata)
			{
				case 2:
					return 8;
				case 4:
					return 15;
				case 7:
					return 12;
			}
		}

		return 0;
	}

	@Override
	public boolean hasTileEntity(int metadata)
	{
		switch(blockType)
		{
			case BASIC_BLOCK_1:
				switch(metadata)
				{
					case 6:
					case 9:
					case 10:
					case 11:
					case 12:
					case 14:
					case 15:
						return true;
					default:
						return false;
				}
			case BASIC_BLOCK_2:
				switch(metadata)
				{
					case 0:
						return true;
					default:
						return false;
				}
			default:
				return false;
		}
	}

	@Override
	public TileEntity createTileEntity(World world, int metadata)
	{
		switch(blockType)
		{
			case BASIC_BLOCK_1:
				switch(metadata)
				{
					case 6:
						return new TileEntityBin();
					case 9:
						return new TileEntityDynamicTank();
					case 10:
						return new TileEntityDynamicTank();
					case 11:
						return new TileEntityDynamicValve();
					case 14:
						return new TileEntitySalinationController();
					case 15:
						return new TileEntitySalinationValve();
					default:
						return null;
				}
			case BASIC_BLOCK_2:
				switch(metadata)
				{
					case 0:
						return new TileEntitySalinationBlock();
					default:
						return null;
				}
			default:
				return null;
		}
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityliving, ItemStack itemstack)
	{
		if(world.getTileEntity(new BlockPos(x, y, z)) instanceof TileEntityBasicBlock)
		{
			TileEntityBasicBlock tileEntity = (TileEntityBasicBlock)world.getTileEntity(new BlockPos(x, y, z));
			int side = MathHelper.floor_double((entityliving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
			int height = Math.round(entityliving.rotationPitch);
			int change = 3;

			if(tileEntity.canSetFacing(0) && tileEntity.canSetFacing(1))
			{
				if(height >= 65)
				{
					change = 1;
				}
				else if(height <= -65)
				{
					change = 0;
				}
			}

			if(change != 0 && change != 1)
			{
				switch(side)
				{
					case 0: change = 2; break;
					case 1: change = 5; break;
					case 2: change = 3; break;
					case 3: change = 4; break;
				}
			}

			tileEntity.setFacing((short)change);
			tileEntity.redstone = world.isBlockIndirectlyGettingPowered(x, y, z);

			if(tileEntity instanceof IBoundingBlock)
			{
				((IBoundingBlock)tileEntity).onPlace();
			}
		}

		world.func_147479_m(x, y, z);
		world.updateLightByType(EnumSkyBlock.Block, x, y, z);
	    world.updateLightByType(EnumSkyBlock.Sky, x, y, z);

		if(!world.isRemote && world.getTileEntity(new BlockPos(x, y, z)) != null)
		{
			TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));

			if(tileEntity instanceof TileEntityDynamicTank)
			{
				((TileEntityDynamicTank)tileEntity).update();
			}
		}
	}

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z)
	{
		ItemStack ret = new ItemStack(this, 1, world.getBlockMetadata(x, y, z));

		if(blockType == BasicBlock.BASIC_BLOCK_1)
		{
			if(ret.getItemDamage() == 6)
			{
				TileEntityBin tileEntity = (TileEntityBin)world.getTileEntity(new BlockPos(x, y, z));
				InventoryBin inv = new InventoryBin(ret);

				inv.setItemCount(tileEntity.getItemCount());

				if(tileEntity.getItemCount() > 0)
				{
					inv.setItemType(tileEntity.itemType);
				}
			}
		}

		return ret;
	}

	@Override
	public Item getItemDropped(int i, Random random, int j)
	{
		return null;
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest)
	{
		if(!player.capabilities.isCreativeMode && !world.isRemote && canHarvestBlock(player, world.getBlockMetadata(x, y, z)))
		{

			float motion = 0.7F;
			double motionX = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
			double motionY = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
			double motionZ = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;

			EntityItem entityItem = new EntityItem(world, x + motionX, y + motionY, z + motionZ, getPickBlock(null, world, x, y, z));

			world.spawnEntityInWorld(entityItem);
		}

		return world.setBlockToAir(x, y, z);
	}

	public ItemStack dismantleBlock(World world, int x, int y, int z, boolean returnBlock)
	{
		ItemStack itemStack = getPickBlock(null, world, x, y, z);

		world.setBlockToAir(x, y, z);

		if(!returnBlock)
		{
			float motion = 0.7F;
			double motionX = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
			double motionY = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
			double motionZ = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;

			EntityItem entityItem = new EntityItem(world, x + motionX, y + motionY, z + motionZ, itemStack);

			world.spawnEntityInWorld(entityItem);
		}

		return itemStack;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side)
	{
		Coord4D obj = new Coord4D(x, y, z).getFromSide(EnumFacing.getOrientation(side).getOpposite());
		if(blockType == BasicBlock.BASIC_BLOCK_1 && obj.getMetadata(world) == 10)
		{
			return ctms[10][0].shouldRenderSide(world, x, y, z, side);
		}
		else {
			return super.shouldSideBeRendered(world, x, y, z, side);
		}
	}

	@Override
	public EnumFacing[] getValidRotations(World world, int x, int y, int z)
	{
		TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
		EnumFacing[] valid = new EnumFacing[6];
		
		if(tile instanceof TileEntityBasicBlock)
		{
			TileEntityBasicBlock basicTile = (TileEntityBasicBlock)tile;
			
			for(EnumFacing dir : EnumFacing.VALID_DIRECTIONS)
			{
				if(basicTile.canSetFacing(dir.ordinal()))
				{
					valid[dir.ordinal()] = dir;
				}
			}
		}
		return valid;
	}

	@Override
	public boolean rotateBlock(World world, int x, int y, int z, EnumFacing axis)
	{
		TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
		
		if(tile instanceof TileEntityBasicBlock)
		{
			TileEntityBasicBlock basicTile = (TileEntityBasicBlock)tile;
			
			if(basicTile.canSetFacing(axis.ordinal()))
			{
				basicTile.setFacing((short)axis.ordinal());
				return true;
			}
		}
		
		return false;
	}

	@Override
	public CTMData getCTMData(IBlockAccess world, int x, int y, int z, int meta)
	{
		if(ctms[meta][1] != null && MekanismUtils.isActive(world, x, y, z))
		{
			return ctms[meta][1];
		}

		return ctms[meta][0];
	}

	public static enum BasicBlock
	{
		BASIC_BLOCK_1,
		BASIC_BLOCK_2;
	}
}