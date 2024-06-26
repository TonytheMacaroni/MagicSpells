package com.nisovin.magicspells.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellUtil;

public class MagicPlayerListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		// Setup spell book
		Spellbook spellbook = new Spellbook(player);
		MagicSpells.getSpellbooks().put(player.getName(), spellbook);
		
		// Setup mana bar
		if (MagicSpells.getManaHandler() != null) MagicSpells.getManaHandler().createManaBar(player);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Spellbook spellbook = MagicSpells.getSpellbooks().remove(event.getPlayer().getName());
		if (spellbook != null) spellbook.destroy();
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
		if (!MagicSpells.arePlayerSpellsSeparatedPerWorld()) return;
		Player player = event.getPlayer();
		MagicSpells.debug("Player '" + player.getName() + "' changed from world '" + event.getFrom().getName() + "' to '" + player.getWorld().getName() + "', reloading spells");
		MagicSpells.getSpellbook(player).reload();
	}

	@EventHandler(ignoreCancelled = true)
	public void onDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		SpellUtil.updateManaBar(player);
	}

	@EventHandler(ignoreCancelled = true)
	public void onHealthRegain(EntityRegainHealthEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		SpellUtil.updateManaBar(player);
	}

	@EventHandler(ignoreCancelled = true)
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		SpellUtil.updateManaBar((Player) event.getEntity());
	}
	
}
