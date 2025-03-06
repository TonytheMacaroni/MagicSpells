package com.nisovin.magicspells.util;

import java.util.*;

import org.bukkit.entity.*;
import org.bukkit.GameMode;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;

import org.jetbrains.annotations.NotNull;

import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.registry.RegistryKey;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;

public class ValidTargetList {
	
	public enum TargetingElement {
		
		TARGET_SELF,
		TARGET_PLAYERS,
		TARGET_INVISIBLES,
		TARGET_NONPLAYERS,
		TARGET_MONSTERS,
		TARGET_ANIMALS,
		TARGET_NONLIVING_ENTITIES,
		TARGET_MOUNTS,
		TARGET_PASSENGERS,
		TARGET_CASTER_MOUNT,
		TARGET_CASTER_PASSENGER,
		TARGET_ENTITY_TARGET,
		TARGET_MARKER_ARMOR_STANDS
		
	}

	private final Set<GameMode> gameModes = EnumSet.noneOf(GameMode.class);
	private final Set<EntityType> types = EnumSet.noneOf(EntityType.class);

	private final List<String> targetList = new ArrayList<>();

	private Spell spell;

	private boolean targetSelf = false;
	private boolean targetAnimals = false;
	private boolean targetPlayers = false;
	private boolean targetMonsters = false;
	private boolean targetInvisibles = false;
	private boolean targetNonPlayers = false;
	private boolean targetNonLivingEntities = false; // This will be kept as false for now during restructuring
	private boolean targetMounts = false;
	private boolean targetPassengers = false;
	private boolean targetCasterMount = false;
	private boolean targetCasterPassenger = false;
	private boolean targetEntityTarget = false;
	private boolean targetMarkerArmorStands = false;

	public ValidTargetList(Spell spell, String list) {
		this.spell = spell;
		if (list != null) {
			String[] listSplit = list.replace(" ", "").split(",");
			targetList.addAll(Arrays.asList(listSplit));
			init(spell, targetList);
		}
	}

	public ValidTargetList(Spell spell, List<String> list) {
		this.spell = spell;
		if (list != null) {
			targetList.addAll(list);
			init(spell, targetList);
		}
	}
	
	public void enforce(TargetingElement element, boolean value) {
		switch (element) {
			case TARGET_SELF -> targetSelf = value;
			case TARGET_ANIMALS -> targetAnimals = value;
			case TARGET_INVISIBLES -> targetInvisibles = value;
			case TARGET_MONSTERS -> targetMonsters = value;
			case TARGET_NONLIVING_ENTITIES -> targetNonLivingEntities = value;
			case TARGET_NONPLAYERS -> targetNonPlayers = value;
			case TARGET_PLAYERS -> targetPlayers = value;
			case TARGET_MOUNTS -> targetMounts = value;
			case TARGET_PASSENGERS -> targetPassengers = value;
			case TARGET_CASTER_MOUNT -> targetCasterMount = value;
			case TARGET_CASTER_PASSENGER -> targetCasterPassenger = value;
			case TARGET_ENTITY_TARGET -> targetEntityTarget = value;
			case TARGET_MARKER_ARMOR_STANDS -> targetMarkerArmorStands = value;
		}
	}
	
	public void enforce(TargetingElement[] elements, boolean value) {
		for (TargetingElement e : elements) {
			enforce(e, value);
		}
	}
	
