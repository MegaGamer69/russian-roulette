import java.net.*;
import java.util.*;
import java.io.*;

public class Game
{
	public static final String CURRENT_VERSION = "JRR-v1.0.4";
	public static final int MAX_PLAYERS = 5;
	public static final int MIN_PLAYERS = 2;
	
	public static ServerSocket socket;
	
	public static boolean started;
	
	private static int round;
	
	private static Revolver revolver = new Revolver(2);
	private static List<Player> players = Collections.synchronizedList(new ArrayList<Player>());
	private static List<PrintWriter> printWriters = Collections.synchronizedList(new ArrayList<PrintWriter>());
	
	public static void main(String[] args)
	{
		try
		{
			System.out.println("Iniciando servidor na porta 8080...");
			
			socket = new ServerSocket(8080);
			
			System.out.println("Servidor inicializado na porta 8080.");
			System.out.println("Version: " + CURRENT_VERSION);
			
			while(true)
			{
				Socket clientSocket = socket.accept();
				
				new ClientHandler(clientSocket).start();
			}
		}
		catch(IOException exception)
		{
			exception.printStackTrace();
		}
	}
	
	public static synchronized List<Player> getPlayers()
	{
		return(Collections.unmodifiableList(players));
	}
	
	public static synchronized ServerSocket getServerSocket()
	{
		return(socket);
	}
	
	public static synchronized boolean addPlayer(Player player)
	{
		String username = player.getUsername();
		
		if(username.trim().isEmpty())
		{
			try
			{
				player.getPrintWriter().println("[Local] O nome solicitado está vazio. Recusamos a conexão.");
				player.getPrintWriter().flush();
				player.getSocket().close();
			}
			catch(IOException exception)
			{
				// Faça nada.
			}
			
			return(false);
		}
		else
		{
			for(Player other : players)
			{
				if(username.equalsIgnoreCase(other.getUsername()))
				{
					try
					{
						player.getPrintWriter().println("[Local] O nome solicitado já está em uso. Recusamos a conexão.");
						player.getPrintWriter().flush();
						player.getSocket().close();
					}
					catch(IOException exception)	
					{
						// Faça nada.
					}
					
					return(false);
				}
			}
		}
		
		if(players.size() >= MAX_PLAYERS)
		{
			try
			{
				player.getPrintWriter().println(String.format("[Local] Esta partida encontra-se lotada (atual: %d, máximo: %d)!", players.size(), MAX_PLAYERS));
				player.getPrintWriter().flush();
				player.getSocket().close();
			}
			catch(IOException exception)
			{
				// Faça nada (novamente).
			}
			
			return(false);
		}
		
		players.add(player);
		
		broadcast(String.format("%s entrou na partida.\n", username));
		unicast(player, String.format("Saudações, %s!\n", username));
		
		return(true);
	}
	
	public static synchronized void removePlayer(Player player)
	{
		if(!players.contains(player))
		{
			return;
		}
		
		int index = players.indexOf(player);
		
		players.remove(player);
		printWriters.remove(player.getPrintWriter());
		
		if(index < round)
		{
			round--;
		}
		
		if(round >= players.size())
		{
			round = 0;
		}
	}
	
	public static synchronized void nextRound()
	{
		Player player1 = players.get(round);
		
		if(players.size() <= 1)
		{
			broadcast(String.format("O jogador \"%s\" venceu a partida.", player1.getUsername()));
			
			return;
		}
		
		if(player1.haveRevolverOn())
		{
			player1.dropRevolver();
		}
		
		round = (round + 1) % players.size();
		
		players.get(round).pickRevolver();
	}
	
	public static synchronized void chatMessage(Player sender, String message)
	{
		serverUnicast(String.format("%s disse: %s", sender.getUsername(), message));
		
		synchronized(players)
		{
			for(Player player : players)
			{
				if(player.isStarted())
				{
					player.getPrintWriter().printf("[Chat] %s: %s\n", sender.getUsername(), message);
					player.getPrintWriter().flush();
				}
			}
		}
	}
	
	public static synchronized void serverUnicast(String message)
	{
		System.out.println("[SERVER] " + message);
	}
	
	public static synchronized void broadcast(String message)
	{
		synchronized(players)
		{
			for(Player player : players)
			{
				if(player.isStarted())
				{
					player.getPrintWriter().println("[Global] " + message);
					player.getPrintWriter().flush();
				}
			}
		}
		
		serverUnicast(message);
	}
	
	public static synchronized void unicast(Player player, String message)
	{
		if(player.isStarted())
		{
			player.getPrintWriter().println("[Local] " + message);
			player.getPrintWriter().flush();
		}
		else
		{
			return;
		}
	}
	
