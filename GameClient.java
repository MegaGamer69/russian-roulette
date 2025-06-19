import java.net.*;
import java.util.*;
import java.io.*;
import org.json.*;

public class GameClient
{
	public static class UpdateManager
	{
		public static final String CURRENT_VERSION = "JRR-v1.0.1-HF1";
		public static final String REPO_URL = "https://github.com/MegaGamer69/russian-roulette/releases/latest/";
		
		public static void checkForUpdates()
		{
			try
			{
				URL url = new URI(REPO_URL).toURL();
				HttpURLConnection connection = (HttpURLConnection)(url.openConnection());
				
				connection.setRequestMethod("GET");
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder response = new StringBuilder();
				String line;
				
				while((line = reader.readLine()) != null)
				{
					response.append(line);
				}
				
				reader.close();
				
				String json = response.toString();
				String latestVersion = json.split("\"tag_name\":\"")[1].split("\"")[0];
				
				if(isNewerVersion(latestVersion, CURRENT_VERSION))
				{
					System.out.println("Caro jogador, encontramos uma atualização disponível.");
					System.out.printf("Versão atual: %s, Versão nova: %s", CURRENT_VERSION, latestVersion);
				}
			}
			catch(Exception exception)
			{
				exception.printStackTrace();
			}
		}
		
		private static boolean isNewerVersion(String newVersion, String currentVersion)
		{
			String[] newParts = newVersion.split("\\.");
			String[] currentParts = currentVersion.split("\\.");
			
			for(int i = 0; i < Math.min(newParts.length, currentParts.length); i++)
			{
				int newPart = Integer.parseInt(newParts[i]);
				int currentPart = Integer.parseInt(currentParts[i]);
				
				if(newPart > currentPart)
				{
					return(true);
				}
				
				if(newPart < currentPart)
				{
					return(false);
				}
			}
			
			return(newParts.length > currentParts.length);
		}
	}
	
	public static void main(String[] args)
	{
		System.out.println("Boas vindas! Versão do jogo: " + UpdateManager.CURRENT_VERSION);
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
