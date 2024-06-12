package com.nisovin.magicspells.debug;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.intellij.lang.annotations.PrintFormat;

import java.util.Iterator;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.logging.Logger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;

import org.bukkit.Keyed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.config.ConfigData;

public class MagicDebug {

	private static Section section = new Section(null, null, DebugCategory.DEFAULT, new ArrayDeque<>(), 0, false, false, false);

	@NotNull
	public static Section.Builder section() {
		return new Section.Builder();
	}

	public static Section section(@NotNull DebugCategory category) {
		return new Section.Builder().category(category).build();
	}

	@NotNull
	public static Section section(@NotNull Consumer<Section.Builder> consumer) {
		Section.Builder builder = new Section.Builder();
		consumer.accept(builder);

		return builder.build();
	}

	@NotNull
	public static Section section(@NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		return new Section.Builder().message(message, args).build();
	}

	@NotNull
	public static Section section(@Nullable DebugConfig config, @NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		return new Section.Builder().config(config).message(message, args).build();
	}

	@NotNull
	public static Section section(@NotNull DebugCategory category, @NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		return new Section.Builder().category(category).message(message, args).build();
	}

	@NotNull
	public static Section section(@NotNull DebugCategory category, @Nullable DebugConfig config, @NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		return new Section.Builder().category(category).config(config).message(message, args).build();
	}

	@NotNull
	public static Section suppressWarnings() {
		return new Section.Builder().suppressWarnings(true).build();
	}

	public static void info(@NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		log(section.config, section.category, DebugLevel.INFO, null, message, args);
	}

	public static void info(@NotNull DebugCategory category, @NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		log(section.config, category, DebugLevel.INFO, null, message, args);
	}

	public static void info(@NotNull Throwable throwable, @NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		log(section.config, section.category, DebugLevel.INFO, throwable, message, args);
	}

	public static void info(@NotNull DebugCategory category, @NotNull Throwable throwable, @NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		log(section.config, category, DebugLevel.INFO, throwable, message, args);
	}

	public static void warn(@NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		log(section.config, section.category, DebugLevel.WARNING, null, message, args);
	}

	public static void warn(@NotNull DebugCategory category, @NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		log(section.config, category, DebugLevel.WARNING, null, message, args);
	}

	public static void warn(@NotNull Throwable throwable, @NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		log(section.config, section.category, DebugLevel.WARNING, throwable, message, args);
	}

	public static void warn(@NotNull DebugCategory category, @NotNull Throwable throwable, @NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		log(section.config, category, DebugLevel.WARNING, throwable, message, args);
	}

	public static void error(@NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		log(section.config, section.category, DebugLevel.ERROR, null, message, args);
	}

	public static void error(@NotNull DebugCategory category, @NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		log(section.config, category, DebugLevel.ERROR, null, message, args);
	}

	public static void error(@NotNull Throwable throwable, @NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		log(section.config, section.category, DebugLevel.ERROR, throwable, message, args);
	}

	public static void error(@NotNull DebugCategory category, @NotNull Throwable throwable, @NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		log(section.config, category, DebugLevel.ERROR, throwable, message, args);
	}

	private static void log(@Nullable DebugConfig config, @NotNull DebugCategory category, @NotNull DebugLevel level,
							@Nullable Throwable throwable, @NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
		Preconditions.checkNotNull(category, "category");
		Preconditions.checkNotNull(level, "level");
		Preconditions.checkNotNull(message, "message");
		Preconditions.checkNotNull(args, "args");

		if (section.suppressWarnings) level = DebugLevel.INFO;
		if (suppressLog(config, category, level)) return;

		if (config == null) config = section.config();

		String indentation;
		if (config != null) {
			int depth = section.depth();
			if (depth > 0 && section.logged && !isEnabled(section)) depth++;

			int adjustment = level == DebugLevel.ERROR ? -1 : 0;
			int indent = Math.max(depth * config.getIndent() + adjustment, 0);

			indentation = config.getIndentCharacter().repeat(indent);
		} else indentation = "";

		Stream<String> lines = message.formatted(replaceArguments(args)).lines();
		if (throwable != null) {
			StringWriter writer = new StringWriter();
			throwable.printStackTrace(new PrintWriter(writer));

			lines = Stream.concat(lines, writer.toString().lines());
		}

		Logger logger = MagicSpells.getInstance().getLogger();
		Level logLevel = level.getLogLevel();

		lines.forEach(line -> logger.log(logLevel, indentation + line));
	}

