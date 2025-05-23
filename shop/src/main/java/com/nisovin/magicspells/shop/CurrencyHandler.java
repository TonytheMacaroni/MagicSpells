package com.nisovin.magicspells.shop;

import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.compat.CompatBasics;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

public class CurrencyHandler {
	
	private static final Pattern PATTERN_CURRENCY_ITEM_BASIC = Pattern.compile("^[0-9]+$");
	private static final Pattern PATTERN_CURRENCY_ITEM_ADVANCED = Pattern.compile("^[0-9]+:[0-9]+$");
	
	private HashMap<String,String> currencies = new HashMap<>();
	private String defaultCurrency;
	private Economy economy;
	
	public CurrencyHandler(Configuration config) {
		ConfigurationSection sec = config.getConfigurationSection("currencies");
		if (sec == null) {
			this.defaultCurrency = "money";
			this.currencies.put("money", "vault");
		} else {
			Set<String> keys = sec.getKeys(false);
			for (String key : keys) {
				if (this.defaultCurrency == null) this.defaultCurrency = key;
				this.currencies.put(key.toLowerCase(), sec.getString(key).toLowerCase());
			}
			if (this.defaultCurrency == null) {
				this.defaultCurrency = "money";
				this.currencies.put("money", "vault");
			}
		}

		// Set up vault hook
		if (this.currencies.containsValue("vault") && CompatBasics.pluginEnabled("Vault")) {
			RegisteredServiceProvider<Economy> provider = CompatBasics.getServiceProvider(Economy.class);
			if (provider != null) this.economy = provider.getProvider();
		}
	}
	
	public boolean has(Player player, double amount) {
		return has(player, amount, this.defaultCurrency);
	}
	
	public boolean has(Player player, double amount, String currency) {
		String c = currency == null ? null : this.currencies.get(currency.toLowerCase());
		if (c == null) c = this.currencies.get(this.defaultCurrency);
		
		if (c == null) return false;
		if (c.equalsIgnoreCase("vault") && this.economy != null) return this.economy.has(player.getName(), amount);
		if (c.equalsIgnoreCase("levels")) return player.getLevel() >= (int)amount;
		if (c.equalsIgnoreCase("experience") || c.equalsIgnoreCase("xp")) return player.calculateTotalExperiencePoints() >= amount;
		if (PATTERN_CURRENCY_ITEM_BASIC.asMatchPredicate().test(c)) return inventoryContains(player.getInventory(), new ItemStack(Util.getMaterial(c), (int) amount));
		if (PATTERN_CURRENCY_ITEM_ADVANCED.asMatchPredicate().test(c)) {
			String[] s = c.split(":");
			short data = Short.parseShort(s[1]);
			return inventoryContains(player.getInventory(), new ItemStack(Util.getMaterial(s[0]), (int) amount, data));
		}
		return false;
	}
	
	public void remove(Player player, double amount) {
		remove(player, amount, this.defaultCurrency);
	}
	
	public void remove(Player player, double amount, String currency) {
		String c = currency == null ? null : this.currencies.get(currency.toLowerCase());
		if (c == null) c = this.currencies.get(this.defaultCurrency);
		
		if (c == null) {
		} else if (c.equalsIgnoreCase("vault") && this.economy != null) {
			this.economy.withdrawPlayer(player.getName(), amount);
		} else if (c.equalsIgnoreCase("levels")) {
			player.setLevel(player.getLevel() - (int)amount);
		} else if (c.equalsIgnoreCase("experience") || c.equalsIgnoreCase("xp")) {
			Util.addExperience(player, (int) -amount);
		} else if (PATTERN_CURRENCY_ITEM_BASIC.asMatchPredicate().test(c)) {
			removeFromInventory(player.getInventory(), new ItemStack(Util.getMaterial(c), (int) amount));
			player.updateInventory();
		} else if (PATTERN_CURRENCY_ITEM_ADVANCED.asMatchPredicate().test(c)) {
			String[] s = c.split(":");
			short data = Short.parseShort(s[1]);
			removeFromInventory(player.getInventory(), new ItemStack(Util.getMaterial(s[0]), (int) amount, data));
			player.updateInventory();
		}
	}
	
	public boolean isValidCurrency(String currency) {
		return currency != null && this.currencies.containsKey(currency);
	}
	
	private boolean inventoryContains(Inventory inventory, ItemStack item) {
		int count = 0;
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null && items[i].getType() == item.getType() && items[i].getDurability() == item.getDurability()) {
				count += items[i].getAmount();
			}
			if (count >= item.getAmount()) return true;
		}
		return false;
	}
	
	private void removeFromInventory(Inventory inventory, ItemStack item) {
		int amt = item.getAmount();
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] == null) continue;
			if (items[i].getType() == item.getType() && items[i].getDurability() == item.getDurability()) {
				if (items[i].getAmount() > amt) {
					items[i].setAmount(items[i].getAmount() - amt);
					break;
				} else if (items[i].getAmount() == amt) {
					items[i] = null;
					break;
				} else {
					amt -= items[i].getAmount();
					items[i] = null;
				}
			}
		}
		inventory.setContents(items);
	}
	
}
