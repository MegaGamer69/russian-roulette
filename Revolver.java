import java.util.*;

public class Revolver
{
	private Random random = new Random();
	
	private int maxCheats = 5;
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
		
		Game.unicast(player, "Restam " + this.maxCheats + " trapaças globais.");
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
			Game.unicast(player, "Você secretamente espiou a cápusla seguinte à próxima.");
			Game.unicast(player, "E a câmara espiada está " + (bullets[position] ? "pronta." : "vazia."));
			this.decMaxCheats(player);
			Game.nextRound();
			
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
			
			Game.nextRound();
			
			return(false);
		}
		
		if(player.haveRevolverOn() && !target.isDeadOn())
		{
			if(bullets[currentBullet])
			{
				target.kill();
				
				this.bulletAmount--;
				
				Game.broadcast("Restam " + bulletAmount + " balas.");
			}
			else
			{
				if(player != target)
				{
					Game.broadcast(String.format("%s mirou em %s mas não conseguiu atirar.", playerUsername, targetUsername));
				}
				else
				{
					Game.broadcast(String.format("%s mirou em sí mesmo mas não conseguiu atirar.", playerUsername));
				}
			}
			
			Game.nextRound();
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
		
		Game.nextRound();
	}
}
