package com.nisovin.magicspells.spells.targeted;

import java.util.*;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.conversion.Converters;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.MagicSpellsBlockBreakEvent;

public class ZapSpell extends TargetedSpell implements TargetedLocationSpell {

	private final Set<BlockData> allowedBlockTypes;
	private final Set<BlockData> disallowedBlockTypes;

	private final String strCantZap;

	private final ConfigData<Boolean> dropBlock;
	private final ConfigData<Boolean> dropNormal;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> playBreakEffect;

	public ZapSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		allowedBlockTypes = getConfigCollection("allowed-block-types", true, HashSet::new, Converters.BLOCK_DATA);
		disallowedBlockTypes = getConfigCollection("disallowed-block-types", true, HashSet::new, Converters.BLOCK_DATA);

		strCantZap = getConfigString("str-cant-zap", "");

		dropBlock = getConfigDataBoolean("drop-block", false);
		dropNormal = getConfigDataBoolean("drop-normal", true);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		playBreakEffect = getConfigDataBoolean("play-break-effect", true);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<Location> info = getTargetedBlockLocation(data);
		if (info.noTarget()) return noTarget(strCantZap, info);

		return castAtLocation(info.spellData());
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Location location = data.location();

		Block target = location.getBlock();
		if (!canZap(target)) return noTarget(strCantZap, data);

		if (checkPlugins.get(data) && data.caster() instanceof Player caster) {
			MagicSpellsBlockBreakEvent event = new MagicSpellsBlockBreakEvent(target, caster);
			if (!event.callEvent()) return noTarget(strCantZap, data);
		}

		if (playBreakEffect.get(data)) target.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, target.getBlockData());

		if (dropBlock.get(data)) {
			if (dropNormal.get(data)) target.breakNaturally();
			else location.getWorld().dropItemNaturally(location, new ItemStack(target.getBlockData().getPlacementMaterial()));
		}

		target.setType(Material.AIR);
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private boolean canZap(Block target) {
		BlockData bd = target.getBlockData();

		if (disallowedBlockTypes != null) {
			for (BlockData data : disallowedBlockTypes)
				if (bd.matches(data))
					return false;
		}

		if (allowedBlockTypes == null) return true;

		for (BlockData data : allowedBlockTypes)
			if (bd.matches(data))
				return true;

		return false;
	}

}
