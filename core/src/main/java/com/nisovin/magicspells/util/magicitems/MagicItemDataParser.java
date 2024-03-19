package com.nisovin.magicspells.util.magicitems;

import java.util.*;

import com.google.common.collect.Multimap;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import net.kyori.adventure.text.Component;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.RegistryAccess;

import org.bukkit.*;
import org.bukkit.potion.PotionType;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffect;
import org.bukkit.block.banner.Pattern;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.block.banner.PatternType;
import org.bukkit.attribute.AttributeModifier;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.ColorUtil;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.handlers.EnchantmentHandler;
import com.nisovin.magicspells.util.itemreader.PotionHandler;
import com.nisovin.magicspells.util.itemreader.AttributeHandler;
import com.nisovin.magicspells.util.itemreader.FireworkEffectHandler;
import com.nisovin.magicspells.util.itemreader.SuspiciousStewHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.*;

public class MagicItemDataParser {

	private static final Gson gson = new GsonBuilder().setLenient().create();

	/* splits the saved magicItemData string by the "|" char
		itemType{data}|itemType{data}...
	*/
	public static final String DATA_REGEX = "(?=(?:(?:[^\"]*\"){2})*[^\"]*$)(?![^{]*})(?![^\\[]*])\\|+";

	public static MagicItemData parseMagicItemData(String str) {
		try (var ignored = MagicDebug.section(DebugCategory.MAGIC_ITEMS, "Parsing string-based magic item '%s'.", str)) {
			return parseInternal(str);
		}
	}

