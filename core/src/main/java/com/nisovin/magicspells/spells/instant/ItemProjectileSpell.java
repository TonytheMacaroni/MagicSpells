package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.trackers.ItemProjectileTracker;

public class ItemProjectileSpell extends InstantSpell implements TargetedLocationSpell {

	private static Set<ItemProjectileTracker> trackerSet;

	private String spellOnTickName;
	private String spellOnDelayName;
	private String spellOnHitEntityName;
	private String spellOnHitGroundName;

	private ItemStack item;

	private final ConfigData<Component> itemName;

	private final ConfigData<Integer> spellDelay;
	private final ConfigData<Integer> pickupDelay;
	private final ConfigData<Integer> removeDelay;
	private final ConfigData<Integer> tickInterval;
	private final ConfigData<Integer> spellInterval;
	private final ConfigData<Integer> itemNameDelay;
	private final ConfigData<Integer> specialEffectInterval;

	private final ConfigData<Float> speed;
	private final ConfigData<Float> yOffset;
	private final ConfigData<Float> hitRadius;
	private final ConfigData<Float> vertSpeed;
	private final ConfigData<Float> vertHitRadius;
	private final ConfigData<Float> rotationOffset;

	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> stopOnHitGround;
	private final ConfigData<Boolean> stopOnHitEntity;
	private final ConfigData<Boolean> projectileHasGravity;

	private final ConfigData<Vector> relativeOffset;

	private Subspell spellOnTick;
	private Subspell spellOnDelay;
	private Subspell spellOnHitEntity;
	private Subspell spellOnHitGround;

	public ItemProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		trackerSet = new HashSet<>();

		MagicItem magicItem = MagicItems.getMagicItemFromString(getConfigString("item", "iron_sword"));
		if (magicItem != null) item = magicItem.getItemStack();

