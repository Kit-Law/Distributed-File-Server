import logger.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client extends ClientBackend
{
	//java Client cport timeout
	public static void main(String[] args)
	{
		new Client(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
	}
	
	private Client(final int cport, final int timeout)
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
						if (command.length == 2) store(command[1]);
						break;
					case "LOAD":
						if (command.length == 2) load(command[1]);
						break;
					case "REMOVE":
						if (command.length == 2) remove(command[1]);
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
