package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.TEXTURE;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.SIGNATURE;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.SKULL_OWNER;

public class SkullHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof SkullMeta skullMeta)) return true;

		String signature = null, skullOwner = null, texture = null;
		UUID uuid = null;

		if (config.isString(MagicItemAttributes.UUID.getKey())) {
			String uuidString = config.getString(MagicItemAttributes.UUID.getKey(), "");

			try {
				uuid = UUID.fromString(uuidString);
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid UUID '%s' %s.", uuidString, MagicDebug.resolveFullPath("uuid"));
				return false;
			}

			data.setAttribute(MagicItemAttributes.UUID, uuid);
		} else if (!invalidIfSet(config, MagicItemAttributes.UUID)) return false;

		if (config.isString(TEXTURE.getKey())) {
			texture = config.getString(TEXTURE.getKey());
			data.setAttribute(TEXTURE, texture);
		} else if (!invalidIfSet(config, TEXTURE)) return false;

		if (config.isString(SIGNATURE.getKey())) {
			signature = config.getString(SIGNATURE.getKey());
			data.setAttribute(SIGNATURE, signature);
		} else if (!invalidIfSet(config, SIGNATURE)) return false;

		if (config.isString(SKULL_OWNER.getKey())) {
			skullOwner = config.getString(SKULL_OWNER.getKey());
			data.setAttribute(SKULL_OWNER, skullOwner);
		} else if (!invalidIfSet(config, SKULL_OWNER)) return false;

		if (uuid == null && skullOwner == null) {
			if (texture != null) {
				MagicDebug.warn("Cannot set textures for magic item %s - 'uuid' and/or 'skull-owner' must be specified.", MagicDebug.resolveFullPath());
				return false;
			}

			return true;
		}

		PlayerProfile profile = Bukkit.createProfile(uuid, skullOwner);
		if (texture != null) profile.setProperty(new ProfileProperty("textures", texture, signature));

		skullMeta.setPlayerProfile(profile);

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof SkullMeta skullMeta)) return;

		String signature = null, skullOwner = null, texture = null;
		UUID uuid = null;

		if (data.hasAttribute(SKULL_OWNER)) skullOwner = data.getAttribute(SKULL_OWNER);
		if (data.hasAttribute(SIGNATURE)) signature = data.getAttribute(SIGNATURE);
		if (data.hasAttribute(TEXTURE)) texture = data.getAttribute(TEXTURE);
		if (data.hasAttribute(MagicItemAttributes.UUID)) uuid = data.getAttribute(MagicItemAttributes.UUID);

		if ((uuid != null || skullOwner != null) && texture != null) {
			PlayerProfile profile = Bukkit.createProfile(uuid, skullOwner);
			profile.setProperty(new ProfileProperty("textures", texture, signature));

			skullMeta.setPlayerProfile(profile);
		}
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof SkullMeta skullMeta)) return;

		PlayerProfile profile = skullMeta.getPlayerProfile();
		if (profile == null) return;

		UUID id = profile.getId();
		if (id != null) data.setAttribute(MagicItemAttributes.UUID, id);

		String name = profile.getName();
		if (name != null) data.setAttribute(SKULL_OWNER, name);

		if (profile.hasTextures()) {
			for (ProfileProperty property : profile.getProperties()) {
				if (property.getName().equals("textures")) {
					data.setAttribute(TEXTURE, property.getValue());
					if (property.isSigned()) data.setAttribute(SIGNATURE, property.getSignature());
					break;
				}
			}
		}
	}

}
