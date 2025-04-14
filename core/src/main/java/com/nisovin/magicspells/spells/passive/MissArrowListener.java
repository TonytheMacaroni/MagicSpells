package com.nisovin.magicspells.spells.passive;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;

import org.bukkit.entity.Arrow;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

@Name("missarrow")
public class MissArrowListener extends PassiveListener {

	private static ArrowTracker tracker;

	private final Set<MagicItemData> items = new HashSet<>();

	@Override
	public void initialize(@NotNull String var) {
		if (tracker == null) {
			tracker = new ArrowTracker();
			MagicSpells.registerEvents(tracker);
		}

		if (var.isEmpty()) return;

		for (String s : var.split(MagicItemDataParser.DATA_REGEX)) {
			s = s.trim();

			MagicItemData itemData = MagicItems.getMagicItemDataFromString(s);
			if (itemData == null) {
				MagicSpells.error("Invalid magic item '" + s + "' in missarrow trigger on passive spell '" + passiveSpell.getInternalName() + "'");
				continue;
			}

			items.add(itemData);
		}
	}

	@Override
	public void turnOff() {
		tracker = null;
	}

	@OverridePriority
	@EventHandler
	public void onHit(ProjectileHitEvent event) {
		if (event.getHitBlock() == null) return;
		if (!(event.getEntity() instanceof Arrow arrow) || !tracker.isTracking(arrow, this)) return;
		if (!(arrow.getShooter() instanceof LivingEntity caster) || !canTrigger(caster)) return;

		tracker.removeTracking(arrow, this);

		if (!items.isEmpty()) {
			ItemStack weapon = arrow.getWeapon();
			if (weapon == null) return;

			MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(weapon);
			if (itemData == null || !contains(itemData)) return;
		}

		passiveSpell.activate(caster, arrow.getLocation());
	}

	private boolean contains(MagicItemData itemData) {
		for (MagicItemData data : items)
			if (data.matches(itemData))
				return true;

		return false;
	}

	private static class ArrowTracker implements Listener {

		private final Multimap<UUID, MissArrowListener> untracked = HashMultimap.create();
		private final Set<UUID> tracking = new HashSet<>();

		@EventHandler
		public void onShoot(ProjectileLaunchEvent event) {
			if (!(event.getEntity() instanceof Arrow arrow)) return;
			if (!(arrow.getShooter() instanceof LivingEntity)) return;

			tracking.add(arrow.getUniqueId());
		}

		@EventHandler
		public void onRemove(EntityRemoveEvent event) {
			UUID uuid = event.getEntity().getUniqueId();
			tracking.remove(uuid);
			untracked.removeAll(uuid);
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onDamage(EntityDamageByEntityEvent event) {
			UUID uuid = event.getEntity().getUniqueId();
			tracking.remove(uuid);
			untracked.removeAll(uuid);
		}

		public boolean isTracking(Arrow arrow, MissArrowListener instance) {
			UUID uuid = arrow.getUniqueId();
			return tracking.contains(uuid) && !untracked.containsEntry(uuid, instance);
		}

		public void removeTracking(Arrow arrow, MissArrowListener instance) {
			untracked.put(arrow.getUniqueId(), instance);
		}

	}

}
