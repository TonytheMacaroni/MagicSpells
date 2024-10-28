package com.nisovin.magicspells.util;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.nio.file.Path;
import java.io.FilenameFilter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.debug.MagicDebug;

public class MagicConfig {

	private static final FilenameFilter FILENAME_FILTER = (File dir, String name) -> name.startsWith("spell") && name.endsWith(".yml");
	private static final FilenameFilter DIRECTORY_FILTER = (File dir, String name) -> name.startsWith("spells");

	private final Map<String, String> spellFiles;
	private final Map<String, String> recipeFiles;
	private final Map<String, String> modifierFiles;
	private final Map<String, String> variableFiles;
	private final Map<String, String> magicItemFiles;

	private YamlConfiguration mainConfig;
	private YamlConfiguration defaultSpellConfig;

	public MagicConfig() {
		magicItemFiles = new HashMap<>();
		spellFiles = new HashMap<>();
		recipeFiles = new HashMap<>();
		modifierFiles = new HashMap<>();
		variableFiles = new HashMap<>();

		try {
			File folder = MagicSpells.getInstance().getDataFolder();

			// Load main config
			mainConfig = new YamlConfiguration();
			if (!mainConfig.contains("general")) mainConfig.createSection("general");
			if (!mainConfig.contains("mana")) mainConfig.createSection("mana");
			if (!mainConfig.contains("spells")) mainConfig.createSection("spells");

			// Load general
			File generalConfigFile = new File(folder, "general.yml");
			if (generalConfigFile.exists()) {
				YamlConfiguration generalConfig = new YamlConfiguration();
				try {
					generalConfig.load(generalConfigFile);

					Set<String> keys = generalConfig.getKeys(false);

					for (String key : keys) {
					    Category category = switch (key) {
							case "magic-items" -> Category.MAGIC_ITEMS;
							case "modifiers" -> Category.MODIFIERS;
							case "recipes" -> Category.RECIPES;
							case "variables" -> Category.VARIABLES;
							default -> null;
						};

						if (category != null) {
							initSection(category, generalConfig, "general.yml");
							continue;
						}

						setOrCreateSection(generalConfig, mainConfig, key, "general." + key);
					}
				} catch (Exception e) {
					MagicDebug.error(e, "Encountered an exception while loading 'general.yml'.");
				}
			}

			// Load mana
			File manaConfigFile = new File(folder, "mana.yml");
			if (manaConfigFile.exists()) {
				YamlConfiguration manaConfig = new YamlConfiguration();

				try {
					manaConfig.load(manaConfigFile);

					for (String key : manaConfig.getKeys(false))
						setOrCreateSection(manaConfig, mainConfig, key, "mana." + key);
				} catch (Exception e) {
					MagicDebug.error(e, "Encountered an exception while loading 'mana.yml'.");
				}
			}

			// Load no magic zones
			File zonesConfigFile = new File(folder, "zones.yml");
			if (zonesConfigFile.exists()) {
				YamlConfiguration zonesConfig = new YamlConfiguration();

				try {
					zonesConfig.load(zonesConfigFile);

					for (String key : zonesConfig.getKeys(false))
						setOrCreateSection(zonesConfig, mainConfig, key, "no-magic-zones." + key);
				} catch (Exception e) {
					MagicDebug.error(e, "Encountered an exception while loading 'zones.yml'.");
				}
			}

			// Load default spell values
			File defaultsConfigFile = new File(folder, "defaults.yml");
			if (defaultsConfigFile.exists()) {
				defaultSpellConfig = new YamlConfiguration();

				try {
					defaultSpellConfig.load(defaultsConfigFile);
				} catch (Exception e) {
					MagicDebug.error(e, "Encountered an exception while loading 'defaults.yml'.");
				}
			}

			// Load spell folders
			for (File directoryFile : folder.listFiles(DIRECTORY_FILTER)) {
				if (!directoryFile.isDirectory()) continue;
				for (File spellConfigFile : directoryFile.listFiles(FILENAME_FILTER)) {
					loadSpellFiles(spellConfigFile);
				}
			}

			// load spell configs
			for (File spellConfigFile : folder.listFiles(FILENAME_FILTER)) {
				loadSpellFiles(spellConfigFile);
			}

			// Load mini configs
			File spellConfigsFolder = new File(folder, "spellconfigs");
			if (spellConfigsFolder.exists()) loadSpellConfigs(spellConfigsFolder);
		} catch (Exception ex) {
			MagicDebug.error(ex, "Encountered an exception while loading config files.");
		}
	}

