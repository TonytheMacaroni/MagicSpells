package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.RayTraceResult;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class BombSpell extends TargetedSpell implements TargetedLocationSpell {

	private final Set<Block> blocks;

	private final ConfigData<Material> material;

	private final ConfigData<Integer> fuse;
	private final ConfigData<Integer> interval;

	private Subspell targetSpell;
	
	public BombSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		material = getConfigDataMaterial("block", Material.STONE);

		fuse = getConfigDataInt("fuse", 100);
		interval = getConfigDataInt("interval", 20);


		blocks = new HashSet<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		targetSpell = initSubspell("spell", "", true);
	}

	@Override
	public void turnOff() {
		super.turnOff();

		for (Block b : blocks) {
			b.setType(Material.AIR);
		}

		blocks.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		RayTraceResult result = rayTraceBlocks(data);
		if (result == null) return noTarget(data);

		Location target = result.getHitBlock().getRelative(result.getHitBlockFace()).getLocation().add(0.5, 0, 0.5);

		SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data, target);
		if (!event.callEvent()) return noTarget(event);

		return castAtLocation(event.getSpellData());
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Material material = this.material.get(data);
		if (!material.isBlock()) return noTarget(data);

		Location loc = data.location();
		Block block = loc.getBlock();
		if (!block.getType().isAir()) return noTarget(data);

		blocks.add(block);
		block.setType(material);

		playSpellEffects(data);

		int interval = this.interval.get(data);
		int fuse = this.fuse.get(data);

		new SpellAnimation(interval, interval, true, false) {

			private int time = 0;

			@Override
			protected void onTick(int tick) {
				time += interval;
				if (time >= fuse) {
					stop(true);
					if (material.equals(block.getType())) {
						blocks.remove(block);
						block.setType(Material.AIR);
						playSpellEffects(EffectPosition.DELAYED, loc, data);
						if (targetSpell != null) targetSpell.subcast(data);
					}
				} else if (!material.equals(block.getType())) stop(true);
				else playSpellEffects(EffectPosition.SPECIAL, loc, data);
			}

		};

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
