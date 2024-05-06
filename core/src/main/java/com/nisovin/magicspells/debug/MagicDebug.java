package com.nisovin.magicspells.debug;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.intellij.lang.annotations.PrintFormat;

import java.util.Objects;
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

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.config.ConfigData;

public class MagicDebug {

	private static Section section = new Section(null, null, DebugCategory.DEFAULT, new ArrayDeque<>(), 0, false, false);

	@NotNull
	public static Section.Builder section() {
		return new Section.Builder();
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
			int adjustment = level == DebugLevel.ERROR ? -1 : 0;
			int indent = Math.max(section.depth() * config.getIndent() + adjustment, 0);
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

	public static void pushPath(String node, String text) {
		Preconditions.checkArgument(node != null || text != null, "The node and text cannot both be null.");
		section.path.addFirst(new Path(node, text));
	}

	public static void popPath(@Nullable String node) {
		Path path = section.path.getFirst();
		Preconditions.checkArgument(Objects.equals(path.node, node), "Attempted to pop path with a non-matching node.");
		section.path.removeFirst();
	}

	@NotNull
	public static Supplier<String> resolvePath() {
		return () -> {
			StringBuilder builder = new StringBuilder();

			boolean prev = false;
			for (Path path : section.path) {
				if (path.text == null) continue;

				if (prev) builder.append(" ");
				else prev = true;

				builder.append(path.text);
			}

			return builder.toString();
		};
	}

	@NotNull
	public static Supplier<String> resolvePath(@NotNull String subPath) {
		return () -> {
			StringBuilder builder = new StringBuilder("' ");

			boolean prev = false;
			for (Path path : section.path) {
				if (path.text == null) continue;

				if (prev) builder.append(" ");
				else prev = true;

				builder.append(path.text);
			}

			String currentPath = subPath;

			int separator = currentPath.indexOf('.');
			if (separator != -1) {
				int start = 0;

				Iterator<Path> it = section.path.descendingIterator();
				while (it.hasNext() && separator != -1) {
					Path path = it.next();

					if (path.node != null && currentPath.regionMatches(start, path.node, 0, separator - start)) {
						start = separator + 1;
						separator = currentPath.indexOf('.', start);
					} else if (start != 0) break;
				}

				currentPath = currentPath.substring(start);
			}

			return "'" + currentPath + builder;
		};
	}

	@NotNull
	private static String getIndent(@Nullable DebugConfig config, int depth) {
		if (config == null) {
			config = MagicSpells.getDebugConfig();
			if (config == null) return "";
		}

		int indent = config.getIndent();
		indent *= section.depth + depth;

		return config.getIndentCharacter().repeat(indent);
	}

	private static Object[] replaceArguments(Object[] args) {
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			if (arg instanceof ConfigData<?> data && data.isConstant()) args[i] = data.get();
			else if (arg instanceof Supplier<?> supp) args[i] = supp.get();
			else if (arg instanceof Component comp) args[i] = ANSIComponentSerializer.ansi().serialize(comp);
			else if (arg instanceof Keyed keyed) args[i] = keyed.getKey().asMinimalString();
		}

		return args;
	}

	private record Path(@Nullable String node, @Nullable String text) {

	}

	public record Section(@Nullable Section previous, @Nullable DebugConfig config, @NotNull DebugCategory category,
						  @NotNull ArrayDeque<Path> path, int depth, boolean all, boolean suppressWarnings) implements AutoCloseable {

		public DebugConfig config() {
			return config == null ? MagicSpells.getDebugConfig() : config;
		}

		public Section path(@Nullable String node, @Nullable String text) {
			Preconditions.checkArgument(node != null || text != null, "The node and text cannot both be null.");
			path.addFirst(new Path(node, text));
			return this;
		}

		@Override
		public void close() {
			section = previous;
		}

		public static class Builder {

			private final ArrayDeque<Path> path;
			private boolean suppressWarnings;
			private DebugCategory category;
			private DebugConfig config;

			private String message;
			private Object[] args;

			private Builder() {
				suppressWarnings = section.suppressWarnings;
				category = section.category;
				config = section.config;
				path = new ArrayDeque<>(section.path);
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

			public Builder path(@Nullable String node, @Nullable String text) {
				Preconditions.checkArgument(node != null || text != null, "The node and text cannot both be null.");
				path.addFirst(new Path(node, text));
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
				int depth = section.depth;

				if (message == null) return section = new Section(section, config, category, path, depth, all, suppressWarnings);

				boolean enabled = all || isEnabled(config, category);
				if (enabled) {
					if (!isEnabled(section)) depth++;
					depth++;
				}

				if (enabled || section.category != DebugCategory.DEFAULT && !suppressLog(DebugLevel.INFO)) {
					DebugConfig debugConfig = config;
					if (debugConfig == null) debugConfig = MagicSpells.getDebugConfig();

					String indentation;
					if (debugConfig != null) {
						int indent = Math.max((depth - 1) * debugConfig.getIndent(), 0);
						indentation = debugConfig.getIndentCharacter().repeat(indent);
					} else indentation = "";

					Logger logger = MagicSpells.getInstance().getLogger();
					logger.info(indentation + message.formatted(replaceArguments(args)));
				}

				return section = new Section(section, config, category, path, depth, all, suppressWarnings);
			}

		}

	}

}
