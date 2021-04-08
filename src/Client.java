import logger.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client extends ClientBackend
{
	//java Client cport timeout
	public static void main(String args[])
	{
		new Client(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
	}
	
	private Client(int cport, int timeout)
	{
		super(cport, timeout);
		
		pollUser();
	}
	
	private void pollUser()
	{
		while (true)
		{
			try
			{
				System.out.print("<Client> ");
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String[] command = br.readLine().split(" ");
				
				switch (command[0])
				{
					case "STORE":
						store(command[1]);
						break;
					case "LOAD":
						load(command[1]);
						break;
					case "REMOVE":
						remove(command[1]);
						break;
					case "--help":
						Logger.info.log("Usage: STORE filename");
						Logger.info.log("       LOAD filename");
						Logger.info.log("       REMOVE filename");
						break;
					case "EXIT":
						System.exit(0);
					default:
						Logger.err.log("Parsing the command. Try --help for usage.");
				}
			}
			catch (IOException e) { Logger.err.log(e.getMessage()); }
		}
	}
}