	private void loadSpellFiles(File spellFile) {
		String fileName = MagicSpells.getInstance().getDataFolder().toPath().relativize(spellFile.toPath()).toString();
		YamlConfiguration config = new YamlConfiguration();

		try {
			config.load(spellFile);

			for (String key : config.getKeys(false)) {
				Category category = switch (key) {
					case "magic-items" -> Category.MAGIC_ITEMS;
					case "modifiers" -> Category.MODIFIERS;
					case "recipes" -> Category.RECIPES;
					case "variables" -> Category.VARIABLES;
					default -> Category.SPELLS;
				};

				if (category == Category.SPELLS) {
					mainConfig.set("spells." + key, config.get(key));
					spellFiles.put(key, fileName);
					continue;
				}

				initSection(category, config, fileName);
			}
		} catch (Exception e) {
			MagicDebug.error(e, "Encountered error while reading spell file '%s'.", fileName);
		}
	}

	private void initSection(Category category, YamlConfiguration config, String fileName) {
		ConfigurationSection general = mainConfig.getConfigurationSection(category.key);
		if (general == null) general = mainConfig.createSection(category.key);

		ConfigurationSection section = config.getConfigurationSection(category.section);
		if (section == null) {
			MagicDebug.warn("Invalid '%s' section found in '%s'.", category.section, fileName);
			return;
		}

		for (String internalName : section.getKeys(false)) {
			if (general.isSet(internalName)) {
				MagicDebug.warn("Found %s with duplicate internal name '%s' in '%s'.",  category.type, internalName, fileName);
				continue;
			}

			setOrCreateSection(section, general, internalName, internalName);
			setFile(category, internalName, fileName);
		}
	}

	private void loadSpellConfigs(File folder) {
		File[] files = folder.listFiles();
		if (files == null || files.length == 0) return;

		Path dataFolder = MagicSpells.getInstance().getDataFolder().toPath();
		YamlConfiguration config = new YamlConfiguration();

		for (File file : files) {
		    if (file.isDirectory()) {
				loadSpellConfigs(folder);
				continue;
			}

			String name = file.getName();
			if (!name.endsWith(".yml")) continue;

			String path = dataFolder.relativize(file.toPath()).toString();
			name = name.substring(0, name.length() - 4);

			try {
				config.load(file);

				ConfigurationSection section = mainConfig.createSection(name);
				for (String key : config.getKeys(false))
					setOrCreateSection(config, section, key, key);

				spellFiles.put(name, path);
			} catch (Exception e) {
				MagicDebug.error(e, "Encountered error while reading spell config file '%s'.", path);
			}
		}
	}

	private void setOrCreateSection(ConfigurationSection source, ConfigurationSection destination, String sourcePath, String destinationPath) {
		Object value = source.get(sourcePath);

		if (value instanceof ConfigurationSection section)
			destination.createSection(destinationPath, section.getValues(true));
		else
			destination.set(destinationPath, value);
	}

	public YamlConfiguration getMainConfig() {
		return mainConfig;
	}

	public boolean isLoaded() {
		return mainConfig.contains("general") && mainConfig.contains("spells");
	}

	public boolean contains(String path) {
		return mainConfig.contains(path);
	}

	public boolean isInt(String path) {
		return mainConfig.isInt(path);
	}

	public int getInt(String path) {
		return mainConfig.getInt(path);
	}

	public int getInt(String path, int def) {
		return mainConfig.getInt(path, def);
	}

	public boolean isLong(String path) {
		return mainConfig.isLong(path);
	}

	public long getLong(String path, long def) {
		return mainConfig.getLong(path, def);
	}

	public boolean isDouble(String path) {
		return mainConfig.isInt(path) || mainConfig.isDouble(path);
	}