	public static boolean suppressLog(DebugLevel level) {
		DebugConfig config = section.config();
		if (config == null) return false;

		return !section.all && config.suppressDebug(section.category, level);
	}

	public static boolean suppressLog(@NotNull DebugCategory category, DebugLevel level) {
		return suppressLog(section.config(), category, level);
	}

	public static boolean suppressLog(@Nullable DebugConfig config, @NotNull DebugCategory category, DebugLevel level) {
		if (config == null) config = MagicSpells.getDebugConfig();
		return config != null && !section.all && config.suppressDebug(category, level) && config.suppressDebug(section.category, level);
	}

	public static boolean isEnabled(@NotNull Section section) {
		return section.all || section.config().isEnabled(section.category);
	}

	public static boolean isEnabled(@Nullable DebugConfig config, @NotNull DebugCategory category) {
		if (config == null) config = MagicSpells.getDebugConfig();
		return config.isEnabled(category);
	}

	public static boolean isEnhanced(@Nullable DebugConfig config, @NotNull DebugCategory category) {
		if (config == null) config = MagicSpells.getDebugConfig();
		return config.isEnhanced(category);
	}

	@NotNull
	public static Section getSection() {
		return section;
	}

	public static DebugConfig getDebugConfig() {
		return section.config();
	}

	public static DebugPath pushPath(@NotNull String node, @NotNull DebugPath.Type type) {
		return pushPath(node, type, type != DebugPath.Type.FILE);
	}

	public static DebugPath pushPath(@NotNull String node, @NotNull DebugPath.Type type, boolean concrete) {
		DebugPath path = new DebugPath(node, type, concrete);
		MagicDebug.pushPath(section.paths, path);
		return path;
	}

	public static void pushPath(@NotNull DebugPath path) {
		MagicDebug.pushPath(section.paths, path);
	}

	private static void pushPath(@NotNull ArrayDeque<DebugPath> paths, @NotNull DebugPath path) {
		Preconditions.checkNotNull(path, "Path cannot be null");

		DebugPath prev = paths.isEmpty() ? null : paths.getLast();
		if (prev != null && !path.concrete() && prev.concrete())
			throw new IllegalArgumentException("Cannot append a non-concrete path to a concrete path: '%s' was appended to '%s'".formatted(path, prev));

		switch (path.type()) {
			case FILE -> {
				if (prev != null)
					throw new IllegalArgumentException("Cannot append a file path to another path: '%s' was appended to '%s'".formatted(path, prev));
			}
			case SECTION -> {
				if (prev != null && prev.type() == DebugPath.Type.LIST)
					throw new IllegalArgumentException("Cannot append a section path to a list path: '%s' was appended to '%s'".formatted(path, prev));
			}
			case LIST -> {
				if (prev != null && prev.type() == DebugPath.Type.LIST)
					throw new IllegalArgumentException("Cannot append a list path to a list path: '%s' was appended to '%s'".formatted(path, prev));
			}
			case LIST_ENTRY -> {
				if (prev == null || prev.type() != DebugPath.Type.LIST)
					throw new IllegalArgumentException("Cannot append a list entry path to a non-list path: '%s' was appended to '%s'".formatted(path, prev));
			}
		}

		paths.addLast(path);
	}

	public static void popPath(@NotNull DebugPath path) {
		popPath(section.paths, path);
	}