	public static class ClientHandler extends Thread
	{
		private Socket socket;
		private PrintWriter out;
		
		public ClientHandler(Socket socket)
		{
			this.socket = socket;
		}
		
		public void run()
		{
			try
			{
				out = new PrintWriter(socket.getOutputStream());
				
				printWriters.add(out);
				
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String username = in.readLine();
				
				Player newPlayer = new Player(username, socket);
				
				boolean added = addPlayer(newPlayer);
				
				if(!added)
				{
					printWriters.remove(out);
					
					try
					{
						socket.close();
					}
					catch(IOException exception)
					{
						// Faça nada.
					}
					
					return;
				}
				
				Player player = newPlayer;
				
				player.setStarted(true);
				
				unicast(player, "Esperando por jogadores...");
				
				while(true)
				{
					String userInput = in.readLine();
					
					if(userInput == null)
					{
						Game.unicast(player, "Sua entrada está vazia.");
						
						continue;
					}
					
					if(!started)
					{
						if(userInput.startsWith("/Send "))
						{
							String arg = userInput.substring(6);
							
							chatMessage(player, arg);
						}
						else if("/Start".equals(userInput))
						{
							if(players.size() >= MIN_PLAYERS)
							{
								broadcast("Partida inicializada!");
								Collections.shuffle(players);
								
								started = true;
								
								nextRound();
							}
							else
							{
								unicast(player, "Você está sozinho nesta partida...");
							}
						}
						else if("/Help".equals(userInput) || "/?".equals(userInput))
						{
							unicast(player, "Comandos:");
							unicast(player, "/Help ou /? mostra esta lista.");
							unicast(player, "/Start inicializa a partida.");
						}
						else
						{
							unicast(player, "Comando inválido, digite /Help ou /? para obter a lista de comandos.");
						}
					}
					else
					{
						if(userInput == null)
						{
							break;
						}
						
						if(player.isDeadOn())
						{
							break;
						}
						
						if(userInput.startsWith("/Fire "))
						{
							synchronized(players)
							{
								String target = userInput.substring(6);
								
								if(target == null || target.trim().isEmpty())
								{
									unicast(player, "Parece que o jogador solicitado não existe.");
									
									continue;
								}
								
								if(player.haveRevolverOn())
								{
									List<Player> localPlayers = new ArrayList<Player>(players);
									
									for(Player targetPlayer : localPlayers)
									{
										if(targetPlayer != null && targetPlayer.getUsername().equalsIgnoreCase(target))
										{
											revolver.fire(player, targetPlayer);
										}
										else
										{
											continue;
										}
									}
								}
								else
								{
									unicast(player, "Não pôde pegar a arma.");
								}
							}
						}
						else if(userInput.startsWith("/Send "))
						{
							String arg = userInput.substring(6);
							
							chatMessage(player, arg);
						}
						else if("/Spin".equals(userInput))
						{
							if(player.haveRevolverOn())
							{
								broadcast(String.format("%s acaba de ciclar o tambor.", username));
								revolver.spin();
							}
							else
							{
								unicast(player, "Não pôde ciclar o tambor, você não está com a arma.");
							}
						}
						else if("/Cheat".equals(userInput))
						{
							if(player.haveRevolverOn())
							{
								if(revolver.getMaxCheats() > 0)
								{
									revolver.cheatBullet(player);
								}
							}
							else
							{
								unicast(player, "Não pôde trapacear agora, você não está com a arma.");
							}
						}
						else if("/Pass".equals(userInput))
						{
							if(player.haveRevolverOn())
							{
								broadcast(player.getUsername() + " largou a arma.");
								nextRound();
							}
							else
							{
								unicast(player, "Não pode pegar a arma.");
							}
						}
						else if("/Help".equals(userInput) || "/?".equals(userInput))
						{
							unicast(player, "Comandos:");
							unicast(player, "/Help ou /? mostra esta lista.");
							unicast(player, "/Fire <substitua_isto_pelo_nome_de_alguém> atira no jogador com o nome solicitado (caso haja a existência desse nome).");
							unicast(player, "/Spin cicle o revólver para uma câmara aleatória.");
							unicast(player, "/Cheat espia a próxima câmara em relação a atual.");
							unicast(player, "/Pass larga a arma se tiver.");
						}
						else
						{
							unicast(player, "Comando inválido, digite /Help ou /? para obter a lista de comandos.");
						}
					}
				}
			}
			catch(IOException exception)
			{
				System.out.println("Jogador desconectado.");
			}
			finally
			{
				printWriters.remove(out);
				
				try
				{
					socket.close();
				}
				catch(IOException exception)
				{
					// Faça nada.
				}
			}
		}
	}
}
