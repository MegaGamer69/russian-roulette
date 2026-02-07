import java.net.*;
import java.util.*;
import java.io.*;
import org.json.*;

public class GameClient
{
	public static String ipAddress = "";
	
	public static class UpdateManager
	{
		public static final String CURRENT_VERSION = "JRR-v1.0.3";
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
					System.err.println("Não pôde obter a atualização, código: " + connectionCode);
					
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
					System.out.println("// OKAY: Seu jogo está atualizado!.");
				}
				else
				{
					if(versionSufix.equals("-B"))
					{
						System.out.println("// ATENÇÃO: Nova versão beta: " + latestVersion + ".");
					}
					else
					{
						System.out.println("// ATENÇÃO: Nova versão estável: " + latestVersion + ".");
					}
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
		
		if(UpdateManager.CURRENT_VERSION.equals(Game.CURRENT_VERSION))
		{
			// Faça nada.
		}
		else
		{
			return;
		}
		
		System.out.println("Saudações! Versão: " + UpdateManager.CURRENT_VERSION);
		pickIPAddress();
		System.out.print("Escreva seu nome de usuário: ");
		
		try
		{
			Socket socket = new Socket(ipAddress, 8080);
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
	
	private static void pickIPAddress()
	{
		try
		{
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			
			while(interfaces.hasMoreElements())
			{
				NetworkInterface nwInterface = interfaces.nextElement();
				
				if(nwInterface.isLoopback() || !nwInterface.isUp())
				{
					continue;
				}
				
				Enumeration<InetAddress> addresses = nwInterface.getInetAddresses();
				
				while(addresses.hasMoreElements())
				{
					InetAddress address = addresses.nextElement();
					
					if(address instanceof java.net.Inet4Address && !address.isLoopbackAddress())
					{
						String ip = address.getHostAddress();
						
						if(ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172."))
						{
							ipAddress = ip;
							
							System.out.println(String.format("Endereço LAN de conexão detectado (IPv4: %s)!", ipAddress));
							
							return;
						}
					}
				}
			}
		}
		catch(SocketException exception)
		{
			exception.printStackTrace();
		}
	}
}
