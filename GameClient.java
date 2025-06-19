import java.net.*;
import java.util.*;
import java.io.*;

public class GameClient
{
	public static void main(String[] args)
	{
		System.out.println("Boas vindas!");
		System.out.print("Primeiramente, nós precisamos saber o seu IP: ");
		
		try
		{
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			String ip = console.readLine();
			Socket socket = new Socket(ip, 8080);
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
			
			new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						String serverMessage;
						
						while((serverMessage = reader.readLine()) != null)
						{
							System.out.println(serverMessage);
						}
					}
					catch(IOException exception)
					{
						exception.printStackTrace();
					}
				}
			}).start();
			
			String input;
			
			while((input = console.readLine()) != null)
			{
				printWriter.println(input);
			}
		}
		catch(UnknownHostException exception)
		{
			exception.printStackTrace();
		}
		catch(IOException exception)
		{
			exception.printStackTrace();
		}
	}
}