	private static MagicItemData parseInternal(String str) {
		str = str.trim();
		if (str.isEmpty()) return null;

		String[] args = str.split("\\{", 2);
		if (args.length < 2) {
			Material type = Util.getMaterial(str);
			if (type == null) {
				MagicDebug.warn("Invalid item type '%s' %s.", str, MagicDebug.resolvePath());
				return null;
			}

			MagicItemData magicItemData = new MagicItemData();
			magicItemData.setAttribute(TYPE, type);

			return magicItemData;
		}

		String base = args[0].trim();
		args[1] = "{" + args[1];

		MagicItemData data;

		Material type = Util.getMaterial(base);
		if (type != null) {
			if (!type.isItem()) {
				MagicDebug.warn("Invalid item type '%s' %s.", base, MagicDebug.resolvePath());
				return null;
			}

			data = new MagicItemData();
			data.setAttribute(TYPE, type);
		} else {
			MagicItem magicItem = MagicItems.getMagicItems().get(base);
			if (magicItem == null) {
				MagicDebug.warn("Invalid item type or magic item '%s' %s.", base, MagicDebug.resolvePath());
				return null;
			}

			data = magicItem.getMagicItemData().clone();
			type = data.getAttribute(TYPE);
		}
		if (type.isAir()) return data;

		String key = null;
		try {
			JsonObject object = gson.fromJson(args[1], JsonObject.class);

			for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
				key = entry.getKey();
				JsonElement value = entry.getValue();

				switch (key.toLowerCase()) {
					case "amount" ->
						data.setAttribute(AMOUNT, value.getAsInt());
					case "author" ->
						data.setAttribute(AUTHOR, Util.getMiniMessage(value.getAsString()));
					case "block-data", "block_data", "blockdata" ->
						data.setAttribute(BLOCK_DATA, Bukkit.createBlockData(type, value.getAsString()));
					case "custom-model-data", "custom_model_data", "custommodeldata" ->
						data.setAttribute(CUSTOM_MODEL_DATA, value.getAsInt());
					case "durability" ->
						data.setAttribute(DURABILITY, value.getAsInt());
					case "hide-tooltip", "hide_tooltip", "hidetooltip" ->
						data.setAttribute(HIDE_TOOLTIP, value.getAsBoolean());
					case "name" ->
						data.setAttribute(NAME, Util.getMiniMessage(value.getAsString()));
					case "power" ->
						data.setAttribute(POWER, value.getAsInt());
					case "repair-cost", "repair_cost", "repaircost" ->
						data.setAttribute(REPAIR_COST, value.getAsInt());
					case "signature" ->
						data.setAttribute(SIGNATURE, value.getAsString());
					case "skull-owner", "skull_owner", "skullowner" ->
						data.setAttribute(SKULL_OWNER, value.getAsString());
					case "strict-block-data", "strict_block_data", "strictblockdata" ->
						data.setStrictBlockData(value.getAsBoolean());
					case "strict-durability", "strict_durability", "strictdurability" ->
						data.setStrictDurability(value.getAsBoolean());
					case "strict-enchant-level", "strict_enchant_level", "strictenchantlevel" ->
						data.setStrictEnchantLevel(value.getAsBoolean());
					case "strict-enchants", "strict_enchants", "strictenchants" ->
						data.setStrictEnchants(value.getAsBoolean());
					case "texture" ->
						data.setAttribute(TEXTURE, value.getAsString());
					case "title" ->
						data.setAttribute(TITLE, Util.getMiniMessage(value.getAsString()));
					case "unbreakable" ->
						data.setAttribute(UNBREAKABLE, value.getAsBoolean());
					case "uuid" ->
						data.setAttribute(UUID, java.util.UUID.fromString(value.getAsString()));
					case "attributes" -> {
						Multimap<Attribute, AttributeModifier> attributes = AttributeHandler.getAttributeModifiers(gson.fromJson(value, List.class));
						if (attributes == null) return null;

						if (!attributes.isEmpty()) data.setAttribute(ATTRIBUTES, attributes);
					}
					case "blacklisted-attributes", "blacklisted_attributes", "blacklistedattributes" -> {
						Set<MagicItemAttribute<?>> blacklistedAttributes = data.getBlacklistedAttributes();

						JsonArray attributeStrings = value.getAsJsonArray();
						for (JsonElement element : attributeStrings) {
							String attributeString = element.getAsString();

							MagicItemAttribute<?> attribute = MagicItemAttribute.fromString(attributeString);
							if (attribute == null) {
								MagicDebug.warn("Invalid blacklisted attribute '%s' %s.", attributeString, MagicDebug.resolvePath());
								return null;
							}

							blacklistedAttributes.add(attribute);
						}
					}
					case "color", "potion-color", "potion_color", "potioncolor" -> {
						String colorString = value.getAsString();
						Color color = ColorUtil.getColorFromHexString(colorString, false);
						if (color == null) {
							MagicDebug.warn("Invalid color '%s' %s.", colorString, MagicDebug.resolvePath());
							return null;
						}

						data.setAttribute(COLOR, color);
					}
					case "enchants", "enchantments" -> {
						Map<String, Integer> rawEnchants = gson.fromJson(value, new TypeToken<>() {});

						Map<Enchantment, Integer> enchants = new HashMap<>();
						for (String enchantString : rawEnchants.keySet()) {
							Enchantment enchantment = EnchantmentHandler.getEnchantment(enchantString);

							if (enchantment == null) {
								MagicDebug.warn("Invalid enchantment '%s' %s.", enchantString, MagicDebug.resolvePath());
								return null;
							}

							enchants.put(enchantment, rawEnchants.get(enchantString));
						}
						if (enchants.isEmpty()) continue;

						if (data.hasAttribute(FAKE_GLINT)) {
							boolean fakeGlint = data.getAttribute(FAKE_GLINT);
							if (fakeGlint) data.removeAttribute(FAKE_GLINT);
						}

						data.setAttribute(ENCHANTS, enchants);
					}
					case "fake-glint", "fake_glint", "fakeglint" -> {
						if (!value.getAsBoolean()) continue;

						if (data.hasAttribute(ENCHANTS)) {
							Map<Enchantment, Integer> enchantments = data.getAttribute(ENCHANTS);
							if (!enchantments.isEmpty()) continue;
						}

						data.setAttribute(FAKE_GLINT, true);
					}
					case "firework-effect", "firework_effect", "fireworkeffect" -> {
						String effectString = value.getAsString();

						String[] values = effectString.split(" ");
						if (values.length < 3 || values.length > 5) {
							MagicDebug.warn("Invalid firework effect '%s' %s - missing or too many values.", effectString, MagicDebug.resolvePath());
							return null;
						}

						FireworkEffect.Type fireworkType = FireworkEffect.Type.valueOf(values[0].toUpperCase());
						boolean trail = Boolean.parseBoolean(values[1]);
						boolean flicker = Boolean.parseBoolean(values[2]);

						List<Color> colors = values.length > 3 ? FireworkEffectHandler.getColorsFromString(values[3], "colors", true) : List.of();
						if (colors == null) return null;

						List<Color> fadeColors = values.length > 4 ? FireworkEffectHandler.getColorsFromString(values[4], "fade colors", true) : List.of();
						if (fadeColors == null) return null;

						FireworkEffect effect = FireworkEffect.builder()
							.flicker(flicker)
							.trail(trail)
							.with(fireworkType)
							.withColor(colors)
							.withFade(fadeColors)
							.build();

						data.setAttribute(FIREWORK_EFFECT, effect);
					}
					case "firework-effects", "firework_effects", "fireworkeffects" -> {
						List<FireworkEffect> effects = new ArrayList<>();

						JsonArray effectStrings = value.getAsJsonArray();
						for (JsonElement element : effectStrings) {
							String effectString = element.getAsString();

							String[] values = effectString.split(" ");
							if (values.length != 4 && values.length != 5) {
								MagicDebug.warn("Invalid firework effect '%s' %s - missing or too many values.", effectString, MagicDebug.resolvePath());
								return null;
							}

							FireworkEffect.Type fireworkType = FireworkEffect.Type.valueOf(values[0].toUpperCase());
							boolean trail = Boolean.parseBoolean(values[1]);
							boolean flicker = Boolean.parseBoolean(values[2]);

							List<Color> colors = FireworkEffectHandler.getColorsFromString(values[3], "colors", true);
							if (colors == null) return null;

							List<Color> fadeColors = values.length > 4 ? FireworkEffectHandler.getColorsFromString(values[4], "fade colors", true) : List.of();
							if (fadeColors == null) return null;

							FireworkEffect effect = FireworkEffect.builder()
								.flicker(flicker)
								.trail(trail)
								.with(fireworkType)
								.withColor(colors)
								.withFade(fadeColors)
								.build();

							effects.add(effect);
						}

						if (!effects.isEmpty()) data.setAttribute(FIREWORK_EFFECTS, effects);
					}
					case "ignored-attributes", "ignored_attributes", "ignoredattributes" -> {
						Set<MagicItemAttribute<?>> ignoredAttributes = data.getIgnoredAttributes();

						JsonArray attributeStrings = value.getAsJsonArray();
						for (JsonElement element : attributeStrings) {
							String attributeString = element.getAsString();

							MagicItemAttribute<?> attribute = MagicItemAttribute.fromString(attributeString);
							if (attribute == null) {
								MagicDebug.warn("Invalid ignored attribute '%s' %s.", attributeString, MagicDebug.resolvePath());
								return null;
							}

							ignoredAttributes.add(attribute);
						}
					}
					case "lore" -> {
						List<Component> lore = new ArrayList<>();

						JsonArray loreArray = value.getAsJsonArray();
						for (JsonElement line : loreArray) lore.add(Util.getMiniMessage(line.getAsString()));

						if (!lore.isEmpty()) data.setAttribute(LORE, lore);
					}
					case "pages" -> {
						List<Component> pages = new ArrayList<>();

						JsonArray pageArray = value.getAsJsonArray();
						for (JsonElement page : pageArray) pages.add(Util.getMiniMessage(page.getAsString()));

						if (!pages.isEmpty()) data.setAttribute(PAGES, pages);
					}
					case "patterns" -> {
						List<Pattern> patterns = new ArrayList<>();

						Registry<PatternType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN);

						JsonArray patternStrings = value.getAsJsonArray();
						for (JsonElement element : patternStrings) {
							String patternString = element.getAsString();
							String[] pattern = patternString.split(" ");

							if (pattern.length != 2) {
								MagicDebug.warn("Invalid pattern '%s' %s - missing or too many values.", patternString, MagicDebug.resolvePath());
								return null;
							}

							NamespacedKey namespacedKey = NamespacedKey.fromString(pattern[0]);
							if (namespacedKey == null) {
								MagicDebug.warn("Invalid banner pattern '%s' %s.", pattern[0], MagicDebug.resolvePath());
								return null;
							}

							PatternType patternType = registry.get(namespacedKey);
							if (patternType == null) {
								MagicDebug.warn("Invalid banner pattern '%s' %s.", pattern[0], MagicDebug.resolvePath());
								return null;
							}

							DyeColor dyeColor = DyeColor.valueOf(pattern[1]);

							patterns.add(new Pattern(dyeColor, patternType));
						}

						if (!patterns.isEmpty()) data.setAttribute(PATTERNS, patterns);
					}
					case "potion-effects", "potion_effects", "potioneffects" -> {
						List<PotionEffect> effects = new ArrayList<>();

						JsonArray effectStrings = value.getAsJsonArray();
						for (JsonElement element : effectStrings) {
							String effectString = element.getAsString();

							PotionEffect effect = type == Material.SUSPICIOUS_STEW ? SuspiciousStewHandler.buildSuspiciousStewPotionEffect(effectString) : PotionHandler.buildPotionEffect(effectString);
							if (effect == null) return null;

							effects.add(effect);
						}

						if (!effects.isEmpty()) data.setAttribute(POTION_EFFECTS, effects);
					}
					case "potion-type", "potion_type", "potiontype", "potion-data", "potion_data", "potiondata" -> {
						String potionTypeString = value.getAsString();

						PotionType potionType = PotionHandler.getPotionType(potionTypeString);
						if (potionType == null) {
							MagicDebug.warn("Invalid potion type '%s' %s.", potionTypeString, MagicDebug.resolvePath());
							return null;
						}

						data.setAttribute(POTION_TYPE, potionType);
					}
				}
			}
		} catch (Exception e) {
			if (key == null) MagicDebug.warn(e, "Encountered error while reading value %s:", MagicDebug.resolvePath());
			else MagicDebug.warn(e, "Encountered error while reading option '%s' %s:", key, MagicDebug.resolvePath());

			return null;
		}

		return data;
	}

}
