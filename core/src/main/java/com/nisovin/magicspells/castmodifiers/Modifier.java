package com.nisovin.magicspells.castmodifiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.*;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.booleans.BooleanPredicate;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.util.ModifierResult;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.castmodifiers.customdata.CustomData;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

public class Modifier implements IModifier {

	private static final Pattern MODIFIER_STR_FAILED_PATTERN = Pattern.compile("\\$\\$");

	private String text;

	private Condition condition;
	private ModifierType type;

	private String strModifierFailed;

	private CustomData customActionData;

	private boolean negated = false;
	private boolean initialized = false;

	@Deprecated
	public Modifier() {

	}

	@Nullable
	public static Modifier fromString(@NotNull String string) {
		try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Resolving modifier '%s'.", string)) {
			Modifier modifier = new Modifier();

			String[] modifierAndFailureMessage = MODIFIER_STR_FAILED_PATTERN.split(string);
			modifier.text = modifierAndFailureMessage[0].trim();

			String[] modifierData = modifier.text.split(" ", 4);
			if (modifierData.length < 2) {
				MagicDebug.warn("Invalid modifier '%s' %s: Missing values.", string, MagicDebug.resolveFullPath());
				return null;
			}

			String conditionName = modifierData[0];
			if (conditionName.startsWith("!")) {
				modifier.negated = true;
				conditionName = conditionName.substring(1);
			}

			modifier.condition = MagicSpells.getConditionManager().getConditionByName(conditionName.replace("_", ""));
			if (modifier.condition == null) {
				MagicDebug.warn("Invalid modifier '%s' %s: No such condition '%s'.", string, MagicDebug.resolveFullPath(), conditionName);
				return null;
			}

			String actionData = null;

			modifier.type = getTypeByName(modifierData[1]);
			if (modifier.type == null) {
				if (modifierData.length == 2) {
					MagicDebug.warn("Invalid modifier '%s' %s: No such action '%s'.", string, MagicDebug.resolveFullPath(), modifierData[1]);
					return null;
				}

				if (!modifier.condition.initialize(modifierData[1])) {
					MagicDebug.warn("Failed to initialize modifier '%s' %s.", string, MagicDebug.resolveFullPath());
					return null;
				}

				modifier.type = getTypeByName(modifierData[2]);
				if (modifier.type == null) {
					MagicDebug.warn("Invalid modifier '%s' %s: No such action '%s'.", string, MagicDebug.resolveFullPath(), modifierData[2]);
					return null;
				}

				if (modifierData.length > 3) actionData = modifierData[3];
			} else {
				if (!modifier.condition.initialize("")) {
					MagicDebug.warn("Failed to initialize modifier '%s' %s.", string, MagicDebug.resolveFullPath());
					return null;
				}

				if (modifierData.length == 3) actionData = modifierData[2];
				else if (modifierData.length == 4) actionData = modifierData[2] + " " + modifierData[3];
			}

			if (!modifier.type.usesCustomData() && actionData != null) {
				MagicDebug.warn("Invalid modifier '%s' %s: Too many values.", string, MagicDebug.resolveFullPath());
				return null;
			}

			if (modifier.type.usesCustomData()) {
				if (actionData == null) {
					MagicDebug.warn("Invalid modifier '%s' %s: Missing values.", string, MagicDebug.resolveFullPath());
					return null;
				}

				modifier.customActionData = modifier.type.buildCustomActionData(actionData);
				if (modifier.customActionData == null) {
					MagicDebug.warn("Invalid modifier '%s' %s: Invalid action data '%s'.", string, MagicDebug.resolveFullPath(), actionData);
					return null;
				}

				if (!modifier.customActionData.isValid()) {
					MagicDebug.warn("Invalid modifier '%s' %s: %s", string, MagicDebug.resolveFullPath(), modifier.customActionData.getInvalidText());
					return null;
				}
			}

			if (modifierAndFailureMessage.length > 1)
				modifier.strModifierFailed = modifierAndFailureMessage[1].trim();

			modifier.initialized = true;

			return modifier;
		}

	}

	@Deprecated
	public boolean process(String string) {
		if (MagicSpells.getConditionManager() == null) return false;

		String[] s = MODIFIER_STR_FAILED_PATTERN.split(string);
		if (s == null || s.length == 0) return false;
		String[] data = s[0].trim().split(" ", 4);

		if (data.length < 2) return false;

		// Get condition
		if (data[0].startsWith("!")) {
			negated = true;
			data[0] = data[0].substring(1);
		}

		condition = MagicSpells.getConditionManager().getConditionByName(data[0].replace("_", ""));
		if (condition == null) return false;

		String modifierVar = null;

		// Get type and vars
		type = getTypeByName(data[1]);
		if (type == null && data.length > 2) {
			boolean init = condition.initialize(data[1]);
			if (!init) return false;
			type = getTypeByName(data[2]);
			if (data.length > 3) modifierVar = data[3];
		} else if (data.length == 3) {
			modifierVar = data[2];
		} else if (data.length == 4) {
			modifierVar = data[2] + " " + data[3];
		} else {
			boolean init = condition.initialize("");
			if (!init) return false;
		}

		// Check type
		if (type == null) return false;

		// Process modifierVar
		if (type.usesCustomData()) {
			customActionData = type.buildCustomActionData(modifierVar);
			if (customActionData == null || !customActionData.isValid()) return false;
		}

		// Check for failed string
		if (s.length > 1) strModifierFailed = s[1].trim();

		initialized = true;
		return true;
	}

	@Deprecated
	public boolean isInitialized() {
		return initialized;
	}

	public String getStrModifierFailed() {
		return strModifierFailed;
	}

	public void setStrModifierFailed(String strModifierFailed) {
		this.strModifierFailed = strModifierFailed;
	}

	public CustomData getCustomActionData() {
		return customActionData;
	}

	public void setCustomActionData(CustomData customActionData) {
		this.customActionData = customActionData;
	}

	@Override
	public boolean apply(SpellCastEvent event) {
		return applyEvent(
			mod -> mod.apply(event),
			() -> condition.check(event.getCaster()),
			check -> type.apply(event, check, customActionData)
		);
	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		return applyEvent(
			mod -> mod.apply(event),
			() -> condition.check(event.getPlayer()),
			check -> type.apply(event, check, customActionData)
		);
	}

	@Override
	public boolean apply(SpellTargetEvent event) {
		return applyEvent(
			mod -> mod.apply(event),
			() -> condition.check(event.getCaster(), event.getTarget()),
			check -> type.apply(event, check, customActionData)
		);
	}

	@Override
	public boolean apply(SpellTargetLocationEvent event) {
		return applyEvent(
			mod -> mod.apply(event),
			() -> condition.check(event.getCaster(), event.getTargetLocation()),
			check -> type.apply(event, check, customActionData)
		);
	}

	@Override
	public boolean apply(MagicSpellsGenericPlayerEvent event) {
		return applyEvent(
			mod -> mod.apply(event),
			() -> condition.check(event.getPlayer()),
			check -> type.apply(event, check, customActionData)
		);
	}

	@Override
	public ModifierResult apply(LivingEntity caster, SpellData data) {
		return applyResult(
			data,
			mod -> mod.apply(caster, data),
			() -> condition.check(caster),
			result -> type.apply(caster, result, customActionData)
		);
	}

	@Override
	public ModifierResult apply(LivingEntity caster, LivingEntity target, SpellData data) {
		return applyResult(
			data,
			mod -> mod.apply(caster, target, data),
			() -> condition.check(caster, target),
			result -> type.apply(caster, target, result, customActionData)
		);
	}

	@Override
	public ModifierResult apply(LivingEntity caster, Location target, SpellData data) {
		return applyResult(
			data,
			mod -> mod.apply(caster, target, data),
			() -> condition.check(caster, target),
			result -> type.apply(caster, target, result, customActionData)
		);
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(() -> condition.check(livingEntity), this::checkCondition);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity entity) {
		return check(() -> condition.check(livingEntity, entity), this::checkCondition);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return check(() -> condition.check(livingEntity, location), this::checkCondition);
	}

	private boolean applyEvent(Predicate<IModifier> modifier, BooleanSupplier condition, BooleanPredicate action) {
		try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Checking modifier '%s'.", text)) {
			boolean check;
			try (var ignored1 = MagicDebug.section("Checking modifier condition...")) {
				check = this.condition instanceof IModifier mod ? modifier.test(mod) : condition.getAsBoolean();
				if (negated) check = !check;

				if (check) MagicDebug.info("Modifier condition passed.");
				else MagicDebug.info("Modifier condition failed.");
			}

			return action.test(check);
		}
	}

	private ModifierResult applyResult(SpellData data, Function<IModifier, ModifierResult> modifier, BooleanSupplier condition, UnaryOperator<ModifierResult> action) {
		try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Checking modifier '%s'.", text)) {
			ModifierResult result;
			try (var ignored1 = MagicDebug.section("Checking modifier condition...")) {
				if (condition instanceof IModifier mod) {
					result = modifier.apply(mod);
					if (negated) result = new ModifierResult(result.data(), !result.check());
				} else {
					boolean check = condition.getAsBoolean();
					if (negated) check = !check;

					result = new ModifierResult(data, check);
				}

				if (result.check()) MagicDebug.info("Modifier condition passed.");
				else MagicDebug.info("Modifier condition failed.");
			}

			return action.apply(result);
		}
	}

	private boolean check(BooleanSupplier condition, BooleanPredicate action) {
		try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Checking modifier '%s'.", text)) {
			boolean check;
			try (var ignored1 = MagicDebug.section("Checking modifier condition...")) {
				check = condition.getAsBoolean();
				if (negated) check = !check;

				if (check) MagicDebug.info("Modifier condition passed.");
				else MagicDebug.info("Modifier condition failed.");
			}

			return action.test(check);
		}
	}

	private boolean checkCondition(boolean check) {
		try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing simplified '%s' modifier action check.", type.name().toLowerCase())) {
			return switch (type) {
				case REQUIRED -> {
					if (check) MagicDebug.info("Condition passed - continuing.");
					else MagicDebug.info("Condition failed - stopping.");

					yield check;
				}
				case DENIED -> {
					if (check) MagicDebug.info("Condition passed - stopping.");
					else MagicDebug.info("Condition failed - continuing.");

					yield !check;
				}
				default -> {
					MagicDebug.info("Invalid modifier action - continuing.");
					yield true;
				}
			};
		}
	}

	private static ModifierType getTypeByName(String name) {
		return ModifierType.getModifierTypeByName(name);
	}

}
