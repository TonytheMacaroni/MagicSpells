package com.nisovin.magicspells.spells.passive.util;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.DependsOn;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.ValidTargetList;

/**
 * Annotations:
 * <ul>
 *     <li>{@link Name} (required): Holds the configuration name of the passive listener.</li>
 *     <li>{@link DependsOn} (optional): Requires listed plugins to be enabled before this passive listener is created.</li>
 * </ul>
 */
public abstract class PassiveListener implements Listener {

	protected PassiveSpell passiveSpell;

	protected EventPriority priority;

	public PassiveSpell getPassiveSpell() {
		return passiveSpell;
	}

	public void setPassiveSpell(PassiveSpell passiveSpell) {
		this.passiveSpell = passiveSpell;
	}

	public EventPriority getEventPriority() {
		return priority;
	}

	public void setEventPriority(EventPriority priority) {
		this.priority = priority;
	}

	public boolean canTrigger(LivingEntity caster) {
		return canTrigger(caster, true);
	}

	public boolean canTrigger(LivingEntity caster, boolean ignoreGameMode) {
		ValidTargetList triggerList = passiveSpell.getTriggerList();
		if (!triggerList.canTarget(caster, ignoreGameMode)) return false;

		if (caster instanceof Player player && !passiveSpell.isHelperSpell()) {
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			return spellbook.hasSpell(passiveSpell, true);
		}

		return true;
	}
		
	public boolean cancelDefaultAction(boolean casted) {
		if (passiveSpell == null) return true;
		if (casted && passiveSpell.cancelDefaultAction()) return true;
		if (!casted && passiveSpell.cancelDefaultActionWhenCastFails()) return true;
		return false;
	}
	
	public boolean isCancelStateOk(boolean cancelled) {
		if (passiveSpell == null) return false;
		if (passiveSpell.ignoreCancelled() && cancelled) return false;
		if (passiveSpell.requireCancelledEvent() && !cancelled) return false;
		return true;
	}
	
	public abstract void initialize(@NotNull String var);

	public boolean initialize(@NotNull ConfigurationSection config) {
		return false;
	}

	public void turnOff() {
		// No op
	}
	
	@Override
	public boolean equals(Object other) {
		return this == other; // Don't want to make things equal unless they are the same object
	}
	
}
