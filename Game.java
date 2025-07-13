import java.net.*;
import java.util.*;
import java.io.*;

public class Game
{
	public static final String CURRENT_VERSION = "JRR-v1.0.2-B";
	
	public static ServerSocket socket;
	
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
	
	public static synchronized ServerSocket getServerSocket()
	{
		return(socket);
	}
	
	public static synchronized void addPlayer(Player player)
	{
		players.add(player);
		
		String username = player.getUsername();
		
		broadcast(String.format("%s entrou na partida.\n", username));
		unicast(player, String.format("Saudações, %s!\n", username));
	}
	
	public static synchronized void removePlayer(Player player)
	{
		players.remove(player);
		printWriters.remove(player.getPrintWriter());
	}
	
	public static synchronized void broadcast(String message)
	{
		synchronized(printWriters)
		{
			for(PrintWriter printWriter : printWriters)
			{
				printWriter.println("[Global] " + message);
				printWriter.flush();
			}
		}
		
		System.out.println("[SERVER] " + message);
	}
	
	public static synchronized void unicast(Player player, String message)
	{
		player.getPrintWriter().println("[Local] " + message);
		player.getPrintWriter().flush();
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
				Player player = players.get(round);
				
				addPlayer(player);
				
				while(true)
				{
					String userInput = in.readLine();
					
					if(userInput == null)
					{
						break;
					}
					
					if(player.isDeadOn())
					{
						break;
					}
					
					if("/Pick".equals(userInput))
					{
						synchronized(players)
						{
							boolean canPick = true;
							
							for(Player other : players)
							{
								if(other != player && other.haveRevolverOn())
								{
									canPick = false;
									
									break;
								}
							}
							
							if(canPick)
							{
								player.pickRevolver();
							}
							else
							{
								unicast(player, "Não pôde pegar a arma.");
							}
						}
					}
					else if(userInput.startsWith("/Fire "))
					{
						synchronized(players)
						{
							String target = userInput.substring(6);
							
							if(target == null)
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
					else if(userInput.startsWith("/Pass "))
					{
						synchronized(players)
						{
							String targetUsername = userInput.substring(6);
							Player targetPlayer = null;
							
							for(Player p : players)
							{
								if(p.getUsername().equalsIgnoreCase(targetUsername))
								{
									targetPlayer = p;
									
									break;
								}
							}
							
							if(targetPlayer == null)
							{
								unicast(player, "Parece que o jogador solicitado não existe.");
							}
							else if(!player.haveRevolverOn())
							{
								unicast(player, "Não pôde pegar o revolver.");
							}
							else if(targetPlayer.isDeadOn())
							{
								unicast(player, "O jogador solicitado está morto.");
							}
							else
							{
								player.setRevolverOn(false);
								targetPlayer.pickRevolver();
								Game.broadcast(String.format("%s passou a arma para %s.", player.getUsername(), targetUsername));
							}
						}
					}
					else if(userInput.startsWith("/Send "))
					{
						String arg = userInput.substring(6);
						String msg = "%c[35m%s disse: %c[0m%s";
						
						Game.broadcast(String.format(msg, (char)(27), username, (char)(27), arg));
					}
					else if("/Spin".equals(userInput))
					{
						if(player.haveRevolverOn())
						{
							revolver.spin();
							
							Game.broadcast(String.format("%s acaba de ciclar o tambor.", username));
						}
						else
						{
							Game.unicast(player, "Não pôde ciclar o tambor.");
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
							Game.unicast(player, "Não pôde trapacear agora.");
						}
					}
					else if("/Drop".equals(userInput))
					{
						if(player.haveRevolverOn())
						{
							player.setRevolverOn(false);
							Game.broadcast(player.getUsername() + " largou a arma.");
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
						unicast(player, "/Pick pega a arma quando estiver disponível.");
						unicast(player, "/Pass <substitua_isto_pelo_nome_de_alguém> passa a arma para a pessoa com o nome.");
						unicast(player, "/Fire <substitua_isto_pelo_nome_de_alguém> atira na pessoa com o nome.");
						unicast(player, "/Spin cicle o revólver para uma câmara aleatória.");
						unicast(player, "/Cheat espia a próxima câmara depois da atual.");
						unicast(player, "/Drop larga a arma se tiver.");
					}
					else
					{
						Game.unicast(player, "Comando inválido, digite /Help ou /? para obter a lista de comandos.");
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
					for(Player player : players)
					{
						unicast(player, "Você foi desconectado.");
					}
					
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

class Revolver
{
	private Random random = new Random();
	
	private int maxCheats = 3;
	private int currentBullet;
	private int bulletAmount;
	private boolean[] bullets = new boolean[6];
	
	public Revolver(int max)
	{
		Arrays.fill(bullets, false);
		
		for(int i = 0; i < max; i++)
		{
			bullets[random.nextInt(bullets.length)] = true;
			
			bulletAmount++;
		}
	}
	
	public synchronized void decMaxCheats(Player player)
	{
		this.maxCheats--;
		
		Game.unicast(player, "Restam " + this.maxCheats + " trapaças");
	}
	
	public synchronized int getMaxCheats()
	{
		return(maxCheats);
	}
	
	public synchronized boolean cheatBullet(Player player)
	{
		int position = (currentBullet + 1) % bullets.length;
		
		if(player.haveRevolverOn())
		{
			Game.unicast(player, "Você secretamente espiou a cápusla seguinte à atual.");
			Game.unicast(player, "E a câmara espiada está " + (bullets[position] ? "pronta." : "vazía."));
			player.setRevolverOn(false);
			this.decMaxCheats(player);
			Game.broadcast(player.getUsername() + " largou a arma.");
			
			return(bullets[position]);
		}
		else
		{
			Game.unicast(player, "Você está sem a arma.");
		}
		
		return(false);
	}
	
	public synchronized boolean fire(Player player, Player target)
	{
		String playerUsername = player.getUsername();
		String targetUsername = target.getUsername();
		
		boolean fired = bullets[currentBullet];
		
		if(bulletAmount <= 0)
		{
			Game.broadcast("Sem balas, recarregando.");
			
			for(int i = 0; i < 2; i++)
			{
				bullets[random.nextInt(bullets.length)] = true;
				
				bulletAmount++;
			}
			
			player.setRevolverOn(false);
			
			return(false);
		}
		
		if(player.haveRevolverOn())
		{
			if(bullets[currentBullet])
			{
				target.kill();
				player.setRevolverOn(false);
				
				this.bulletAmount--;
				
				Game.broadcast("Restam " + bulletAmount + " balas.");
			}
			else
			{
				if(player != target)
				{
					Game.broadcast(String.format("%s mirou em %s mas não conseguiu atirar.", playerUsername, targetUsername));
					player.setRevolverOn(false);
					target.setRevolverOn(true);
				}
				else
				{
					Game.broadcast(String.format("%s mirou em sí mesmo mas não conseguiu atirar.", playerUsername));
					player.setRevolverOn(false);
				}
			}
		}
		else
		{
			Game.unicast(player, "Você está sem a arma.");
		}
		
		currentBullet = (currentBullet + 1) % bullets.length;
		
		return(fired);
	}
	
	public synchronized void spin()
	{
		currentBullet = random.nextInt(bullets.length);
	}
}

class Player
{
	private String username;
	private Socket socket;
	private PrintWriter out;
	
	private boolean haveRevolver = false;
	private boolean isDead = false;
	
	public Player(String username, Socket socket) throws IOException
	{
		this.username = username;
		this.socket = socket;
		this.out = new PrintWriter(socket.getOutputStream(), true);
	}
	
	public synchronized String getUsername()
	{
		return(this.username);
	}
	
	public synchronized PrintWriter getPrintWriter()
	{
		return(this.out);
	}
	
	public synchronized void kill()
	{
		if(isDead)
		{
			Game.unicast(this, "Você está morto.");
			
			return;
		}
		
		isDead = true;
		
		Game.broadcast(String.format("%s levou um tiro e morreu.", this.username));
		Game.unicast(this, "Você levou um tiro e morreu.");
		Game.removePlayer(this);
		
		try
		{
			socket.close();
		}
		catch(IOException exception)
		{
			// Faz nada.
		}
		
		haveRevolver = false;
	}
	
	public synchronized boolean isDeadOn()
	{
		return(isDead);
	}
	
	public synchronized void pickRevolver()
	{
		if(isDead)
		{
			Game.unicast(this, "Você está morto.");
			
			return;
		}
		
		if(haveRevolver)
		{
			Game.unicast(this, "Você já está com a arma.");
			
			return;
		}
		else
		{
			haveRevolver = true;
			
			Game.broadcast(String.format("%s pegou a arma.", this.username));
			Game.unicast(this, "Você pegou a arma.");
		}
	}
	
	public synchronized boolean haveRevolverOn()
	{
		return(haveRevolver);
	}
	
	public synchronized void setRevolverOn(boolean value)
	{
		this.haveRevolver = value;
	}
}

