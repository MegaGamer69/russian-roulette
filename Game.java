import java.net.*;
import java.util.*;
import java.io.*;

public class Game
{
	public static ServerSocket socket;
	
	private static Player[] players = new Player[4];
	private static List<PrintWriter> printWriters = new ArrayList<PrintWriter>();
	
	public static void main(String[] args)
	{
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
	
	public static void addPlayer(Player player)
	{
		for(int i = 0; i < players.length; i++)
		{
			if(players[i] != null)
			{
				continue;
			}
			else
			{
				players[i] = player;
				
				String username = player.getUsername();
				
				broadcast(String.format("%s acaba de entrar na partida.\n", username));
				unicast(player, String.format("Saudações, %s!\n", username));
				
				break;
			}
		}
	}
	
	public static synchronized void broadcast(String message)
	{
		for(PrintWriter printWriter : printWriters)
		{
			printWriter.println("[BROAD] " + message);
			printWriter.flush();
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
					
					if("/PickRevolver".equals(userInput))
					{
						for(Player other : players)
						{
							if(other == null)
							{
								continue;
							}
							
							if(other != player && other.haveRevolverOn())
							{
								unicast(player, "Você não pode pegar o revolver.");
								
								break;
							}
							
							player.pickRevolver();
							
							break;
						}
					}
					else if("/FireRevolver".equals(userInput))
					{
						Revolver revolver = new Revolver();
						
						revolver.fire(player);
					}
					else if(userInput.startsWith("/Send "))
					{
						Game.broadcast((char)(27) + "[35m" + username + " disse: " + (char)(27) + "[0m " + userInput.substring(6));
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
	
	private int currentBullet;
	private boolean[] bullets = new boolean[8];
	
	public Revolver()
	{
		bullets[random.nextInt(bullets.length)] = true;
	}
	
	public synchronized boolean fire(Player player)
	{
		boolean fired = bullets[0];
		
		if(player.haveRevolverOn())
		{
			if(bullets[currentBullet])
			{
				player.kill();
				
				bullets[currentBullet] = false;
			}
			else
			{
				Game.broadcast(String.format("O revolver não atirou."));
				player.setRevolverOn(false);
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
	
	public void kill()
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
		}
		else
		{
			Game.unicast(this, "Você não está com o revolver.");
		}
		
		haveRevolver = false;
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
			Game.unicast(this, "Você já está com o revolver.");
			
			return;
		}
		else
		{
			haveRevolver = true;
			
			Game.broadcast(String.format("%s pegou o revolver.", this.username));
			Game.unicast(this, "Você pegou o revolver.");
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

