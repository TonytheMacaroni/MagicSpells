package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;

public class OffhandCooldownSpell extends InstantSpell {

	private final List<Player> players = new ArrayList<>();

	private ItemStack item;

	private Spell spellToCheck;

	public OffhandCooldownSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		if (isConfigString("item")) {
			MagicItem magicItem = MagicItems.getMagicItemFromString(getConfigString("item", "stone"));
			if (magicItem != null) item = magicItem.getItemStack();
		} else if (isConfigSection("item")) {
			MagicItem magicItem = MagicItems.getMagicItemFromSection(getConfigSection("item"));
			if (magicItem != null) item = magicItem.getItemStack();
		}
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (item == null) {
			MagicDebug.warn("Invalid 'item' specified for OffhandCooldownSpell %s.", MagicDebug.resolveFullPath());
			return;
		}

		String spellToCheckName = getConfigString("spell", "");
		spellToCheck = MagicSpells.getSpellByInternalName(spellToCheckName);
		if (spellToCheck == null) {
			MagicDebug.warn("Invalid spell '%s' %s.", spellToCheckName, MagicDebug.resolveFullPath("spell"));
			return;
		}

		MagicSpells.scheduleRepeatingTask(() -> {
			Iterator<Player> iter = players.iterator();
			while (iter.hasNext()) {
				Player pl = iter.next();
				if (!pl.isValid()) {
					iter.remove();
					continue;
				}
				float cd = spellToCheck.getCooldown(pl);
				int amt = 1;
				if (cd > 0) amt = (int) Math.ceil(cd);

				PlayerInventory inventory = pl.getInventory();
				ItemStack off = inventory.getItemInOffHand();

				if (!off.isSimilar(item)) {
					off = item.clone();
					inventory.setItemInOffHand(off);
				}

				off.setAmount(amt);
			}
		}, TimeUtil.TICKS_PER_SECOND, TimeUtil.TICKS_PER_SECOND);
	}

	@Override
	public CastResult cast(SpellData data) {
		if (item == null || !(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		players.add(caster);
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
