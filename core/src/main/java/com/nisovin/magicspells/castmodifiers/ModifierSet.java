package com.nisovin.magicspells.castmodifiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.debug.DebugPath;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.util.ModifierResult;

public class ModifierSet {

	public static ModifierListener modifierListener;

	private final List<ModifierData> modifiers;
	private final String path;

	@Deprecated
	public ModifierSet(List<String> data, Spell spell) {
		this(data, spell, false);
	}

	@Deprecated
	public ModifierSet(List<String> data, boolean isFromManaSystem) {
		this(data, null, isFromManaSystem);
	}

	private ModifierSet(List<String> data, Spell spell, boolean isFromManaSystem) {
		path = "<unknown>";

		try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Resolving legacy modifier set.")
			.pushPaths(path, DebugPath.Type.LIST)
		) {
			if (data.isEmpty()) {
				MagicDebug.info("No modifiers found.");
				modifiers = new ArrayList<>();
				return;
			}

			modifiers = getModifierData(spell, data);
		}
	}

	private ModifierSet(@NotNull List<ModifierData> modifiers, @NotNull String path) {
		this.modifiers = modifiers;
		this.path = path;
	}

	@NotNull
	public static ModifierSet fromConfig(@NotNull ConfigurationSection config, @NotNull String path) {
		return fromConfig(null, config, path);
	}

	@NotNull
	public static ModifierSet fromConfig(@Nullable Spell spell, @NotNull ConfigurationSection config, @NotNull String path) {
		try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Resolving modifier set '%s'.", MagicDebug.resolveShortPath(config, path))
			.pushPaths(path, DebugPath.Type.LIST)
		) {
			List<String> modifierStrings = config.getStringList(path);
			if (modifierStrings.isEmpty()) {
				MagicDebug.info("No modifiers found.");
				return new ModifierSet(Collections.emptyList(), path);
			}

			return new ModifierSet(getModifierData(spell, modifierStrings), path);
		}
	}

	static List<ModifierData> getModifierData(@Nullable Spell spell, @NotNull List<String> modifierStrings) {
		List<ModifierData> modifiers = new ArrayList<>();
		for (int i = 0; i < modifierStrings.size(); i++) {
			try (var ignored1 = MagicDebug.pushListEntry(i)) {
				Modifier modifier = Modifier.fromString(modifierStrings.get(i));
				if (modifier == null) continue;

				if (spell != null && modifier.getStrModifierFailed() == null)
					modifier.setStrModifierFailed(spell.getStrModifierFailed());

				modifiers.add(new ModifierData(modifier, i));
			}
		}

		return modifiers;
	}

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

	public void apply(SpellCastEvent event) {
		check(event.getCaster(), event.getSpellData(), modifier -> modifier.apply(event));
	}

	public void apply(ManaChangeEvent event) {
		check(event.getPlayer(), SpellData.NULL, modifier -> modifier.apply(event));
	}

	public void apply(SpellTargetEvent event) {
		check(event.getCaster(), event.getSpellData(), modifier -> modifier.apply(event));
	}

	public void apply(MagicSpellsGenericPlayerEvent event) {
		check(event.getPlayer(), SpellData.NULL, modifier -> modifier.apply(event));
	}

	public void apply(SpellTargetLocationEvent event) {
		check(event.getCaster(), event.getSpellData(), modifier -> modifier.apply(event));
	}

	public ModifierResult apply(LivingEntity caster, SpellData data) {
		return checkResult(caster, data, modifier -> modifier.apply(caster, data));
	}

	public ModifierResult apply(LivingEntity caster, LivingEntity target, SpellData data) {
		return checkResult(caster, data, modifier -> modifier.apply(caster, target, data));
	}

	public ModifierResult apply(LivingEntity caster, Location target, SpellData data) {
		return checkResult(caster, data, modifier -> modifier.apply(caster, target, data));
	}

	public boolean check(LivingEntity caster) {
		return check(caster, SpellData.NULL, modifier -> modifier.check(caster));
	}

	public boolean check(LivingEntity caster, LivingEntity target) {
		return check(caster, SpellData.NULL, modifier -> modifier.check(caster, target));
	}

	public boolean check(LivingEntity caster, Location location) {
		return check(caster, SpellData.NULL, modifier -> modifier.check(caster, location));
	}

	private boolean check(LivingEntity caster, SpellData data, Predicate<Modifier> check) {
		if (modifiers.isEmpty()) return true;

		try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Processing modifier set '%s'.", MagicDebug.resolveShortPath(path))
			.pushPaths(path, DebugPath.Type.LIST)
		) {
			for (ModifierData modifierData : modifiers) {
				try (var ignored1 = MagicDebug.pushListEntry(modifierData.ordinal)) {
					Modifier modifier = modifierData.modifier;
					if (check.test(modifier)) continue;

					String message = modifier.getStrModifierFailed();
					if (message != null) MagicSpells.sendMessage(message, caster, data);

					MagicDebug.info("Modifier set failed.");
					return false;
				}
			}

			MagicDebug.info("Modifier set passed.");
			return true;
		}
	}

	private ModifierResult checkResult(LivingEntity caster, SpellData data, Function<Modifier, ModifierResult> apply) {
		if (modifiers.isEmpty()) return new ModifierResult(data, true);

		try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Processing modifier set '%s'.", MagicDebug.resolveShortPath(path))
			.pushPaths(path, DebugPath.Type.LIST)
		) {
			for (ModifierData modifierData : modifiers) {
				try (var ignored1 = MagicDebug.pushListEntry(modifierData.ordinal)) {
					Modifier modifier = modifierData.modifier;

					ModifierResult result = apply.apply(modifier);
					if (result.check()) {
						data = result.data();
						continue;
					}

					String message = modifier.getStrModifierFailed();
					if (message != null) MagicSpells.sendMessage(message, caster, data);

					MagicDebug.info("Modifier set failed.");
					return result;
				}
			}

			MagicDebug.info("Modifier set passed.");
			return new ModifierResult(data, true);
		}
	}

	record ModifierData(Modifier modifier, int ordinal) {

	}

}
