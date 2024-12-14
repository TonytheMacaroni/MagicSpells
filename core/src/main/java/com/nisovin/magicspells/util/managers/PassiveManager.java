package com.nisovin.magicspells.util.managers;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.event.EventPriority;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.DependsOn;
import com.nisovin.magicspells.spells.passive.*;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class PassiveManager {

	private static final Map<String, EventPriority> eventPriorities = new HashMap<>();

	private static final Map<String, Class<? extends PassiveListener>> listeners = new HashMap<>();

	public PassiveManager() {
		initialize();
	}

	/**
	 * @param listener must be annotated with {@link Name}.
	 */
	public void addListener(Class<? extends PassiveListener> listener) {
		Name name = listener.getAnnotation(Name.class);
		if (name == null) throw new IllegalStateException("Missing 'Name' annotation on PassiveListener class: " + listener.getName());
		listeners.put(name.value(), listener);
	}

	/**
	 * @deprecated Use {@link PassiveManager#addListener(Class)}
	 */
	@Deprecated(forRemoval = true)
	public void addListener(String name, Class<? extends PassiveListener> listener) {
		listeners.put(name.toLowerCase(), listener);
	}

	/**
	 * @deprecated Use {@link PassiveManager#addListener(Class)}
	 */
	@Deprecated(forRemoval = true)
	public void addListener(Class<? extends PassiveListener> listener, String name) {
		listeners.put(name.toLowerCase(), listener);
	}

	public Map<String, EventPriority> getEventPriorities() {
		return eventPriorities;
	}

	public Map<String, Class<? extends PassiveListener>> getListeners() {
		return listeners;
	}

	public PassiveListener getListenerByName(String name) {
		Class<? extends PassiveListener> clazz = listeners.get(name.toLowerCase());
		if (clazz == null) return null;

		// Check if depending plugin is enabled.
		DependsOn dependsOn = clazz.getAnnotation(DependsOn.class);
		if (dependsOn != null && !Util.checkPluginsEnabled(dependsOn.value())) return null;

		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return null;
		}
	}

	public EventPriority getEventPriorityFromName(String name) {
		if (name == null) throw new NullPointerException("name");
		for (String str : eventPriorities.keySet()) {
			if (!name.contains(str)) continue;
			return eventPriorities.get(str);
		}
		return null;
	}

	public String getEventPriorityName(EventPriority priority) {
		for (String name : eventPriorities.keySet()) {
			if (!eventPriorities.get(name).equals(priority)) continue;
			return name;
		}
		return null;
	}

	private void initialize() {
		// initialize priorities
		for (EventPriority priority : EventPriority.values()) {
			eventPriorities.put("_" + priority.name().toLowerCase() + "priority", priority);
		}

		addListener(AnvilListener.class);
		addListener(BlockBreakListener.class);
		addListener(BlockPlaceListener.class);
		addListener(BuffListener.class);
		addListener(CraftListener.class);
		addListener(DamageListener.class);
		addListener(DeathListener.class);
		addListener(DropItemListener.class);
		addListener(EnterBedListener.class);
		addListener(DismountListener.class);
		addListener(EnchantListener.class);
		addListener(EntityTargetListener.class);
		addListener(EquipListener.class);
		addListener(FatalDamageListener.class);
		addListener(FishListener.class);
		addListener(FoodLevelChangeListener.class);
		addListener(GameModeChangeListener.class);
		addListener(GiveDamageListener.class);
		addListener(GrindstoneListener.class);
		addListener(HitArrowListener.class);
		addListener(HotbarDeselectListener.class);
		addListener(HotbarSelectListener.class);
		addListener(InsideBlockListener.class);
		addListener(InventoryActionListener.class);
		addListener(InventoryClickListener.class);
		addListener(InventoryCloseListener.class);
		addListener(InventoryOpenListener.class);
		addListener(JoinListener.class);
		addListener(JumpListener.class);
		addListener(KillListener.class);
		addListener(LeaveBedListener.class);
		addListener(LeftClickBlockCoordListener.class);
		addListener(LeftClickBlockTypeListener.class);
		addListener(LeftClickItemListener.class);
		addListener(MagicSpellsLoadedListener.class);
		addListener(ManaChangeListener.class);
		addListener(MissArrowListener.class);
		addListener(MountListener.class);
		addListener(OffhandSwapListener.class);
		addListener(PickupItemListener.class);
		addListener(PlayerAnimationListener.class);
		addListener(PlayerMoveListener.class);
		addListener(PlayerMoveToBlockListener.class);
		addListener(PortalEnterListener.class);
		addListener(PortalLeaveListener.class);
		addListener(PotionEffectListener.class);
		addListener(PrepareEnchantListener.class);
		addListener(QuitListener.class);
		addListener(RegainHealthListener.class);
		addListener(ResourcePackListener.class);
		addListener(RespawnListener.class);
		addListener(RightClickBlockCoordListener.class);
		addListener(RightClickBlockTypeListener.class);
		addListener(RightClickEntityListener.class);
		addListener(RightClickItemListener.class);
		addListener(SheepShearListener.class);
		addListener(ShootListener.class);
		addListener(SignBookListener.class);
		addListener(SmithListener.class);
		addListener(SpellCastedListener.class);
		addListener(SpellCastListener.class);
		addListener(SpellSelectListener.class);
		addListener(SpellTargetedListener.class);
		addListener(SpellTargetListener.class);
		addListener(StartFlyListener.class);
		addListener(StartGlideListener.class);
		addListener(StartPoseListener.class);
		addListener(StartSneakListener.class);
		addListener(StartSprintListener.class);
		addListener(StartSwimListener.class);
		addListener(StopFlyListener.class);
		addListener(StopGlideListener.class);
		addListener(StopPoseListener.class);
		addListener(StopSneakListener.class);
		addListener(StopSprintListener.class);
		addListener(StopSwimListener.class);
		addListener(TakeDamageListener.class);
		addListener(TeleportListener.class);
		addListener(TicksListener.class);
		addListener(UnequipListener.class);
		addListener(WorldChangeListener.class);
	}

}
