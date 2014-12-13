package mekanism.common.network;

import java.util.Random;

import mekanism.api.Coord4D;
import mekanism.common.PacketHandler;
import mekanism.common.network.PacketPortalFX.PortalFXMessage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import io.netty.buffer.ByteBuf;

public class PacketPortalFX implements IMessageHandler<PortalFXMessage, IMessage>
{
	@Override
	public IMessage onMessage(PortalFXMessage message, MessageContext context) 
	{
		EntityPlayer player = PacketHandler.getPlayer(context);
		
		Random random = new Random();

		for(int i = 0; i < 50; i++)
		{
			player.worldObj.spawnParticle("portal", message.coord4D.getPos().getX() + random.nextFloat(), message.coord4D.getPos().getY() + random.nextFloat(), message.coord4D.getPos().getZ() + random.nextFloat(), 0.0F, 0.0F, 0.0F);
			player.worldObj.spawnParticle("portal", message.coord4D.getPos().getX() + random.nextFloat(), message.coord4D.getPos().getY() + 1 + random.nextFloat(), message.coord4D.getPos().getZ() + random.nextFloat(), 0.0F, 0.0F, 0.0F);
		}
		
		return null;
	}
	
	public static class PortalFXMessage implements IMessage
	{
		public Coord4D coord4D;
	
		public PortalFXMessage() {}
		
		public PortalFXMessage(Coord4D coord)
		{
			coord4D = coord;
		}
	
		@Override
		public void toBytes(ByteBuf dataStream)
		{
			dataStream.writeInt(coord4D.getPos().getX());
			dataStream.writeInt(coord4D.getPos().getY());
			dataStream.writeInt(coord4D.getPos().getZ());
			dataStream.writeInt(coord4D.dimensionId);
		}
	
		@Override
		public void fromBytes(ByteBuf dataStream)
		{
			coord4D = new Coord4D(dataStream.readInt(), dataStream.readInt(), dataStream.readInt(), dataStream.readInt());
		}
	}
}
