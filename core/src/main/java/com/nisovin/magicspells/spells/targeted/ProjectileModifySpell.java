package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import org.apache.commons.math3.util.FastMath;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.instant.ParticleProjectileSpell;
import com.nisovin.magicspells.util.trackers.ParticleProjectileTracker;

public class ProjectileModifySpell extends TargetedSpell implements TargetedLocationSpell {

	private ConfigData<Integer> cone;
	private ConfigData<Integer> vRadius;
	private ConfigData<Integer> hRadius;
	private ConfigData<Integer> maxTargets;

	private boolean pointBlank;
	private boolean claimProjectiles;

	private SpellFilter filter;

	private ConfigData<Float> velocity;

	private ConfigData<Float> acceleration;
	private ConfigData<Integer> accelerationDelay;

	private ConfigData<Float> projectileTurn;
	private ConfigData<Float> projectileVertGravity;
	private ConfigData<Float> projectileHorizGravity;

	private ConfigData<Integer> tickInterval;
	private ConfigData<Integer> spellInterval;
	private ConfigData<Integer> tickSpellLimit;
	private ConfigData<Integer> maxEntitiesHit;
	private ConfigData<Integer> intermediateEffects;
	private ConfigData<Integer> intermediateHitboxes;
	private ConfigData<Integer> specialEffectInterval;

	private ConfigData<Float> hitRadius;
	private ConfigData<Float> verticalHitRadius;
	private ConfigData<Integer> groundHitRadius;
	private ConfigData<Integer> groundVerticalHitRadius;

	private ConfigData<Double> maxDuration;
	private ConfigData<Double> maxDistance;

	private boolean hugSurface;
	private ConfigData<Float> heightFromSurface;

	private boolean controllable;
	private boolean hitGround;
	private boolean hitAirAtEnd;
	private boolean hitAirDuring;
	private boolean hitAirAfterDuration;
	private boolean stopOnHitGround;
	private boolean stopOnModifierFail;

	private Subspell airSpell;
	private Subspell selfSpell;
	private Subspell tickSpell;
	private Subspell entitySpell;
	private Subspell groundSpell;
	private Subspell durationSpell;
	private Subspell modifierSpell;
	private Subspell entityLocationSpell;
	private String airSpellName;
	private String selfSpellName;
	private String tickSpellName;
	private String entitySpellName;
	private String groundSpellName;
	private String durationSpellName;
	private String modifierSpellName;
	private String entityLocationSpellName;

	private ModifierSet projModifiers;
	private List<String> projModifiersStrings;

	public ProjectileModifySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		cone = getConfigDataInt("cone", 0);
		vRadius = getConfigDataInt("vertical-radius", 5);
		hRadius = getConfigDataInt("horizontal-radius", 10);
		maxTargets = getConfigDataInt("max-targets", 0);

		pointBlank = getConfigBoolean("point-blank", true);
		claimProjectiles = getConfigBoolean("claim-projectiles", false);

		List<String> spells = getConfigStringList("spells", null);
		List<String> deniedSpells = getConfigStringList("denied-spells", null);
		List<String> spellTags = getConfigStringList("spell-tags", null);
		List<String> deniedSpellTags = getConfigStringList("denied-spell-tags", null);

		filter = new SpellFilter(spells, deniedSpells, spellTags, deniedSpellTags);

		velocity = getConfigDataFloat("projectile-velocity", 1F);
		acceleration = getConfigDataFloat("projectile-acceleration", 0F);
		accelerationDelay = getConfigDataInt("projectile-acceleration-delay", 0);

		projectileTurn = getConfigDataFloat("projectile-turn", 0);
		projectileVertGravity = getConfigDataFloat("projectile-vert-gravity", 0F);
		projectileHorizGravity = getConfigDataFloat("projectile-horiz-gravity", 0F);

