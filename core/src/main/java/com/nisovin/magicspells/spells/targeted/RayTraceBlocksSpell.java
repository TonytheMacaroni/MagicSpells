package com.nisovin.magicspells.spells.targeted;

import java.util.*;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.block.BlockFace;
import org.bukkit.util.RayTraceResult;
import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.entity.LookAnchor;
import io.papermc.paper.raytracing.RayTraceTarget;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class RayTraceBlocksSpell extends TargetedSpell implements TargetedLocationSpell {

	private final Map<BlockFace, Subspell> blockFaces = new EnumMap<>(BlockFace.class);
	private Subspell spell;

	private final ConfigData<HitLocation> hitLocation;
	private final ConfigData<LookAnchor> anchor;
	private final ConfigData<Boolean> allowAir;
	private final ConfigData<Boolean> center;

	public RayTraceBlocksSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		anchor = getConfigDataEnum("anchor", LookAnchor.class, LookAnchor.EYES);
		center = getConfigDataBoolean("center", false);
		allowAir = getConfigDataBoolean("allow-air", false);
		hitLocation = getConfigDataEnum("hit-location", HitLocation.class, HitLocation.HIT_POSITION);
	}

	@Override
	protected void initialize() {
		super.initialize();

		String spellString = getConfigString("spell", null);
		spell = initSubspell(spellString, "Invalid spell '" + spellString + "' for 'spell' in RayTraceBlocksSpell '" + internalName + "'.");

		ConfigurationSection blockFacesConfig = getConfigSection("block-faces");
		if (blockFacesConfig == null) return;

		for (String blockFaceString : blockFacesConfig.getKeys(false)) {
			BlockFace blockFace;
			try {
				blockFace = BlockFace.valueOf(blockFaceString.toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid block face '" + blockFaceString + "' in RayTraceBlocksSpell '" + internalName + "'.");
				continue;
			}

			if (!blockFace.isCartesian()) {
				MagicSpells.error("Invalid block face '" + blockFaceString + "' in RayTraceBlocksSpell '" + internalName + "'.");
				continue;
			}

			Object object = blockFacesConfig.get(blockFaceString);
			if (!(object instanceof String subspellString)) {
				MagicSpells.error("Invalid spell '" + object + "' in RayTraceBlocksSpell '" + internalName + "'.");
				continue;
			}

			Subspell subspell = initSubspell(subspellString, "Invalid spell '" + subspellString + "' for block face '" + blockFaceString + "' in RayTraceBlocksSpell '" + internalName + "'.");
			if (subspell == null) continue;

			Subspell old = blockFaces.put(blockFace, subspell);
			if (old != null)
				MagicSpells.error("Duplicate block face '" + blockFaceString + "' specified in RayTraceBlocksSpell '" + internalName + "'.");
		}
	}

	@Override
	public CastResult cast(SpellData data) {
		Location start = anchor.get(data) == LookAnchor.EYES ? data.caster().getEyeLocation() : data.caster().getLocation();
		return castAtLocation(data.location(start));
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Location start = data.location();
		Vector direction = start.getDirection();
		World world = start.getWorld();
		double range = getRange(data);

		RayTraceResult result = world.rayTrace(builder -> builder
			.start(start)
			.direction(direction)
			.maxDistance(range)
			.fluidCollisionMode(losFluidCollisionMode.get(data))
			.ignorePassableBlocks(losIgnorePassableBlocks.get(data))
			.blockFilter(block -> !losTransparentBlocks.contains(block.getType()))
			.targets(RayTraceTarget.BLOCK)
		);

		Location location;
		if (result != null) {
			HitLocation hitLocation = this.hitLocation.get(data);
			location = hitLocation.getHitLocation(result, world);
		} else {
			if (!allowAir.get(data)) return noTarget(data);
			location = start.add(direction.multiply(range));
		}
		if (center.get(data)) location = location.toCenterLocation();

		Vector hitDirection = location.toVector().subtract(start.toVector());
		location.setDirection(hitDirection.isZero() ? start.getDirection() : hitDirection);

		SpellData subData = data.location(location);

		if (result != null) {
			Subspell subspell = blockFaces.get(result.getHitBlockFace());

			if (subspell != null) {
				if (subspell.subcast(subData).success()) {
					playSpellEffects(subData);
					return new CastResult(PostCastAction.HANDLE_NORMALLY, subData);
				}

				return noTarget(subData);
			}
		}

		if (spell == null) return noTarget(subData);

		if (spell.subcast(subData).success()) {
			playSpellEffects(subData);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, subData);
		}

		return noTarget(subData);
	}

	private enum HitLocation {

		HIT_POSITION,
		HIT_BLOCK,
		LAST_BLOCK;

		public Location getHitLocation(RayTraceResult result, World world) {
			return switch (this) {
				case HIT_POSITION -> result.getHitPosition().toLocation(world);
				case HIT_BLOCK -> result.getHitBlock().getLocation();
				case LAST_BLOCK -> {
					Block block = result.getHitBlock().getRelative(result.getHitBlockFace());
					yield block.getLocation();
				}
			};
		}

	}

}
