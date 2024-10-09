package com.nisovin.magicspells.castmodifiers.conditions;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.debug.DebugPath;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.util.ModifierResult;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.castmodifiers.Modifier;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.castmodifiers.IModifier;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

/*
 * Just a heads-up that for the modifier actions inside this, I recommend that you use
 * stop rather than denied most of the time, because the denied action will actually cancel
 * the event being processed whereas the stop action will just say that this specific check
 * counts as a fail.
 *
 * in the general config, you can define a set of modifiers like this
 *
 * general:
 *     modifiers:
 *         modifier_name:
 *             checks:
 *                 - condition condition_var action action_var
 *                 - condition condition_var action action_var
 *             pass-condition: a string value that can be one of the following ANY, ALL, XOR
 *
 * You can also define some in the spell*.yml files as follows
 *
 * modifiers:
 *     modifier_name:
 *         checks:
 *             - condition condition_var action action_var
 *             - condition condition_var action action_var
 *         pass-condition: a string value that can be one of the following ANY, ALL, XOR
 *
 * to reference the modifier collection, you just slip this into your modifiers listed on a spell
 * - collection <modifier_name> action action_var
 * where <modifier_name> is the name that you assigned to the modifier collection as shown above
 */
@Name("collection")
public class MultiCondition extends Condition implements IModifier {

	private PassCondition passCondition = PassCondition.ALL;
	private List<ModifierSet.ModifierData> modifiers;
	private String name;

	// TODO: It's weird that this re-initializes the modifier collection for each usage of the modifier collection.
	//  Extract it out?
	@Override
	public boolean initialize(@NotNull String var) {
		if (var.isEmpty()) return false;

		MagicConfig config = MagicSpells.getMagicConfig();
		String prefix = "general.modifiers." + var;

		try (var ignored = MagicDebug.section(builder -> builder
			.category(DebugCategory.MODIFIERS)
			.message("Resolving modifier collection '%s'.", var)
			.resetPath()
			.path(config.getFile(MagicConfig.Category.MODIFIERS, var), DebugPath.Type.FILE)
			.path("general", DebugPath.Type.SECTION, false)
			.path("modifiers", DebugPath.Type.SECTION)
			.paths(var, DebugPath.Type.SECTION)
		)) {
			if (!config.isSection(prefix)) {
				MagicDebug.warn("Invalid modifier collection '%s' specified %s.", var, MagicDebug.resolveFullPath());
				return false;
			}

			List<String> modifierStrings = config.getStringList(prefix + ".checks", null);
			if (modifierStrings == null || modifierStrings.isEmpty()) {
				MagicDebug.warn("Invalid modifier collection '%s' specified %s - 'checks' is not defined.", var, MagicDebug.resolveFullPath());
				return false;
			}

			String passConditionString = config.getString(prefix + ".pass-condition", "ALL");
			try {
				passCondition = PassCondition.valueOf(passConditionString.toUpperCase());
			} catch (IllegalArgumentException badPassCondition) {
				MagicDebug.warn("Invalid pass condition '%s' specified %s. Defaulting to 'ALL'.", passConditionString, MagicDebug.resolveFullPath());
				passCondition = PassCondition.ALL;
			}

			modifiers = ModifierSet.getModifierData(null, modifierStrings);

			if (modifiers.isEmpty()) {
				MagicDebug.warn("Invalid modifier collection '%s' specified %s - could not load any modifiers from 'checks'.", var, MagicDebug.resolveFullPath());
				return false;
			}
		}

		name = var;

		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return check(caster, SpellData.NULL, modifier -> modifier.check(caster));
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return check(caster, SpellData.NULL, modifier -> modifier.check(caster));

	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return check(caster, SpellData.NULL, modifier -> modifier.check(caster));

	}

	@Override
	public boolean apply(SpellCastEvent event) {
		return check(event.getCaster(), event.getSpellData(), modifier -> modifier.apply(event));

	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		return check(event.getPlayer(), SpellData.NULL, modifier -> modifier.apply(event));
	}

	@Override
	public boolean apply(SpellTargetEvent event) {
		return check(event.getCaster(), event.getSpellData(), modifier -> modifier.apply(event));

	}

	@Override
	public boolean apply(SpellTargetLocationEvent event) {
		return check(event.getCaster(), event.getSpellData(), modifier -> modifier.apply(event));

	}

	@Override
	public boolean apply(MagicSpellsGenericPlayerEvent event) {
		return check(event.getPlayer(), SpellData.NULL, modifier -> modifier.apply(event));
	}

