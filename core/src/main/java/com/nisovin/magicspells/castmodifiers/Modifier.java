package com.nisovin.magicspells.castmodifiers;

import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.castmodifiers.customdata.CustomData;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

public class Modifier implements IModifier {

	private static final Pattern MODIFIER_STR_FAILED_PATTERN = Pattern.compile("\\$\\$");

	private Condition condition;
	private ModifierType type;

	private String modifierVar;
	private String strModifierFailed;

	private CustomData customActionData;

	// Is this a condition that will want to access the events directly?
	private boolean alertCondition = false;

	private boolean negated = false;
	private boolean initialized = false;

	public Modifier() {

	}

	public boolean process(String string) {
		if (MagicSpells.getConditionManager() == null) return false;

		String[] s = RegexUtil.split(MODIFIER_STR_FAILED_PATTERN, string, 0);
		if (s == null || s.length <= 0) return false;
		String[] data = s[0].trim().split(" ", 4);

		if (data.length < 2) return false;

		// Get condition
		if (data[0].startsWith("!")) {
			negated = true;
			data[0] = data[0].substring(1);
		}

		condition = MagicSpells.getConditionManager().getConditionByName(data[0].replace("_", ""));
		if (condition == null) return false;

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

		// Check for the alert condition
		if (condition instanceof IModifier) alertCondition = true;

		initialized = true;
		return true;
	}

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
		LivingEntity caster = event.getCaster();
		boolean check;
		if (alertCondition) check = ((IModifier) condition).apply(event);
		else check = condition.check(caster);
		if (negated) check = !check;
		return type.apply(event, check, customActionData);
	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		Player player = event.getPlayer();
		boolean check;
		if (alertCondition) check = ((IModifier) condition).apply(event);
		else check = condition.check(player);
		if (negated) check = !check;
		return type.apply(event, check, customActionData);
	}

	@Override
	public boolean apply(SpellTargetEvent event) {
		LivingEntity caster = event.getCaster();
		boolean check;
		if (alertCondition) check = ((IModifier) condition).apply(event);
		else check = condition.check(caster, event.getTarget());
		if (negated) check = !check;
		return type.apply(event, check, customActionData);
	}

	@Override
	public boolean apply(SpellTargetLocationEvent event) {
		LivingEntity caster = event.getCaster();
		boolean check;
		if (alertCondition) check = ((IModifier) condition).apply(event);
		else check = condition.check(caster, event.getTargetLocation());
		if (negated) check = !check;
		return type.apply(event, check, customActionData);
	}

	@Override
	public boolean apply(MagicSpellsGenericPlayerEvent event) {
		boolean check = condition.check(event.getPlayer());
		if (negated) check = !check;
		return type.apply(event, check, customActionData);
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		boolean check = condition.check(livingEntity);
		return checkCondition(check);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity entity) {
		boolean check = condition.check(livingEntity, entity);
		return checkCondition(check);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		boolean check = condition.check(livingEntity, location);
		return checkCondition(check);
	}

	private boolean checkCondition(boolean check) {
		if (negated) check = !check;
		if (!check && type == ModifierType.REQUIRED) return false;
		if (check && type == ModifierType.DENIED) return false;
		return true;
	}

	private static ModifierType getTypeByName(String name) {
		return ModifierType.getModifierTypeByName(name);
	}

}
