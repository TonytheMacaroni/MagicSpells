package com.nisovin.magicspells.spells.passive;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityPoseChangeEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("startpose")
public class StartPoseListener extends PassiveListener {

	private Set<Pose> poses;

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		poses = Conversion.convert(
			ConversionSource.split(var, ","),
			Converters.enumConverter(Pose.class),
			ConversionTarget.enumSet(Pose.class)
		);
	}

	@OverridePriority
	@EventHandler
	public void onPoseChange(EntityPoseChangeEvent event) {
		if (!(event.getEntity() instanceof LivingEntity caster) || !canTrigger(caster)) return;
		if (poses != null && !poses.contains(event.getPose())) return;

		passiveSpell.activate(caster);
	}

}
