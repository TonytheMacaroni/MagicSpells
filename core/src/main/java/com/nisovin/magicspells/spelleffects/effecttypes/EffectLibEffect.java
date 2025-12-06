package com.nisovin.magicspells.spelleffects.effecttypes;

import java.awt.Font;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.lang.reflect.Field;

import com.google.common.base.CaseFormat;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.util.CustomSound;
import de.slikey.effectlib.effect.ModifiedEffect;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

@Name("effectlib")
public class EffectLibEffect extends SpellEffect {

	private static final Set<String> CLASS_STRINGS = Set.of("class", "effectClass", "subEffectClass", "subEffectAtEndClass", "subEffectAtEndCachedClass");

	protected EffectManager manager;
	protected String className;

	protected ConfigurationSection effectLibSection;
	protected Map<String, ConfigData<?>> options;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		effectLibSection = config.getConfigurationSection("effectlib");
		if (effectLibSection == null) return;

		className = effectLibSection.getString("class");
		manager = MagicSpells.getEffectManager();

		Effect effect = manager.getEffectByClassName(className);
		if (effect == null) {
			MagicDebug.warn("Invalid EffectLib effect class '%s' defined %s.", className, MagicDebug.resolveFullPath());
			return;
		}
		Class<? extends Effect> effectClass = effect.getClass();

		options = new HashMap<>();
		resolveOptions(effectLibSection, effectClass, options, "");
		if (options.isEmpty()) options = null;
	}

	private void resolveOptions(ConfigurationSection section, Class<?> effectClass, Map<String, ConfigData<?>> options, String path) {
		Set<String> keys = section.getKeys(false);
		for (String actualKey : keys) {
			if (!section.isString(actualKey) && !section.isConfigurationSection(actualKey)) continue;

			String key = formatKey(actualKey);
			if (CLASS_STRINGS.contains(key)) continue;

			Field field;
			try {
				field = effectClass.getField(key);
			} catch (NoSuchFieldException e) {
				MagicDebug.warn("Invalid effect option %s.", MagicDebug.resolvePath(section, actualKey));
				continue;
			}

			Class<?> type = field.getType();
			if (type.equals(int.class) || type.equals(Integer.class) || type.equals(byte.class) || type.equals(Byte.class) || type.equals(short.class) || type.equals(Short.class))
				options.put(path + actualKey, ConfigDataUtil.getInteger(section, actualKey, 0));
			else if (type.equals(long.class) || type.equals(Long.class))
				options.put(path + actualKey, ConfigDataUtil.getLong(section, actualKey, 0));
			else if (type.equals(float.class) || type.equals(Float.class) || type.equals(double.class) || type.equals(Double.class))
				options.put(path + actualKey, ConfigDataUtil.getDouble(section, actualKey, 0));
			else if (type.equals(boolean.class) || type.equals(Boolean.class))
				options.put(path + actualKey, ConfigDataUtil.getBoolean(section, actualKey, false));
			else if (Enum.class.isAssignableFrom(type)
				|| type.equals(String.class)
				|| type.equals(Color.class)
				|| Font.class.isAssignableFrom(type)
				|| CustomSound.class.isAssignableFrom(type)
				|| BlockData.class.isAssignableFrom(type)) {

				ConfigData<?> data = ConfigDataUtil.getString(section, actualKey, null);
				if (!data.isConstant()) options.put(path + actualKey, data);
			} else if (ConfigurationSection.class.isAssignableFrom(type)) {
				ConfigurationSection subSection = section.getConfigurationSection(actualKey);
				if (subSection == null) continue;

				if (key.equals("effect") && ModifiedEffect.class.isAssignableFrom(effectClass)) {
					String classString = findStringByFormattedKey(section, "effectClass");
					if (classString == null) classString = subSection.getString("class");
					if (classString == null) continue;

					Effect effect = manager.getEffectByClassName(classString);
					if (effect == null) continue;

					resolveOptions(subSection, effect.getClass(), options, path + actualKey + ".");
					continue;
				}

				String classString = findStringByFormattedKey(subSection, key + "Class");
				if (classString == null) continue;

				Effect effect = manager.getEffectByClassName(classString);
				if (effect == null) continue;

				resolveOptions(subSection, effect.getClass(), options, path + actualKey + ".");
			}

			if (options.containsKey(actualKey)) effectLibSection.set(actualKey, null);
		}
	}

	private String formatKey(String key) {
		if (key.contains("-")) key = key.replace("-", "_");
		if (key.contains("_")) key = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, key);

		return key;
	}

	private String findStringByFormattedKey(ConfigurationSection section, String formattedKey) {
		String ret = section.getString(formattedKey);
		if (ret != null) return ret;

		for (String key : section.getKeys(false))
			if (formatKey(key).equals(formattedKey))
				return section.getString(key);

		return null;
	}

	@Override
	protected Runnable playEffectLocation(Location location, SpellData data) {
		if (!initialize()) return null;
		manager.start(className, getParameters(data), location);
		return null;
	}

	@Override
	protected Effect playEffectLibLocation(Location location, SpellData data) {
		if (!initialize()) return null;
		return manager.start(className, getParameters(data), location);
	}

	protected boolean initialize() {
		updateManager();
		if (manager.getEffects().size() >= MagicSpells.getEffectlibInstanceLimit()) {
			if (MagicSpells.shouldTerminateEffectlibEffects()) {
				MagicSpells.resetEffectlib();
				updateManager();
			} else return false;
		}
		return true;
	}

	protected void updateManager() {
		if (manager == null || manager.isDisposed()) manager = MagicSpells.getEffectManager();
	}

	protected ConfigurationSection getParameters(SpellData data) {
		if (options == null || effectLibSection == null) return effectLibSection;

		ConfigurationSection parameters = new MemoryConfiguration();
		for (String key : effectLibSection.getKeys(true)) {
			Object value = effectLibSection.get(key);
			if (value instanceof ConfigurationSection) continue;

			parameters.set(key, value);
		}

		for (Map.Entry<String, ConfigData<?>> option : options.entrySet())
			parameters.set(option.getKey(), option.getValue().get(data));

		return parameters;
	}

}
