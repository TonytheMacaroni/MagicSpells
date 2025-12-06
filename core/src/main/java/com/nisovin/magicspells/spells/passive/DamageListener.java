package com.nisovin.magicspells.spells.passive;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

import net.kyori.adventure.key.Key;

import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.damage.DamageSource;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import io.papermc.paper.registry.RegistryKey;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.RegistryEntryPredicate;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@SuppressWarnings("UnstableApiUsage")
@Name("damage")
public class DamageListener extends PassiveListener {

	private Mode mode;

	private Predicate<DamageType> damageTypes;

	private List<MagicItemData> projectileItems;
	private List<MagicItemData> weaponItems;

	private ConfigData<Double> minimumDamage;

	private boolean indirectDamager;

	@Override
	public void initialize(@NotNull String var) {
		MagicDebug.warn("The 'damage' trigger does not have a string format: %s", MagicDebug.resolveFullPath());
	}

	@Override
	public boolean initialize(@NotNull ConfigurationSection config) {
		mode = getMode(config);
		if (mode == null) return false;

		damageTypes = initializeDamageTypes(config);

		weaponItems = Conversion.convert(
			ConversionSource.listFromConfig(config, "weapon-items"),
			Converters.MAGIC_ITEM_DATA,
			ConversionTarget.list()
		);

		projectileItems = Conversion.convert(
			ConversionSource.listFromConfig(config, "projectile-items"),
			Converters.MAGIC_ITEM_DATA,
			ConversionTarget.list()
		);

		minimumDamage = ConfigDataUtil.getDouble(config, "minimum-damage", -1);

		indirectDamager = config.getBoolean("indirect-damager", true);

		return true;
	}

	private Mode getMode(@NotNull ConfigurationSection config) {
		String modeString = config.getString("mode");
		if (modeString == null) {
			MagicDebug.warn("No 'mode' defined %s.", MagicDebug.resolveFullPath());
			return null;
		}

		return switch (modeString.toLowerCase()) {
			case "give" -> Mode.GIVE;
			case "take" -> Mode.TAKE;
			default -> {
				MagicDebug.warn("Invalid 'mode' value '%s' defined %s'", modeString, MagicDebug.resolveFullPath());
				yield null;
			}
		};
	}

	private Predicate<DamageType> initializeDamageTypes(@NotNull ConfigurationSection config) {
		if (config.isString("damage-types")) {
			String damageTypesString = config.getString("damage-types");
			return RegistryEntryPredicate.fromString(RegistryKey.DAMAGE_TYPE, damageTypesString);
		}

		Set<Key> types = Conversion.convert(
			ConversionSource.listFromConfig(config, "damage-types"),
			Converters.registryEntryOrTagKeys(RegistryKey.DAMAGE_TYPE),
			ConversionTarget.set()
		);

		return entry -> types.contains(entry.key());
	}

	@OverridePriority
	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		DamageSource source = event.getDamageSource();
		if (damageTypes != null && !damageTypes.test(source.getDamageType())) return;

		Entity damaged = event.getEntity();

		LivingEntity livingDamaged = damaged instanceof LivingEntity le ? le : null;
		if (mode == Mode.TAKE && (livingDamaged == null || !canTrigger(livingDamaged))) return;

		Entity damager = null;
		if (event instanceof EntityDamageByEntityEvent byEvent) {
			if (!indirectDamager) damager = byEvent.getDamager();
			else damager = Objects.requireNonNullElseGet(source.getCausingEntity(), byEvent::getDamager);
		}

		LivingEntity livingDamager = damager instanceof LivingEntity le ? le : null;
		if (mode == Mode.GIVE && (livingDamager == null || livingDamaged == null || !canTrigger(livingDamager))) return;

		SpellData data = switch (mode) {
			case GIVE -> new SpellData(livingDamager, livingDamaged);
			case TAKE -> new SpellData(livingDamaged, livingDamager);
		};

		double minimumDamage = this.minimumDamage.get(data);
		if (minimumDamage >= 0 && event.getFinalDamage() < minimumDamage) return;

		Entity directDamager;
		if (event instanceof EntityDamageByEntityEvent byEvent) directDamager = byEvent.getDamager();
		else directDamager = source.getDirectEntity();

		if (weaponItems != null) {
			ItemStack item = switch (directDamager) {
				case AbstractArrow arrow -> arrow.getWeapon();
				case Entity entity when source.getCausingEntity() == null || !source.isIndirect() -> {
					if (!(entity instanceof LivingEntity livingEntity)) yield null;
					if (!livingEntity.canUseEquipmentSlot(EquipmentSlot.HAND)) yield null;

					EntityEquipment equipment = livingEntity.getEquipment();
					if (equipment == null) yield null;

					yield equipment.getItem(EquipmentSlot.HAND);
				}
				case null, default -> null;
			};

			if (item == null || !matches(weaponItems, item)) return;
		}

		if (projectileItems != null) {
			ItemStack item = switch (directDamager) {
				case AbstractArrow arrow -> arrow.getItemStack();
				case ThrowableProjectile projectile -> projectile.getItem();
				case null, default -> null;
			};

			if (item == null || !matches(projectileItems, item)) return;
		}

		boolean casted = passiveSpell.activate(data);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

	private boolean matches(List<MagicItemData> items, ItemStack item) {
		MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
		for (MagicItemData data : items)
			if (data.matches(itemData))
				return true;

		return false;
	}

	private enum Mode {
		GIVE,
		TAKE
	}

}
