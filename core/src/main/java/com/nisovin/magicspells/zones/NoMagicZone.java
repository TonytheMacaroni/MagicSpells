package com.nisovin.magicspells.zones;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.DependsOn;
import com.nisovin.magicspells.util.SpellFilter;

/**
 * Annotations:
 * <ul>
 *     <li>{@link Name} (required): Holds the configuration name of the no magic zone.</li>
 *     <li>{@link DependsOn} (optional): Requires listed plugins to be enabled before this no magic zone is created.</li>
 * </ul>
 */
public abstract class NoMagicZone implements Comparable<NoMagicZone> {

	private String id;
	private String message;

	private int priority;

	private boolean allowAll;
	private boolean disallowAll;

	private SpellFilter spellFilter;
	
	public final void create(String id, ConfigurationSection config) {
		this.id = id;
		message = config.getString("message", "You are in a no-magic zone.");

		priority = config.getInt("priority", 0);

		allowAll = config.getBoolean("allow-all", false);
		disallowAll = config.getBoolean("disallow-all", true);
		spellFilter = SpellFilter.fromLegacySection(config, "");

		initialize(config);
	}
	
	public abstract void initialize(ConfigurationSection config);
	
	public final ZoneCheckResult check(Player player, Spell spell) {
		return check(player.getLocation(), spell);
	}
	
	public final ZoneCheckResult check(Location location, Spell spell) {
		if (!inZone(location)) return ZoneCheckResult.IGNORED;
		if (disallowAll) return ZoneCheckResult.DENY;
		if (allowAll) return ZoneCheckResult.ALLOW;
		return spellFilter.check(spell) ? ZoneCheckResult.ALLOW : ZoneCheckResult.DENY;
	}
	
	public abstract boolean inZone(Location location);
	
	public String getId() {
		return id;
	}
	
	public String getMessage() {
		return message;
	}
	
	@Override
	public int compareTo(NoMagicZone other) {
		if (priority < other.priority) return 1;
		if (priority > other.priority) return -1;
		return id.compareTo(other.id);
	}
	
	public enum ZoneCheckResult {
		
		ALLOW,
		DENY,
		IGNORED
		
	}
	
}