	public double getDouble(String path) {
		if (mainConfig.contains(path) && mainConfig.isInt(path)) return mainConfig.getInt(path);
		return mainConfig.getDouble(path);
	}

	public double getDouble(String path, double def) {
		if (mainConfig.contains(path) && mainConfig.isInt(path)) return mainConfig.getInt(path);
		return mainConfig.getDouble(path, def);
	}

	public boolean isBoolean(String path) {
		return mainConfig.isBoolean(path);
	}

	public boolean getBoolean(String path, boolean def) {
		return mainConfig.getBoolean(path, def);
	}

	public boolean isString(String path) {
		return mainConfig.contains(path) && mainConfig.isString(path);
	}

	public String getString(String path, String def) {
		if (!mainConfig.contains(path)) return def;
		return mainConfig.get(path).toString();
	}

	public boolean isList(String path) {
		return mainConfig.contains(path) && mainConfig.isList(path);
	}

	public List<Integer> getIntList(String path, List<Integer> def) {
		if (!mainConfig.contains(path)) return def;
		return mainConfig.getIntegerList(path);
	}

	public List<Byte> getByteList(String path, List<Byte> def) {
		if (!mainConfig.contains(path)) return def;
		return mainConfig.getByteList(path);
	}

	public List<String> getStringList(String path, List<String> def) {
		if (!mainConfig.contains(path)) return def;
		return mainConfig.getStringList(path);
	}

	public List<?> getList(String path, List<?> def) {
		if (!mainConfig.contains(path)) return def;
		return mainConfig.getList(path);
	}

	public Set<String> getKeys(String path) {
		if (!mainConfig.contains(path)) return null;
		if (!mainConfig.isConfigurationSection(path)) return null;
		return mainConfig.getConfigurationSection(path).getKeys(false);
	}

	public boolean isSection(String path) {
		return mainConfig.isConfigurationSection(path);
	}

	public ConfigurationSection getSection(String path) {
		return mainConfig.getConfigurationSection(path);
	}

	public Set<String> getSpellKeys() {
		if (mainConfig == null) return null;
		if (!mainConfig.contains("spells")) return null;
		if (!mainConfig.isConfigurationSection("spells")) return null;
		return mainConfig.getConfigurationSection("spells").getKeys(false);
	}

	public ConfigurationSection getDefaults(Class<?> type) {
		if (defaultSpellConfig == null) return null;

		String spellClass = type.getCanonicalName();

		ConfigurationSection defaults = defaultSpellConfig.getConfigurationSection(spellClass);
		if (defaults != null || !spellClass.startsWith("com.nisovin.magicspells.spells"))
			return defaults;

		// Check shortened spell class.
		return defaultSpellConfig.getConfigurationSection(spellClass.substring(30));
	}

	public String getSpellFile(Spell spell) {
		return spellFiles.get(spell.getInternalName());
	}

	public String getFile(Category category, String internalName) {
		Map<String, String> map = switch (category) {
			case MAGIC_ITEMS -> magicItemFiles;
			case MODIFIERS -> modifierFiles;
			case RECIPES -> recipeFiles;
			case VARIABLES -> variableFiles;
			case SPELLS -> spellFiles;
		};

		return map.get(internalName);
	}

	private void setFile(Category category, String internalName, String fileName) {
		Map<String, String> map = switch (category) {
			case MAGIC_ITEMS -> magicItemFiles;
			case MODIFIERS -> modifierFiles;
			case RECIPES -> recipeFiles;
			case VARIABLES -> variableFiles;
			case SPELLS -> spellFiles;
		};

		map.put(internalName, fileName);
	}

	public enum Category {

		MAGIC_ITEMS("general.magic-items", "magic-items", "magic item"),
		MODIFIERS("general.modifiers", "modifiers", "modifier"),
		RECIPES("general.recipes", "recipes", "recipe"),
		VARIABLES("general.variables", "variables", "variable"),
		SPELLS("spells", "spells", "spell");

		private final String section;
		private final String type;
		private final String key;

		Category(String key, String section, String type) {
			this.key = key;
			this.type = type;
			this.section = section;
		}

		public String getSection() {
			return section;
		}

		public String getType() {
			return type;
		}

		public String getKey() {
			return key;
		}

	}

}
