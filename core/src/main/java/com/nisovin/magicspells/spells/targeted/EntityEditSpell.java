package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Collection;

import com.google.common.collect.Multimap;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.itemreader.AttributeHandler;

public class EntityEditSpell extends TargetedSpell implements TargetedEntitySpell {

	private Multimap<Attribute, AttributeModifier> attributes;

	private final ConfigData<Boolean> force;
	private final ConfigData<Boolean> remove;
	private final ConfigData<Boolean> toggle;
	private final ConfigData<Boolean> permanent;

	public EntityEditSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		List<?> attributeList = getConfigList("attributes", null);
		if (attributeList != null && !attributeList.isEmpty())
			attributes = AttributeHandler.getAttributeModifiers(attributeList, "attributes", internalName);

		force = getConfigDataBoolean("force", false);
		remove = getConfigDataBoolean("remove", false);
		toggle = getConfigDataBoolean("toggle", false);
		permanent = getConfigDataBoolean("permanent", true);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (attributes == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		Set<Map.Entry<Attribute, Collection<AttributeModifier>>> entries = attributes.asMap().entrySet();
		LivingEntity target = data.target();

		if (remove.get(data)) {
			entries.forEach(entry -> {
				AttributeInstance attributeInstance = target.getAttribute(entry.getKey());
				if (attributeInstance == null) return;

				entry.getValue().forEach(modifier -> attributeInstance.removeModifier(modifier.key()));
			});

			playSpellEffects(data);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		if (toggle.get(data)) {
			boolean apply = entries.stream().noneMatch(entry -> {
				AttributeInstance attributeInstance = target.getAttribute(entry.getKey());
				if (attributeInstance == null) return false;

				return entry.getValue().stream()
					.anyMatch(modifier -> attributeInstance.getModifier(modifier.getKey()) != null);
			});

			if (apply) {
				boolean permanent = this.permanent.get(data);
				boolean force = this.force.get(data);

				entries.forEach(entry -> {
					Attribute attribute = entry.getKey();

					AttributeInstance attributeInstance = target.getAttribute(attribute);
					if (attributeInstance == null) {
						if (!force) return;

						target.registerAttribute(attribute);

						attributeInstance = target.getAttribute(attribute);
						if (attributeInstance == null) return;
					}

					if (permanent) entry.getValue().forEach(attributeInstance::addModifier);
					else entry.getValue().forEach(attributeInstance::addTransientModifier);
				});
			} else {
				entries.forEach(entry -> {
					AttributeInstance attributeInstance = target.getAttribute(entry.getKey());
					if (attributeInstance == null) return;

					entry.getValue().forEach(modifier -> attributeInstance.removeModifier(modifier.key()));
				});
			}

			playSpellEffects(data);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		boolean permanent = this.permanent.get(data);
		boolean force = this.force.get(data);

		entries.forEach(entry -> {
			Attribute attribute = entry.getKey();

			AttributeInstance attributeInstance = target.getAttribute(attribute);
			if (attributeInstance == null) {
				if (!force) return;

				target.registerAttribute(attribute);

				attributeInstance = target.getAttribute(attribute);
				if (attributeInstance == null) return;
			}
			AttributeInstance instance = attributeInstance;

			entry.getValue().forEach(modifier -> {
				instance.removeModifier(modifier.key());

				if (permanent) instance.addModifier(modifier);
				else instance.addTransientModifier(modifier);
			});
		});

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