	private static void popPath(@NotNull ArrayDeque<DebugPath> paths, @NotNull DebugPath path) {
		DebugPath last = paths.getLast();
		Preconditions.checkArgument(last.equals(path), "Found mismatch while popping path: path '%s' differs from current path '%s'", path, last);
		paths.removeLast();
	}

	public static Supplier<String> resolveShortPath(@NotNull String subPath) {
		return resolveShortPath(null, subPath);
	}

	public static Supplier<String> resolveShortPath(@Nullable ConfigurationSection config, @NotNull String subPath) {
		return () -> {
			if (section.paths.isEmpty())
				return subPath;

			String currentPath = subPath;

			if (config != null) {
				String path = config.getCurrentPath();
				if (path != null && !path.isEmpty()) currentPath = path + "." + currentPath;
			}

			int start = 0, initialEnd = currentPath.indexOf('.'), end = initialEnd;
			if (end != -1) {
				Iterator<DebugPath> it = section.paths.iterator();

				do {
					DebugPath path = it.next();
					if (path.type() != DebugPath.Type.SECTION) {
						start = 0;
						end = initialEnd;
						continue;
					}

					int length = end - start;
					if (length == path.node().length() && currentPath.regionMatches(start, path.node(), 0, length)) {
						start = end + 1;
						end = currentPath.indexOf('.', start);
					} else if (start != 0) {
						if (it.hasNext()) start = 0;
						break;
					}
				} while (end != -1 && it.hasNext());
			}

			return currentPath.substring(start);
		};
	}

	@NotNull
	public static Supplier<String> resolveFullPath() {
		return resolveFullPath("");
	}

	@NotNull
	public static Supplier<String> resolveFullPath(@NotNull String subPath) {
		return () -> {
			if (section.paths.isEmpty())
				return subPath.isEmpty() ? "" : "at '" + subPath + "'";

			StringBuilder builder = new StringBuilder();

			boolean prev = false;
			for (DebugPath path : section.paths) {
				if (!path.concrete()) continue;

				switch (path.type()) {
					case SECTION, LIST -> {
						if (prev) builder.append('.');

						builder.append(path.node());
					}
					case LIST_ENTRY -> builder.append('[').append(path.node()).append(']');
				}

				prev = true;
			}

			if (!subPath.isEmpty())
				builder.append(".").append(subPath);

			DebugPath first = section.paths.getFirst();
			if (first.type() == DebugPath.Type.FILE) {
				String file = "in '" + first.node() + "'";
				if (!prev) return file;

				builder.append("' ").append(file);
			} else {
				if (!prev) return "";

				builder.append("'");
			}

			return "at '" + builder;
		};
	}

	@NotNull
	public static Supplier<String> resolvePath(@Nullable ConfigurationSection config, @NotNull String subPath) {
		return () -> {
			String currentPath = subPath;

			if (config != null) {
				String path = config.getCurrentPath();
				if (path != null && !path.isEmpty()) currentPath = path + "." + currentPath;
			}

			String shortPath = resolveShortPath(currentPath).get();
			String extra = "";

			if (shortPath.length() != currentPath.length()) {
				int index = shortPath.lastIndexOf('.');

				if (index != -1) {
					extra = shortPath.substring(0, index);
					shortPath = shortPath.substring(index + 1);
				}
			}

			String fullPath = resolveFullPath(extra).get();
			if (fullPath.isEmpty()) return "'" + shortPath + "' ";

			return "'" + shortPath + "' " + fullPath;
		};
	}

	private static Object[] replaceArguments(Object[] args) {
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			if (arg instanceof ConfigData<?> data && data.isConstant()) {
				args[i] = data.get();
				i--;
			} else if (arg instanceof Supplier<?> supp) {
				args[i] = supp.get();
				i--;
			} else if (arg instanceof Component comp) args[i] = ANSIComponentSerializer.ansi().serialize(comp);
			else if (arg instanceof Keyed keyed) args[i] = keyed.getKey().asMinimalString();
			else if (arg instanceof Player player) args[i] = player.getName();
			else if (arg instanceof Entity entity) args[i] = entity.getUniqueId();
			else if (arg instanceof CommandSender sender) args[i] = sender.getName();
		}

