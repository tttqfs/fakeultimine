package dev.fakeultimine;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 方块过滤。
 *
 * <ul>
 *     <li>{@link Mode#ONLY}：只连锁（且只允许挖）列表里的方块 —— 例如只砍橡木原木，别的方块一律不碰；</li>
 *     <li>{@link Mode#EXCEPT}：除列表里的方块外都能连锁 —— 例如保护橡木原木，其它方块照常连锁；</li>
 *     <li>{@link Mode#OFF}：不做限制。</li>
 * </ul>
 *
 * <p>列表元素既可以是方块 id（{@code minecraft:oak_log}），也可以方块标签（{@code #minecraft:logs}）。
 */
public final class BlockFilter {
	public enum Mode {
		OFF("off"),
		ONLY("only"),
		EXCEPT("except");

		private final String id;

		Mode(String id) {
			this.id = id;
		}

		public String id() {
			return id;
		}

		public static Mode byId(String id) {
			if (id == null) {
				return OFF;
			}
			String trimmed = id.trim().toLowerCase(Locale.ROOT);
			for (Mode mode : values()) {
				if (mode.id.equals(trimmed)) {
					return mode;
				}
			}
			return OFF;
		}
	}

	private static final BlockFilter OFF_FILTER = new BlockFilter(Mode.OFF, Set.of(), List.of());

	private final Mode mode;
	private final Set<Block> blocks;
	private final List<TagKey<Block>> tags;

	private BlockFilter(Mode mode, Set<Block> blocks, List<TagKey<Block>> tags) {
		this.mode = mode;
		this.blocks = blocks;
		this.tags = tags;
	}

	public static BlockFilter off() {
		return OFF_FILTER;
	}

	/** 按假人设置构建过滤器；列表为空时一律视为不限制。 */
	public static BlockFilter of(BotSettings settings) {
		if (settings == null) {
			return OFF_FILTER;
		}
		Mode mode = Mode.byId(settings.filterMode);
		if (mode == Mode.OFF || settings.filterBlocks == null || settings.filterBlocks.isEmpty()) {
			return OFF_FILTER;
		}
		Set<Block> blocks = new LinkedHashSet<>();
		List<TagKey<Block>> tags = new ArrayList<>();
		for (String entry : settings.filterBlocks) {
			Identifier id = parseId(entry);
			if (id == null) {
				continue;
			}
			if (entry.startsWith("#")) {
				tags.add(TagKey.of(RegistryKeys.BLOCK, id));
			} else {
				Block block = Registries.BLOCK.get(id);
				if (block != Blocks.AIR) {
					blocks.add(block);
				}
			}
		}
		if (blocks.isEmpty() && tags.isEmpty()) {
			return OFF_FILTER;
		}
		return new BlockFilter(mode, Set.copyOf(blocks), List.copyOf(tags));
	}

	public Mode mode() {
		return mode;
	}

	public boolean isEmpty() {
		return mode == Mode.OFF || (blocks.isEmpty() && tags.isEmpty());
	}

	public boolean matches(BlockState state) {
		if (state.isAir()) {
			return false;
		}
		if (blocks.contains(state.getBlock())) {
			return true;
		}
		for (TagKey<Block> tag : tags) {
			if (state.isIn(tag)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 校验并规范化一条过滤项，非法时返回 null。
	 * 支持 {@code minecraft:oak_log} 与 {@code #minecraft:logs} 两种写法。
	 */
	public static String canonicalize(String raw) {
		if (raw == null) {
			return null;
		}
		String entry = raw.trim().toLowerCase(Locale.ROOT);
		if (entry.isEmpty()) {
			return null;
		}
		boolean tag = entry.startsWith("#");
		Identifier id = parseId(entry);
		if (id == null) {
			return null;
		}
		if (tag) {
			return "#" + id;
		}
		if (!Registries.BLOCK.containsId(id) || Registries.BLOCK.get(id) == Blocks.AIR) {
			return null;
		}
		return id.toString();
	}

	private static Identifier parseId(String entry) {
		String value = entry.startsWith("#") ? entry.substring(1) : entry;
		if (value.isEmpty() || !value.contains(":")) {
			value = "minecraft:" + value;
		}
		return Identifier.tryParse(value);
	}
}
