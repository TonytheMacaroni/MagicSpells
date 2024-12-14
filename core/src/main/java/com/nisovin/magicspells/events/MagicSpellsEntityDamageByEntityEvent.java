package com.nisovin.magicspells.events;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.Spell;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

public class MagicSpellsEntityDamageByEntityEvent extends EntityDamageByEntityEvent implements IMagicSpellsCompatEvent {

	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Spell spell;

	public MagicSpellsEntityDamageByEntityEvent(Entity damager, Entity damagee, DamageCause cause, double damage, Spell spell) {
		super(damager, damagee, cause, getModTemplate(damage), getModifierFunctionTemplate(0D));
		this.spell = spell;
	}

	private static Map<DamageModifier, Double> getModTemplate(double baseDamage) {
		return new HashMap<>(ImmutableMap.of(DamageModifier.BASE, baseDamage));
	}

	private static Map<DamageModifier, Function<Double, Double>> getModifierFunctionTemplate(final double baseDamage) {
		return new HashMap<>(ImmutableMap.of(DamageModifier.BASE, getConstantFunction(baseDamage)));
	}

	private static Function<Double, Double> getConstantFunction(final double value) {
		return arg0 -> value;
	}

	@NotNull
	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return super.getHandlers();
	}

	public Spell getSpell() {
		return spell;
	}

}