	@SuppressWarnings("UnstableApiUsage")
	private void init(Spell spell, List<String> list) {
		for (String s : list) {
			s = s.trim();

			switch (s.toLowerCase()) {
				case "self", "caster" -> targetSelf = true;
				case "invisible", "invisibles" -> targetInvisibles = true;
				case "nonplayer", "nonplayers" -> targetNonPlayers = true;
				case "monster", "monsters" -> targetMonsters = true;
				case "animal", "animals" -> targetAnimals = true;
				case "mount", "mounts" -> targetMounts = true;
				case "rider", "passenger", "passengers", "riders" -> targetPassengers = true;
				case "castermount", "selfmount" -> targetCasterMount = true;
				case "casterpassenger", "selfpassenger" -> targetCasterPassenger = true;
				case "entitytarget", "mobtarget" -> targetEntityTarget = true;
				case "markerstand", "markerstands", "markerarmorstand", "markerarmorstands" -> targetMarkerArmorStands = true;
				case "player", "players" -> {
					gameModes.add(GameMode.SURVIVAL);
					gameModes.add(GameMode.ADVENTURE);
					targetPlayers = true;
				}
				default -> {
					try {
						gameModes.add(GameMode.valueOf(s.toUpperCase()));
						targetPlayers = true;
						continue;
					} catch (IllegalArgumentException ignored) {}

					EntityType type = MobUtil.getEntityType(s);
					if (type != null) {
						types.add(type);
						continue;
					}

					if (s.startsWith("#")) {
						NamespacedKey key = NamespacedKey.fromString(s.substring(1).toLowerCase());

						if (key != null) {
							TagKey<EntityType> tagKey = TagKey.create(RegistryKey.ENTITY_TYPE, key);

							if (Registry.ENTITY_TYPE.hasTag(tagKey)) {
								Tag<@NotNull EntityType> tag = Registry.ENTITY_TYPE.getTag(tagKey);
								types.addAll(tag.resolve(Registry.ENTITY_TYPE));

								continue;
							}
						}
					}

					MagicSpells.error("Spell '" + spell.getInternalName() + "' has an invalid target type defined: " + s);
				}
			}

			if (gameModes.isEmpty()) {
				gameModes.add(GameMode.SURVIVAL);
				gameModes.add(GameMode.ADVENTURE);
			}
		}
	}
	
	public ValidTargetList(boolean targetPlayers, boolean targetNonPlayers) {
		this.targetPlayers = targetPlayers;
		this.targetNonPlayers = targetNonPlayers;

		gameModes.add(GameMode.SURVIVAL);
		gameModes.add(GameMode.ADVENTURE);
	}
	
	public boolean canTarget(LivingEntity caster, Entity target) {
		if (caster == null) return canTarget(target);
		return canTarget(caster, target, targetPlayers);
	}
	
	public boolean canTarget(LivingEntity caster, Entity target, boolean targetPlayers) {
		if (!(target instanceof LivingEntity) && !targetNonLivingEntities) return false;
		boolean targetIsPlayer = target instanceof Player;

		if (target.equals(caster)) return targetSelf;
		if (targetIsPlayer && !gameModes.contains(((Player) target).getGameMode())) return false;

		if (!targetInvisibles && caster instanceof Player player && !player.canSee(target)) return false;
		if (targetPlayers && targetIsPlayer) return true;
		if (target instanceof ArmorStand stand && stand.isMarker()) return targetMarkerArmorStands;
		if (targetNonPlayers && !targetIsPlayer) return true;

		if (targetMonsters && target instanceof Monster) return true;
		if (targetAnimals && target instanceof Animals) return true;

		if (targetPassengers && target instanceof LivingEntity && target.isInsideVehicle()) return true;
		if (targetMounts && target instanceof LivingEntity && !target.getPassengers().isEmpty() && !target.isInsideVehicle()) return true;
		if (targetCasterPassenger && target instanceof LivingEntity && target.isInsideVehicle() && caster.equals(target.getVehicle())) return true;
		if (targetCasterMount && target instanceof LivingEntity && caster.isInsideVehicle() && target.equals(caster.getVehicle())) return true;

		if (targetEntityTarget && (caster instanceof Creature creature) && creature.getTarget() != null && creature.getTarget().equals(target)) return true;
		if (types.contains(target.getType())) return true;
		return false;
	}
	
	public boolean canTarget(Entity target) {
		if (!(target instanceof LivingEntity) && !targetNonLivingEntities) return false;
		boolean targetIsPlayer = target instanceof Player;

		if (targetIsPlayer && !gameModes.contains(((Player) target).getGameMode())) return false;

		if (targetPlayers && targetIsPlayer) return true;
		if (target instanceof ArmorStand stand && stand.isMarker()) return targetMarkerArmorStands;
		if (targetNonPlayers && !targetIsPlayer) return true;

		if (targetMonsters && target instanceof Monster) return true;
		if (targetAnimals && target instanceof Animals) return true;

		if (targetPassengers && target instanceof LivingEntity && !target.getVehicle().isEmpty()) return true;
		if (targetMounts && target instanceof LivingEntity && !target.getPassengers().isEmpty() && target.getVehicle().isEmpty()) return true;

		if (types.contains(target.getType())) return true;
		return false;
	}

