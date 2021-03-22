import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Controller
{
	private static int cport = -1;
	private static int R = -1;
	private static int timeout = -1;
	
	//java Controller cport R timeout
	public static void main(String args[])
	{
		cport = Integer.parseInt(args[0]);
		R = Integer.parseInt(args[1]);
		timeout = Integer.parseInt(args[2]);
		
		try
		{
			Selector selector = Selector.open();
			ServerSocketChannel serverSocket = ServerSocketChannel.open();
			serverSocket.bind(new InetSocketAddress("localhost", cport));
			serverSocket.configureBlocking(false);
			serverSocket.register(selector, SelectionKey.OP_ACCEPT);
			ByteBuffer buffer = ByteBuffer.allocate(256);
			
			while (true)
			{
				try
				{
					selector.select();
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> iter = selectedKeys.iterator();
					while (iter.hasNext())
					{
						SelectionKey key = iter.next();
						
						if (key.isAcceptable())
						{
					
						}
						
						if (key.isReadable())
						{
				
						}
						
						if (key.isWritable())
						{
						
						}
						
						iter.remove();
					}
				}
				catch (ClosedChannelException e)
				{
					e.printStackTrace();
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}