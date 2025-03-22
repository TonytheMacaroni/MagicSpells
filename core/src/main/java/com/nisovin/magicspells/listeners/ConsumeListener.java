package com.nisovin.magicspells.listeners;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.Spell.SpellCastResult;

public class ConsumeListener implements Listener {

	private Map<CastItem, Spell> consumeCastItems = new HashMap<>();
	private Map<String, Long> lastCast = new HashMap<>();
	
	public ConsumeListener() {
		for (Spell spell : MagicSpells.getSpells().values()) {
			for (CastItem item : spell.getConsumeCastItems()) {
				Spell old = consumeCastItems.put(item, spell);
				if (old == null) continue;

				MagicDebug.warn("The spell '%s' %s has the same 'consume-cast-item' as '%s'.", spell.getInternalName(), MagicDebug.resolveFullPath(), old.getInternalName());
			}
		}
	}
	
	public boolean hasConsumeCastItems() {
		return !consumeCastItems.isEmpty();
	}
	
	@EventHandler
	public void onConsume(final PlayerItemConsumeEvent event) {
		CastItem castItem = new CastItem(event.getItem());
		final Spell spell = consumeCastItems.get(castItem);
		if (spell == null) return;

		Player player = event.getPlayer();
		Long lastCastTime = lastCast.get(player.getName());
		if (lastCastTime != null && lastCastTime + MagicSpells.getGlobalCooldown() > System.currentTimeMillis()) return;
		lastCast.put(player.getName(), System.currentTimeMillis());

		if (MagicSpells.getSpellbook(player).canCast(spell)) {
			SpellCastResult result = spell.hardCast(new SpellData(player));
			if (result.state != SpellCastState.NORMAL) event.setCancelled(true);
		}
	}

}
