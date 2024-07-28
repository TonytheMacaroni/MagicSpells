package com.nisovin.magicspells.castmodifiers;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.debug.DebugPath;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.events.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.ModifierResult;

public class ModifierSet {

	public static ModifierListener modifierListener;

	public static void initializeModifierListeners() {
		modifierListener = new ModifierListener();
		MagicSpells.registerEvents(modifierListener);
	}

	public static void unload() {
		if (modifierListener != null) {
			modifierListener.unload();
			modifierListener = null;
		}
	}

	private final List<Modifier> modifiers;

	public ModifierSet(List<String> data) {
		this(data, null, false);
	}

	public ModifierSet(List<String> data, Spell spell) {
		this(data, spell, false);
	}

	public ModifierSet(List<String> data, boolean isFromManaSystem) {
		this(data, null, isFromManaSystem);
	}

	private ModifierSet(List<String> data, Spell spell, boolean isFromManaSystem) {
		modifiers = new ArrayList<>();

		for (int i = 0; i < data.size(); i++) {
			String modifierString = data.get(i);

			// TODO: This breaks if the constructor is called without setting up the list path properly
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Initializing modifier '%s'.", modifierString)
				.pushPath(String.valueOf(i), DebugPath.Type.LIST_ENTRY)
			) {
				Modifier modifier = new Modifier();

				if (!modifier.process(modifierString)) {
					String extra = "";
					if (modifier.getCustomActionData() != null)
						extra = ": " + modifier.getCustomActionData().getInvalidText();

					MagicDebug.warn("Invalid modifier '%s' %s%s", modifierString, MagicDebug.resolveFullPath(), extra);
					continue;
				}

				if (modifier.getStrModifierFailed() == null && spell != null)
					modifier.setStrModifierFailed(spell.getStrModifierFailed());

				modifiers.add(modifier);
			}
		}
	}

	public void apply(SpellCastEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (cont) continue;

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), event.getCaster(), event.getSpellData());
			break;
		}
	}

	public void apply(ManaChangeEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (cont) continue;

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), event.getPlayer(), MagicSpells.NULL_ARGS);
			break;
		}
	}

	public void apply(SpellTargetEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (cont) continue;

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), event.getCaster(), event.getSpellData());
			break;
		}
	}

	public void apply(MagicSpellsGenericPlayerEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (cont) continue;

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), event.getPlayer(), MagicSpells.NULL_ARGS);
			break;
		}
	}

	public void apply(SpellTargetLocationEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (cont) continue;

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), event.getCaster(), event.getSpellData());
			break;
		}
	}

	public ModifierResult apply(LivingEntity caster, SpellData data) {
		for (Modifier modifier : modifiers) {
			ModifierResult result = modifier.apply(caster, data);
			if (result.check()) {
				data = result.data();
				continue;
			}

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), caster, result.data());
			return result;
		}

		return new ModifierResult(data, true);
	}

	public ModifierResult apply(LivingEntity caster, LivingEntity target, SpellData data) {
		for (Modifier modifier : modifiers) {
			ModifierResult result = modifier.apply(caster, target, data);
			if (result.check()) {
				data = result.data();
				continue;
			}

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), caster, result.data());
			return result;
		}

		return new ModifierResult(data, true);
	}

	public ModifierResult apply(LivingEntity caster, Location target, SpellData data) {
		for (Modifier modifier : modifiers) {
			ModifierResult result = modifier.apply(caster, target, data);
			if (result.check()) {
				data = result.data();
				continue;
			}

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), caster, result.data());
			return result;
		}

		return new ModifierResult(data, true);
	}

	public boolean check(LivingEntity livingEntity) {
		for (Modifier modifier : modifiers) {
			boolean pass = modifier.check(livingEntity);
			if (!pass) return false;
		}
		return true;
	}

	public boolean check(LivingEntity livingEntity, LivingEntity entity) {
		for (Modifier modifier : modifiers) {
			boolean pass = modifier.check(livingEntity, entity);
			if (!pass) return false;
		}
		return true;
	}

	public boolean check(LivingEntity livingEntity, Location location) {
		for (Modifier modifier : modifiers) {
			boolean pass = modifier.check(livingEntity, location);
			if (!pass) return false;
		}
		return true;
	}

}
