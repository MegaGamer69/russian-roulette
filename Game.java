import java.net.*;
import java.util.*;
import java.io.*;

public class Game
{
	public static ServerSocket socket;
	
	private static Revolver revolver = new Revolver(2);
	private static List<Player> players;
	private static List<PrintWriter> printWriters;
	
	public static void main(String[] args)
	{
		players = Collections.synchronizedList(new ArrayList<Player>());
		printWriters = Collections.synchronizedList(new ArrayList<PrintWriter>());
		
		try
		{
			socket = new ServerSocket(8080);
			
			System.out.println("Inicializando servidor na porta 8080...");
			
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
	
	public static synchronized void addPlayer(Player player)
	{
		players.add(player);
		
		String username = player.getUsername();
		
		broadcast(String.format("%s acaba de entrar na roleta-russa.\n", username));
		unicast(player, String.format("Saudações, %s!\n", username));
	}
	
	public static synchronized void removePlayer(Player player)
	{
		players.remove(player);
		printWriters.remove(player.getPrintWriter());
	}
	
	public static synchronized void broadcast(String message)
	{
		for(PrintWriter printWriter : printWriters)
		{
			printWriter.println("[BROAD] " + message);
			printWriter.flush();
			System.out.println("[SERVER]" + message);
		}
	}
	
	public static synchronized void unicast(Player player, String message)
	{
		player.getPrintWriter().println("[UNI] " + message);
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
				Player player = new Player(username, socket);
				
				addPlayer(player);
				
				while(true)
				{
					String userInput = in.readLine();
					
					if(userInput == null)
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
								unicast(player, "Você não pode pegar o revólver.");
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
								unicast(player, "Parece que o jogador solicitado não está na partida.");
								
								continue;
							}
							
							if(player.haveRevolverOn())
							{
								for(Player targetPlayer : players)
								{
									if(targetPlayer.getUsername().equalsIgnoreCase(target))
									{
										revolver.fire(player, targetPlayer);
									}
									else
									{
										continue;
									}
								}
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
								unicast(player, "Parece que o jogador solicitado não está na partida.");
							}
							else if(!player.haveRevolverOn())
							{
								unicast(player, "Você não está com o revólver.");
							}
							else if(targetPlayer.isDeadOn())
							{
								unicast(player, "O alvo solicitado está morto.");
							}
							else
							{
								player.setRevolverOn(false);
								targetPlayer.pickRevolver();
								Game.broadcast(String.format("%s passou o revólver para %s.", player.getUsername(), targetUsername));
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
							
							Game.broadcast("O tambor foi girado, se preparem.");
						}
						else
						{
							Game.unicast(player, "Não pode girar o tambor agora.");
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
							Game.unicast(player, "Não pode girar o tambor agora.");
						}
					}
				}
			}
			catch(IOException exception)
			{
				System.out.println("Jogador desconectado");
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

class Revolver
{
	private Random random = new Random();
	
	private int maxCheats = 4;
	private int currentBullet;
	private boolean[] bullets = new boolean[6];
	
	public Revolver(int max)
	{
		Arrays.fill(bullets, false);
		
		for(int i = 0; i < max; i++)
		{
			bullets[random.nextInt(bullets.length)] = true;
		}
	}
	
	public synchronized void decMaxCheats(Player player)
	{
		this.maxCheats--;
		
		Game.unicast(player, "Faltam " + this.maxCheats + " trapaceadas");
	}
	
	public synchronized int getMaxCheats()
	{
		return(maxCheats);
	}
	
	public synchronized boolean cheatBullet(Player player)
	{
		int position = (currentBullet + 1) % bullets.length;
		
		Game.unicast(player, "Você abriu secretamente o tambor.");
		Game.unicast(player, "A bala correspondente ao espaço espiado está " + (bullets[position] ? "preparada." : "vazía."));
		player.setRevolverOn(false);
		
		decMaxCheats(player);
		
		return(this.bullets[position]);
	}
	
	public synchronized boolean fire(Player player, Player target)
	{
		String playerUsername = player.getUsername();
		String targetUsername = target.getUsername();
		
		boolean fired = bullets[currentBullet];
		
		if(player.haveRevolverOn())
		{
			if(bullets[currentBullet])
			{
				target.kill();
				target.setRevolverOn(false);
			}
			else
			{
				Game.broadcast(String.format("%s mirou em %s mas o revólver não atirou.", playerUsername, targetUsername));
			}
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
	
	public String getUsername()
	{
		return(this.username);
	}
	
	public PrintWriter getPrintWriter()
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
		
		if(haveRevolver)
		{
			isDead = true;
			
			Game.broadcast(String.format("%s apertou o gatilho e morreu.", this.username));
			Game.unicast(this, "Você apertou o gatilho e morreu.");
			Game.removePlayer(this);
			
			try
			{
				socket.close();
			}
			catch(IOException exception)
			{
				// Faz nada.
			}
		}
		else
		{
			Game.unicast(this, "Você não está com o revólver.");
		}
		
		haveRevolver = false;
	}
	
	public boolean isDeadOn()
	{
		return(isDead);
	}
	
	public void pickRevolver()
	{
		if(isDead)
		{
			Game.unicast(this, "Você está morto.");
			
			return;
		}
		
		if(haveRevolver)
		{
			Game.unicast(this, "Você já está com o revólver.");
			
			return;
		}
		else
		{
			haveRevolver = true;
			
			Game.broadcast(String.format("%s pegou o revólver.", this.username));
			Game.unicast(this, "Você pegou o revólver.");
		}
	}
	
	public boolean haveRevolverOn()
	{
		return(haveRevolver);
	}
	
	public void setRevolverOn(boolean value)
	{
		this.haveRevolver = value;
	}
}

