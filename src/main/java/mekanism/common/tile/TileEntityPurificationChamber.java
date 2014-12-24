package mekanism.common.tile;

import java.util.Map;

import mekanism.api.MekanismConfig.usage;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTransmission;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.IGasItem;
import mekanism.api.gas.ITubeConnection;
import mekanism.common.MekanismBlocks;
import mekanism.common.block.states.BlockStateMachine.MachineBlockType;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.machines.PurificationRecipe;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class TileEntityPurificationChamber extends TileEntityAdvancedElectricMachine<PurificationRecipe> implements IGasHandler, ITubeConnection
{
	public TileEntityPurificationChamber()
	{
		super("purification", "PurificationChamber", 1, 200, MachineBlockType.PURIFICATION_CHAMBER);
	}

	@Override
	public Map getRecipes()
	{
		return Recipe.PURIFICATION_CHAMBER.get();
	}

	@Override
	public GasStack getItemGas(ItemStack itemstack)
	{
		if(itemstack.isItemEqual(new ItemStack(Items.flint))) return new GasStack(GasRegistry.getGas("oxygen"), 10);
		if(Block.getBlockFromItem(itemstack.getItem()) == MekanismBlocks.GasTank && ((IGasItem)itemstack.getItem()).getGas(itemstack) != null &&
				((IGasItem)itemstack.getItem()).getGas(itemstack).getGas() == GasRegistry.getGas("oxygen")) return new GasStack(GasRegistry.getGas("oxygen"), 1);

		return null;
	}

	@Override
	public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer)
	{
		if(stack.getGas() == GasRegistry.getGas("oxygen"))
		{
			return gasTank.receive(stack, doTransfer);
		}

		return 0;
	}

	@Override
	public boolean canReceiveGas(EnumFacing side, Gas type)
	{
		return type == GasRegistry.getGas("oxygen");
	}

	@Override
	public void handleSecondaryFuel()
	{
		if(inventory[1] != null && gasTank.getNeeded() > 0 && inventory[1].getItem() instanceof IGasItem)
		{
			GasStack removed = GasTransmission.removeGas(inventory[1], GasRegistry.getGas("oxygen"), gasTank.getNeeded());
			gasTank.receive(removed, true);
			return;
		}

		super.handleSecondaryFuel();
	}

	@Override
	public boolean canTubeConnect(EnumFacing side)
	{
		return true;
	}

	@Override
	public boolean isValidGas(Gas gas)
	{
		return gas == GasRegistry.getGas("oxygen");
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

	}
}
