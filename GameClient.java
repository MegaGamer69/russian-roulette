import java.net.*;
import java.util.*;
import java.io.*;
import org.json.*;

public class GameClient
{
	public static class UpdateManager
	{
		public static final String CURRENT_VERSION = "JRR-v1.0.1-T";
		public static final String REPO_URL = "https://api.github.com/repos/MegaGamer69/russian-roulette/releases/latest";
		
		public static void checkByUpdates()
		{
			try
			{
				URL url = new URI(REPO_URL).toURL();
				HttpURLConnection connection = (HttpURLConnection)(url.openConnection());
				
				connection.setRequestMethod("GET");
				connection.setRequestProperty("Accept", "application/json");
				
				int connectionCode = connection.getResponseCode();
				
				if(connectionCode != 200)
				{
					System.err.println("Não foi possível obter a nova versão, código: " + connectionCode);
					
					return;
				}
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder response = new StringBuilder();
				String line;
				
				while((line = reader.readLine()) != null)
				{
					response.append(line);
				}
				
				String json = response.toString();
				String latestVersion = json.split("\"tag_name\":\"")[1].split("\"")[0];
				String versionSufix = latestVersion.substring(7);
				
				if(CURRENT_VERSION.equals(latestVersion))
				{
					System.out.println("// ATENÇÃO: Versão atualizada.");
				}
				else if(versionSufix.equals("-B"))
				{
					System.out.println("// ATENÇÃO: Nova beta encontrada: " + latestVersion + ".");
				}
				else if(versionSufix.equals("-T"))
				{
					System.out.println("// ATENÇÃO: Versão de teste (" + latestVersion + "), ignore.");
				}
				else
				{
					System.out.println("// ATENÇÃO: Versão mais recente estável: " + latestVersion + ".");
				}
				
				reader.close();
			}
			catch(Exception exception)
			{
				exception.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args)
	{
		UpdateManager.checkByUpdates();
		
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
