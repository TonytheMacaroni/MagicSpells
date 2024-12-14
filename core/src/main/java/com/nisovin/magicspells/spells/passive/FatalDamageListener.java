package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

@Name("fataldamage")
public class FatalDamageListener extends PassiveListener {

	private final EnumSet<DamageCause> damageCauses = EnumSet.noneOf(DamageCause.class);

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;
		for (String causeName : var.split("\\|")) {
			DamageCause cause = null;
			try {
				cause = DamageCause.valueOf(causeName.toUpperCase());
			} catch (IllegalArgumentException ignored) {}
			if (cause == null) {
				MagicSpells.error("Invalid damage cause '" + causeName + "' in fataldamage trigger on passive spell '" + passiveSpell.getInternalName() + "'");
				return;
			}
			damageCauses.add(cause);
		}
	}

	@OverridePriority
	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof LivingEntity caster)) return;
		if (!isCancelStateOk(event.isCancelled())) return;
		if (event.getFinalDamage() < caster.getHealth()) return;
		if (!canTrigger(caster)) return;
		if (!damageCauses.isEmpty() && !damageCauses.contains(event.getCause())) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

	@OverridePriority
	@EventHandler
	public void onLegacyDamage(MagicSpellsEntityDamageByEntityEvent event) {
		onDamage(event);
	}

}
