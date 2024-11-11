package com.nisovin.magicspells.util.magicitems;

import io.papermc.paper.potion.SuspiciousEffectEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Objects;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;

import com.google.common.collect.Multimap;
import com.google.common.collect.Iterables;
import com.google.common.base.Preconditions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.kyori.adventure.text.Component;

import org.bukkit.*;
import org.bukkit.potion.PotionType;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffect;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.attribute.AttributeModifier;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.AttributeUtil;

public class MagicItemData implements Cloneable {

	private final Map<MagicItemAttribute<?>, Object> itemAttributes = new Reference2ObjectArrayMap<>();
	private final Set<MagicItemAttribute<?>> blacklistedAttributes = new ReferenceArraySet<>();
	private final Set<MagicItemAttribute<?>> ignoredAttributes = new ReferenceArraySet<>();

	private boolean strictEnchantLevel = true;
	private boolean strictDurability = true;
	private boolean strictBlockData = true;
	private boolean strictEnchants = true;

	public <T> T getAttribute(@NotNull MagicItemAttribute<T> attr) {
		return (T) itemAttributes.get(attr);
	}

	public <T> void setAttribute(@NotNull MagicItemAttribute<T> attr, @NotNull T value) {
		Preconditions.checkNotNull(value);
		itemAttributes.put(attr, value);
	}

	public void removeAttribute(MagicItemAttribute<?> attr) {
		itemAttributes.remove(attr);
	}

	public boolean hasAttribute(MagicItemAttribute<?> atr) {
		return itemAttributes.containsKey(atr);
	}

	public Set<MagicItemAttribute<?>> getBlacklistedAttributes() {
		return blacklistedAttributes;
	}

	public Set<MagicItemAttribute<?>> getIgnoredAttributes() {
		return ignoredAttributes;
	}

	public boolean isStrictEnchantLevel() {
		return strictEnchantLevel;
	}

	public void setStrictEnchantLevel(boolean strictEnchantLevel) {
		this.strictEnchantLevel = strictEnchantLevel;
	}

	public boolean isStrictDurability() {
		return strictDurability;
	}

	public void setStrictDurability(boolean strictDurability) {
		this.strictDurability = strictDurability;
	}

	public boolean isStrictBlockData() {
		return strictBlockData;
	}

	public void setStrictBlockData(boolean strictBlockData) {
		this.strictBlockData = strictBlockData;
	}

	public boolean isStrictEnchants() {
		return strictEnchants;
	}

	public void setStrictEnchants(boolean strictEnchants) {
		this.strictEnchants = strictEnchants;
	}