		spellDelay = getConfigDataInt("spell-delay", 40);
		pickupDelay = getConfigDataInt("pickup-delay", 100);
		removeDelay = getConfigDataInt("remove-delay", 100);
		tickInterval = getConfigDataInt("tick-interval", 1);
		spellInterval = getConfigDataInt("spell-interval", 2);
		itemNameDelay = getConfigDataInt("item-name-delay", 10);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 2);

		speed = getConfigDataFloat("speed", 1F);
		yOffset = getConfigDataFloat("y-offset", 0F);
		hitRadius = getConfigDataFloat("hit-radius", 1F);
		vertSpeed = getConfigDataFloat("vert-speed", 0F);
		vertHitRadius = getConfigDataFloat("vertical-hit-radius", 1.5F);
		rotationOffset = getConfigDataFloat("rotation-offset", 0F);

		checkPlugins = getConfigDataBoolean("check-plugins", true);
		stopOnHitGround = getConfigDataBoolean("stop-on-hit-ground", true);
		stopOnHitEntity = getConfigDataBoolean("stop-on-hit-entity", true);
		projectileHasGravity = getConfigDataBoolean("gravity", true);

		relativeOffset = getConfigDataVector("relative-offset", new Vector());

		itemName = getConfigDataComponent("item-name", null);

		spellOnTickName = getConfigString("spell-on-tick", "");
		spellOnDelayName = getConfigString("spell-on-delay", "");
		spellOnHitEntityName = getConfigString("spell-on-hit-entity", "");
		spellOnHitGroundName = getConfigString("spell-on-hit-ground", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		spellOnTick = initSubspell(spellOnTickName, true, "spell-on-tick");
		spellOnDelay = initSubspell(spellOnDelayName, true, "spell-on-delay");
		spellOnHitEntity = initSubspell(spellOnHitEntityName, true, "spell-on-hit-entity");
		spellOnHitGround = initSubspell(spellOnHitGroundName, true, "spell-on-hit-ground");

		spellOnTickName = null;
		spellOnDelayName = null;
		spellOnHitEntityName = null;
		spellOnHitGroundName = null;
	}

	@Override
	public void turnOff() {
		for (ItemProjectileTracker tracker : trackerSet) {
			tracker.stop(false);
		}
		trackerSet.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		data = data.location(data.caster().getLocation());

		ItemProjectileTracker tracker = new ItemProjectileTracker(data);
		setupTracker(tracker, data);
		tracker.start();

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		ItemProjectileTracker tracker = new ItemProjectileTracker(data);
		setupTracker(tracker, data);
		tracker.start();

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private void setupTracker(ItemProjectileTracker tracker, SpellData data) {
		tracker.setSpell(this);

		tracker.setItemName(itemName.get(data));
		tracker.setItem(item);

		tracker.setSpellDelay(spellDelay.get(data));
		tracker.setPickupDelay(pickupDelay.get(data));
		tracker.setRemoveDelay(removeDelay.get(data));
		tracker.setTickInterval(tickInterval.get(data));
		tracker.setSpellInterval(spellInterval.get(data));
		tracker.setItemNameDelay(itemNameDelay.get(data));
		tracker.setSpecialEffectInterval(specialEffectInterval.get(data));

		tracker.setSpeed(speed.get(data));

		float yOffset = this.yOffset.get(data);
		tracker.setYOffset(yOffset);

		float vertSpeed = this.vertSpeed.get(data);
		tracker.setVertSpeed(vertSpeed);

		tracker.setHitRadius(hitRadius.get(data));
		tracker.setVertHitRadius(vertHitRadius.get(data));
		tracker.setRotationOffset(rotationOffset.get(data));

		tracker.setCallEvents(checkPlugins.get(data));
		tracker.setVertSpeedUsed(vertSpeed != 0);
		tracker.setStopOnHitGround(stopOnHitGround.get(data));
		tracker.setStopOnHitEntity(stopOnHitEntity.get(data));
		tracker.setProjectileHasGravity(projectileHasGravity.get(data));

		Vector relativeOffset = this.relativeOffset.get(data);
		if (yOffset != 0) relativeOffset.setY(yOffset);
		tracker.setRelativeOffset(relativeOffset);

		tracker.setSpellOnTick(spellOnTick);
		tracker.setSpellOnDelay(spellOnDelay);
		tracker.setSpellOnHitGround(spellOnHitGround);
		tracker.setSpellOnHitEntity(spellOnHitEntity);

		tracker.setTargetList(validTargetList);
	}

	public static Set<ItemProjectileTracker> getProjectileTrackers() {
		return trackerSet;
	}

	public ItemStack getItem() {
		return item;
	}

	public void setItem(ItemStack item) {
		this.item = item;
	}

	public Subspell getSpellOnTick() {
		return spellOnTick;
	}

	public void setSpellOnTick(Subspell spellOnTick) {
		this.spellOnTick = spellOnTick;
	}

	public Subspell getSpellOnDelay() {
		return spellOnDelay;
	}

	public void setSpellOnDelay(Subspell spellOnDelay) {
		this.spellOnDelay = spellOnDelay;
	}

	public Subspell getSpellOnHitEntity() {
		return spellOnHitEntity;
	}

	public void setSpellOnHitEntity(Subspell spellOnHitEntity) {
		this.spellOnHitEntity = spellOnHitEntity;
	}

	public Subspell getSpellOnHitGround() {
		return spellOnHitGround;
	}

	public void setSpellOnHitGround(Subspell spellOnHitGround) {
		this.spellOnHitGround = spellOnHitGround;
	}

	public void playEffects(EffectPosition position, Location loc, SpellData data) {
		playSpellEffects(position, loc, data);
	}

	public void playEffects(EffectPosition position, Entity entity, SpellData data) {
		playSpellEffects(position, entity, data);
	}

	public void playTrackingLineEffects(EffectPosition position, Location startLocation, Location location, LivingEntity caster, Projectile projectile, SpellData data) {
		playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, startLocation, projectile.getLocation(), caster, projectile, data);
	}

}
