package com.nisovin.magicspells.listeners;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.debug.MagicDebug;

public class LeftClickListener implements Listener {

	private final Map<CastItem, Spell> leftClickCastItems = new HashMap<>();
	private final Map<String, Long> lastCast = new HashMap<>();

	public LeftClickListener() {
		for (Spell spell : MagicSpells.getSpells().values()) {
			for (CastItem item : spell.getLeftClickCastItems()) {
				if (item == null) continue;

				Spell old = leftClickCastItems.put(item, spell);
				if (old != null)
					MagicDebug.warn("The spell '%s' has the same left-click-cast-item as '%s'!", spell, old);
			}
		}
	}

	public boolean hasLeftClickCastItems() {
		return !leftClickCastItems.isEmpty();
	}

	@EventHandler
	public void onLeftClick(final PlayerInteractEvent event) {
		if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
		if (!event.hasItem()) return;

		ItemStack item = event.getItem();
		if (item == null) return;

		final Spell spell = leftClickCastItems.get(new CastItem(item));
		if (spell == null) return;

		Player player = event.getPlayer();
		if (MagicSpells.isRespectingItemCooldowns() && player.hasCooldown(item)) return;

		Spellbook spellbook = MagicSpells.getSpellbook(player);
		if (!spellbook.hasSpell(spell) || !spellbook.canCast(spell)) return;

		if (!spell.isIgnoringGlobalCooldown()) {
			Long lastCastTime = lastCast.get(player.getName());
			if (lastCastTime != null && lastCastTime + MagicSpells.getGlobalCooldown() > System.currentTimeMillis()) return;
			lastCast.put(player.getName(), System.currentTimeMillis());
		}

		MagicSpells.scheduleDelayedTask(() -> spell.hardCast(new SpellData(player)), 0);
	}

}