	public boolean matches(MagicItemData data) {
		if (this == data) return true;

		Set<MagicItemAttribute<?>> keysSelf = itemAttributes.keySet();
		Set<MagicItemAttribute<?>> keysOther = data.itemAttributes.keySet();

		for (MagicItemAttribute<?> attr : keysSelf) {
			if (ignoredAttributes.contains(attr)) continue;
			if (!keysOther.contains(attr)) return false;
		}

		for (MagicItemAttribute<?> attr : blacklistedAttributes) {
			if (keysOther.contains(attr)) return false;
		}

		for (MagicItemAttribute<?> attr : keysSelf) {
			if (ignoredAttributes.contains(attr)) continue;
			if (!attr.compare(this, data)) return false;
		}

		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MagicItemData other)) return false;
		return itemAttributes.equals(other.itemAttributes)
			&& ignoredAttributes.equals(other.ignoredAttributes)
			&& blacklistedAttributes.equals(other.blacklistedAttributes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(itemAttributes, ignoredAttributes, blacklistedAttributes);
	}

	@Override
	public MagicItemData clone() {
		MagicItemData data;
		try {
			data = (MagicItemData) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}

		if (!itemAttributes.isEmpty()) data.itemAttributes.putAll(itemAttributes);
		if (!ignoredAttributes.isEmpty()) data.ignoredAttributes.addAll(ignoredAttributes);
		if (!blacklistedAttributes.isEmpty()) data.blacklistedAttributes.addAll(blacklistedAttributes);

		return data;
	}

	public static class MagicItemAttributes {

		public static final MagicItemAttribute<Integer> AMOUNT = new MagicItemAttribute<>("amount");

		public static final MagicItemAttribute<Material> TYPE = new MagicItemAttribute<>("type");

		public static final MagicItemAttribute<Integer> CUSTOM_MODEL_DATA = new MagicItemAttribute<>("custom-model-data");

		public static final MagicItemAttribute<Component> NAME = new ComponentAttribute("name");

		public static final MagicItemAttribute<Integer> REPAIR_COST = new MagicItemAttribute<>("repair-cost");

		public static final MagicItemAttribute<Integer> POWER = new MagicItemAttribute<>("power");

		public static final MagicItemAttribute<Boolean> UNBREAKABLE = new MagicItemAttribute<>("unbreakable");

		public static final MagicItemAttribute<Boolean> HIDE_TOOLTIP = new MagicItemAttribute<>("hide-tooltip");

		public static final MagicItemAttribute<Boolean> FAKE_GLINT = new MagicItemAttribute<>("fake-glint");

		public static final MagicItemAttribute<PotionType> POTION_TYPE = new MagicItemAttribute<>("potion-type");

		public static final MagicItemAttribute<Color> COLOR = new MagicItemAttribute<>("color");

		public static final MagicItemAttribute<FireworkEffect> FIREWORK_EFFECT = new MagicItemAttribute<>("firework-effect");

		public static final MagicItemAttribute<Component> TITLE = new ComponentAttribute("title");

		public static final MagicItemAttribute<Component> AUTHOR = new ComponentAttribute("author");

		public static final MagicItemAttribute<UUID> UUID = new MagicItemAttribute<>("uuid");

		public static final MagicItemAttribute<String> TEXTURE = new MagicItemAttribute<>("texture");

		public static final MagicItemAttribute<String> SIGNATURE = new MagicItemAttribute<>("signature");

		public static final MagicItemAttribute<String> SKULL_OWNER = new MagicItemAttribute<>("skull-owner");

		public static final MagicItemAttribute<List<Component>> LORE = new ListComponentAttribute("lore");

		public static final MagicItemAttribute<List<Component>> PAGES = new ListComponentAttribute("pages");

		public static final MagicItemAttribute<List<PotionEffect>> POTION_EFFECTS = new MagicItemAttribute<>("potion-effects");

		public static final MagicItemAttribute<List<SuspiciousEffectEntry>> STEW_EFFECTS = new MagicItemAttribute<>("stew-effects");

		public static final MagicItemAttribute<List<Pattern>> PATTERNS = new MagicItemAttribute<>("patterns");

		public static final MagicItemAttribute<List<FireworkEffect>> FIREWORK_EFFECTS = new MagicItemAttribute<>("firework-effects");

		public static final MagicItemAttribute<Integer> DURABILITY = new MagicItemAttribute<>("durability") {

			@Override
			public boolean compare(@NotNull MagicItemData data, @NotNull Integer primary, @NotNull Integer secondary) {
				int compare = primary.compareTo(secondary);
				return data.strictDurability ? compare == 0 : compare >= 0;
			}

		};

		public static final MagicItemAttribute<Map<Enchantment, Integer>> ENCHANTS = new MagicItemAttribute<>("enchants") {

			@Override
			public boolean compare(@NotNull MagicItemData data, @NotNull Map<Enchantment, Integer> primary, @NotNull Map<Enchantment, Integer> secondary) {
				if (data.strictEnchants && data.strictEnchantLevel)
					return primary.equals(secondary);

				if (data.strictEnchants && primary.size() != secondary.size()) return false;

				for (Enchantment enchant : primary.keySet()) {
					if (!secondary.containsKey(enchant)) return false;

					Integer levelPrimary = primary.get(enchant);
					Integer levelSecondary = secondary.get(enchant);

					int compare = levelPrimary.compareTo(levelSecondary);
					if (data.strictEnchantLevel ? compare != 0 : compare > 0) return false;
				}

				return true;
			}
		};

		public static final MagicItemAttribute<BlockData> BLOCK_DATA = new MagicItemAttribute<>("block-data") {

			@Override
			public boolean compare(@NotNull MagicItemData data, @NotNull BlockData primary, @NotNull BlockData secondary) {
				return data.strictBlockData ? primary.equals(secondary) : secondary.matches(primary);
			}

		};

		public static final MagicItemAttribute<Multimap<Attribute, AttributeModifier>> ATTRIBUTES = new MagicItemAttribute<>("attributes") {

			@Override
			public boolean compare(@NotNull MagicItemData data, @NotNull Multimap<Attribute, AttributeModifier> primary, @NotNull Multimap<Attribute, AttributeModifier> secondary) {
				return Iterables.elementsEqual(primary.entries(), secondary.entries());
			}

		};

		private static class ComponentAttribute extends MagicItemAttribute<Component> {

			private ComponentAttribute(String key) {
				super(key);
			}

			@Override
			public boolean compare(@NotNull MagicItemData data, @NotNull Component primary, @NotNull Component secondary) {
				String legacySelf = Util.getLegacyFromComponent(primary);
				String legacyOther = Util.getLegacyFromComponent(secondary);

				return legacySelf.equals(legacyOther);
			}

		}

		private static class ListComponentAttribute extends MagicItemAttribute<List<Component>> {

			private ListComponentAttribute(String key) {
				super(key);
			}

			@Override
			public boolean compare(@NotNull MagicItemData data, @NotNull List<Component> primary, @NotNull List<Component> secondary) {
				if (primary.size() != secondary.size()) return false;

				for (int i = 0; i < primary.size(); i++) {
					String legacySelf = Util.getLegacyFromComponent(primary.get(i));
					String legacyOther = Util.getLegacyFromComponent(secondary.get(i));

					if (!legacySelf.equals(legacyOther)) return false;
				}

				return true;
			}

		}

	}

	public static class MagicItemAttribute<T> {

		private static final Map<String, MagicItemAttribute<?>> keyToAttribute = new HashMap<>();

		private final String key;

		private MagicItemAttribute(String key) {
			this.key = key;

			keyToAttribute.put(key, this);
		}

		public String getKey() {
			return key;
		}

		@Override
		public String toString() {
			return key;
		}

		public boolean compare(@NotNull MagicItemData primary, @NotNull MagicItemData secondary) {
			return compare(primary, primary.getAttribute(this), secondary.getAttribute(this));
		}

		public boolean compare(@NotNull MagicItemData data, @NotNull T primary, @NotNull T secondary) {
			return Objects.equals(primary, secondary);
		}

		@Nullable
		public static MagicItemAttribute<?> fromString(@NotNull String string) {
			string = string.toLowerCase().replace("_", "-");

			MagicItemAttribute<?> attribute =  keyToAttribute.get(string);
			if (attribute != null) return attribute;

			return switch (string) {
				case "enchantments" -> MagicItemAttributes.ENCHANTS;
				case "potion-data" -> MagicItemAttributes.POTION_TYPE;
				default -> null;
			};
		}

	}

	@Override
	public String toString() {
		JsonObject magicItem = new JsonObject();

		if (hasAttribute(MagicItemAttributes.NAME))
			magicItem.addProperty("name", Util.getStringFromComponent(getAttribute(MagicItemAttributes.NAME)));

		if (hasAttribute(MagicItemAttributes.AMOUNT))
			magicItem.addProperty("amount", getAttribute(MagicItemAttributes.AMOUNT));

		if (hasAttribute(MagicItemAttributes.DURABILITY))
			magicItem.addProperty("durability", getAttribute(MagicItemAttributes.DURABILITY));

		if (hasAttribute(MagicItemAttributes.REPAIR_COST))
			magicItem.addProperty("repair-cost", getAttribute(MagicItemAttributes.REPAIR_COST));

		if (hasAttribute(MagicItemAttributes.CUSTOM_MODEL_DATA))
			magicItem.addProperty("custom-model-data", getAttribute(MagicItemAttributes.CUSTOM_MODEL_DATA));

		if (hasAttribute(MagicItemAttributes.POWER))
			magicItem.addProperty("power", getAttribute(MagicItemAttributes.POWER));

		if (hasAttribute(MagicItemAttributes.UNBREAKABLE))
			magicItem.addProperty("unbreakable", getAttribute(MagicItemAttributes.UNBREAKABLE));

		if (hasAttribute(MagicItemAttributes.HIDE_TOOLTIP))
			magicItem.addProperty("hide-tooltip", getAttribute(MagicItemAttributes.HIDE_TOOLTIP));

		if (hasAttribute(MagicItemAttributes.COLOR))
			magicItem.addProperty("color", Integer.toHexString(getAttribute(MagicItemAttributes.COLOR).asRGB()));

		if (hasAttribute(MagicItemAttributes.POTION_TYPE)) {
			PotionType potionType = getAttribute(MagicItemAttributes.POTION_TYPE);
			magicItem.addProperty("potion-type", potionType.getKey().getKey());
		}

		if (hasAttribute(MagicItemAttributes.FIREWORK_EFFECT)) {
			FireworkEffect effect = getAttribute(MagicItemAttributes.FIREWORK_EFFECT);

			StringBuilder effectBuilder = new StringBuilder();
			effectBuilder
				.append(effect.getType())
				.append(' ')
				.append(effect.hasTrail())
				.append(' ')
				.append(effect.hasFlicker());

			if (!effect.getColors().isEmpty()) {
				effectBuilder.append(' ');

				boolean previousColor = false;
				for (Color color : effect.getColors()) {
					if (previousColor) effectBuilder.append(',');

					effectBuilder.append(Integer.toHexString(color.asRGB()));
					previousColor = true;
				}

				if (!effect.getFadeColors().isEmpty()) {
					effectBuilder.append(' ');

					previousColor = false;
					for (Color color : effect.getFadeColors()) {
						if (previousColor) effectBuilder.append(',');

						effectBuilder.append(Integer.toHexString(color.asRGB()));
						previousColor = true;
					}
				}
			}

			magicItem.addProperty("firework-effect", effectBuilder.toString());
		}

		if (hasAttribute(MagicItemAttributes.SKULL_OWNER))
			magicItem.addProperty("skull-owner", getAttribute(MagicItemAttributes.SKULL_OWNER));

		if (hasAttribute(MagicItemAttributes.TITLE))
			magicItem.addProperty("title", Util.getStringFromComponent(getAttribute(MagicItemAttributes.TITLE)));

		if (hasAttribute(MagicItemAttributes.AUTHOR))
			magicItem.addProperty("author", Util.getStringFromComponent(getAttribute(MagicItemAttributes.AUTHOR)));

		if (hasAttribute(MagicItemAttributes.UUID))
			magicItem.addProperty("uuid", getAttribute(MagicItemAttributes.UUID).toString());

		if (hasAttribute(MagicItemAttributes.TEXTURE))
			magicItem.addProperty("texture", getAttribute(MagicItemAttributes.TEXTURE));

		if (hasAttribute(MagicItemAttributes.SIGNATURE))
			magicItem.addProperty("signature", getAttribute(MagicItemAttributes.SIGNATURE));

		if (hasAttribute(MagicItemAttributes.ENCHANTS)) {
			Map<Enchantment, Integer> enchants = getAttribute(MagicItemAttributes.ENCHANTS);

			JsonObject enchantsObject = new JsonObject();
			for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet())
				enchantsObject.addProperty(entry.getKey().getKey().getKey(), entry.getValue());

			magicItem.add("enchants", enchantsObject);
		}

		if (hasAttribute(MagicItemAttributes.FAKE_GLINT))
			magicItem.addProperty("fake-glint", getAttribute(MagicItemAttributes.FAKE_GLINT));

		if (hasAttribute(MagicItemAttributes.ATTRIBUTES)) {
			Multimap<Attribute, AttributeModifier> attributes = getAttribute(MagicItemAttributes.ATTRIBUTES);

			JsonArray attributesArray = new JsonArray();
			for (Map.Entry<Attribute, AttributeModifier> entry : attributes.entries()) {
				AttributeModifier modifier = entry.getValue();

				JsonObject modifierObject = new JsonObject();
				modifierObject.addProperty("id", modifier.getKey().asString());
				modifierObject.addProperty("type", entry.getKey().getKey().asString());
				modifierObject.addProperty("operation", AttributeUtil.getOperationName(modifier.getOperation()));
				modifierObject.addProperty("amount", modifier.getAmount());
				modifierObject.addProperty("slot", modifier.getSlotGroup().toString());

				attributesArray.add(modifierObject);
			}

			magicItem.add("attributes", attributesArray);
		}

		if (hasAttribute(MagicItemAttributes.LORE)) {
			List<Component> lore = getAttribute(MagicItemAttributes.LORE);

			JsonArray loreArray = new JsonArray(lore.size());
			for (Component line : lore) loreArray.add(Util.getStringFromComponent(line));

			magicItem.add("lore", loreArray);
		}

		if (hasAttribute(MagicItemAttributes.PAGES)) {
			List<Component> pages = getAttribute(MagicItemAttributes.PAGES);

			JsonArray pagesArray = new JsonArray(pages.size());
			for (Component line : pages) pagesArray.add(Util.getStringFromComponent(line));

			magicItem.add("pages", pagesArray);
		}

		if (hasAttribute(MagicItemAttributes.PATTERNS)) {
			List<Pattern> patterns = getAttribute(MagicItemAttributes.PATTERNS);

			JsonArray patternsArray = new JsonArray(patterns.size());
			for (Pattern pattern : patterns) {
				String patternString = pattern.getPattern().key().asMinimalString().toLowerCase() + " " + pattern.getColor().name().toLowerCase();
				patternsArray.add(patternString);
			}

			magicItem.add("patterns", patternsArray);
		}

		if (hasAttribute(MagicItemAttributes.POTION_EFFECTS)) {
			List<PotionEffect> effects = getAttribute(MagicItemAttributes.POTION_EFFECTS);

			StringBuilder effectBuilder = new StringBuilder();
			JsonArray potionEffectsArray = new JsonArray(effects.size());
			for (PotionEffect effect : effects) {
				effectBuilder.setLength(0);

				effectBuilder
					.append(effect.getType().getKey().getKey())
					.append(' ')
					.append(effect.getAmplifier())
					.append(' ')
					.append(effect.getDuration())
					.append(' ')
					.append(effect.isAmbient())
					.append(' ')
					.append(effect.hasParticles())
					.append(' ')
					.append(effect.hasIcon());

				potionEffectsArray.add(effectBuilder.toString());
			}

			magicItem.add("potion-effects", potionEffectsArray);
		}

		if (hasAttribute(MagicItemAttributes.FIREWORK_EFFECTS)) {
			List<FireworkEffect> effects = getAttribute(MagicItemAttributes.FIREWORK_EFFECTS);

			StringBuilder effectBuilder = new StringBuilder();
			JsonArray fireworkEffectsArray = new JsonArray(effects.size());
			for (FireworkEffect effect : effects) {
				effectBuilder.setLength(0);

				effectBuilder
					.append(effect.getType())
					.append(' ')
					.append(effect.hasTrail())
					.append(' ')
					.append(effect.hasFlicker());

				boolean previousColor = false;
				if (!effect.getColors().isEmpty()) {
					effectBuilder.append(' ');

					for (Color color : effect.getColors()) {
						if (previousColor) effectBuilder.append(',');
						effectBuilder.append(Integer.toHexString(color.asRGB()));
						previousColor = true;
					}

					if (!effect.getFadeColors().isEmpty()) {
						effectBuilder.append(' ');

						previousColor = false;
						for (Color color : effect.getFadeColors()) {
							if (previousColor) effectBuilder.append(',');
							effectBuilder.append(Integer.toHexString(color.asRGB()));
							previousColor = true;
						}
					}
				}

				fireworkEffectsArray.add(effectBuilder.toString());
			}

			magicItem.add("firework-effects", fireworkEffectsArray);
		}

		if (!ignoredAttributes.isEmpty()) {
			JsonArray ignoredAttributesArray = new JsonArray(ignoredAttributes.size());
			for (MagicItemAttribute attribute : ignoredAttributes) ignoredAttributesArray.add(attribute.toString());

			magicItem.add("ignored-attributes", ignoredAttributesArray);
		}

		if (!blacklistedAttributes.isEmpty()) {
			JsonArray blacklistedAttributesArray = new JsonArray(blacklistedAttributes.size());
			for (MagicItemAttribute attribute : blacklistedAttributes) blacklistedAttributesArray.add(attribute.toString());

			magicItem.add("blacklisted-attributes", blacklistedAttributesArray);
		}

		if (!strictEnchants) magicItem.addProperty("strict-enchants", false);
		if (!strictDurability) magicItem.addProperty("strict-durability", false);
		if (!strictBlockData) magicItem.addProperty("strict-block-data", false);
		if (!strictEnchantLevel) magicItem.addProperty("strict-enchant-level", false);

		String output = magicItem.toString();
		if (hasAttribute(MagicItemAttributes.TYPE))
			output = getAttribute(MagicItemAttributes.TYPE).getKey().getKey() + output;

		return output;
	}

}
