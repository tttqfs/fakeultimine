package dev.fakeultimine;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 连锁破坏核心。
 *
 * <p>触发时机：假人真正挖掉一个方块之后（Fabric API 的 {@code PlayerBlockBreakEvents.AFTER}）。
 * 原方块的掉落、经验、工具消耗、统计仍由原版逻辑处理，我们只负责“顺手再多挖几个”。
 */
public final class ChainBreaker {
	private static final String LOG_PREFIX = "[FakePlayerUltimine] ";

	/** 正在连锁时忽略自身再触发的破坏事件，避免无限递归。 */
	private static boolean chaining = false;

	/** 全服每刻的连锁预算，防止一 tick 内破坏量爆炸。 */
	private static int budgetTick = -1;
	private static int budgetUsed = 0;

	/** durability=false 时，记录原方块被挖前的工具耐久，在本刻结束时还原（扣耐久发生在破坏事件之后）。 */
	private static final Map<UUID, Integer> SAVED_TOOL_DAMAGE = new HashMap<>();
	private static final Map<UUID, Integer> PENDING_RESTORE = new HashMap<>();

	private ChainBreaker() {
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register(ChainBreaker::beforeBlockBreak);
		PlayerBlockBreakEvents.AFTER.register(ChainBreaker::afterBlockBreak);
		ServerTickEvents.END_SERVER_TICK.register(ChainBreaker::endTick);
	}

	private static void endTick(net.minecraft.server.MinecraftServer server) {
		if (PENDING_RESTORE.isEmpty()) {
			return;
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			Integer saved = PENDING_RESTORE.remove(player.getUuid());
			if (saved == null) {
				continue;
			}
			ItemStack tool = player.getMainHandStack();
			if (!tool.isEmpty() && tool.isDamageable()) {
				tool.setDamage(Math.min(saved, Math.max(0, tool.getMaxDamage() - 1)));
			}
		}
	}

