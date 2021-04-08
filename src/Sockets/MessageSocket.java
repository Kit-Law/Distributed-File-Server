package Sockets;

import logger.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MessageSocket
{
	public static void sendMessage(final int opcode, final String msg, SocketChannel socket) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(64);
		
		buffer.put((opcode + " " + msg).getBytes());
		buffer.flip();
		int bytesWritten = socket.write(buffer);
		
		Logger.info.log("Sent opcode: " + opcode + ", With message: \"" + msg + "\", " + bytesWritten + " bytes to: " + socket + ".");
	}
	
	public static String receiveMessage(SocketChannel socket) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(64);
		
		socket.read(buffer);
		String response = new String(buffer.array()).trim();
		Logger.info.log("Received response: \"" + response + "\" from socket: " + socket);
		
		return response;
	}
	
	public static int getOpcode(final String msg)
	{
		return Integer.parseInt(msg.substring(0, msg.indexOf(' ')));
	}
	
	public static String getOperand(final String msg)
	{
		return msg.substring(msg.indexOf(' ') + 1);
	}
}