	public boolean canTarget(Entity target, boolean ignoreGameMode) {
		if (!(target instanceof LivingEntity) && !targetNonLivingEntities) return false;
		boolean targetIsPlayer = target instanceof Player;

		if (!ignoreGameMode && targetIsPlayer && !gameModes.contains(((Player) target).getGameMode())) return false;

		if (targetPlayers && targetIsPlayer) return true;
		if (target instanceof ArmorStand stand && stand.isMarker()) return targetMarkerArmorStands;
		if (targetNonPlayers && !targetIsPlayer) return true;

		if (targetMonsters && target instanceof Monster) return true;
		if (targetAnimals && target instanceof Animals) return true;

		if (targetPassengers && target instanceof LivingEntity && !target.getVehicle().isEmpty()) return true;
		if (targetMounts && target instanceof LivingEntity && !target.getPassengers().isEmpty() && target.getVehicle().isEmpty()) return true;

		if (types.contains(target.getType())) return true;
		return false;
	}
	
	public List<LivingEntity> filterTargetListCastingAsLivingEntities(LivingEntity caster, List<Entity> targets) {
		return filterTargetListCastingAsLivingEntities(caster, targets, targetPlayers);
	}
	
	public List<LivingEntity> filterTargetListCastingAsLivingEntities(LivingEntity caster, List<Entity> targets, boolean targetPlayers) {
		List<LivingEntity> realTargets = new ArrayList<>();
		for (Entity e : targets) {
			if (!canTarget(caster, e, targetPlayers)) continue;
			realTargets.add((LivingEntity) e);
		}
		return realTargets;
	}

	public void addEntityTarget(Entity entity) {
		types.add(entity.getType());
	}

	public void addEntityTarget(EntityType entityType) {
		types.add(entityType);
	}

	public void setTargetCaster(boolean targetCaster) {
		targetSelf = targetCaster;
	}

	public boolean canTargetEntity(Entity entity) {
		return types.contains(entity.getType());
	}

	public boolean canTargetEntity(EntityType entityType) {
		return types.contains(entityType);
	}
	
	public boolean canTargetPlayers() {
		return targetPlayers;
	}

	public boolean canTargetAnimals() {
		return targetAnimals;
	}

	public boolean canTargetMonsters() {
		return targetMonsters;
	}

	public boolean canTargetNonPlayers() {
		return targetNonPlayers;
	}

	public boolean canTargetInvisibles() {
		return targetInvisibles;
	}

	public boolean canTargetSelf() {
		return targetSelf;
	}

	public boolean canTargetMounts() {
		return targetMounts;
	}

	public boolean canTargetPassengers() {
		return targetPassengers;
	}

	public boolean canTargetCasterMount() {
		return targetCasterMount;
	}

	public boolean canTargetCasterPassenger() {
		return targetCasterPassenger;
	}

	public boolean canTargetEntityTarget() {
		return targetEntityTarget;
	}

	public boolean canTargetLivingEntities() {
		return targetNonPlayers || targetMonsters || targetAnimals;
	}

	public boolean canTargetNonLivingEntities() {
		return targetNonLivingEntities;
	}

	public boolean canTargetOnlyCaster() {
		if (targetAnimals) return false;
		if (targetPlayers) return false;
		if (targetMonsters) return false;
		if (targetInvisibles) return false;
		if (targetNonPlayers) return false;
		if (targetNonLivingEntities) return false;
		if (targetMounts) return false;
		if (targetPassengers) return false;
		if (targetCasterMount) return false;
		if (targetCasterPassenger) return false;
		if (targetEntityTarget) return false;
		return targetSelf;
	}

	@Override
	public String toString() {
		return "ValidTargetList:["
			+ "targetSelf=" + targetSelf
			+ ",targetPlayers=" + targetPlayers
			+ ",targetInvisibles=" + targetInvisibles
			+ ",targetNonPlayers=" + targetNonPlayers
			+ ",targetMonsters=" + targetMonsters
			+ ",targetAnimals=" + targetAnimals
			+ ",targetMounts=" + targetMounts
			+ ",targetPassengers=" + targetPassengers
			+ ",targetCasterMount=" + targetCasterMount
			+ ",targetCasterPassenger=" + targetCasterPassenger
			+ ",targetEntityTarget=" + targetEntityTarget
			+ ",targetNonLivingEntities=" + targetNonLivingEntities
			+ ",targetMarkerArmorStands=" + targetMarkerArmorStands
			+ ",types=" + types
			+ ",gameModes=" + gameModes
			+ ']';
	}

	@Override
	public ValidTargetList clone() {
		return new ValidTargetList(spell, targetList);
	}
	
}
