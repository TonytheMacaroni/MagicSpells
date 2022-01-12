package com.nisovin.magicspells.castmodifiers.customdata;

import org.apache.commons.math3.util.Pair;

import org.bukkit.entity.LivingEntity;

import de.slikey.exp4j.Expression;

import com.nisovin.magicspells.events.*;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.FunctionData;

public class CustomDataFloat extends CustomData {

	private String invalidText;
	private boolean isValid = false;
	private ConfigData<Float> customData;

	public CustomDataFloat(String data) {
		if (data == null) {
			invalidText = "Number was not defined.";
			return;
		}

		Pair<Expression, Boolean> expression = FunctionData.buildExpression(data);
		if (expression == null) {
			invalidText = "Number or function is invalid.";
			return;
		}

		customData = new FunctionData.FloatData(expression.getFirst(), 0f, expression.getSecond());
		isValid = true;
	}

	@Override
	public boolean isValid() {
		return isValid;
	}

	@Override
	public String getInvalidText() {
		return invalidText;
	}

	public float get(LivingEntity caster, LivingEntity target, float power, String[] args) {
		return customData.get(caster, target, power, args);
	}

	public static float from(CustomData data, SpellCastEvent event) {
		return ((CustomDataFloat) data).get(event.getCaster(), null, event.getPower(), event.getSpellArgs());
	}

	public static float from(CustomData data, ManaChangeEvent event) {
		return ((CustomDataFloat) data).get(event.getPlayer(), null, 1f, null);
	}

	public static float from(CustomData data, SpellTargetEvent event) {
		return ((CustomDataFloat) data).get(event.getCaster(), event.getTarget(), event.getPower(), event.getSpellArgs());
	}

	public static float from(CustomData data, SpellTargetLocationEvent event) {
		return ((CustomDataFloat) data).get(event.getCaster(), null, event.getPower(), event.getSpellArgs());
	}

	public static float from(CustomData data, MagicSpellsGenericPlayerEvent event) {
		return ((CustomDataFloat) data).get(event.getPlayer(), null, 1f, null);
	}

}
