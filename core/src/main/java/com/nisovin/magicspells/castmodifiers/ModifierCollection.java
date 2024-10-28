package com.nisovin.magicspells.castmodifiers;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.debug.DebugPath;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.util.ModifierResult;
import com.nisovin.magicspells.castmodifiers.ModifierSet.ModifierData;

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
public class ModifierCollection {

	private final List<ModifierData> modifiers;
	private final PassCondition condition;
	private final String name;

	ModifierCollection(String name, List<ModifierData> modifiers, PassCondition condition) {
		this.modifiers = modifiers;
		this.condition = condition;
		this.name = name;
	}

	public boolean check(LivingEntity caster, SpellData data, Predicate<Modifier> check) {
		try (var ignored = MagicDebug.section(builder -> builder
			.category(DebugCategory.MODIFIERS)
			.message("Processing modifier collection '%s'.", name)
			.resetPath()
			.path(MagicSpells.getMagicConfig().getFile(MagicConfig.Category.MODIFIERS, name), DebugPath.Type.FILE)
			.path("general", DebugPath.Type.SECTION, false)
			.path("modifiers", DebugPath.Type.SECTION)
			.path(name, DebugPath.Type.LIST)
		)) {
			MagicDebug.info("Pass condition: %s", condition);
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

					MagicDebug.info("Passed: %s, Failed: %s.", pass, fail);

					if (!condition.shouldContinue(pass, fail)) break;
				}
			}

			boolean passed = condition.hasPassed(pass, fail);
			if (passed) MagicDebug.info("Modifier collection passed.");
			else MagicDebug.info("Modifier collection failed.");

			return passed;
		}
	}

	public ModifierResult checkResult(LivingEntity caster, SpellData data, Function<Modifier, ModifierResult> check) {
		try (var ignored = MagicDebug.section(builder -> builder
			.category(DebugCategory.MODIFIERS)
			.message("Processing modifier collection '%s'.", name)
			.resetPath()
			.path(MagicSpells.getMagicConfig().getFile(MagicConfig.Category.MODIFIERS, name), DebugPath.Type.FILE)
			.path("general", DebugPath.Type.SECTION, false)
			.path("modifiers", DebugPath.Type.SECTION)
			.path(name, DebugPath.Type.LIST)
		)) {
			MagicDebug.info("Pass condition: %s", condition);
			int pass = 0, fail = 0;

			for (ModifierSet.ModifierData modifierData : modifiers) {
				try (var ignored1 = MagicDebug.pushPath(modifierData.ordinal(), DebugPath.Type.LIST_ENTRY)) {
					Modifier modifier = modifierData.modifier();

					ModifierResult result = check.apply(modifier);
					data = result.data();

					if (result.check()) pass++;
					else {
						fail++;

						String message = modifier.getStrModifierFailed();
						if (message != null) MagicSpells.sendMessage(message, caster, data);
					}

					MagicDebug.info("Passed: %s, Failed: %s", pass, fail);

					if (!condition.shouldContinue(pass, fail)) break;
				}
			}

			boolean passed = condition.hasPassed(pass, fail);
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