		tickInterval = getConfigDataInt("tick-interval", 2);
		spellInterval = getConfigDataInt("spell-interval", 20);
		intermediateEffects = getConfigDataInt("intermediate-effects", 0);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 1);

		maxDuration = getConfigDataDouble("max-duration", 0);
		maxDistance = getConfigDataDouble("max-distance", 15);

		intermediateHitboxes = getConfigDataInt("intermediate-hitboxes", 0);
		tickSpellLimit = getConfigDataInt("tick-spell-limit", 0);
		maxEntitiesHit = getConfigDataInt("max-entities-hit", 0);
		hitRadius = getConfigDataFloat("hit-radius", 1.5F);
		verticalHitRadius = getConfigDataFloat("vertical-hit-radius", hitRadius);
		groundHitRadius = getConfigDataInt("ground-hit-radius", 1);
		groundVerticalHitRadius = getConfigDataInt("ground-vertical-hit-radius", groundHitRadius);

		hugSurface = getConfigBoolean("hug-surface", false);
		if (hugSurface) heightFromSurface = getConfigDataFloat("height-from-surface", 0.6F);

		controllable = getConfigBoolean("controllable", false);
		hitGround = getConfigBoolean("hit-ground", true);
		hitAirAtEnd = getConfigBoolean("hit-air-at-end", false);
		hitAirDuring = getConfigBoolean("hit-air-during", false);
		hitAirAfterDuration = getConfigBoolean("hit-air-after-duration", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", true);
		stopOnModifierFail = getConfigBoolean("stop-on-modifier-fail", true);

		airSpellName = getConfigString("spell-on-hit-air", "");
		selfSpellName = getConfigString("spell-on-hit-self", "");
		tickSpellName = getConfigString("spell-on-tick", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		entitySpellName = getConfigString("spell-on-hit-entity", "");
		durationSpellName = getConfigString("spell-on-duration-end", "");
		modifierSpellName = getConfigString("spell-on-modifier-fail", "");
		entityLocationSpellName = getConfigString("spell-on-entity-location", "");

		projModifiersStrings = getConfigStringList("projectile-modifiers", null);
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();

		if (projModifiersStrings != null && !projModifiersStrings.isEmpty()) {
			projModifiers = new ModifierSet(projModifiersStrings, this);
			projModifiersStrings = null;
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		airSpell = new Subspell(airSpellName);
		if (!airSpell.process() || !airSpell.isTargetedLocationSpell()) {
			if (!airSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-hit-air defined!");
			airSpell = null;
		}

		selfSpell = new Subspell(selfSpellName);
		if (!selfSpell.process()) {
			if (!selfSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-hit-self defined!");
			selfSpell = null;
		}

		tickSpell = new Subspell(tickSpellName);
		if (!tickSpell.process() || !tickSpell.isTargetedLocationSpell()) {
			if (!tickSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-tick defined!");
			tickSpell = null;
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process() || !groundSpell.isTargetedLocationSpell()) {
			if (!groundSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
			groundSpell = null;
		}

		entitySpell = new Subspell(entitySpellName);
		if (!entitySpell.process()) {
			if (!entitySpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-hit-entity defined!");
			entitySpell = null;
		}

		durationSpell = new Subspell(durationSpellName);
		if (!durationSpell.process()) {
			if (!durationSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-duration-end defined!");
			durationSpell = null;
		}

		modifierSpell = new Subspell(modifierSpellName);
		if (!modifierSpell.process()) {
			if (!modifierSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-modifier-fail defined!");
			modifierSpell = null;
		}

		entityLocationSpell = new Subspell(entityLocationSpellName);
		if (!entityLocationSpell.process() || !entityLocationSpell.isTargetedLocationSpell()) {
			if (!entityLocationSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-entity-location defined!");
			entityLocationSpell = null;
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			if (pointBlank) loc = caster.getLocation();
			else {
				try {
					Block block = getTargetedBlock(caster, power);
					if (block != null && !BlockUtils.isAir(block.getType())) loc = block.getLocation();
				} catch (IllegalStateException ignored) {}
			}
			if (loc == null) return noTarget(caster);

			modify(caster, loc, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		return modify(caster, target, power, args);
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return modify(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return modify(null, target, power, args);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return modify(null, target, power, null);
	}

	private boolean modify(LivingEntity caster, Location location, float power, String[] args) {
		int count = 0;

		Vector facing = caster != null ? caster.getLocation().getDirection() : location.getDirection();
		Vector vLoc = caster != null ? caster.getLocation().toVector() : location.toVector();

		BoundingBox box = new BoundingBox(location, hRadius.get(caster, null, power, args), vRadius.get(caster, null, power, args));

		Set<ParticleProjectileTracker> trackerSet = ParticleProjectileSpell.getProjectileTrackers();

		int maxTargets = this.maxTargets.get(caster, null, power, args);
		int cone = this.cone.get(caster, null, power, args);
		for (ParticleProjectileTracker tracker : trackerSet) {
			if (tracker == null) continue;
			if (!tracker.getCurrentLocation().getWorld().equals(location.getWorld())) continue;
			if (!box.contains(tracker.getCurrentLocation())) continue;
			if (tracker.getSpell() != null && !filter.check(tracker.getSpell())) continue;

			if (pointBlank && cone > 0) {
				Vector dir = tracker.getCurrentLocation().toVector().subtract(vLoc);
				if (FastMath.abs(dir.angle(facing)) > cone) continue;
			}

			if (claimProjectiles) tracker.setCaster(caster);

			tracker.setAcceleration(acceleration.get(caster, null, power, args));
			tracker.setAccelerationDelay(accelerationDelay.get(caster, null, power, args));

			tracker.setProjectileTurn(projectileTurn.get(caster, null, power, args));
			tracker.setProjectileVertGravity(projectileVertGravity.get(caster, null, power, args));
			tracker.setProjectileHorizGravity(projectileHorizGravity.get(caster, null, power, args));
			tracker.setTickInterval(tickInterval.get(caster, null, power, args));
			tracker.setSpellInterval(spellInterval.get(caster, null, power, args));
			tracker.setIntermediateEffects(intermediateEffects.get(caster, null, power, args));
			tracker.setIntermediateHitboxes(intermediateHitboxes.get(caster, null, power, args));
			tracker.setSpecialEffectInterval(specialEffectInterval.get(caster, null, power, args));
			double maxDistance = this.maxDistance.get(caster, null, power, args);
			tracker.setMaxDistanceSquared(maxDistance * maxDistance);
			tracker.setMaxDuration(maxDuration.get(caster, null, power, args) * TimeUtil.MILLISECONDS_PER_SECOND);
			tracker.setMaxEntitiesHit(maxEntitiesHit.get(caster, null, power, args));
			tracker.setHorizontalHitRadius(hitRadius.get(caster, null, power, args));
			tracker.setVerticalHitRadius(verticalHitRadius.get(caster, null, power, args));
			tracker.setGroundHorizontalHitRadius(groundHitRadius.get(caster, null, power, args));
			tracker.setGroundVerticalHitRadius(groundVerticalHitRadius.get(caster, null, power, args));
			tracker.setHugSurface(hugSurface);
			tracker.setHeightFromSurface(hugSurface ? heightFromSurface.get(caster, null, power, args) : 0);
			tracker.setControllable(controllable);
			tracker.setHitGround(hitGround);
			tracker.setHitAirAtEnd(hitAirAtEnd);
			tracker.setHitAirDuring(hitAirDuring);
			tracker.setHitAirAfterDuration(hitAirAfterDuration);
			tracker.setStopOnHitGround(stopOnHitGround);
			tracker.setStopOnModifierFail(stopOnModifierFail);
			tracker.setProjectileModifiers(projModifiers);
			tracker.setTickSpellLimit(tickSpellLimit.get(caster, null, power, args));
			if (airSpell != null) tracker.setAirSpell(airSpell);
			if (tickSpell != null) tracker.setTickSpell(tickSpell);
			if (selfSpell != null) tracker.setCasterSpell(selfSpell);
			if (groundSpell != null) tracker.setGroundSpell(groundSpell);
			if (entitySpell != null) tracker.setEntitySpell(entitySpell);
			if (durationSpell != null) tracker.setDurationSpell(durationSpell);
			if (modifierSpell != null) tracker.setModifierSpell(modifierSpell);
			if (entityLocationSpell != null) tracker.setEntityLocationSpell(entityLocationSpell);

			tracker.getCurrentVelocity().multiply(velocity.get(caster, null, power, args));

			playSpellEffects(EffectPosition.TARGET, tracker.getCurrentLocation());
			playSpellEffectsTrail(location, tracker.getCurrentLocation());
			if (caster != null) playSpellEffectsTrail(caster.getLocation(), tracker.getCurrentLocation());

			count++;

			if (maxTargets > 0 && count >= maxTargets) break;
		}

		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster);
		playSpellEffects(EffectPosition.SPECIAL, location);

		return count > 0;
	}

}