	private static boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (chaining || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return true;
		}
		BotSettings settings = BotRegistry.enabledFor(serverPlayer);
		if (settings == null) {
			return true;
		}
		// only 模式：不是白名单里的方块，连挖都不让挖
		BlockFilter blockFilter = settings.filter();
		if (blockFilter.mode() == BlockFilter.Mode.ONLY && !blockFilter.matches(state)) {
			if (FakeUltimine.config().debug) {
				FakeUltimine.LOGGER.info("{}假人 {} 试图挖不在白名单里的 {}，已阻止",
						LOG_PREFIX, serverPlayer.getGameProfile().getName(), state.getBlock());
			}
			return false;
		}
		if (!settings.damagesTool()) {
			ItemStack tool = serverPlayer.getMainHandStack();
			if (!tool.isEmpty()) {
				SAVED_TOOL_DAMAGE.put(serverPlayer.getUuid(), tool.getDamage());
			}
		}
		return true;
	}

	private static void afterBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (chaining || state.isAir() || !(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}
		BotSettings settings = BotRegistry.enabledFor(serverPlayer);
		if (settings == null) {
			return;
		}
		try {
			if (BotRegistry.onCooldown(serverPlayer)) {
				return;
			}
			BlockFilter blockFilter = settings.filter();
			if (blockFilter.mode() == BlockFilter.Mode.ONLY && !blockFilter.matches(state)) {
				return;
			}
			if (blockFilter.mode() == BlockFilter.Mode.EXCEPT && blockFilter.matches(state)) {
				return;
			}
			int max = settings.maxOrDefault();
			if (max <= 1) {
				return;
			}
			ShapeType shape = settings.shapeType();
			if (shape == null) {
				return;
			}
			Direction direction = facing(serverPlayer, pos);
			Predicate<BlockPos> targetFilter = target -> canBreak(serverWorld, serverPlayer, target, state, settings, blockFilter);
			List<BlockPos> targets = shape.collect(pos, direction, max - 1, targetFilter);
			if (targets.isEmpty()) {
				return;
			}

			BotRegistry.markChained(serverPlayer);
			int broken = chainBreak(serverWorld, serverPlayer, pos, state, settings, targets, blockFilter);
			if (FakeUltimine.config().debug) {
				FakeUltimine.LOGGER.info("{}假人 {} 用【{}】连锁 {} 个方块，起点 {}，维度 {}",
						LOG_PREFIX, serverPlayer.getGameProfile().getName(), shape.id(), broken + 1, pos.toShortString(),
						serverWorld.getRegistryKey().getValue());
			}
		} finally {
			if (!settings.damagesTool()) {
				scheduleRestore(serverPlayer);
			}
		}
	}

	/**
	 * 执行连锁破坏，返回额外挖掉的方块数量。
	 */
	private static int chainBreak(ServerWorld world, ServerPlayerEntity player, BlockPos origin, BlockState originState,
								  BotSettings settings, List<BlockPos> targets, BlockFilter blockFilter) {
		UltimineConfig cfg = FakeUltimine.config();
		boolean drops = settings.drops();
		boolean damageTool = settings.damagesTool();

		int tick = player.getServer() == null ? 0 : player.getServer().getTicks();
		if (tick != budgetTick) {
			budgetTick = tick;
			budgetUsed = 0;
		}

		// 空手也能连锁：只有“本来拿着工具、中途被挖断”才停止
		boolean hadTool = !player.getMainHandStack().isEmpty();

		int broken = 0;
		chaining = true;
		try {
			for (BlockPos target : targets) {
				if (target.equals(origin)) {
					continue;
				}
				if (budgetUsed >= cfg.maxBlocksPerTick) {
					break;
				}
				if (player.isRemoved() || player.getWorld() != world) {
					break;
				}

				ItemStack tool = player.getMainHandStack();
				if (hadTool && tool.isEmpty()) {
					break;
				}
				int damageBefore = tool.isEmpty() ? 0 : tool.getDamage();

				if (!canBreak(world, player, target, originState, settings, blockFilter)) {
					continue;
				}

				boolean success;
				if (drops) {
					success = player.interactionManager.tryBreakBlock(target);
				} else {
					success = world.breakBlock(target, false, player);
				}
				if (!success) {
					continue;
				}

				broken++;
				budgetUsed++;

				if (!damageTool) {
					ItemStack after = player.getMainHandStack();
					if (!after.isEmpty()) {
						after.setDamage(Math.min(damageBefore, Math.max(0, after.getMaxDamage() - 1)));
					}
				}
				if (hadTool && player.getMainHandStack().isEmpty()) {
					break;
				}
			}
		} finally {
			chaining = false;
		}
		return broken;
	}

	/**
	 * 单个方块是否允许连锁破坏。
	 */
	private static boolean canBreak(ServerWorld world, ServerPlayerEntity player, BlockPos target,
									BlockState originState, BotSettings settings, BlockFilter blockFilter) {
		UltimineConfig cfg = FakeUltimine.config();
		if (world.isOutOfHeightLimit(target)) {
			return false;
		}
		BlockState state = world.getBlockState(target);
		if (state.isAir()) {
			return false;
		}
		switch (blockFilter.mode()) {
			case ONLY -> {
				// only 模式：只连锁白名单里的方块（可以是多种，配合 #标签 效果更好）
				if (!blockFilter.matches(state)) {
					return false;
				}
			}
			case EXCEPT -> {
				// except 模式：仍然只连锁同种方块，并且跳过黑名单里的方块
				if (!state.isOf(originState.getBlock()) || blockFilter.matches(state)) {
					return false;
				}
			}
			default -> {
				// 只连锁“同一种方块”
				if (!state.isOf(originState.getBlock())) {
					return false;
				}
			}
		}
		// 基岩之类硬度为 -1 的方块永远不连锁
		if (state.getHardness(world, target) < 0.0F) {
			return false;
		}
		if (!world.getWorldBorder().contains(target)) {
			return false;
		}
		if (cfg.respectBlacklist && state.isIn(FakeUltimine.BLACKLIST)) {
			return false;
		}
		if (cfg.respectUltimineTags && state.isIn(FakeUltimine.ULTIMINE_EXCLUDED)) {
			return false;
		}
		if (settings.needsCorrectTool()) {
			ItemStack tool = player.getMainHandStack();
			if (tool.isEmpty() || !tool.isSuitableFor(state)) {
				return false;
			}
		}
		return true;
	}

	/** durability=false：把本次挖掘（含原方块）消耗的耐久记下来，等这一游戏刻结束再还原。 */
	private static void scheduleRestore(ServerPlayerEntity player) {
		Integer saved = SAVED_TOOL_DAMAGE.remove(player.getUuid());
		if (saved != null) {
			PENDING_RESTORE.put(player.getUuid(), saved);
		}
	}

	/** 从假人眼睛指向被挖方块的方向，用作隧道 / 平面 / 直线的朝向。 */
	private static Direction facing(ServerPlayerEntity player, BlockPos pos) {
		Vec3d look = player.getRotationVector();
		if (look.lengthSquared() < 1.0E-4) {
			look = Vec3d.ofCenter(pos).subtract(player.getEyePos());
		}
		if (look.lengthSquared() < 1.0E-4) {
			return Direction.DOWN;
		}
		return Direction.getFacing(look.x, look.y, look.z);
	}
}
