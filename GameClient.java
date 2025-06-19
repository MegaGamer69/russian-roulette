import java.net.*;
import java.util.*;
import java.io.*;

public class GameClient
{
	public static void main(String[] args)
	{
		System.out.println("Boas vindas!");
		System.out.print("Primeiramente, nós precisamos saber o seu apelido: ");
		
		try
		{
			Socket socket = new Socket("localhost", 8080);
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			
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
		catch(IOException exception)
		{
			exception.printStackTrace();
		}
	}
}
