package com.nisovin.magicspells.volatilecode;

import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

import io.papermc.paper.advancement.AdvancementDisplay.Frame;

import com.nisovin.magicspells.util.glow.GlowManager;

public abstract class VolatileCodeHandle {

	protected final VolatileCodeHelper helper;

	public VolatileCodeHandle(VolatileCodeHelper helper) {
		this.helper = helper;
	}

	public abstract void addPotionGraphicalEffect(LivingEntity entity, int color, long duration);

	public abstract void sendFakeSlotUpdate(Player player, int slot, ItemStack item);

	public abstract boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire);

	public abstract void playDragonDeathEffect(Location location);

	public abstract void setClientVelocity(Player player, Vector velocity);

	public abstract void startAutoSpinAttack(Player player, int ticks);

	public abstract void playHurtAnimation(LivingEntity entity, float yaw);

	public abstract Recipe createSmithingRecipe(
			@NotNull NamespacedKey namespacedKey,
			@NotNull ItemStack result,
			@NotNull RecipeChoice template,
			@NotNull RecipeChoice base,
			@NotNull RecipeChoice addition,
			boolean copyNbt
	);

	public abstract void sendToastEffect(Player receiver, ItemStack icon, Frame frameType, Component text);

	public abstract void sendStatusUpdate(Player player, double health, int food, float saturation);

	public abstract void addGameTestMarker(Player player, Location location, int color, String name, int lifetime);

	public abstract void clearGameTestMarkers(Player player);

	public abstract byte getEntityMetadata(Entity entity);

	public abstract GlowManager getGlowManager();

}
