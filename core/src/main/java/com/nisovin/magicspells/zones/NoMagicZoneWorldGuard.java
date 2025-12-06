package com.nisovin.magicspells.zones;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.regions.RegionType;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.DependsOn;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.debug.MagicDebug;

@Name("worldguard")
@DependsOn("WorldGuard")
public class NoMagicZoneWorldGuard extends NoMagicZone {

	private String worldName;
	private String regionName;

	@Override
	public void initialize(ConfigurationSection config) {
		worldName = config.getString("world");
		regionName = config.getString("region");
	}

	@Override
	public boolean inZone(Location location) {
		if (regionName == null) {
			MagicDebug.warn(DebugCategory.NO_MAGIC_ZONE, "Failed to access WorldGuard region - no region specified.");
			return false;
		}

		if (worldName == null) {
			MagicDebug.warn(DebugCategory.NO_MAGIC_ZONE, "Failed to access WorldGuard region '%s' - no world specified.", regionName);
			return false;
		}

		World world = location.getWorld();
		if (world == null || !world.getName().equals(worldName)) return false;

		RegionManager regionManager = WorldGuard.getInstance()
			.getPlatform()
			.getRegionContainer()
			.get(BukkitAdapter.adapt(world));

		ProtectedRegion region = regionManager == null ? null : regionManager.getRegion(regionName);
		if (region == null) {
			MagicDebug.warn(DebugCategory.NO_MAGIC_ZONE, "Failed to access WorldGuard region %s' - no such region exists.", regionName);
			return false;
		}

		return region.getType() == RegionType.GLOBAL || region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

}
