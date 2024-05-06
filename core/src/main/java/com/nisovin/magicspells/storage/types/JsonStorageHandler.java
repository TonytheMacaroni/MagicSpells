package com.nisovin.magicspells.storage.types;

import java.util.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.function.Supplier;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.storage.StorageHandler;
import com.nisovin.magicspells.Spellbook.ItemBindings;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

public class JsonStorageHandler implements StorageHandler {

	private final Gson gson = new Gson();

	@Override
	public void load(Spellbook spellbook) {
		Player player = spellbook.getPlayer();
		UUID uuid = player.getUniqueId();

		Path dataFolder = null;
		Path spellbookFolder = null;
		Path spellbookPath = null;

		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Loading spellbook of UUID '%s' from JSON.", player.getUniqueId())) {
			dataFolder = MagicSpells.getInstance().getDataFolder().toPath();
			spellbookFolder = dataFolder.resolve("spellbooks");

			if (MagicSpells.arePlayerSpellsSeparatedPerWorld()) {
				String worldName = player.getWorld().getName();
				spellbookFolder = spellbookFolder.resolve(worldName);
			}

			spellbookPath = spellbookFolder.resolve(uuid + ".json");
			if (Files.notExists(spellbookPath)) {
				Path legacySpellbookPath = spellbookFolder.resolve(uuid.toString().replace("-", "") + ".txt");
				if (Files.notExists(legacySpellbookPath)) {
					legacySpellbookPath = spellbookFolder.resolve(player.getName().toLowerCase() + ".txt");

					if (Files.notExists(legacySpellbookPath)) {
						MagicDebug.info("No spellbook file found matching UUID '%s'.", uuid);
						return;
					}
				}

				loadLegacy(spellbook, legacySpellbookPath);
				return;
			}

			Path finalDataFolder = dataFolder, finalSpellbookPath = spellbookPath;
			try (var ignored1 = MagicDebug.section("Loading spellbook from file '%s'.", (Supplier<String>) () -> finalDataFolder.relativize(finalSpellbookPath).toString())) {
				JsonObject spellbookObject;
				try (BufferedReader reader = Files.newBufferedReader(spellbookPath, StandardCharsets.UTF_8)) {
					spellbookObject = JsonParser.parseReader(reader).getAsJsonObject();
				}

				spells:
				try (var ignored2 = MagicDebug.section("Loading spells.")) {
					JsonArray spellsArray = spellbookObject.get("spells").getAsJsonArray();
					if (spellsArray.isEmpty()) {
						MagicDebug.info("No spells found.");
						break spells;
					}

					for (JsonElement element : spellsArray) {
						String spellName = element.getAsString();

						Spell spell = MagicSpells.getSpellByInternalName(spellName);
						if (spell == null) {
							MagicDebug.info("Skipping invalid spell '%s'.", spellName);
							continue;
						}

						spellbook.addSpell(spell);
					}
				}

				bindings:
				try (var ignored2 = MagicDebug.section("Loading spell bindings.")) {
					JsonObject bindings = spellbookObject.get("bindings").getAsJsonObject();
					if (bindings.isEmpty()) {
						MagicDebug.info("No bindings found.");
						break bindings;
					}

					for (String castItemString : bindings.keySet()) {
						try (var ignored3 = MagicDebug.section("Adding bindings for cast item '%s'.", castItemString)) {
							CastItem castItem;
							try (var ignored4 = MagicDebug.section("Parsing cast item '%s'.", castItemString)) {
								castItem = new CastItem(castItemString);
							}
							MagicDebug.info("Parsed cast item as '%s'.", castItem);

							JsonArray boundSpellsArray = bindings.get(castItemString).getAsJsonArray();

							List<Spell> boundSpells = new ArrayList<>();
							for (JsonElement element : boundSpellsArray) {
								String spellName = element.getAsString();

								Spell spell = MagicSpells.getSpellByInternalName(spellName);
								if (spell == null) {
									MagicDebug.info("Skipping invalid spell '%s'.", spellName);
									continue;
								}

								MagicDebug.info("Loaded bound spell of '%s'.", spellName);
								boundSpells.add(spell);
							}

							if (boundSpells.isEmpty()) {
								MagicDebug.info("No bound spells loaded - skipping.");
								continue;
							}

							spellbook.setCustomBindings(castItem, boundSpells);
						}
					}
				}
			}
		} catch (Exception e) {
			MagicDebug.error(DebugCategory.SPELLBOOK, e, "Encountered an error while attempting to load spellbook for player '%s'.", player.getName());

			if (spellbookPath != null && Files.exists(spellbookPath)) {
				try {
					Path backupFolder = spellbookFolder.resolve("backups");
					MagicDebug.error("Saving backup of invalid spellbook for player '%s' to folder '%s'", player.getName(), dataFolder.relativize(backupFolder));

					Files.createDirectories(backupFolder);
					Files.copy(spellbookPath, backupFolder.resolve(spellbookPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception ex) {
					MagicDebug.error(DebugCategory.SPELLBOOK, e, "Encountered an error while attempting to create backup for invalid spellbook for player '%s'.", player.getName());
				}
			}
		}
	}

	private void loadLegacy(Spellbook spellbook, Path path) {
		Player player = spellbook.getPlayer();

		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Loading legacy text file spell book.")) {
			try (Scanner scanner = new Scanner(path.toFile(), StandardCharsets.UTF_8)) {
				while (scanner.hasNext()) {
					String line = scanner.nextLine();
					if (line.isEmpty()) continue;

					String[] data = line.split(":", 2);

					Spell spell = MagicSpells.getSpellByInternalName(data[0]);
					if (spell == null) {
						MagicDebug.info("Skipping invalid spell '%s'.", line);
						continue;
					}

					try (var ignored1 = MagicDebug.section(spell.getDebugConfig(), "Adding spell '%s'.", data[0])) {
						spellbook.addSpell(spell);

						if (data.length == 1) {
							MagicDebug.info("No cast items found.");
							continue;
						}

						try (var ignored2 = MagicDebug.section("Adding cast items.")) {
							String[] castItemStrings = data[1].split(MagicItemDataParser.DATA_REGEX);

							for (String castItemString : castItemStrings) {
								CastItem item;

								try (var ignored3 = MagicDebug.section("Parsing cast item '%s'.", castItemString)) {
									item = new CastItem(castItemString);
								}

								MagicDebug.info("Added cast item '%s'.", item);
								spellbook.addCustomBinding(item, spell);
							}
						}
					}
				}
			}

			spellbook.sortCustomBindings();

			MagicDebug.info("Saving spellbook to new format.");
			spellbook.save();

			MagicDebug.info("Deleting legacy spellbook file.");
			Files.delete(path);
		} catch (Exception e) {
			MagicDebug.error(DebugCategory.SPELLBOOK, e, "Encountered an error while attempting to load legacy text file spellbook for player '%s'.", player.getName());
		}
	}

