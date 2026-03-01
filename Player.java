import java.net.*;
import java.util.*;
import java.io.*;

public class Player
{
	private String username;
	private Socket socket;
	private PrintWriter out;
	
	private boolean haveRevolver = false;
	private boolean isDead = false;
	private boolean started = false;
	
	public Player(String username, Socket socket) throws IOException
	{
		this.username = username.trim();
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
			Game.unicast(this, "Você já está morto.");
			
			return;
		}
		
		isDead = true;
		
		Game.broadcast(String.format("%s levou um tiro e morreu.", username));
		Game.unicast(this, "Você levou um tiro e morreu.");
		Game.unicast(this, "Você foi desconectado.");
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
	
	public synchronized boolean isStarted()
	{
		return(started);
	}
	
	public synchronized Socket getSocket()
	{
		return(socket);
	}
	
	public synchronized void dropRevolver()
	{
		if(isDead)
		{
			Game.unicast(this, "Você está morto.");
			
			return;
		}
		
		if(!haveRevolver)
		{
			Game.unicast(this, "Você não possui a arma.");
			
			return;
		}
		else
		{
			haveRevolver = false;
			
			Game.broadcast(String.format("%s largou a arma.", username));
		}
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
			
			Game.broadcast(String.format("%s pegou a arma.", username));
		}
	}
	
	public synchronized void setStarted(boolean value)
	{
		started = value;
	}
	
	public synchronized boolean haveRevolverOn()
	{
		return(haveRevolver);
	}
}
