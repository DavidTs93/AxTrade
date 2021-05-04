package me.DMan16.AxTrade;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.Aldreda.AxUtils.AxUtils;
import me.Aldreda.AxUtils.Classes.Listener;
import me.Aldreda.AxUtils.Utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Trade extends Listener {
	private static final String translateTrade = "trade.aldreda.trade";
	private static final String translateWait = "trade.aldreda.trade_wait";
	private static final String translateCancelled = "trade.aldreda.trade_cancelled";
	private static final String translateMoney = "bank.aldreda.money";
	private static final ItemStack money = Utils.makeItem(Material.EMERALD,Component.translatable(translateMoney,
			NamedTextColor.GOLD).decoration(TextDecoration.ITALIC,false),ItemFlag.values());
	private static final ItemStack wait = Utils.makeItem(Material.YELLOW_WOOL,Component.translatable(translateWait,
			NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false),ItemFlag.values());
	private static final ItemStack accept = Utils.makeItem(Material.GREEN_WOOL,Component.translatable(translateTrade,
			NamedTextColor.GREEN).decoration(TextDecoration.ITALIC,false),ItemFlag.values());
	private static final ItemStack accepted = accept.clone();
	private static final ItemStack emptyBlack = Utils.makeItem(Material.BLACK_STAINED_GLASS_PANE,Component.text(" "),ItemFlag.values());
	private static final ItemStack emptyGray = Utils.makeItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE,Component.text(" "),ItemFlag.values());
	
	static {
		accepted.addUnsafeEnchantment(Enchantment.DURABILITY,1);
	}
	
	List<Integer> tradeSlots = Arrays.asList(10,11,12,19,20,21,28,29,30,37,38,39);
	private int closeSlot = 49;
	private int moneySlot1 = 47;
	private int moneySlot2 = 51;
	private int acceptSlot1 = 45;
	private int acceptSlot2 = 53;
	private boolean accept1 = false;
	private boolean accept2 = false;
	private boolean economy = AxUtils.getEconomy() != null;
	private boolean autoAccept = false;
	private boolean done = false;
	
	private final InventoryHolder initiator;
	private final Player responder;
	private final Inventory inventory1;
	private final Inventory inventory2;
	private int acceptStart = 4;
	private int acceptTimer = acceptStart;
	private BukkitTask timer = null;
	private double money1 = 0;
	private double money2 = 0;
	private MoneyListener MoneyListener1 = null;
	private MoneyListener MoneyListener2 = null;
	
	public Trade(InventoryHolder initiator, Player responder) {
		if (initiator == null || responder == null) Objects.requireNonNull(null,"Traders can't be null!");
		this.initiator = initiator;
		this.responder = responder;
		if (this.initiator instanceof Player) {
			AxTrade.getTradeListener().addTrading((Player) initiator);
			autoAccept = Utils.isPlayerNPC((Player) initiator);
		}
		AxTrade.getTradeListener().addTrading(responder);
		inventory1 = Utils.makeInventory(initiator,6,Component.translatable(translateTrade).color(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC,false));
		inventory2 = Utils.makeInventory(responder,6,Component.translatable(translateTrade).color(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC,false));
		for (int i = 0; i < 6 * 9; i++) {
			inventory1.setItem(i,emptyBlack);
			inventory2.setItem(i,emptyBlack);
		}
		for (int i : tradeSlots) {
			inventory1.setItem(i,null);
			inventory1.setItem(i + 4,emptyGray);
			inventory2.setItem(i,null);
			inventory2.setItem(i + 4,emptyGray);
		}
		inventory1.setItem(closeSlot,Utils.makeItem(Material.BARRIER,Component.translatable("spectatorMenu.close",
				NamedTextColor.RED).decoration(TextDecoration.ITALIC,false),ItemFlag.values()));
		inventory2.setItem(closeSlot,Utils.makeItem(Material.BARRIER,Component.translatable("spectatorMenu.close",
				NamedTextColor.RED).decoration(TextDecoration.ITALIC,false),ItemFlag.values()));
		setMoney();
		register(AxTrade.getInstance());
		if ((initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator)) ((Player) initiator).openInventory(inventory1);
		responder.openInventory(inventory2);
	}
	
	private void finish1() {
		unregister();
		 if (MoneyListener1 != null) MoneyListener1.unregister();
		 if (MoneyListener2 != null) MoneyListener2.unregister();
		ItemStack cursor1 = (initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator) ? ((Player) initiator).getItemOnCursor() : null;
		ItemStack cursor2 = responder.getItemOnCursor();
		if (!Utils.isNull(cursor1)) {
			((Player) initiator).setItemOnCursor(null);
			Utils.givePlayer(((Player) initiator),cursor1,false);
		}
		if (!Utils.isNull(cursor2)) {
			responder.setItemOnCursor(null);
			Utils.givePlayer(responder,cursor2,false);
		}
		if ((initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator)) ((Player) initiator).closeInventory();
		responder.closeInventory();
	}
	
	private void finish2() {
		inventory1.clear();
		inventory2.clear();
		if ((initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator)) AxTrade.getTradeListener().removeTrading((Player) initiator);
		AxTrade.getTradeListener().removeTrading(responder);
	}
	
	private void cancel() {
		finish1();
		Component msg = Component.translatable(translateCancelled).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC,false);
		if ((initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator)) ((Player) initiator).sendMessage(msg);
		responder.sendMessage(msg);
		for (int i : tradeSlots) {
			ItemStack item1 = inventory1.getItem(i);
			ItemStack item2 = inventory2.getItem(i);
			
			if (!Utils.isNull(item1) && (initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator)) Utils.givePlayer((Player) initiator,item1,false);
			if (!Utils.isNull(item2)) Utils.givePlayer(responder,item2,false);
		}
		finish2();
	}
	
	private void checkDone() {
		if (autoAccept && done) accept1 = true;
		if (accept1) {
			inventory1.setItem(acceptSlot1,accepted);
			inventory2.setItem(acceptSlot2,accepted);
		}
		if (accept2) {
			inventory1.setItem(acceptSlot2,accepted);
			inventory2.setItem(acceptSlot1,accepted);
		}
		if (!accept1 || !accept2) return;
		finish1();
		double diff = money1 - money2;
		if (diff != 0 && (initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator)) {
			Player init = (Player) initiator;
			Player remove = null;
			Player add = null;
			Utils.broadcast(Component.text("diff = " + diff));
			if (diff > 0) {
				remove = init;
				add = responder;
			} else if (diff < 0) {
				remove = responder;
				add = init;
			}
			diff = Math.abs(diff);
			if (remove != null) if (AxUtils.getEconomy().withdrawBankPlayer(remove,diff).transactionSuccess())
				if (!AxUtils.getEconomy().depositBankPlayer(add,diff).transactionSuccess()) AxUtils.getEconomy().depositBankPlayer(remove,diff);
		} else if (diff > 0) AxUtils.getEconomy().depositBankPlayer(responder,Math.abs(diff)).transactionSuccess();
		else if (diff < 0) AxUtils.getEconomy().withdrawBankPlayer(responder,Math.abs(diff)).transactionSuccess();
		for (int i : tradeSlots) {
			ItemStack item1 = inventory1.getItem(i);
			ItemStack item2 = inventory2.getItem(i);
			if (!Utils.isNull(item2) && (initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator)) Utils.givePlayer((Player) initiator,item2,false);
			if (!Utils.isNull(item1)) Utils.givePlayer(responder,item1,false);
		}
		finish2();
	}
	
	private void updateTrade() {
		boolean empty = true;
		for (int i : tradeSlots) {
			ItemStack item1 = inventory1.getItem(i);
			ItemStack item2 = inventory2.getItem(i);
			empty = empty && Utils.isNull(item1) && Utils.isNull(item2);
			new BukkitRunnable() {
				public void run() {
					inventory1.setItem(i + 4,Utils.isNull(item2) ? emptyGray : item2);
					inventory2.setItem(i + 4,Utils.isNull(item1) ? emptyGray : item1);
				}
			}.runTask(AxTrade.getInstance());
		}
		if (timer != null) {
			timer.cancel();
			acceptTimer = acceptStart;
		}
		boolean clear = empty;
		Utils.broadcast(Component.text("empty: " + clear));
		timer = new BukkitRunnable() {
			public void run() {
				if (clear && money1 - money2 != 0) {
					cancel();
					timer = null;
					inventory1.setItem(acceptSlot1,emptyBlack);
					inventory1.setItem(acceptSlot2,emptyBlack);
					inventory2.setItem(acceptSlot1,emptyBlack);
					inventory2.setItem(acceptSlot2,emptyBlack);
				} else {
					ItemStack item = acceptTimer == 0 ? accept : wait.asQuantity(acceptTimer);
					inventory1.setItem(acceptSlot1,item);
					inventory1.setItem(acceptSlot2,accept);
					inventory2.setItem(acceptSlot1,item);
					inventory2.setItem(acceptSlot2,accept);
					if (acceptTimer <= 0) {
						cancel();
						acceptTimer = acceptStart;
						timer = null;
					} else acceptTimer--;
				}
			}
		}.runTaskTimer(AxTrade.getInstance(),1,20);
	}
	
	private void setMoney() {
		inventory1.setItem(moneySlot1,economy ? Utils.cloneChange(money,Component.empty(),
				Arrays.asList(Component.text(AxUtils.getEconomy().format(money1))),-1,false) : emptyBlack);
		inventory1.setItem(moneySlot2,economy ? Utils.cloneChange(money,Component.empty(),
				Arrays.asList(Component.text(AxUtils.getEconomy().format(money2))),-1,false) : emptyBlack);
		inventory2.setItem(moneySlot1,economy ? Utils.cloneChange(money,Component.empty(),
				Arrays.asList(Component.text(AxUtils.getEconomy().format(money2))),-1,false) : emptyBlack);
		inventory2.setItem(moneySlot2,economy ? Utils.cloneChange(money,Component.empty(),
				Arrays.asList(Component.text(AxUtils.getEconomy().format(money1))),-1,false) : emptyBlack);
		updateTrade();
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onClose(InventoryCloseEvent event) {
		if ((event.getView().getTopInventory().equals(inventory1) && MoneyListener1 != null) ||
				(event.getView().getTopInventory().equals(inventory2)) && MoneyListener2 != null) cancel();
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onClick(InventoryClickEvent event) {
		if (event.isCancelled() || !((event.getView().getTopInventory().equals(inventory1) && event.getWhoClicked().equals(this.initiator)) ||
				(event.getView().getTopInventory().equals(inventory2)) && event.getWhoClicked().equals(this.responder))) return;
		boolean player1 = event.getView().getTopInventory().equals(inventory1);
		if (event.getClick() == ClickType.CONTROL_DROP || event.getClick() == ClickType.DROP || event.getClick() == ClickType.SWAP_OFFHAND) {
			event.setCancelled(true);
			return;
		}
		int slot = event.getRawSlot();
		if (tradeSlots.contains(slot) || slot >= 6 * 9) {
			if (slot >= 6 * 9 && !event.isShiftClick()) return;
			updateTrade();
		} else {
			event.setCancelled(true);
			if (slot == closeSlot) cancel();
			else if (slot == moneySlot1 && economy) {
				if (player1) MoneyListener1 = new MoneyListener((Player) this.initiator,true);
				else MoneyListener2 = new MoneyListener(this.responder,false);
			} else if (slot == acceptSlot1 && timer == null) {
				if (player1) accept1 = !accept1;
				else accept2 = !accept2;
				checkDone();
			}
		}
	}
	
	public Trade setItemInitiator(int slot, ItemStack item) {
		if (((this.initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator)) || Utils.isNull(item) || slot < 0 || slot >= 6 * 9) return this;
		inventory1.setItem(slot,item);
		updateTrade();
		return this;
	}
	
	public Trade setMoneyInitiator(double amount) {
		if ((this.initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator)) return this;
		money1 = Math.max(amount,0);
		setMoney();
		return this;
	}
	
	public Trade setAcceptInitiator(boolean val) {
		if ((this.initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator)) return this;
		accept1 = val;
		return this;
	}
	
	public Trade setAutoAcceptWhenDone(boolean val) {
		if ((this.initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator)) return this;
		autoAccept = val;
		return this;
	}
	
	public Trade setDoneInitiator(boolean val) {
		if ((this.initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator)) return this;
		done = val;
		return this;
	}
	
	public ItemStack getItemInitiator(int slot) {
		return inventory1.getItem(slot);
	}
	
	public double getMoneyInitiator(double amount) {
		return money1;
	}
	
	public ItemStack getItemResponder(int slot) {
		return inventory2.getItem(slot);
	}
	
	public double getMoneyResponder(double amount) {
		return money2;
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void cancelOnLeaveEvent(PlayerQuitEvent event) {
		if (((initiator instanceof Player) && !Utils.isPlayerNPC((Player) initiator) && event.getPlayer().getUniqueId().equals(((Player) initiator).getUniqueId())) ||
				event.getPlayer().getUniqueId().equals(responder.getUniqueId())) cancel();
	}
	
	private class MoneyListener extends Listener {
		private Player player;
		private double limit;
		private boolean player1;
		
		public MoneyListener(Player player, boolean player1) {
			if (!economy) return;
			this.player = player;
			this.limit = AxUtils.getEconomy().getBalance(player);
			this.player1 = player1;
			player.closeInventory();
			player.sendMessage(Component.translatable(translateMoney).append(Component.text(": 0 - " +
					limit)).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false));
			register(AxTrade.getInstance());
		}
		
		@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
		public void onChatAmount(AsyncChatEvent event) {
			if (!event.getPlayer().equals(player)) return;
			event.setCancelled(true);
			boolean done = false;
			try {
				String read = ((TextComponent) event.message()).content().trim();
				double amount = Double.parseDouble(read);
				if (amount < 0 || amount > limit) throw new Exception();
				if (player1) money1 = amount;
				else money2 = amount;
				done = true;
			} catch (ClassCastException e) {
				done = true;
			} catch (Exception e) {
				Utils.chatColors(player,"&cError"); // error
			}
			if (done) {
				unregister();
				new BukkitRunnable() {
					public void run() {
						setMoney();
						if (player1) MoneyListener1 = null;
						else MoneyListener2 = null;
						player.openInventory(player1 ? inventory1 : inventory2);
					}
				}.runTask(AxTrade.getInstance());
			}
		}
		
		@Override
		public void unregister() {
			super.unregister();
		}
		
		@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
		public void unregisterOnLeaveEvent(PlayerQuitEvent event) {
			if (event.getPlayer().getUniqueId().equals(player.getUniqueId())) unregister();
		}
		
		@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
		public void onMove(PlayerMoveEvent event) {
			if (event.getPlayer().equals(player)) event.setCancelled(true);
		}
		
		@EventHandler(priority = EventPriority.LOWEST)
		public void onInteract(PlayerInteractEvent event) {
			if (event.getPlayer().equals(player)) event.setCancelled(true);
		}
		
		@EventHandler(priority = EventPriority.LOWEST)
		public void onSwap(PlayerSwapHandItemsEvent event) {
			if (event.getPlayer().equals(player)) event.setCancelled(true);
		}
		
		@EventHandler(priority = EventPriority.LOWEST)
		public void onDrop(PlayerDropItemEvent event) {
			if (event.getPlayer().equals(player)) event.setCancelled(true);
		}
		
		@EventHandler(priority = EventPriority.LOWEST)
		public void onClick(InventoryClickEvent event) {
			if (event.getWhoClicked().getUniqueId().equals(player.getUniqueId())) event.setCancelled(true);
		}
		
		@EventHandler(priority = EventPriority.LOWEST)
		public void onCommand(PlayerCommandPreprocessEvent event) {
			if (event.getPlayer().equals(player)) event.setCancelled(true);
		}
		
		@EventHandler(priority = EventPriority.LOWEST)
		public void onHotbar(PlayerItemHeldEvent event) {
			if (event.getPlayer().equals(player)) event.setCancelled(true);
		}
	}
}