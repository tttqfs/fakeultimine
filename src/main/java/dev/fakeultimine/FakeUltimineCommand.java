package dev.fakeultimine;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * /fakeultimine 指令树。
 */
public final class FakeUltimineCommand {
	private static final String PREFIX = "§e[假人连锁]§r ";

	private static final SuggestionProvider<ServerCommandSource> FAKE_PLAYERS = (context, builder) -> {
		List<String> names = new ArrayList<>();
		boolean allowReal = FakeUltimine.config().allowRealPlayers;
		for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
			if (allowReal || CarpetCompat.isFakePlayer(player)) {
				names.add(player.getGameProfile().getName());
			}
		}
		return CommandSource.suggestMatching(names, builder);
	};

	private static final SuggestionProvider<ServerCommandSource> SHAPES = (context, builder) ->
			CommandSource.suggestMatching(ShapeType.ids(), builder);

	private static final SuggestionProvider<ServerCommandSource> BLOCK_IDS = (context, builder) -> {
		List<String> ids = new ArrayList<>();
		for (Identifier id : Registries.BLOCK.getIds()) {
			ids.add(id.toString());
		}
		return CommandSource.suggestMatching(ids, builder);
	};

	private static final SuggestionProvider<ServerCommandSource> BLOCK_TAGS = (context, builder) ->
			CommandSource.suggestMatching(List.of(
					"minecraft:logs",
					"minecraft:logs_that_burn",
					"minecraft:planks",
					"minecraft:leaves",
					"minecraft:flowers",
					"minecraft:stone_bricks",
					"minecraft:base_stone_overworld",
					"minecraft:dirt",
					"minecraft:sand",
					"minecraft:coal_ores",
					"minecraft:iron_ores",
					"minecraft:gold_ores",
					"minecraft:diamond_ores",
					"minecraft:emerald_ores",
					"minecraft:redstone_ores",
					"minecraft:lapis_ores",
					"minecraft:copper_ores",
					"c:ores",
					"c:stones"
			), builder);

	private FakeUltimineCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(buildRoot("fakeultimine"));
		dispatcher.register(buildRoot("ful"));
	}

	private static LiteralArgumentBuilder<ServerCommandSource> buildRoot(String name) {
		LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal(name)
				.requires(source -> source.hasPermissionLevel(FakeUltimine.config().permissionLevel))
				.executes(context -> help(context.getSource()));

		root.then(CommandManager.literal("help").executes(context -> help(context.getSource())));
		root.then(CommandManager.literal("shapes").executes(context -> listShapes(context.getSource())));
		root.then(CommandManager.literal("list").executes(context -> listBots(context.getSource())));
		root.then(CommandManager.literal("status")
				.executes(context -> status(context.getSource(), null))
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.executes(context -> status(context.getSource(), EntityArgumentType.getPlayer(context, "player")))));
		root.then(CommandManager.literal("on")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.executes(context -> setEnabled(context, true))));
		root.then(CommandManager.literal("off")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.executes(context -> setEnabled(context, false))));
		root.then(CommandManager.literal("toggle")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.executes(FakeUltimineCommand::toggle)));
		root.then(CommandManager.literal("all")
				.then(CommandManager.argument("enabled", BoolArgumentType.bool())
						.executes(FakeUltimineCommand::setAll)));
		root.then(CommandManager.literal("shape")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.then(CommandManager.argument("shape", StringArgumentType.word())
								.suggests(SHAPES)
								.executes(FakeUltimineCommand::setShape))));
		root.then(CommandManager.literal("max")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.then(CommandManager.argument("blocks", IntegerArgumentType.integer(1, 4096))
								.executes(FakeUltimineCommand::setMax))));
		root.then(CommandManager.literal("drop")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.then(CommandManager.argument("enabled", BoolArgumentType.bool())
								.executes(FakeUltimineCommand::setDrop))));
		root.then(CommandManager.literal("durability")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.then(CommandManager.argument("enabled", BoolArgumentType.bool())
								.executes(FakeUltimineCommand::setDurability))));
		root.then(CommandManager.literal("tool")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.then(CommandManager.argument("enabled", BoolArgumentType.bool())
								.executes(FakeUltimineCommand::setRequireTool))));
		root.then(CommandManager.literal("only")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.then(CommandManager.argument("block", IdentifierArgumentType.identifier())
								.suggests(BLOCK_IDS)
								.executes(context -> setFilter(context, BlockFilter.Mode.ONLY, false)))));
		root.then(CommandManager.literal("except")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.then(CommandManager.argument("block", IdentifierArgumentType.identifier())
								.suggests(BLOCK_IDS)
								.executes(context -> setFilter(context, BlockFilter.Mode.EXCEPT, false)))));
		root.then(CommandManager.literal("onlytag")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.then(CommandManager.argument("tag", IdentifierArgumentType.identifier())
								.suggests(BLOCK_TAGS)
								.executes(context -> setFilter(context, BlockFilter.Mode.ONLY, true)))));
		root.then(CommandManager.literal("excepttag")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.then(CommandManager.argument("tag", IdentifierArgumentType.identifier())
								.suggests(BLOCK_TAGS)
								.executes(context -> setFilter(context, BlockFilter.Mode.EXCEPT, true)))));
		root.then(CommandManager.literal("filter")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.suggests(FAKE_PLAYERS)
						.then(CommandManager.literal("list").executes(FakeUltimineCommand::listFilter))
						.then(CommandManager.literal("clear").executes(FakeUltimineCommand::clearFilter))));
		root.then(CommandManager.literal("reload").executes(FakeUltimineCommand::reload));
		root.then(CommandManager.literal("prune").executes(FakeUltimineCommand::prune));
		return root;
	}

	private static int help(ServerCommandSource source) {
		source.sendFeedback(() -> Text.literal(PREFIX + "§b假人连锁破坏 §7- 指令帮助"), false);
		source.sendFeedback(() -> Text.literal("§e/ful on|off <假人> §7开关某个假人的连锁破坏"), false);
		source.sendFeedback(() -> Text.literal("§e/ful toggle <假人> §7切换开关"), false);
		source.sendFeedback(() -> Text.literal("§e/ful all <true|false> §7开关当前所有在线假人"), false);
		source.sendFeedback(() -> Text.literal("§e/ful list §7列出已开启的假人"), false);
		source.sendFeedback(() -> Text.literal("§e/ful status [假人] §7查看全局/单个假人配置"), false);
		source.sendFeedback(() -> Text.literal("§e/ful shape <假人> <形状> §7设置形状（/ful shapes 查看）"), false);
		source.sendFeedback(() -> Text.literal("§e/ful max <假人> <数量> §7单次最多破坏多少方块"), false);
		source.sendFeedback(() -> Text.literal("§e/ful drop <假人> <true|false> §7是否掉落物品"), false);
		source.sendFeedback(() -> Text.literal("§e/ful durability <假人> <true|false> §7是否消耗工具耐久"), false);
		source.sendFeedback(() -> Text.literal("§e/ful tool <假人> <true|false> §7是否要求手持正确工具"), false);
		source.sendFeedback(() -> Text.literal("§e/ful only <假人> <方块> §7只挖、只连锁这个方块（可重复执行添加多个）"), false);
		source.sendFeedback(() -> Text.literal("§e/ful except <假人> <方块> §7除这个方块外都能连锁"), false);
		source.sendFeedback(() -> Text.literal("§e/ful onlytag|excepttag <假人> <标签> §7同上，但用方块标签，如 logs"), false);
		source.sendFeedback(() -> Text.literal("§e/ful filter <假人> list|clear §7查看/取消方块限制"), false);
		source.sendFeedback(() -> Text.literal("§e/ful reload §7重载 config/fakeultimine.json"), false);
		source.sendFeedback(() -> Text.literal("§e/ful prune §7清理已关闭的假人记录"), false);
		source.sendFeedback(() -> Text.literal("§7配置文件: §fconfig/fakeultimine.json"), false);
		return 1;
	}

	private static int listShapes(ServerCommandSource source) {
		StringBuilder builder = new StringBuilder(PREFIX + "§b可用形状§7: ");
		boolean first = true;
		for (ShapeType type : ShapeType.values()) {
			if (!first) {
				builder.append("§7, ");
			}
			builder.append("§e").append(type.id()).append("§7(").append(type.displayName()).append(")");
			first = false;
		}
		String message = builder.toString();
		source.sendFeedback(() -> Text.literal(message), false);
		return 1;
	}

	private static int listBots(ServerCommandSource source) {
		List<String> names = BotRegistry.enabledNames();
		if (names.isEmpty()) {
			source.sendFeedback(() -> Text.literal(PREFIX + "§7当前没有假人开启连锁破坏。"), false);
			return 0;
		}
		source.sendFeedback(() -> Text.literal(PREFIX + "§a已开启(" + names.size() + ")§7: §f" + String.join("§7, §f", names)), false);
		return names.size();
	}

	private static int status(ServerCommandSource source, ServerPlayerEntity player) {
		UltimineConfig cfg = FakeUltimine.config();
		if (player == null) {
			source.sendFeedback(() -> Text.literal(PREFIX + "§b全局配置"), false);
			source.sendFeedback(() -> Text.literal("§7  权限等级: §f" + cfg.permissionLevel
					+ " §7| 单次上限: §f" + cfg.maxBlocks
					+ " §7| 每刻预算: §f" + cfg.maxBlocksPerTick
					+ " §7| 冷却: §f" + cfg.cooldownTicks + "刻"), false);
			source.sendFeedback(() -> Text.literal("§7  默认形状: §f" + cfg.defaultShape
					+ " §7| 掉落: §f" + cfg.dropItems
					+ " §7| 耗耐久: §f" + cfg.damageTool
					+ " §7| 需工具: §f" + cfg.requireCorrectTool), false);
			source.sendFeedback(() -> Text.literal("§7  允许真实玩家: §f" + cfg.allowRealPlayers
					+ " §7| 调试: §f" + cfg.debug), false);
			return 1;
		}
		String name = player.getGameProfile().getName();
		BotSettings settings = BotRegistry.get(name);
		source.sendFeedback(() -> Text.literal(PREFIX + "§b假人 §f" + name + "§7（"
				+ CarpetCompat.fakeKind(player).orElse("真实玩家") + "）"), false);
		if (settings == null) {
			source.sendFeedback(() -> Text.literal("§7  未配置，默认关闭。用 §e/ful on " + name + " §7开启。"), false);
			return 1;
		}
		source.sendFeedback(() -> Text.literal("§7  开关: " + (settings.enabled ? "§a开" : "§c关")
				+ " §7| 形状: §f" + (settings.shape.isEmpty() ? "默认(" + cfg.defaultShape + ")" : settings.shape)
				+ " §7| 上限: §f" + (settings.maxBlocks > 0 ? String.valueOf(settings.maxBlocks) : "默认(" + cfg.maxBlocks + ")")), false);
		source.sendFeedback(() -> Text.literal("§7  掉落: §f" + settings.drops()
				+ " §7| 耗耐久: §f" + settings.damagesTool()
				+ " §7| 需工具: §f" + settings.needsCorrectTool()), false);
		BlockFilter filter = settings.filter();
		source.sendFeedback(() -> Text.literal("§7  方块限制: §f" + (filter.isEmpty() ? "无（什么都挖、什么都连锁）"
				: filter.mode().id() + " §7→ §f" + String.join("§7, §f", settings.filterBlocks))), false);
		return 1;
	}

	private static int setFilter(CommandContext<ServerCommandSource> context, BlockFilter.Mode mode, boolean isTag) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
		if (!checkTarget(context, player)) {
			return 0;
		}
		Identifier id = IdentifierArgumentType.getIdentifier(context, isTag ? "tag" : "block");
		String raw = isTag ? "#" + id : id.toString();
		String entry = BlockFilter.canonicalize(raw);
		if (entry == null) {
			error(context, "无法识别的" + (isTag ? "方块标签 §f#" : "方块 id §f") + id
					+ "§c，方块 id 例如 minecraft:oak_log，标签例如 minecraft:logs");
			return 0;
		}
		String name = player.getGameProfile().getName();
		BotSettings settings = BotRegistry.getOrCreate(name);
		boolean switched = !mode.id().equals(settings.filterMode);
		if (switched) {
			settings.filterBlocks.clear();
			settings.filterMode = mode.id();
		}
		if (settings.filterBlocks.stream().noneMatch(entry::equalsIgnoreCase)) {
			settings.filterBlocks.add(entry);
		}
		FakeUltimine.config().save();

		String modeText = mode == BlockFilter.Mode.ONLY
				? "只挖、只连锁这些方块"
				: "除了这些方块，其它方块都能连锁";
		feedback(context, (switched ? "§7(已切换模式，旧列表已清空) " : "")
				+ "§a假人 §f" + name + " §a的限制：§f" + modeText
				+ " §7→ §f" + String.join("§7, §f", settings.filterBlocks));
		return 1;
	}

	private static int listFilter(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
		String name = player.getGameProfile().getName();
		BotSettings settings = BotRegistry.get(name);
		if (settings == null || settings.filter().isEmpty()) {
			feedback(context, "§7假人 §f" + name + " §7当前没有方块限制（什么都挖、什么都连锁）");
			return 1;
		}
		feedback(context, "§a假人 §f" + name + " §a的限制模式：§f" + settings.filterMode
				+ " §7→ §f" + String.join("§7, §f", settings.filterBlocks));
		return 1;
	}

	private static int clearFilter(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
		if (!checkTarget(context, player)) {
			return 0;
		}
		String name = player.getGameProfile().getName();
		BotSettings settings = BotRegistry.get(name);
		if (settings != null) {
			settings.filterMode = "off";
			settings.filterBlocks.clear();
			FakeUltimine.config().save();
		}
		feedback(context, "§a已取消假人 §f" + name + " §a的方块限制");
		return 1;
	}

	private static int setEnabled(CommandContext<ServerCommandSource> context, boolean enabled) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
		if (!checkTarget(context, player)) {
			return 0;
		}
		String name = player.getGameProfile().getName();
		BotSettings settings = BotRegistry.getOrCreate(name);
		settings.enabled = enabled;
		FakeUltimine.config().save();
		if (enabled) {
			String shape = settings.shapeType() == null ? "cube3" : settings.shapeType().id();
			feedback(context, "§a已为假人 §f" + name + " §a开启连锁破坏 §7(形状 " + shape + ", 上限 "
					+ settings.maxOrDefault() + ", 掉落 " + settings.drops() + ")");
		} else {
			feedback(context, "§c已关闭假人 §f" + name + " §c的连锁破坏");
		}
		return 1;
	}

	private static int toggle(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
		if (!checkTarget(context, player)) {
			return 0;
		}
		String name = player.getGameProfile().getName();
		BotSettings settings = BotRegistry.getOrCreate(name);
		settings.enabled = !settings.enabled;
		FakeUltimine.config().save();
		boolean enabled = settings.enabled;
		feedback(context, enabled
				? "§a已为假人 §f" + name + " §a开启连锁破坏"
				: "§c已关闭假人 §f" + name + " §c的连锁破坏");
		return 1;
	}

	private static int setAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		boolean enabled = BoolArgumentType.getBool(context, "enabled");
		int changed = 0;
		for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
			if (!FakeUltimine.config().allowRealPlayers && !CarpetCompat.isFakePlayer(player)) {
				continue;
			}
			BotRegistry.getOrCreate(player.getGameProfile().getName()).enabled = enabled;
			changed++;
		}
		FakeUltimine.config().save();
		int total = changed;
		feedback(context, enabled
				? "§a已为 §f" + total + " §a个在线假人开启连锁破坏"
				: "§c已关闭 §f" + total + " §c个在线假人的连锁破坏");
		return changed;
	}

	private static int setShape(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
		if (!checkTarget(context, player)) {
			return 0;
		}
		String shapeId = StringArgumentType.getString(context, "shape");
		ShapeType shape = ShapeType.byId(shapeId);
		if (shape == null) {
			error(context, "未知形状 §f" + shapeId + "§c，可用: §f" + String.join(", ", ShapeType.ids()));
			return 0;
		}
		BotSettings settings = BotRegistry.getOrCreate(player.getGameProfile().getName());
		settings.shape = shape.id();
		FakeUltimine.config().save();
		feedback(context, "§a假人 §f" + player.getGameProfile().getName() + " §a的形状已设为 §f"
				+ shape.id() + "§7(" + shape.displayName() + ")");
		return 1;
	}

	private static int setMax(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
		if (!checkTarget(context, player)) {
			return 0;
		}
		int blocks = IntegerArgumentType.getInteger(context, "blocks");
		BotSettings settings = BotRegistry.getOrCreate(player.getGameProfile().getName());
		settings.maxBlocks = blocks;
		FakeUltimine.config().save();
		feedback(context, "§a假人 §f" + player.getGameProfile().getName() + " §a的单次连锁上限已设为 §f" + blocks);
		return 1;
	}

	private static int setDrop(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
		if (!checkTarget(context, player)) {
			return 0;
		}
		boolean enabled = BoolArgumentType.getBool(context, "enabled");
		BotRegistry.getOrCreate(player.getGameProfile().getName()).dropItems = enabled;
		FakeUltimine.config().save();
		feedback(context, "§a假人 §f" + player.getGameProfile().getName() + " §a连锁掉落已设为 §f" + enabled);
		return 1;
	}

	private static int setDurability(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
		if (!checkTarget(context, player)) {
			return 0;
		}
		boolean enabled = BoolArgumentType.getBool(context, "enabled");
		BotRegistry.getOrCreate(player.getGameProfile().getName()).damageTool = enabled;
		FakeUltimine.config().save();
		feedback(context, "§a假人 §f" + player.getGameProfile().getName() + " §a连锁消耗耐久已设为 §f" + enabled);
		return 1;
	}

	private static int setRequireTool(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
		if (!checkTarget(context, player)) {
			return 0;
		}
		boolean enabled = BoolArgumentType.getBool(context, "enabled");
		BotRegistry.getOrCreate(player.getGameProfile().getName()).requireCorrectTool = enabled;
		FakeUltimine.config().save();
		feedback(context, "§a假人 §f" + player.getGameProfile().getName() + " §a要求正确工具已设为 §f" + enabled);
		return 1;
	}

	private static int reload(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		UltimineConfig loaded = UltimineConfig.load();
		UltimineConfig current = FakeUltimine.config();
		current.permissionLevel = loaded.permissionLevel;
		current.maxBlocks = loaded.maxBlocks;
		current.maxBlocksPerTick = loaded.maxBlocksPerTick;
		current.cooldownTicks = loaded.cooldownTicks;
		current.defaultShape = loaded.defaultShape;
		current.requireCorrectTool = loaded.requireCorrectTool;
		current.dropItems = loaded.dropItems;
		current.damageTool = loaded.damageTool;
		current.allowRealPlayers = loaded.allowRealPlayers;
		current.respectBlacklist = loaded.respectBlacklist;
		current.respectUltimineTags = loaded.respectUltimineTags;
		current.debug = loaded.debug;
		current.bots.clear();
		current.bots.putAll(loaded.bots);
		BotRegistry.clearCooldowns();
		feedback(context, "§a配置已重载 §7(假人记录 " + current.bots.size() + " 条)");
		return 1;
	}

	private static int prune(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		int removed = BotRegistry.pruneDisabled();
		FakeUltimine.config().save();
		feedback(context, "§a已清理 §f" + removed + " §a条已关闭的假人记录");
		return removed;
	}

	private static boolean checkTarget(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
		if (FakeUltimine.config().allowRealPlayers || CarpetCompat.isFakePlayer(player)) {
			return true;
		}
		error(context, "§f" + player.getGameProfile().getName()
				+ " §c不是假人（只对地毯 / GCA 假人生效，可在配置里打开 allowRealPlayers）");
		return false;
	}

	private static void feedback(CommandContext<ServerCommandSource> context, String message) {
		ServerCommandSource source = context.getSource();
		source.sendFeedback(() -> Text.literal(PREFIX + message), true);
	}

	private static void error(CommandContext<ServerCommandSource> context, String message) {
		context.getSource().sendError(Text.literal(PREFIX + message).formatted(Formatting.RED));
	}
}
