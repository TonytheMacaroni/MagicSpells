package com.nisovin.magicspells.spells.instant;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.BufferedWriter;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class MarkSpell extends InstantSpell implements TargetedLocationSpell {

	private Map<UUID, Location> marks;

	private Location defaultMark;

	private boolean permanentMarks;
	private boolean enableDefaultMarks;
	private boolean useAsRespawnLocation;

	public MarkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		marks = new HashMap<>();

		permanentMarks = getConfigBoolean("permanent-marks", true);
		enableDefaultMarks = getConfigBoolean("enable-default-marks", false);
		useAsRespawnLocation = getConfigBoolean("use-as-respawn-location", false);

		if (enableDefaultMarks) {
			defaultMark = LocationUtil.fromString(getConfigString("default-mark", "world,0,0,0"));
			if (defaultMark == null) MagicDebug.warn("Invalid 'default-mark' defined %s.", MagicDebug.resolveFullPath());
		}

		if (permanentMarks) loadMarks();
	}

	@Override
	public CastResult cast(SpellData data) {
		marks.put(getKey(data.caster()), data.caster().getLocation());
		if (permanentMarks) saveMarks();
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		marks.put(getKey(data.caster()), data.location());
		if (permanentMarks) saveMarks();
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (!permanentMarks) marks.remove(getKey(event.getPlayer()));
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (!useAsRespawnLocation) return;
		Location loc = marks.get(getKey(event.getPlayer()));
		if (loc != null) event.setRespawnLocation(loc);
		else if (enableDefaultMarks && defaultMark != null) event.setRespawnLocation(defaultMark);
	}

	public Location getDefaultMark() {
		return defaultMark;
	}

	public void setDefaultMark(Location defaultMark) {
		this.defaultMark = defaultMark;
	}

	public boolean shouldMarksBePermanent() {
		return permanentMarks;
	}

	public void setPermanentMarks(boolean permanentMarks) {
		this.permanentMarks = permanentMarks;
	}

	public boolean areDefaultMarksEnabled() {
		return enableDefaultMarks;
	}

	public void setEnableDefaultMarks(boolean enableDefaultMarks) {
		this.enableDefaultMarks = enableDefaultMarks;
	}

	public boolean shouldUseAsRespawnLocation() {
		return useAsRespawnLocation;
	}

	public void setUseAsRespawnLocation(boolean useAsRespawnLocation) {
		this.useAsRespawnLocation = useAsRespawnLocation;
	}

	public Map<UUID, Location> getMarks() {
		return marks;
	}

	public void setMarks(Map<UUID, Location> newMarks) {
		marks = newMarks;
		if (permanentMarks) saveMarks();
	}

	private void loadMarks() {
		try {
			Scanner scanner = new Scanner(new File(MagicSpells.plugin.getDataFolder(), "marks-" + internalName + ".txt"));
			while (scanner.hasNext()) {
				String line = scanner.nextLine();
				if (!line.isEmpty()) {
					try {
						String[] data = line.split(":");

						UUID uuid = UUID.fromString(data[0].toLowerCase());
						World world = Bukkit.getWorld(data[1]);
						double x = Double.parseDouble(data[2]);
						double y = Double.parseDouble(data[3]);
						double z = Double.parseDouble(data[4]);
						float yaw = Float.parseFloat(data[5]);
						float pitch = Float.parseFloat(data[6]);

						marks.put(uuid, new Location(world, x, y, z, yaw, pitch));
					} catch (Exception e) {
						MagicDebug.warn(e, "Failed to load mark '%s' for MarkSpell '%s'.", line, this);
					}
				}
			}
			scanner.close();
		} catch (Exception e) {
			MagicDebug.warn(e, "Failed to load marks file for MarkSpell '%s'.", this);
		}
	}

	private void saveMarks() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(MagicSpells.plugin.getDataFolder(), "marks-" + internalName + ".txt"), false));
			for (Map.Entry<UUID, Location> entry : marks.entrySet()) {
				UUID uuid = entry.getKey();
				Location loc = entry.getValue();
				if (!(Bukkit.getEntity(uuid) instanceof Player)) continue;

				writer.append(uuid.toString())
					.append(":")
					.append(loc.getWorld().getName())
					.append(":")
					.append(String.valueOf(loc.getX()))
					.append(":")
					.append(String.valueOf(loc.getY()))
					.append(":")
					.append(String.valueOf(loc.getZ()))
					.append(":")
					.append(String.valueOf(loc.getYaw()))
					.append(":")
					.append(String.valueOf(loc.getPitch()));
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			MagicDebug.warn(e, "Failed to save marks file for MarkSpell '%s'.", this);
			MagicSpells.error("Error saving marks with MarkSpell: " + internalName);
		}
	}

	public UUID getKey(LivingEntity livingEntity) {
		if (livingEntity == null) return null;
		return livingEntity.getUniqueId();
	}

	public boolean usesDefaultMark() {
		return enableDefaultMarks;
	}

	public Location getEffectiveMark(LivingEntity livingEntity) {
		Location mark = marks.get(getKey(livingEntity));
		if (mark == null) return enableDefaultMarks ? defaultMark : null;
		return mark;
	}

	public Location getEffectiveMark(String player) {
		Player pl = Bukkit.getPlayer(player);
		if (pl == null) return null;
		if (!pl.isOnline()) return null;
		Location mark = marks.get(pl.getUniqueId());
		if (mark == null) return enableDefaultMarks ? defaultMark : null;
		return mark;
	}

}