		return args;
	}

	public record Section(
		@Nullable Section previous, @Nullable DebugConfig config, @NotNull DebugCategory category, @NotNull ArrayDeque<DebugPath> paths,
		int depth, boolean all, boolean suppressWarnings, boolean logged
	) implements AutoCloseable {

		public DebugConfig config() {
			return config == null ? MagicSpells.getDebugConfig() : config;
		}

		public Section pushPath(@NotNull String node, @NotNull DebugPath.Type type) {
			MagicDebug.pushPath(paths, new DebugPath(node, type, type != DebugPath.Type.FILE));
			return this;
		}

		public Section pushPath(@NotNull String node, @NotNull DebugPath.Type type, boolean concrete) {
			MagicDebug.pushPath(paths, new DebugPath(node, type, concrete));
			return this;
		}

		public Section pushPath(@NotNull DebugPath path) {
			MagicDebug.pushPath(paths, path);
			return this;
		}

		public Section popPath(@NotNull DebugPath path) {
			MagicDebug.popPath(paths, path);
			return this;
		}

		@Override
		public void close() {
			Preconditions.checkState(section == this, "Section mis-match");
			section = previous;
		}

		public static class Builder {

			private final ArrayDeque<DebugPath> paths;
			private boolean suppressWarnings;
			private DebugCategory category;
			private DebugConfig config;

			private String message;
			private Object[] args;

			private Builder() {
				suppressWarnings = section.suppressWarnings;
				category = section.category;
				config = section.config;
				paths = new ArrayDeque<>(section.paths);
			}

			public Builder category(@NotNull DebugCategory category) {
				Preconditions.checkNotNull(category);
				this.category = category;
				return this;
			}

			public Builder config(@Nullable DebugConfig config) {
				this.config = config;
				return this;
			}

			public Builder path(@NotNull String node, @NotNull DebugPath.Type type) {
				MagicDebug.pushPath(paths, new DebugPath(node, type, type != DebugPath.Type.FILE));
				return this;
			}

			public Builder path(@NotNull String node, @NotNull DebugPath.Type type, boolean concrete) {
				MagicDebug.pushPath(paths, new DebugPath(node, type, concrete));
				return this;
			}

			public Builder message(@NotNull @PrintFormat String message, @Nullable Object @NotNull ... args) {
				Preconditions.checkNotNull(category);
				Preconditions.checkNotNull(args);
				this.message = message;
				this.args = args;
				return this;
			}

			public Builder suppressWarnings(boolean suppressWarnings) {
				this.suppressWarnings = suppressWarnings;
				return this;
			}

			public Section build() {
				boolean all = section.all || MagicDebug.isEnhanced(config, category);
				boolean logged = section.logged;
				int depth = section.depth;

				if (message == null) return section = new Section(section, config, category, paths, depth, all, suppressWarnings, logged);

				boolean enabled = all || isEnabled(config, category);
				if (enabled) {
					if (logged && !isEnabled(section))
						depth++;

					depth++;
				}

				if (enabled || !suppressLog(DebugLevel.INFO)) {
					logged = true;

					DebugConfig debugConfig = config;
					if (debugConfig == null) debugConfig = MagicSpells.getDebugConfig();

					String indentation;
					if (debugConfig != null) {
						int indent = Math.max((enabled ? depth - 1 : depth) * debugConfig.getIndent(), 0);
						indentation = debugConfig.getIndentCharacter().repeat(indent);
					} else indentation = "";

					Logger logger = MagicSpells.getInstance().getLogger();
					logger.info(indentation + message.formatted(replaceArguments(args)));
				}

				return section = new Section(section, config, category, paths, depth, all, suppressWarnings, logged);
			}

		}

	}

}