	@Override
	public ModifierResult apply(LivingEntity caster, SpellData data) {
		return apply(caster, data, modifier -> modifier.apply(caster, data));
	}

	@Override
	public ModifierResult apply(LivingEntity caster, LivingEntity target, SpellData data) {
		return apply(caster, data, modifier -> modifier.apply(caster, target, data));
	}

	@Override
	public ModifierResult apply(LivingEntity caster, Location target, SpellData data) {
		return apply(caster, data, modifier -> modifier.apply(caster, target, data));

	}

	private boolean check(LivingEntity caster, SpellData data, Predicate<Modifier> check) {
		try (var ignored = MagicDebug.section(builder -> builder
			.category(DebugCategory.MODIFIERS)
			.message("Resolving modifier collection '%s'.", name)
			.resetPath()
			.path(MagicSpells.getMagicConfig().getFile(MagicConfig.Category.MODIFIERS, name), DebugPath.Type.FILE)
			.path("general", DebugPath.Type.SECTION, false)
			.path("modifiers", DebugPath.Type.SECTION)
			.paths(name, DebugPath.Type.SECTION)
		)) {
			int pass = 0, fail = 0;

			for (ModifierSet.ModifierData modifierData : modifiers) {
				try (var ignored1 = MagicDebug.pushPath(modifierData.ordinal(), DebugPath.Type.LIST_ENTRY)) {
					Modifier modifier = modifierData.modifier();

					if (check.test(modifier)) pass++;
					else {
						fail++;

						String message = modifier.getStrModifierFailed();
						if (message != null) MagicSpells.sendMessage(message, caster, data);
					}

					if (!passCondition.shouldContinue(pass, fail)) break;
				}
			}

			boolean passed = passCondition.hasPassed(pass, fail);
			if (passed) MagicDebug.info("Modifier collection passed.");
			else MagicDebug.info("Modifier collection failed.");

			return passed;
		}
	}

	private ModifierResult apply(LivingEntity caster, SpellData data, Function<Modifier, ModifierResult> apply) {
		try (var ignored = MagicDebug.section(builder -> builder
			.category(DebugCategory.MODIFIERS)
			.message("Resolving modifier collection '%s'.", name)
			.resetPath()
			.path(MagicSpells.getMagicConfig().getFile(MagicConfig.Category.MODIFIERS, name), DebugPath.Type.FILE)
			.path("general", DebugPath.Type.SECTION, false)
			.path("modifiers", DebugPath.Type.SECTION)
			.paths(name, DebugPath.Type.SECTION)
		)) {
			int pass = 0, fail = 0;

			for (ModifierSet.ModifierData modifierData : modifiers) {
				try (var ignored1 = MagicDebug.pushPath(modifierData.ordinal(), DebugPath.Type.LIST_ENTRY)) {
					Modifier modifier = modifierData.modifier();

					ModifierResult result = apply.apply(modifier);
					data = result.data();

					if (result.check()) pass++;
					else {
						fail++;

						String message = modifier.getStrModifierFailed();
						if (message != null) MagicSpells.sendMessage(message, caster, data);
					}

					if (!passCondition.shouldContinue(pass, fail)) break;
				}
			}

			boolean passed = passCondition.hasPassed(pass, fail);
			if (passed) MagicDebug.info("Modifier collection passed.");
			else MagicDebug.info("Modifier collection failed.");

			return new ModifierResult(data, passed);
		}
	}

	public enum PassCondition {

		ALL {
			@Override
			public boolean hasPassed(int passes, int fails) {
				return fails == 0;
			}

			@Override
			public boolean shouldContinue(int passes, int fails) {
				return fails == 0;
			}

		},

		ANY {
			@Override
			public boolean hasPassed(int passes, int fails) {
				return passes > 0;
			}

			@Override
			public boolean shouldContinue(int passes, int fails) {
				return passes == 0;
			}
		},

		NONE {
			@Override
			public boolean hasPassed(int passes, int fails) {
				return passes == 0;
			}

			@Override
			public boolean shouldContinue(int passes, int fails) {
				return passes == 0;
			}
		},

		XOR {
			@Override
			public boolean hasPassed(int passes, int fails) {
				return passes == 1;
			}

			@Override
			public boolean shouldContinue(int passes, int fails) {
				return passes <= 1;
			}

		};

		PassCondition() {

		}

		public abstract boolean hasPassed(int passes, int fails);

		public abstract boolean shouldContinue(int passes, int fails);

	}

}
