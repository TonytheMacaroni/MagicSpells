package com.nisovin.magicspells.handlers;

import java.util.Map;
import java.util.Objects;
import java.util.Collection;
import java.util.stream.Collectors;

import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.debug.DebugLevel;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.DeprecationNotice;

import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;

public class DeprecationHandler {

	private final Multimap<DeprecationNotice, String> deprecations = HashMultimap.create();

	public void addDeprecation(@NotNull DeprecationNotice deprecationNotice) {
		deprecations.put(deprecationNotice, MagicDebug.resolveFullPath().get());
	}

	public <T extends Spell> void addDeprecation(@NotNull DeprecationNotice deprecationNotice, boolean check) {
		if (check) addDeprecation(deprecationNotice);
	}

	@SuppressWarnings("PatternValidation")
	public void printDeprecationNotices() {
		if (deprecations.isEmpty()) return;

		try (var ignored = MagicDebug.section(builder -> builder
			.message("Usage of deprecated features found. All such usages should be examined and replaced with supported alternatives.")
			.category(DebugCategory.DEPRECATIONS)
			.level(DebugLevel.WARNING)
		)) {
			for (Map.Entry<DeprecationNotice, Collection<String>> entry : deprecations.asMap().entrySet()) {
				DeprecationNotice notice = entry.getKey();

				try (var ignored1 = MagicDebug.section(notice.reason())) {
					MagicDebug.warn("Steps to take: " + notice.replacement());
					MagicDebug.warn("Context: " + notice.context());

					try (var ignored2 = MagicDebug.section("Usages:")) {
						MagicDebug.warn(
							entry.getValue().stream()
								.sorted()
								.collect(Collectors.joining("\n- ", "- ", ""))
						);
					}
				}
			}
		}
	}

}
