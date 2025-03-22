package com.nisovin.magicspells.zones;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.DependsOn;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.zones.NoMagicZone.ZoneCheckResult;

public class NoMagicZoneManager {

	private Map<String, Class<? extends NoMagicZone>> zoneTypes;
	private Map<String, NoMagicZone> zones;
	private Set<NoMagicZone> zonesOrdered;

	public NoMagicZoneManager() {
		// Create zone types
		zoneTypes = new HashMap<>();
		addZoneType(NoMagicZoneCuboid.class);
		addZoneType(NoMagicZoneWorldGuard.class);
	}

	public void load(MagicConfig config) {
		// Get zones
		zones = new HashMap<>();
		zonesOrdered = new TreeSet<>();

		try (var ignored = MagicDebug.section(DebugCategory.NO_MAGIC_ZONE, "Loading no-magic zones.")) {
			Set<String> zoneNodes = config.getKeys("no-magic-zones");
			if (zoneNodes == null) {
				MagicDebug.info("No no-magic zones found.");
				return;
			}

			for (String node : zoneNodes) {
				try (var ignored1 = MagicDebug.section("Loading no-magic zone '%s'.", node)) {
					ConfigurationSection zoneConfig = config.getSection("no-magic-zones." + node);
					if (!zoneConfig.getBoolean("enabled", true)) {
						MagicDebug.info("Zone disabled - skipping.");
						continue;
					}

					String type = zoneConfig.getString("type", null);
					if (type == null) {
						MagicDebug.warn("No 'type' specified for no-magic zone '%s'.", node);
						continue;
					}

					Class<? extends NoMagicZone> clazz = zoneTypes.get(type);
					if (clazz == null) {
						MagicDebug.warn("Invalid no-magic zone type '%s' specified on no-magic zone '%s'.", type, node);
						continue;
					}

					DependsOn dependsOn = clazz.getAnnotation(DependsOn.class);
					if (dependsOn != null && !Util.checkPluginsEnabled(dependsOn.value())) {
						MagicDebug.warn("Missing required dependencies to load no-magic zone type '%s'.", type);
						continue;
					}

					NoMagicZone zone;
					try {
						zone = clazz.getDeclaredConstructor().newInstance();
					} catch (Exception e) {
						MagicDebug.warn(e, "Encountered an error while attempting to load no-magic zone '%s'.", node);
						continue;
					}

					zone.create(node, zoneConfig);
					zones.put(node, zone);
					zonesOrdered.add(zone);
				}
			}
		}

		MagicDebug.info(DebugCategory.NO_MAGIC_ZONE, "Loaded %d no-magic zones", zones.size());
	}

	public boolean willFizzle(LivingEntity livingEntity, Spell spell) {
		return willFizzle(livingEntity.getLocation(), spell);
	}

	public boolean willFizzle(Location location, Spell spell) {
		if (zonesOrdered == null || zonesOrdered.isEmpty()) return false;
		for (NoMagicZone zone : zonesOrdered) {
			if (zone == null) return false;
			ZoneCheckResult result = zone.check(location, spell);
			if (result == ZoneCheckResult.DENY) return true;
			if (result == ZoneCheckResult.ALLOW) return false;
		}
		return false;
	}

	public boolean inZone(Player player, String zoneName) {
		return inZone(player.getLocation(), zoneName);
	}

	public boolean inZone(Location loc, String zoneName) {
		NoMagicZone zone = zones.get(zoneName);
		return zone != null && zone.inZone(loc);
	}

	@Deprecated
	public void sendNoMagicMessage(LivingEntity caster, Spell spell) {
		sendNoMagicMessage(spell, caster, null);
	}

	@Deprecated
	public void sendNoMagicMessage(Spell spell, LivingEntity caster, String[] args) {
		for (NoMagicZone zone : zonesOrdered) {
			ZoneCheckResult result = zone.check(caster.getLocation(), spell);
			if (result != ZoneCheckResult.DENY) continue;
			MagicSpells.sendMessage(zone.getMessage(), caster, args);
			return;
		}
	}

	public void sendNoMagicMessage(Spell spell, SpellData data) {
		for (NoMagicZone zone : zonesOrdered) {
			ZoneCheckResult result = zone.check(data.caster().getLocation(), spell);
			if (result != ZoneCheckResult.DENY) continue;
			MagicSpells.sendMessage(zone.getMessage(), data.caster(), data);
			return;
		}
	}

	public Map<String, NoMagicZone> getZones() {
		return zones;
	}

	/**
	 * @param type must be annotated with {@link Name}.
	 */
	public void addZoneType(Class<? extends NoMagicZone> type) {
		Name name = type.getAnnotation(Name.class);
		if (name == null) throw new IllegalStateException("Missing 'Name' annotation on NoMagicZone class: " + type.getName());
		zoneTypes.put(name.value(), type);
	}

	/**
	 * @deprecated Use {@link NoMagicZoneManager#addZoneType(Class)}
	 */
	@Deprecated(forRemoval = true)
	public void addZoneType(String name, Class<? extends NoMagicZone> clazz) {
		zoneTypes.put(name, clazz);
	}

	public void disable() {
		if (zoneTypes != null) zoneTypes.clear();
		if (zones != null) zones.clear();
		zoneTypes = null;
		zones = null;
	}

}
