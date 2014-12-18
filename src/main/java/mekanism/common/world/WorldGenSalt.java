package mekanism.common.world;

import java.util.Random;

import mekanism.common.MekanismBlocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

public class WorldGenSalt extends WorldGenerator
{
    private Block blockGen;
    
    private int numberOfBlocks;

    public WorldGenSalt(int blockNum)
    {
        blockGen = MekanismBlocks.SaltBlock;
        numberOfBlocks = blockNum;
    }

    @Override
    public boolean generate(World world, Random random, BlockPos pos)
    {
        if(world.getBlockState(pos).getBlock().getMaterial() != Material.water)
        {
            return false;
        }
        else {
            int toGenerate = random.nextInt(numberOfBlocks - 2) + 2;
            byte yOffset = 1;

            for(int xPos = pos.getX() - toGenerate; xPos <= pos.getX() + toGenerate; xPos++)
            {
                for(int zPos = pos.getZ() - toGenerate; zPos <= pos.getZ() + toGenerate; zPos++)
                {
                    int xOffset = xPos - pos.getX();
                    int zOffset = zPos - pos.getZ();

                    if((xOffset*xOffset) + (zOffset*zOffset) <= toGenerate*toGenerate)
                    {
                        for(int yPos = pos.getY() - yOffset; yPos <= pos.getY() + yOffset; yPos++)
                        {
                            Block block = world.getBlockState(new BlockPos(xPos, yPos, zPos)).getBlock();

                            if(block == Blocks.dirt || block == Blocks.clay || block == MekanismBlocks.SaltBlock)
                            {
                                world.setBlockState(pos, blockGen.getDefaultState(), 2);
                            }
                        }
                    }
                }
            }

            return true;
        }
    }
}