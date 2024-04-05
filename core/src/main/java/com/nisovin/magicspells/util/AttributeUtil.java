package com.nisovin.magicspells.util;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Registry;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;

import static org.bukkit.attribute.AttributeModifier.Operation;

public class AttributeUtil {

	private static final Map<String, Attribute> attributeNameMap = new HashMap<>();
	private static final Map<String, Operation> operationNameMap = new HashMap<>();

	static {
		for (Operation operation : Operation.values()) {
			String name = operation.name().toLowerCase();

			operationNameMap.put(name, operation);
			operationNameMap.put(name.replaceAll("_", ""), operation);
		}

		operationNameMap.put("multiply_scalar", Operation.MULTIPLY_SCALAR_1);
		operationNameMap.put("multiplyscalar", Operation.MULTIPLY_SCALAR_1);
		operationNameMap.put("add", Operation.ADD_NUMBER);
		operationNameMap.put("multiply_base", Operation.ADD_SCALAR);
		operationNameMap.put("multiply", Operation.MULTIPLY_SCALAR_1);
		operationNameMap.put("multiply_total", Operation.MULTIPLY_SCALAR_1);

		operationNameMap.put("add", Operation.ADD_NUMBER);
		operationNameMap.put("multiply_base", Operation.ADD_SCALAR);
		operationNameMap.put("multiply", Operation.MULTIPLY_SCALAR_1);
		operationNameMap.put("multiply_total", Operation.MULTIPLY_SCALAR_1);

		operationNameMap.put("add_value", Operation.ADD_NUMBER);
		operationNameMap.put("add_multiplied_base", Operation.ADD_SCALAR);
		operationNameMap.put("add_multiplied_total", Operation.MULTIPLY_SCALAR_1);

		for (Attribute attribute : Attribute.values()) {
			String key = attribute.getKey().getKey();
			String rep = key.replaceAll("_", "");

			attributeNameMap.put(attribute.name().toLowerCase(), attribute);

			attributeNameMap.put(key, attribute);
			attributeNameMap.put(key.substring(key.indexOf('.') + 1), attribute);

			attributeNameMap.put(rep, attribute);
			attributeNameMap.put(rep.substring(rep.indexOf('.') + 1), attribute);
		}
	}

	public static Attribute getAttribute(String attribute) {
		Attribute attr = attributeNameMap.get(attribute.toLowerCase());
		if (attr != null) return attr;

		NamespacedKey key = NamespacedKey.fromString(attribute);
		if (key == null) return null;

		return Registry.ATTRIBUTE.get(key);
	}

	public static Operation getOperation(String operation) {
		return operationNameMap.get(operation.toLowerCase());
	}

	public static String getOperationName(Operation operation) {
		return switch (operation) {
			case ADD_NUMBER -> "add_value";
			case ADD_SCALAR -> "add_multiplied_base";
			case MULTIPLY_SCALAR_1 -> "add_multiplied_total";
		};
	}

}
