package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.block.data.BlockData;

import de.slikey.effectlib.util.VectorUtils;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.managers.VariableManager;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class AreaScanSpell extends TargetedSpell implements TargetedLocationSpell {

	private Set<BlockData> blocks;
	private Set<BlockData> deniedBlocks;

	private final ConfigData<Integer> xRadius;
	private final ConfigData<Integer> yRadius;
	private final ConfigData<Integer> zRadius;
	private final ConfigData<Integer> maxBlocks;
	private final ConfigData<Integer> xInnerRadius;
	private final ConfigData<Integer> yInnerRadius;
	private final ConfigData<Integer> zInnerRadius;

	private final ConfigData<Float> tolerance;
	private final ConfigData<Float> innerTolerance;

	private final ConfigData<Shape> shape;

	private final ConfigData<Vector> absoluteOffset;
	private final ConfigData<Vector> relativeOffset;

	private final ConfigData<String> xVariable;
	private final ConfigData<String> yVariable;
	private final ConfigData<String> zVariable;

	private final ConfigData<Boolean> pointBlank;
	private final ConfigData<Boolean> blockCoords;
	private final ConfigData<Boolean> failIfNoTargets;
	private final ConfigData<Boolean> powerAffectsRadius;
	private final ConfigData<Boolean> powerAffectsMaxBlocks;

	private String spellToCastName;
	private Subspell spellToCast;

	private List<String> scanModifierStrings;
	private ModifierSet scanModifiers;

	public AreaScanSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		ConfigData<Integer> radius = getConfigDataInt("radius", 0);
		xRadius = getConfigDataInt("x-radius", radius);
		yRadius = getConfigDataInt("y-radius", radius);
		zRadius = getConfigDataInt("z-radius", radius);

		ConfigData<Integer> innerRadius = getConfigDataInt("inner-radius", -1);
		xInnerRadius = getConfigDataInt("inner-x-radius", innerRadius);
		yInnerRadius = getConfigDataInt("inner-y-radius", innerRadius);
		zInnerRadius = getConfigDataInt("inner-z-radius", innerRadius);

		maxBlocks = getConfigDataInt("max-blocks", 0);

		tolerance = getConfigDataFloat("tolerance", 1);
		innerTolerance = getConfigDataFloat("inner-tolerance", tolerance);

		absoluteOffset = getConfigDataVector("absolute-offset", new Vector());
		relativeOffset = getConfigDataVector("relative-offset", new Vector());

		shape = getConfigDataEnum("shape", Shape.class, Shape.BOX);

		xVariable = getConfigDataString("x-variable", null);
		yVariable = getConfigDataString("y-variable", null);
		zVariable = getConfigDataString("z-variable", null);
		spellToCastName = getConfigString("spell", "");

		pointBlank = getConfigDataBoolean("point-blank", false);
		blockCoords = getConfigDataBoolean("block-coords", false);
		failIfNoTargets = getConfigDataBoolean("fail-if-not-found", true);
		powerAffectsRadius = getConfigDataBoolean("power-affects-radius", true);
		powerAffectsMaxBlocks = getConfigDataBoolean("power-affects-max-blocks", true);

		List<String> blockStrings = getConfigStringList("blocks", null);
		if (blockStrings != null && !blockStrings.isEmpty()) {
			blocks = new HashSet<>();

			for (String blockDataString : blockStrings) {
				try {
					blocks.add(Bukkit.createBlockData(blockDataString));
				} catch (IllegalArgumentException e) {
					MagicSpells.error("Invalid block '" + blockDataString + "' in AreaScanSpell '" + internalName + "'.");
				}
			}

			if (blocks.isEmpty()) blocks = null;
		}

		List<String> deniedBlockStrings = getConfigStringList("denied-blocks", null);
		if (deniedBlockStrings != null && !deniedBlockStrings.isEmpty()) {
			deniedBlocks = new HashSet<>();

			for (String blockDataString : deniedBlockStrings) {
				try {
					deniedBlocks.add(Bukkit.createBlockData(blockDataString));
				} catch (IllegalArgumentException e) {
					MagicSpells.error("Invalid denied block '" + blockDataString + "' in AreaScanSpell '" + internalName + "'.");
				}

			}

			if (deniedBlocks.isEmpty()) deniedBlocks = null;
		}

		scanModifierStrings = getConfigStringList("scan-modifiers", null);
	}

	@Override
	public void initialize() {
		super.initialize();

		spellToCast = initSubspell(spellToCastName, false, "for 'spell'");
		spellToCastName = null;
	}

	@Override
	protected void initializeModifiers() {
		super.initializeModifiers();

		if (scanModifierStrings != null && !scanModifierStrings.isEmpty())
			scanModifiers = new ModifierSet(scanModifierStrings, this);

		scanModifierStrings = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		if (pointBlank.get(data)) {
			SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data, data.caster().getLocation());
			if (!event.callEvent()) return noTarget(event);
			data = event.getSpellData();
		} else {
			TargetInfo<Location> info = getTargetedBlockLocation(data);
			if (info.noTarget()) return noTarget(info);
			data = info.spellData();
		}

		return castAtLocation(data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Location origin = data.location();
		if (blockCoords.get(data)) {
			origin.set(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
			data = data.location(origin);
		}

		Vector relativeOffset = this.relativeOffset.get(data);
		if (!relativeOffset.isZero()) {
			origin.add(VectorUtils.rotateVector(relativeOffset, origin));
			data = data.location(origin);
		}

		Vector absoluteOffset = this.absoluteOffset.get(data);
		if (!absoluteOffset.isZero()) {
			origin.add(absoluteOffset);
			data = data.location(origin);
		}

		boolean failIfNoTargets = this.failIfNoTargets.get(data);

		String xVariable = this.xVariable.get(data);
		String yVariable = this.yVariable.get(data);
		String zVariable = this.zVariable.get(data);

		int xRadius = this.xRadius.get(data);
		int yRadius = this.yRadius.get(data);
		int zRadius = this.zRadius.get(data);
		if (xRadius < 0 || yRadius < 0 || zRadius < 0) return noTarget(data);

		int xInnerRadius = this.xInnerRadius.get(data);
		int yInnerRadius = this.yInnerRadius.get(data);
		int zInnerRadius = this.zInnerRadius.get(data);

		if (powerAffectsRadius.get(data)) {
			xRadius = Math.round(xRadius * data.power());
			yRadius = Math.round(yRadius * data.power());
			zRadius = Math.round(zRadius * data.power());

			xInnerRadius = Math.round(xInnerRadius * data.power());
			yInnerRadius = Math.round(yInnerRadius * data.power());
			zInnerRadius = Math.round(zInnerRadius * data.power());
		}

		xRadius = Math.min(xRadius, MagicSpells.getGlobalRadius());
		yRadius = Math.min(yRadius, MagicSpells.getGlobalRadius());
		zRadius = Math.min(zRadius, MagicSpells.getGlobalRadius());

		xInnerRadius = Math.min(xInnerRadius, MagicSpells.getGlobalRadius());
		yInnerRadius = Math.min(yInnerRadius, MagicSpells.getGlobalRadius());
		zInnerRadius = Math.min(zInnerRadius, MagicSpells.getGlobalRadius());

		int count = this.maxBlocks.get(data);
		if (powerAffectsMaxBlocks.get(data)) count = Math.round(count * data.power());

		Shape shape = this.shape.get(data);
		float xRadiusInv = shape == Shape.X_CYLINDER || xRadius == 0 ? 0 : 1f / (xRadius * xRadius);
		float yRadiusInv = shape == Shape.Y_CYLINDER || yRadius == 0 ? 0 : 1f / (yRadius * yRadius);
		float zRadiusInv = shape == Shape.Z_CYLINDER || zRadius == 0 ? 0 : 1f / (zRadius * zRadius);
		float xInnerRadiusInv = shape == Shape.X_CYLINDER ? 0 : 1f / (xInnerRadius * xInnerRadius);
		float yInnerRadiusInv = shape == Shape.Y_CYLINDER ? 0 : 1f / (yInnerRadius * yInnerRadius);
		float zInnerRadiusInv = shape == Shape.Z_CYLINDER ? 0 : 1f / (zInnerRadius * zInnerRadius);

		float tolerance = this.tolerance.get(data);
		float innerTolerance = this.innerTolerance.get(data);

		boolean cull = xInnerRadius >= 0 && yInnerRadius >= 0 && zInnerRadius >= 0;
		boolean boxCull = shape == Shape.BOX && cull;

		VariableManager manager = MagicSpells.getVariableManager();
		String playerCaster = data.caster() instanceof Player player ? player.getName() : null;

		int minRadius = boxCull ? Math.min(xInnerRadius, Math.min(yInnerRadius, zInnerRadius)) : 0;
		int maxRadius = Math.max(xRadius, Math.max(yRadius, zRadius));

		boolean found = false;
		loop:
		for (int d = minRadius; d <= maxRadius; d++) {
			int xBound = Math.min(xRadius, d);
			if (boxCull && xBound <= xInnerRadius) continue;

			for (int xOffset = -xBound; xOffset <= xBound; xOffset++) {
				int yBound = Math.min(yRadius, d);
				if (boxCull && yBound <= yInnerRadius) continue;

				for (int yOffset = -yBound; yOffset <= yBound; yOffset++) {
					int zBound = Math.min(zRadius, d);
					if (boxCull && zBound <= zInnerRadius) continue;

					for (int zOffset = -zBound; zOffset <= zBound; zOffset++) {
						if (xOffset != d && xOffset != -d && yOffset != d && yOffset != -d && zOffset != d && zOffset != -d) continue;

						if (shape != Shape.BOX) {
							int xOffsetSq = xOffset * xOffset;
							int yOffsetSq = yOffset * yOffset;
							int zOffsetSq = zOffset * zOffset;

							if (xRadiusInv * xOffsetSq + yRadiusInv * yOffsetSq + zRadiusInv * zOffsetSq > tolerance) continue;
							if (cull && xInnerRadiusInv * xOffsetSq + yInnerRadiusInv * yOffsetSq + zInnerRadiusInv * zOffsetSq <= innerTolerance) {
								switch (shape) {
									case ELLIPSOID -> {
										continue;
									}
									case X_CYLINDER -> {
										if (xOffset >= -xInnerRadius && xOffset <= xInnerRadius) continue;
									}
									case Y_CYLINDER -> {
										if (yOffset >= -yInnerRadius && yOffset <= yInnerRadius) continue;
									}
									case Z_CYLINDER -> {
										if (zOffset >= -zInnerRadius && zOffset <= zInnerRadius) continue;
									}
								}
							}
						}

						Location target = origin.clone().add(xOffset, yOffset, zOffset);
						if (!check(target.getBlock().getBlockData())) continue;

						if (playerCaster != null) {
							if (xVariable != null) manager.set(xVariable, playerCaster, target.getX());
							if (yVariable != null) manager.set(yVariable, playerCaster, target.getY());
							if (zVariable != null) manager.set(zVariable, playerCaster, target.getZ());
						}

						SpellData subData = data.location(target);
						if (scanModifiers != null) {
							ModifierResult result = scanModifiers.apply(subData.caster(), target, subData);
							if (!result.check()) continue;

							subData = result.data();
						}

						found = true;

						if (spellToCast != null) spellToCast.subcast(subData);

						playSpellEffects(EffectPosition.TARGET, target, subData);
						playSpellEffectsTrail(origin, target, subData);

						if (count == 1) break loop;
						else if (count > 0) count--;
					}
				}
			}
		}

		boolean success = found || !failIfNoTargets;
		if (success && data.hasCaster()) playSpellEffects(EffectPosition.CASTER, data.caster(), data);

		return success ? new CastResult(PostCastAction.HANDLE_NORMALLY, data) : noTarget(data);
	}

	private boolean check(BlockData data) {
		if (deniedBlocks != null)
			for (BlockData bd : deniedBlocks)
				if (data.matches(bd))
					return false;

		if (blocks != null)
			for (BlockData bd : blocks)
				if (data.matches(bd))
					return true;

		return blocks == null;
	}

	private enum Shape {
		BOX,
		X_CYLINDER,
		Y_CYLINDER,
		Z_CYLINDER,
		ELLIPSOID
	}

}
