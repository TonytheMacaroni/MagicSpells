package com.nisovin.magicspells.castmodifiers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class ModifierListener implements Listener {

	private List<IModifier> preModifierHooks;
	private List<IModifier> postModifierHooks;

	public ModifierListener() {
		preModifierHooks = new CopyOnWriteArrayList<>();
		postModifierHooks = new CopyOnWriteArrayList<>();
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onSpellCast(SpellCastEvent event) {
		ModifierSet m = event.getSpell().getModifiers();
		for (IModifier preMod : preModifierHooks) {
			if (!preMod.apply(event)) return;
		}
		if (m != null) m.apply(event);
		for (IModifier postMod : postModifierHooks) {
			if (!postMod.apply(event)) return;
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onSpellTarget(SpellTargetEvent event) {
		ModifierSet m = event.getSpell().getTargetModifiers();
		for (IModifier preMod : preModifierHooks) {
			if (!preMod.apply(event)) return;
		}
		if (m != null) m.apply(event);
		for (IModifier postMod : postModifierHooks) {
			if (!postMod.apply(event)) return;
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onSpellTarget(SpellTargetLocationEvent event) {
		ModifierSet m = event.getSpell().getLocationModifiers();
		for (IModifier preMod : preModifierHooks) {
			if (!preMod.apply(event)) return;
		}
		if (m != null) m.apply(event);
		for (IModifier postMod : postModifierHooks) {
			if (!postMod.apply(event)) return;
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onManaRegen(ManaChangeEvent event) {
		if (event.getReason() != ManaChangeReason.REGEN) return;
		ModifierSet modifiers = MagicSpells.getManaHandler().getModifiers();
		for (IModifier preMod : preModifierHooks) {
			if (!preMod.apply(event)) return;
		}
		if (modifiers != null) modifiers.apply(event);
		for (IModifier postMod : postModifierHooks) {
			if (!postMod.apply(event)) return;
		}
	}

	public void addPreModifierHook(IModifier hook) {
		if (hook == null) return;
		preModifierHooks.add(hook);
	}

	public void addPostModifierHook(IModifier hook) {
		if (hook == null) return;
		postModifierHooks.add(hook);
	}

	public void unload() {
		preModifierHooks.clear();
		preModifierHooks = null;
		postModifierHooks.clear();
		postModifierHooks = null;
	}

}
