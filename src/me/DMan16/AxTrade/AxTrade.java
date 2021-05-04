package me.DMan16.AxTrade;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import me.Aldreda.AxUtils.Utils.Utils;

public class AxTrade extends JavaPlugin {
	private static AxTrade instance = null;
	private static TradeListener TradeListener;
	
	public void onEnable() {
		instance = this;
		TradeListener = new TradeListener();
		Utils.chatColorsLogPlugin("&fAxTrade &aloaded!");
	}
	
	static TradeListener getTradeListener() {
		return TradeListener;
	}
	
	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
		Utils.chatColorsLogPlugin("&fAxTrade &adisabed");
	}
	
	public static FileConfiguration config() {
		return instance.getConfig();
	}
	
	public static AxTrade getInstance() {
		return instance;
	}
}