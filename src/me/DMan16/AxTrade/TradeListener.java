package me.DMan16.AxTrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.Aldreda.AxUtils.Classes.Listener;
import me.Aldreda.AxUtils.Utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class TradeListener implements CommandExecutor,TabCompleter {
	private HashMap<Player,List<Player>> players;
	private HashMap<Long,TradeRequest> requests;
	private int autoCancelTime = 16; // Seconds
	private Set<Player> trading;
	
	public TradeListener() {
		PluginCommand command = AxTrade.getInstance().getCommand("trade");
		command.setExecutor(this);
		players = new HashMap<Player,List<Player>>();
		requests = new HashMap<Long,TradeRequest>();
		trading = new HashSet<Player>();
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) return true;
		if (args.length < 1) return true;
		Player player1 = (Player) sender;
		if (args.length >= 4 && args[1].equals("request") && args[2].equals("ID")) {
			if (args[0].equals("accept")) {
				try {
					long id = Long.parseLong(args[3]);
					if (requests.containsKey(id)) {
						requests.get(id).accept();
						return false;
					}
				} catch (Exception e) {}
			} else if (args[0].equals("deny")) {
				try {
					long id = Long.parseLong(args[3]);
					if (requests.containsKey(id)) {
						requests.get(id).deny();
						return false;
					}
				} catch (Exception e) {}
			}
		}
		Player player2 = Bukkit.getPlayer(args[0]);
		if (player2 != null) {
			if (players.containsKey(player2) && players.get(player2).contains(player1)) for (TradeRequest request : requests.values())
				if (request.player1.equals(player2) && request.player2.equals(player1)) {
					request.accept();
					break;
			} else new TradeRequest(player1,player2);
		}
		return true;
	}
	
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length > 1) return null;
		List<String> resultList = new ArrayList<String>();
		if (args.length == 1) for (Player player : Bukkit.getOnlinePlayers()) if (contains(args[0],player.getName())) resultList.add(player.getName());
		return resultList;
	}
	
	private void removeTrade(long id) {
		TradeRequest request = requests.get(id);
		if (request == null) return;
		players.get(request.player1).remove(request.player2);
		if (players.get(request.player1).isEmpty()) players.remove(request.player1);
		requests.remove(id);
	}
	
	void addTrading(Player player) {
		trading.add(player);
	}
	
	void removeTrading(Player player) {
		trading.remove(player);
	}
	
	private boolean contains(String arg1, String arg2) {
		return (arg1 == null || arg1.isEmpty() || arg2.toLowerCase().contains(arg1.toLowerCase()));
	}
	
	private class TradeRequest extends Listener {
		private final long id;
		private final Player player1;
		private final Player player2;
		private BukkitTask cancelTask;
		
		private TradeRequest(Player player1, Player player2) {
			this.player1 = player1;
			this.player2 = player2;
			this.id = Utils.newSessionID();
			if (!allowTrade()) return;
			if (!players.containsKey(player1)) players.put(player1, new ArrayList<Player>());
			players.get(player1).add(player2);
			requests.put(this.id,this);
			player2.sendMessage(Component.translatable("trade.aldreda.trade_request",NamedTextColor.GREEN).args(player1.displayName()).append(Component.text(" [").append(
					Component.translatable("trade.aldreda.accept")).append(Component.text("]")).color(NamedTextColor.GREEN).clickEvent(
							ClickEvent.runCommand("trade accept request ID " + id))).append(Component.text(" [").append(
					Component.translatable("trade.aldreda.deny")).append(Component.text("]")).color(NamedTextColor.RED).clickEvent(ClickEvent.runCommand(
							"trade deny request ID " + id))));
			cancelTask = new BukkitRunnable() {
				public void run() {
					removeTrade(id);
				}
			}.runTaskLater(AxTrade.getInstance(),autoCancelTime * 20);
		}
		
		private boolean allowTrade() {
			boolean allowTrade = true;
			if (players.containsKey(player1)) allowTrade = allowTrade && !players.get(player1).contains(player2);
			
			return allowTrade;
		}
		
		private void accept() {
			if (trading.contains(player1) || trading.contains(player2)) return;
			cancelTask.cancel();
			addTrading(player1);
			addTrading(player2);
			removeTrade(id);
			player1.sendMessage(Component.translatable("trade.aldreda.trade_accepted",NamedTextColor.GREEN).args(player2.displayName()));
			new BukkitRunnable() {
				public void run() {
					new Trade(player1,player2);
				}
			}.runTaskLater(AxTrade.getInstance(),1 * 20);
		}
		
		private void deny() {
			cancelTask.cancel();
			removeTrade(id);
			player1.sendMessage(Component.translatable("trade.aldreda.trade_denied",NamedTextColor.RED).args(player2.displayName()));
		}
	}
}