package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.FluidCollisionMode;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.trackers.Interaction;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;
import com.nisovin.magicspells.util.trackers.ParticleProjectileTracker;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

public class ParticleProjectileSpell extends InstantSpell implements TargetedLocationSpell, TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private static Set<ParticleProjectileTracker> trackerSet;

	private final ConfigData<Float> targetYOffset;
	private final ConfigData<Float> startXOffset;
	private final ConfigData<Float> startYOffset;
	private final ConfigData<Float> startZOffset;
	private final ConfigData<Vector> relativeOffset;
	private final ConfigData<Vector> effectOffset;

	private final ConfigData<Float> acceleration;
	private final ConfigData<Integer> accelerationDelay;
	private final ConfigData<Float> projectileTurn;
	private final ConfigData<Float> projectileVelocity;
	private final ConfigData<Float> projectileVertOffset;
	private final ConfigData<Float> projectileHorizOffset;
	private final ConfigData<Double> verticalRotation;
	private final ConfigData<Double> horizontalRotation;
	private final ConfigData<Double> xRotation;
	private final ConfigData<Float> projectileVertSpread;
	private final ConfigData<Float> projectileHorizSpread;
	private final ConfigData<Float> projectileVertGravity;
	private final ConfigData<Float> projectileHorizGravity;

	private final ConfigData<Integer> tickInterval;
	private final ConfigData<Integer> spellInterval;
	private final ConfigData<Integer> intermediateEffects;
	private final ConfigData<Integer> specialEffectInterval;

	private final ConfigData<Integer> tickSpellLimit;
	private final ConfigData<Integer> intermediateHitboxes;
	private final ConfigData<Integer> maxEntitiesHit;
	private final ConfigData<Float> hitRadius;
	private final ConfigData<Float> verticalHitRadius;
	private final ConfigData<Float> groundHitRadius;
	private final ConfigData<Float> groundVerticalHitRadius;
	private final Set<Material> groundMaterials;
	private final Set<Material> disallowedGroundMaterials;

	private final ConfigData<Double> maxDuration;
	private final ConfigData<Double> maxDistance;
	private final ConfigData<Double> maxHeightCheck;
	private final ConfigData<Double> startHeightCheck;

	private final ConfigData<Boolean> hugSurface;
	private final ConfigData<Float> heightFromSurface;

	private final ConfigData<Boolean> controllable;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> changePitch;
	private final ConfigData<Boolean> hitGround;
	private final ConfigData<Boolean> hitAirAtEnd;
	private final ConfigData<Boolean> hitAirDuring;
	private final ConfigData<Boolean> hitAirAfterDuration;
	private final ConfigData<Boolean> stopOnHitEntity;
	private final ConfigData<Boolean> stopOnHitGround;
	private final ConfigData<Boolean> stopOnModifierFail;
	private final ConfigData<Boolean> allowCasterInteract;
	private final ConfigData<Boolean> ignorePassableBlocks;
	private final ConfigData<Boolean> powerAffectsVelocity;

	private final ConfigData<FluidCollisionMode> fluidCollisionMode;

	private ModifierSet projModifiers;
	private final List<?> interactionData;
	private List<Interaction> interactions;

	private Subspell airSpell;
	private Subspell selfSpell;
	private Subspell tickSpell;
	private Subspell entitySpell;
	private Subspell groundSpell;
	private Subspell durationSpell;
	private Subspell modifierSpell;
	private Subspell entityLocationSpell;

	public ParticleProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		trackerSet = new HashSet<>();

		// Compatibility with start-forward-offset
		startXOffset = getConfigDataFloat("start-x-offset", getConfigDataFloat("start-forward-offset", 1F));
		startYOffset = getConfigDataFloat("start-y-offset", 1F);
		startZOffset = getConfigDataFloat("start-z-offset", 0F);
		targetYOffset = getConfigDataFloat("target-y-offset", 0F);

		relativeOffset = getConfigDataVector("relative-offset", new Vector(1, 1, 0));
		effectOffset = getConfigDataVector("effect-offset", new Vector());

		acceleration = getConfigDataFloat("projectile-acceleration", 0F);
		accelerationDelay = getConfigDataInt("projectile-acceleration-delay", 0);

		projectileTurn = getConfigDataFloat("projectile-turn", 0);
		projectileVelocity = getConfigDataFloat("projectile-velocity", 10F);
		projectileVertOffset = getConfigDataFloat("projectile-vert-offset", 0F);
		projectileHorizOffset = getConfigDataFloat("projectile-horiz-offset", 0F);
		verticalRotation = getConfigDataDouble("vertical-rotation", 0F);
		horizontalRotation = getConfigDataDouble("horizontal-rotation", 0F);
		xRotation = getConfigDataDouble("x-rotation", 0F);

		projectileVertGravity = getConfigDataFloat("projectile-vert-gravity", getConfigDataFloat("projectile-gravity", 0F));
		projectileHorizGravity = getConfigDataFloat("projectile-horiz-gravity", 0F);

		projectileVertSpread = getConfigDataFloat("projectile-vertical-spread", getConfigDataFloat("projectile-spread", 0F));
		projectileHorizSpread = getConfigDataFloat("projectile-horizontal-spread", getConfigDataFloat("projectile-spread", 0F));

		tickInterval = getConfigDataInt("tick-interval", 2);
		spellInterval = getConfigDataInt("spell-interval", 20);
		intermediateEffects = getConfigDataInt("intermediate-effects", 0);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 1);

		maxDistance = getConfigDataDouble("max-distance", 15);
		maxDuration = getConfigDataDouble("max-duration", 0);

		tickSpellLimit = getConfigDataInt("tick-spell-limit", 0);
		intermediateHitboxes = getConfigDataInt("intermediate-hitboxes", 0);
		maxEntitiesHit = getConfigDataInt("max-entities-hit", 0);
		hitRadius = getConfigDataFloat("hit-radius", 1.5F);
		verticalHitRadius = getConfigDataFloat("vertical-hit-radius", hitRadius);
		groundHitRadius = getConfigDataFloat("ground-hit-radius", 0);
		groundVerticalHitRadius = getConfigDataFloat("ground-vertical-hit-radius", groundHitRadius);

		groundMaterials = new HashSet<>();
		List<String> groundMaterialNames = getConfigStringList("ground-materials", null);
		if (groundMaterialNames != null) {
			for (String str : groundMaterialNames) {
				Material material = Util.getMaterial(str);
				if (material == null || !material.isBlock()) continue;

				groundMaterials.add(material);
			}
		}

		disallowedGroundMaterials = new HashSet<>();
		List<String> disallowedGroundMaterialNames = getConfigStringList("disallowed-ground-materials", null);
		if (disallowedGroundMaterialNames != null) {
			for (String str : disallowedGroundMaterialNames) {
				Material material = Util.getMaterial(str);
				if (material == null || !material.isBlock()) continue;

				disallowedGroundMaterials.add(material);
			}
		}

		hugSurface = getConfigDataBoolean("hug-surface", false);
		heightFromSurface = getConfigDataFloat("height-from-surface", 0.6F);
		maxHeightCheck = getConfigDataDouble("max-height-check", 10);
		startHeightCheck = getConfigDataDouble("start-height-check", 10);

		controllable = getConfigDataBoolean("controllable", false);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		changePitch = getConfigDataBoolean("change-pitch", true);
		hitGround = getConfigDataBoolean("hit-ground", true);
		hitAirAtEnd = getConfigDataBoolean("hit-air-at-end", false);
		hitAirDuring = getConfigDataBoolean("hit-air-during", false);
		hitAirAfterDuration = getConfigDataBoolean("hit-air-after-duration", false);
		stopOnHitGround = getConfigDataBoolean("stop-on-hit-ground", true);
		stopOnHitEntity = getConfigDataBoolean("stop-on-hit-entity", true);
		stopOnModifierFail = getConfigDataBoolean("stop-on-modifier-fail", true);
		allowCasterInteract = getConfigDataBoolean("allow-caster-interact", true);
		ignorePassableBlocks = getConfigDataBoolean("ignore-passable-blocks", true);
		powerAffectsVelocity = getConfigDataBoolean("power-affects-velocity", true);

		fluidCollisionMode = getConfigDataEnum("fluid-collision-mode", FluidCollisionMode.class, FluidCollisionMode.NEVER);

		// Target List
		boolean hitSelf = getConfigBoolean("hit-self", false);
		validTargetList.enforce(ValidTargetList.TargetingElement.TARGET_SELF, hitSelf);

		boolean hitPlayers = getConfigBoolean("hit-players", false);
		validTargetList.enforce(ValidTargetList.TargetingElement.TARGET_PLAYERS, hitPlayers);

		boolean hitNonPlayers = getConfigBoolean("hit-non-players", true);
		validTargetList.enforce(ValidTargetList.TargetingElement.TARGET_NONPLAYERS, hitNonPlayers);

		interactionData = getConfigList("interactions", null);
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();

		projModifiers = initModifierSet("projectile-modifiers");
	}

	@Override
	public void initialize() {
		super.initialize();

		String defaultSpellName = getConfigString("spell", "");

		airSpell = initSubspell("spell-on-hit-air", defaultSpellName, true);
		selfSpell = initSubspell("spell-on-hit-self", defaultSpellName, true);
		tickSpell = initSubspell("spell-on-tick", defaultSpellName, true);
		groundSpell = initSubspell("spell-on-hit-ground", defaultSpellName, true);
		entitySpell = initSubspell("spell-on-hit-entity", defaultSpellName, true);
		durationSpell = initSubspell("spell-on-duration-end", defaultSpellName, true);
		modifierSpell = initSubspell("spell-on-modifier-fail", defaultSpellName, true);
		entityLocationSpell = initSubspell("spell-on-entity-location", defaultSpellName, true);

		if (interactionData == null || interactionData.isEmpty()) return;
		interactions = Interaction.read(this, interactionData);
	}

	@Override
	public void turnOff() {
		for (ParticleProjectileTracker tracker : trackerSet) {
			tracker.stop(false);
		}
		trackerSet.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		ParticleProjectileTracker tracker = new ParticleProjectileTracker(data);
		setupTracker(tracker, data);
		tracker.start(data.caster().getLocation());

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		ParticleProjectileTracker tracker = new ParticleProjectileTracker(data);
		setupTracker(tracker, data);
		tracker.start(data.location());

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster() || !data.caster().getWorld().equals(data.target().getWorld()))
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		ParticleProjectileTracker tracker = new ParticleProjectileTracker(data);
		setupTracker(tracker, data);
		tracker.startTarget(data.caster().getLocation(), data.target());

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		Location from = data.location();
		if (!from.getWorld().equals(data.target().getWorld()))
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (Float.isNaN(from.getPitch()))
			from.setPitch(0);

		ParticleProjectileTracker tracker = new ParticleProjectileTracker(data);
		setupTracker(tracker, data);
		tracker.startTarget(from, data.target());

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public static Set<ParticleProjectileTracker> getProjectileTrackers() {
		return trackerSet;
	}

	public void playEffects(EffectPosition position, Location loc, SpellData data) {
		playSpellEffects(position, loc, data);
	}

	public void playEffects(EffectPosition position, Entity entity, SpellData data) {
		playSpellEffects(position, entity, data);
	}

	public Set<EffectlibSpellEffect> playEffectsProjectile(EffectPosition position, Location location, SpellData data) {
		return playSpellEffectLibEffects(position, location, data);
	}

	public Map<SpellEffect, DelayableEntity<Entity>> playEntityEffectsProjectile(EffectPosition position, Location location, SpellData data) {
		return playSpellEntityEffects(position, location, data);
	}

	public Set<DelayableEntity<ArmorStand>> playArmorStandEffectsProjectile(EffectPosition position, Location location, SpellData data) {
		return playSpellArmorStandEffects(position, location, data);
	}

	private void setupTracker(ParticleProjectileTracker tracker, SpellData data) {
		tracker.setSpell(this);

		Vector relativeOffset = this.relativeOffset.get(data);

		float startXOffset = (float) relativeOffset.getX();
		if (startXOffset == 1) startXOffset = this.startXOffset.get(data);

		float startYOffset = (float) relativeOffset.getY();
		if (startYOffset == 1) startYOffset = this.startYOffset.get(data);

		float startZOffset = (float) relativeOffset.getZ();
		if (startZOffset == 0) startZOffset = this.startZOffset.get(data);

		tracker.setStartXOffset(startXOffset);
		tracker.setStartYOffset(startYOffset);
		tracker.setStartZOffset(startZOffset);
		tracker.setTargetYOffset(targetYOffset.get(data));
		tracker.setEffectOffset(effectOffset.get(data));

		tracker.setAcceleration(acceleration.get(data));
		tracker.setAccelerationDelay(accelerationDelay.get(data));

		tracker.setProjectileTurn(projectileTurn.get(data));
		tracker.setProjectileVelocity(projectileVelocity.get(data));
		tracker.setVerticalRotation(AccurateMath.toRadians(verticalRotation.get(data)));
		tracker.setHorizontalRotation(AccurateMath.toRadians(horizontalRotation.get(data)));
		tracker.setXRotation(AccurateMath.toRadians(xRotation.get(data)));
		tracker.setProjectileVertOffset(projectileVertOffset.get(data));
		tracker.setProjectileHorizOffset(projectileHorizOffset.get(data));
		tracker.setProjectileVertGravity(projectileVertGravity.get(data));
		tracker.setProjectileHorizGravity(projectileHorizGravity.get(data));
		tracker.setProjectileVertSpread(projectileVertSpread.get(data));
		tracker.setProjectileHorizSpread(projectileHorizSpread.get(data));

		int tickInterval = this.tickInterval.get(data);
		tracker.setTickInterval(tickInterval);
		tracker.setTicksPerSecond(20f / tickInterval);

		tracker.setSpellInterval(spellInterval.get(data));
		tracker.setIntermediateEffects(intermediateEffects.get(data));
		tracker.setIntermediateHitboxes(intermediateHitboxes.get(data));
		tracker.setSpecialEffectInterval(specialEffectInterval.get(data));

		double maxDistance = this.maxDistance.get(data);
		tracker.setMaxDistanceSquared(maxDistance * maxDistance);

		tracker.setMaxDuration(maxDuration.get(data) * TimeUtil.MILLISECONDS_PER_SECOND);

		tracker.setTickSpellLimit(tickSpellLimit.get(data));
		tracker.setMaxEntitiesHit(stopOnHitEntity.get(data) ? 1 : maxEntitiesHit.get(data));
		tracker.setMaxHeightCheck(maxHeightCheck.get(data));
		tracker.setStartHeightCheck(startHeightCheck.get(data));
		tracker.setHorizontalHitRadius(hitRadius.get(data));
		tracker.setVerticalHitRadius(verticalHitRadius.get(data));
		tracker.setGroundHorizontalHitRadius(groundHitRadius.get(data));
		tracker.setGroundVerticalHitRadius(groundVerticalHitRadius.get(data));
		tracker.setGroundMaterials(groundMaterials);
		tracker.setDisallowedGroundMaterials(disallowedGroundMaterials);

		boolean hugSurface = this.hugSurface.get(data);
		tracker.setHugSurface(hugSurface);
		tracker.setHeightFromSurface(hugSurface ? heightFromSurface.get(data) : 0);

		tracker.setControllable(controllable.get(data));
		tracker.setCallEvents(checkPlugins.get(data));
		tracker.setChangePitch(changePitch.get(data));
		tracker.setHitGround(hitGround.get(data));
		tracker.setHitAirAtEnd(hitAirAtEnd.get(data));
		tracker.setHitAirDuring(hitAirDuring.get(data));
		tracker.setHitAirAfterDuration(hitAirAfterDuration.get(data));
		tracker.setStopOnHitGround(stopOnHitGround.get(data));
		tracker.setStopOnModifierFail(stopOnModifierFail.get(data));
		tracker.setAllowCasterInteract(allowCasterInteract.get(data));
		tracker.setIgnorePassableBlocks(ignorePassableBlocks.get(data));
		tracker.setPowerAffectsVelocity(powerAffectsVelocity.get(data));

		tracker.setFluidCollisionMode(fluidCollisionMode.get(data));

		tracker.setTargetList(validTargetList);
		tracker.setProjectileModifiers(projModifiers);
		tracker.setInteractions(interactions);

		tracker.setAirSpell(airSpell);
		tracker.setTickSpell(tickSpell);
		tracker.setCasterSpell(selfSpell);
		tracker.setGroundSpell(groundSpell);
		tracker.setEntitySpell(entitySpell);
		tracker.setDurationSpell(durationSpell);
		tracker.setModifierSpell(modifierSpell);
		tracker.setEntityLocationSpell(entityLocationSpell);
	}

	public Set<Material> getGroundMaterials() {
		return groundMaterials;
	}

	public Set<Material> getDisallowedGroundMaterials() {
		return disallowedGroundMaterials;
	}

	public ModifierSet getProjectileModifiers() {
		return projModifiers;
	}

	public List<Interaction> getInteractions() {
		return interactions;
	}

	public Subspell getAirSpell() {
		return airSpell;
	}

	public void setAirSpell(Subspell airSpell) {
		this.airSpell = airSpell;
	}

	public Subspell getTickSpell() {
		return tickSpell;
	}

	public void setTickSpell(Subspell tickSpell) {
		this.tickSpell = tickSpell;
	}

	public Subspell getEntitySpell() {
		return entitySpell;
	}

	public void setEntitySpell(Subspell entitySpell) {
		this.entitySpell = entitySpell;
	}

	public Subspell getCasterSpell() {
		return selfSpell;
	}

	public void setCasterSpell(Subspell selfSpell) {
		this.selfSpell = selfSpell;
	}

	public Subspell getGroundSpell() {
		return groundSpell;
	}

	public void setGroundSpell(Subspell groundSpell) {
		this.groundSpell = groundSpell;
	}

	public Subspell getDurationSpell() {
		return durationSpell;
	}

	public void setDurationSpell(Subspell durationSpell) {
		this.durationSpell = durationSpell;
	}

	public Subspell getModifierSpell() {
		return modifierSpell;
	}

	public void setModifierSpell(Subspell modifierSpell) {
		this.modifierSpell = modifierSpell;
	}

	public Subspell getEntityLocationSpell() {
		return entityLocationSpell;
	}

	public void setEntityLocationSpell(Subspell entityLocationSpell) {
		this.entityLocationSpell = entityLocationSpell;
	}

}
