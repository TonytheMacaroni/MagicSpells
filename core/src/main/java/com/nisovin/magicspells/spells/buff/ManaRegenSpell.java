package com.nisovin.magicspells.spells.buff;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.util.config.ConfigData;

public class ManaRegenSpell extends BuffSpell {

	private final Map<UUID, ManaRegenData> players;

	private final ConfigData<Integer> regenModAmt;

	private final ConfigData<Boolean> constantRegenModAmt;

	public ManaRegenSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		regenModAmt = getConfigDataInt("regen-mod-amt", 3);

		constantRegenModAmt = getConfigDataBoolean("constant-regen-mod-amt", true);

		players = new HashMap<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
		if (!(data.target() instanceof Player target)) return false;

		boolean constantRegenModAmt = this.constantRegenModAmt.get(data);
		int regenModAmt = constantRegenModAmt ? this.regenModAmt.get(data) : 0;
		players.put(target.getUniqueId(), new ManaRegenData(data, regenModAmt, constantRegenModAmt));

		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return players.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		players.remove(entity.getUniqueId());
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return players.keySet();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onManaRegenTick(ManaChangeEvent event) {
		if (!event.getReason().equals(ManaChangeReason.REGEN)) return;

		Player player = event.getPlayer();
		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		ManaRegenData data = players.get(player.getUniqueId());
		if (data == null) return;

		int regenModAmt = data.constantRegenModAmt ? data.regenModAmt : this.regenModAmt.get(data.spellData);

		int newAmt = event.getNewAmount() + regenModAmt;
		if (newAmt > event.getMaxMana()) newAmt = event.getMaxMana();
		else if (newAmt < 0) newAmt = 0;

		addUseAndChargeCost(player);
		event.setNewAmount(newAmt);
	}

	public Map<UUID, ManaRegenData> getPlayers() {
		return players;
	}

	public record ManaRegenData(SpellData spellData, int regenModAmt, boolean constantRegenModAmt) {
	}

}
