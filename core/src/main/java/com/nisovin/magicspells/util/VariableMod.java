package com.nisovin.magicspells.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Function;
import java.util.function.BinaryOperator;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.FunctionData;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

public class VariableMod {

	public enum VariableOwner {
		CASTER,
		TARGET
	}

	public enum Operation {

		SET((a, b) -> b),
		ADD(Double::sum),
		MULTIPLY((a, b) -> a * b),
		DIVIDE((a, b) -> a / b),
		MODULO((a, b) -> a % b),
		POWER(AccurateMath::pow),
		RANDOM((a, b) -> ThreadLocalRandom.current().nextDouble() * b);

		private final BinaryOperator<Double> operator;

		Operation(BinaryOperator<Double> operator) {
			this.operator = operator;
		}

		public double applyTo(double arg1, double arg2) {
			return operator.apply(arg1, arg2);
		}

		static Operation fromPrefix(String s) {
			char c = s.charAt(0);
			return switch (c) {
				case '=' -> SET;
				case '*' -> MULTIPLY;
				case '/' -> DIVIDE;
				case '^' -> POWER;
				case '%' -> MODULO;
				case '?' -> RANDOM;
				default -> ADD;
			};
		}

	}

	private static final Pattern VARIABLE_MATCHER = Pattern.compile("-?(caster:|target:)?(\\w+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern OPERATION_MATCHER = Pattern.compile("^[=+*/^%?]");

	private VariableOwner variableOwner = VariableOwner.CASTER;
	private String modifyingVariableName = null;
	private ConfigData<Double> functionModifier;
	private double constantModifier;
	private boolean negate = false;
	private String value;
	private Operation op;
	private String text;

	public VariableMod(String data) {
		text = data;
		op = Operation.fromPrefix(data);
		data = OPERATION_MATCHER.matcher(data).replaceFirst("");

		value = data;

		if (RegexUtil.DOUBLE_PATTERN.asMatchPredicate().test(data)) {
			if (data.startsWith("-")) {
				data = data.substring(1);
				negate = true;
			}

			constantModifier = Double.parseDouble(data);
		} else {
			// If it isn't a double, then let's match it as a variable
			Matcher matcher = VARIABLE_MATCHER.matcher(data);
			if (matcher.matches()) {
				if (data.startsWith("-")) negate = true;

				String owner = matcher.group(1);
				if (owner != null && owner.equalsIgnoreCase("target:")) variableOwner = VariableOwner.TARGET;

				modifyingVariableName = matcher.group(2);
			} else functionModifier = FunctionData.build(data, Function.identity(), 0d, true);
		}
	}

	@Deprecated
	public double getValue(Player caster, Player target) {
		return getValue(new SpellData(caster, target, 1f, null));
	}

	@Deprecated
	public double getValue(Player caster, Player target, double baseValue) {
		return getValue(new SpellData(caster, target, 1f, null), baseValue);
	}

	@Deprecated
	public String getStringValue(Player caster, Player target) {
		return getStringValue(new SpellData(caster, target, 1f, null));
	}

	public double getValue(SpellData data) {
		int negationFactor = negate ? -1 : 1;
		if (modifyingVariableName != null) {
			LivingEntity owner = variableOwner == VariableOwner.CASTER ? data.caster() : data.target();
			Player variableHolder = owner instanceof Player p ? p : null;

			return MagicSpells.getVariableManager().getValue(modifyingVariableName, variableHolder) * negationFactor;
		}

		if (functionModifier != null) return functionModifier.get(data);

		return constantModifier * negationFactor;
	}

	public double getValue(SpellData data, double baseValue) {
		double secondValue = getValue(data);
		return getOperation().applyTo(baseValue, secondValue);
	}

	public String getStringValue(SpellData data) {
		return MagicSpells.doReplacements(value, data);
	}

	public String getValue() {
		return value;
	}

	public boolean isConstantValue() {
		return modifyingVariableName == null && functionModifier == null;
	}

	public Operation getOperation() {
		return op;
	}

	public VariableOwner getVariableOwner() {
		return variableOwner;
	}

	@Override
	public String toString() {
		return text;
	}

}