	@Override
	public void save(Spellbook spellbook) {
		Player player = spellbook.getPlayer();
		UUID uuid = player.getUniqueId();

		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Saving spellbook of UUID '%s' to JSON.", player.getUniqueId())) {
			Path dataFolder = MagicSpells.getInstance().getDataFolder().toPath();
			Path spellbookFolder = dataFolder.resolve("spellbooks");

			if (MagicSpells.arePlayerSpellsSeparatedPerWorld()) {
				String worldName = player.getWorld().getName();
				spellbookFolder = spellbookFolder.resolve(worldName);
			}

			Path spellbookPath = spellbookFolder.resolve(uuid + ".json");

			MagicDebug.info("Saving spellbook to '%s'.", (Supplier<String>) () -> dataFolder.relativize(spellbookPath).toString());
			Files.createDirectories(spellbookFolder);

			JsonObject spellbookObject = new JsonObject();
			spellbookObject.addProperty("version", 1);

			try (var ignored1 = MagicDebug.section("Saving spells...")){
				JsonArray spellsArray = new JsonArray();

				Collection<Spell> spells = spellbook.getSpells();
				for (Spell spell : spells) {
					String name = spell.getInternalName();

					MagicDebug.info("Saving spell '%s'.", name);
					spellsArray.add(name);
				}

				spellbookObject.add("spells", spellsArray);
			}

			try (var ignored1 = MagicDebug.section("Saving custom spell bindings...")) {
				JsonObject bindingsObject = new JsonObject();

				Map<CastItem, ItemBindings> itemBindings = spellbook.getItemBindings();
				for (Map.Entry<CastItem, ItemBindings> entry : itemBindings.entrySet()) {
					CastItem item = entry.getKey();

					try (var ignored2 = MagicDebug.section("Saving custom spell bindings for cast item '%s'...", item)) {
						List<Spell> customBindings = entry.getValue().getCustomBindings();
						if (customBindings.isEmpty()) {
							MagicDebug.info("No custom spell bindings - skipping.");
							continue;
						}

						JsonArray spellsArray = new JsonArray();
						for (int i = 0; i < customBindings.size(); i++) {
							Spell spell = customBindings.get(i);
							String name = spell.getInternalName();

							MagicDebug.info("Saving spell '%s' to slot %d.", name, i);
							spellsArray.add(name);
						}

						bindingsObject.add(item.toString(), spellsArray);
					}
				}

				spellbookObject.add("bindings", bindingsObject);
			}

			try (BufferedWriter writer = Files.newBufferedWriter(spellbookPath, StandardCharsets.UTF_8)) {
				MagicDebug.info("Saving to file...");
				gson.toJson(spellbookObject, writer);
			}
		} catch (Exception e) {
			MagicDebug.error(DebugCategory.SPELLBOOK, e, "Encountered an error while attempting to save spellbook for player '%s'.", player.getName());
		}
	}

}
