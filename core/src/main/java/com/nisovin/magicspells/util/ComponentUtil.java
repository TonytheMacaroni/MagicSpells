package com.nisovin.magicspells.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ComponentUtil {

	public static final Style DISPLAY_NAME_STYLE = Style.style(builder -> builder
		.font(Style.DEFAULT_FONT)
		.color(NamedTextColor.WHITE)
		.decorate(TextDecoration.ITALIC)
	);

	public static final Style LORE_STYLE = Style.style(builder -> builder
		.font(Style.DEFAULT_FONT)
		.color(NamedTextColor.DARK_PURPLE)
		.decorate(TextDecoration.ITALIC)
	);

	public static boolean visualCompare(@Nullable Component one, @Nullable Component two, @NotNull Style defaultStyle) {
		if (one == two) return true;
		if (one == null || two == null) return false;

		Iterator<StyledComponent> itOne = new ComponentIterator(one, defaultStyle);
		Iterator<StyledComponent> itTwo = new ComponentIterator(two, defaultStyle);

		if (!itOne.hasNext() && !itTwo.hasNext()) return true;

		StyledComponent styledOne = itOne.next();
		StyledComponent styledTwo = itTwo.next();

		int cursorOne = 0;
		int cursorTwo = 0;

		while (true) {
			if (styledOne == null) {
				if (itOne.hasNext()) styledOne = itOne.next();
				else return styledTwo == null && !itTwo.hasNext();
			}

			if (styledTwo == null) {
				if (itTwo.hasNext()) styledTwo = itTwo.next();
				else return styledOne == null && !itOne.hasNext();
			}

			boolean styleDiff = !styledOne.style.equals(styledTwo.style);

			if (styledOne.component instanceof TextComponent textOne && styledTwo.component instanceof TextComponent textTwo) {
				String contentOne = textOne.content();
				String contentTwo = textTwo.content();

				int length = Math.min(contentOne.length() - cursorOne, contentTwo.length() - cursorTwo);

				if (styleDiff) {
					for (int i = 0; i < length; i++) {
						char chOne = contentOne.charAt(cursorOne + i);
						if (chOne != ' ' && chOne != '\n' || chOne != contentTwo.charAt(cursorTwo + i))
							return false;
					}
				} else {
					if (!contentOne.regionMatches(cursorOne, contentTwo, cursorTwo, length))
						return false;
				}

				cursorOne += length;
				cursorTwo += length;

				if (cursorOne >= contentOne.length()) {
					styledOne = null;
					cursorOne = 0;
				}

				if (cursorTwo >= contentTwo.length()) {
					styledTwo = null;
					cursorTwo = 0;
				}

				continue;
			}

			if (styleDiff || !strip(styledOne.component).equals(strip(styledTwo.component))) return false;

			styledOne = null;
			styledTwo = null;
		}
	}

	public static boolean visualCompare(@Nullable List<Component> oneList, @Nullable List<Component> twoList, @NotNull Style defaultStyle) {
		if (oneList == twoList) return true;
		if (oneList == null || twoList == null) return false;

		int size = oneList.size();
		if (size != twoList.size()) return false;

		for (int i = 0; i < size; i++) {
			Component one = oneList.get(i);
			Component two = twoList.get(i);

			if (!visualCompare(one, two, defaultStyle)) return false;
		}

		return true;
	}

	private static Component strip(Component component) {
		if (!component.style().isEmpty()) component = component.style(Style.empty());
		if (!component.children().isEmpty()) component = component.children(Collections.emptyList());

		return component;
	}

	private static final class ComponentIterator implements Iterator<StyledComponent> {

		private final ArrayDeque<Frame> frames = new ArrayDeque<>();

		private Component nextComponent;
		private StyleFrame nextStyle;

		private ComponentIterator(@NotNull Component component, @NotNull Style defaultStyle) {
			nextComponent = component;
			nextStyle = new StyleFrame(defaultStyle).merge(nextComponent.style());

			while (nextComponent instanceof TextComponent text && text.content().isEmpty()) {
				List<Component> children = nextComponent.children();
				Frame frame;

				if (!children.isEmpty()) frame = new Frame(nextStyle, nextComponent);
				else {
					do {
						if (frames.isEmpty()) {
							nextComponent = null;
							nextStyle = null;

							return;
						}

						frame = frames.removeLast();
						children = frame.parent.children();
					} while (frame.childCursor >= children.size());
				}

				nextComponent = children.get(frame.childCursor++);
				nextStyle = frame.parentStyle.merge(nextComponent.style());

				frames.addLast(frame);
			}
		}

		@Override
		public boolean hasNext() {
			return nextComponent != null;
		}

		@Override
		public StyledComponent next() {
			if (nextComponent == null) throw new NoSuchElementException();

			StyledComponent next = new StyledComponent(nextComponent, nextStyle);

			do {
				List<Component> children = nextComponent.children();
				Frame frame;

				if (!children.isEmpty()) frame = new Frame(nextStyle, nextComponent);
				else {
					do {
						if (frames.isEmpty()) {
							nextComponent = null;
							nextStyle = null;

							return next;
						}

						frame = frames.removeLast();
						children = frame.parent.children();
					} while (frame.childCursor >= children.size());
				}

				nextComponent = children.get(frame.childCursor++);
				nextStyle = frame.parentStyle.merge(nextComponent.style());

				frames.addLast(frame);
			} while (nextComponent instanceof TextComponent text && text.content().isEmpty());

			return next;
		}

	}

	private static final class Frame {

		private final StyleFrame parentStyle;
		private final Component parent;
		private int childCursor;

		private Frame(StyleFrame parentStyle, Component parent) {
			this.parentStyle = parentStyle;
			this.parent = parent;
		}

	}

	private record StyledComponent(Component component, StyleFrame style) {

	}

	private record StyleFrame(Key font, int color, int decorations, ClickEvent clickEvent, HoverEvent<?> hoverEvent, String insertion) {

		private static final int BOLD = 1;
		private static final int ITALIC = 1 << 1;
		private static final int OBFUSCATED = 1 << 2;
		private static final int STRIKETHROUGH = 1 << 3;
		private static final int UNDERLINED = 1 << 4;

		public StyleFrame(Style style) {
			this(
				style.font(),
				style.color() instanceof TextColor tc ? tc.value() : -1,
				applyDecorations(0, style),
				style.clickEvent(),
				style.hoverEvent(),
				style.insertion()
			);
		}

		public StyleFrame merge(Style style) {
			int decorations = applyDecorations(this.decorations, style);

			Key font = style.font();
			TextColor color = style.color();
			ClickEvent clickEvent = style.clickEvent();
			HoverEvent<?> hoverEvent = style.hoverEvent();
			String insertion = style.insertion();

			return new StyleFrame(
				font == null ? this.font : font,
				color == null ? this.color : color.value(),
				decorations,
				clickEvent == null ? this.clickEvent : clickEvent,
				hoverEvent == null ? this.hoverEvent : hoverEvent,
				insertion == null ? this.insertion : insertion
			);
		}

		private static int applyDecorations(int decorations, Style style) {
			decorations = applyDecoration(decorations, BOLD, style.decoration(TextDecoration.BOLD));
			decorations = applyDecoration(decorations, ITALIC, style.decoration(TextDecoration.ITALIC));
			decorations = applyDecoration(decorations, OBFUSCATED, style.decoration(TextDecoration.OBFUSCATED));
			decorations = applyDecoration(decorations, STRIKETHROUGH, style.decoration(TextDecoration.STRIKETHROUGH));
			decorations = applyDecoration(decorations, UNDERLINED, style.decoration(TextDecoration.UNDERLINED));

			return decorations;
		}

		private static int applyDecoration(int decorations, int mask, TextDecoration.State state) {
			return switch (state) {
				case TRUE -> decorations | mask;
				case FALSE -> decorations & ~mask;
				case NOT_SET -> decorations;
			};
		}

	}

}
