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

public class VolatileCodeDisabled extends VolatileCodeHandle {

	public VolatileCodeDisabled() {
		super(null);
	}

	@Override
	public void addPotionGraphicalEffect(LivingEntity entity, int color, long duration) {

	}

	@Override
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {

	}

	@Override
	public boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire) {
		return false;
	}

	@Override
	public void playDragonDeathEffect(Location location) {

	}

	@Override
	public void setClientVelocity(Player player, Vector velocity) {

	}

	@Override
	public void startAutoSpinAttack(Player player, int ticks) {

	}

	@Override
	public void playHurtAnimation(LivingEntity entity, float yaw) {
		entity.playHurtAnimation(yaw);
	}

	@Override
	public Recipe createSmithingRecipe(@NotNull NamespacedKey namespacedKey, @NotNull ItemStack result, @NotNull RecipeChoice template, @NotNull RecipeChoice base, @NotNull RecipeChoice addition, boolean copyNbt) {
		return null;
	}

	@Override
	public void sendToastEffect(Player receiver, ItemStack icon, Frame frameType, Component text) {

	}

	@Override
	public void sendStatusUpdate(Player player, double health, int food, float saturation) {

	}

	@Override
	public void addGameTestMarker(Player player, Location location, int color, String name, int lifetime) {

	}

	@Override
	public void clearGameTestMarkers(Player player) {

	}

	@Override
	public byte getEntityMetadata(Entity entity) {
		return 0;
	}

	@Override
	public GlowManager getGlowManager() {
		return null;
	}

}
