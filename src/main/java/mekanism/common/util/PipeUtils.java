package mekanism.common.util;

import java.util.Arrays;

import mekanism.api.Coord4D;
import mekanism.api.transmitters.IGridTransmitter;
import mekanism.api.transmitters.TransmissionType;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public final class PipeUtils
{
	public static final FluidTankInfo[] EMPTY = new FluidTankInfo[] {};

	/**
	 * Gets all the pipes around a tile entity.
	 * @param tileEntity - center tile entity
	 * @return array of TileEntities
	 */
	public static TileEntity[] getConnectedPipes(TileEntity tileEntity)
	{
		TileEntity[] pipes = new TileEntity[] {null, null, null, null, null, null};

		for(EnumFacing orientation : EnumFacing.values())
		{
			TileEntity pipe = Coord4D.get(tileEntity).offset(orientation).getTileEntity(tileEntity.getWorldObj());

			if(TransmissionType.checkTransmissionType(pipe, TransmissionType.FLUID))
			{
				pipes[orientation.ordinal()] = pipe;
			}
		}

		return pipes;
	}

	/**
	 * Gets all the adjacent connections to a TileEntity.
	 * @param tileEntity - center TileEntity
	 * @return boolean[] of adjacent connections
	 */
	public static boolean[] getConnections(TileEntity tileEntity)
	{
		boolean[] connectable = new boolean[] {false, false, false, false, false, false};

		TileEntity[] connectedPipes = PipeUtils.getConnectedPipes(tileEntity);
		IFluidHandler[] connectedAcceptors = PipeUtils.getConnectedAcceptors(tileEntity);

		for(IFluidHandler container : connectedAcceptors)
		{
			if(container != null)
			{
				int side = Arrays.asList(connectedAcceptors).indexOf(container);

				FluidTankInfo[] infoArray = container.getTankInfo(EnumFacing.getFront(side).getOpposite());

				if(infoArray != null && infoArray.length > 0)
				{
					boolean notNull = false;

					for(FluidTankInfo info : container.getTankInfo(EnumFacing.getFront(side).getOpposite()))
					{
						if(info != null)
						{
							notNull = true;
							break;
						}
					}

					if(notNull)
					{
						connectable[side] = true;
					}
				}
				else if(container.canDrain(EnumFacing.getFront(side).getOpposite(), FluidRegistry.WATER)
						|| container.canFill(EnumFacing.getFront(side).getOpposite(), FluidRegistry.WATER)) //I hesitate to pass null to these.
				{
					connectable[side] = true;
				}
			}
		}

		for(TileEntity tile : connectedPipes)
		{
			if(tile != null)
			{
				int side = Arrays.asList(connectedPipes).indexOf(tile);

				connectable[side] = true;
			}
		}

		return connectable;
	}

	/**
	 * Gets all the acceptors around a tile entity.
	 * @param tileEntity - center tile entity
	 * @return array of IFluidHandlers
	 */
	public static IFluidHandler[] getConnectedAcceptors(TileEntity tileEntity)
	{
		IFluidHandler[] acceptors = new IFluidHandler[] {null, null, null, null, null, null};

		for(EnumFacing orientation : EnumFacing.values())
		{
			TileEntity acceptor = Coord4D.get(tileEntity).offset(orientation).getTileEntity(tileEntity.getWorldObj());

			if(acceptor instanceof IFluidHandler && !(acceptor instanceof IGridTransmitter))
			{
				acceptors[orientation.ordinal()] = (IFluidHandler)acceptor;
			}
		}

		return acceptors;
	}
}
