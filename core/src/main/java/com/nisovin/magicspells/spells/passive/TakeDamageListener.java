package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

// Optional trigger variable of a pipe separated list that can contain
// damage causes or damaging magic items to accept
@Name("takedamage")
public class TakeDamageListener extends PassiveListener {

	private final EnumSet<DamageCause> damageCauses = EnumSet.noneOf(DamageCause.class);
	private final Set<MagicItemData> items = new HashSet<>();

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;
		for (String s : var.split(MagicItemDataParser.DATA_REGEX)) {
			s = s.trim();

			boolean isDamCause = false;
			for (DamageCause c : DamageCause.values()) {
				if (!s.equalsIgnoreCase(c.name())) continue;

				damageCauses.add(c);
				isDamCause = true;
				break;
			}
			if (isDamCause) continue;

			MagicItemData itemData = MagicItems.getMagicItemDataFromString(s);
			if (itemData == null) {
				MagicSpells.error("Invalid damage cause or magic item '" + s + "' in takedamage trigger on passive spell '" + passiveSpell.getInternalName() + "'");
				continue;
			}

			items.add(itemData);
		}
	}

	@OverridePriority
	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof LivingEntity caster)) return;
		if (!isCancelStateOk(event.isCancelled())) return;
		if (event.getFinalDamage() == 0D) return;
		if (!canTrigger(caster)) return;
		if (!damageCauses.isEmpty() && !damageCauses.contains(event.getCause())) return;

		LivingEntity attacker = getAttacker(event);

		if (!items.isEmpty()) {
			if (attacker == null) return;

			EntityEquipment eq = attacker.getEquipment();
			if (eq == null) return;

			MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(eq.getItemInMainHand());
			if (itemData == null || !contains(itemData)) return;
		}

		boolean casted = passiveSpell.activate(caster, attacker);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

	@OverridePriority
	@EventHandler
	public void onLegacyDamage(MagicSpellsEntityDamageByEntityEvent event) {
		onDamage(event);
	}

	private LivingEntity getAttacker(EntityDamageEvent event) {
		if (!(event instanceof EntityDamageByEntityEvent)) return null;
		Entity e = ((EntityDamageByEntityEvent) event).getDamager();
		if (e instanceof LivingEntity) return (LivingEntity) e;
		if (e instanceof Projectile && ((Projectile) e).getShooter() instanceof LivingEntity) {
			return (LivingEntity) ((Projectile) e).getShooter();
		}
		return null;
	}

	private boolean contains(MagicItemData itemData) {
		for (MagicItemData data : items) {
			if (data.matches(itemData)) return true;
		}
		return false;
	}

}
