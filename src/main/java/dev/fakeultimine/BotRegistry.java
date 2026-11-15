package dev.fakeultimine;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 假人开关状态与冷却管理。状态按“假人名字”保存在配置文件里，重启服务器后依然生效。
 */
public final class BotRegistry {
	private static final Map<String, Integer> LAST_CHAIN_TICK = new HashMap<>();

	private BotRegistry() {
	}

	public static BotSettings get(String name) {
		return FakeUltimine.config().bots.get(name);
	}

	public static BotSettings getOrCreate(String name) {
		return FakeUltimine.config().bots.computeIfAbsent(name, key -> new BotSettings());
	}

	/**
	 * 这个玩家是否已被开启连锁（不考虑冷却）；返回 null 表示不连锁。
	 */
	public static BotSettings enabledFor(ServerPlayerEntity player) {
		UltimineConfig cfg = FakeUltimine.config();
		String name = player.getGameProfile().getName();
		BotSettings settings = cfg.bots.get(name);
		if (settings == null || !settings.enabled) {
			return null;
		}
		if (!cfg.allowRealPlayers && !CarpetCompat.isFakePlayer(player)) {
			return null;
		}
		return settings;
	}

	public static boolean onCooldown(ServerPlayerEntity player) {
		UltimineConfig cfg = FakeUltimine.config();
		if (cfg.cooldownTicks <= 0) {
			return false;
		}
		int now = player.getServer() == null ? 0 : player.getServer().getTicks();
		Integer last = LAST_CHAIN_TICK.get(player.getGameProfile().getName());
		return last != null && now - last < cfg.cooldownTicks;
	}

	/**
	 * 判断这个玩家此刻是否应该连锁破坏；返回 null 表示不连锁。
	 */
	public static BotSettings activeFor(ServerPlayerEntity player) {
		BotSettings settings = enabledFor(player);
		if (settings == null || onCooldown(player)) {
			return null;
		}
		return settings;
	}

	public static void markChained(ServerPlayerEntity player) {
		int now = player.getServer() == null ? 0 : player.getServer().getTicks();
		LAST_CHAIN_TICK.put(player.getGameProfile().getName(), now);
	}

	public static List<String> enabledNames() {
		List<String> names = new ArrayList<>();
		for (Map.Entry<String, BotSettings> entry : FakeUltimine.config().bots.entrySet()) {
			if (entry.getValue() != null && entry.getValue().enabled) {
				names.add(entry.getKey());
			}
		}
		names.sort(String::compareToIgnoreCase);
		return names;
	}

	/** 清掉所有“关闭状态”的假人记录，返回清理数量。 */
	public static int pruneDisabled() {
		Map<String, BotSettings> bots = FakeUltimine.config().bots;
		int before = bots.size();
		bots.entrySet().removeIf(entry -> entry.getValue() == null || !entry.getValue().enabled);
		return before - bots.size();
	}

	public static void clearCooldowns() {
		LAST_CHAIN_TICK.clear();
	}
}
