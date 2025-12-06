package com.nisovin.magicspells.util.conversion;

import java.util.List;
import java.util.function.Predicate;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.DebugPath;
import com.nisovin.magicspells.debug.MagicDebug;

record ListConversionSource<T>(ConfigurationSection section, String path, List<T> elements) implements ConversionSource<T> {

	@Override
	public boolean forEach(Predicate<T> action) {
		try (var _ = MagicDebug.section("Resolving values from list '%s'.", MagicDebug.resolveShortPath(section, path))
			.pushPaths(section, path, DebugPath.Type.LIST)
		) {
			int index = 0;
			for (T element : elements) {
			    try (var _ = MagicDebug.section("Resolving value '%s'.", element)
					.pushListEntry(index)
				) {
					if (!action.test(element))
						return false;
				}

				index++;
			}

			return true;
		}
	}

}
