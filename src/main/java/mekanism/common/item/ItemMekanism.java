package mekanism.common.item;

import mekanism.common.Mekanism;

import net.minecraft.item.Item;

public class ItemMekanism extends Item
{
	public ItemMekanism()
	{
		super();
		setCreativeTab(Mekanism.tabMekanism);
	}

/*
	@Override
	public void registerIcons(TextureMap register)
	{
		itemIcon = register.registerIcon("mekanism:" + getUnlocalizedName().replace("item.", ""));
	}
*/
}
