package dev.fakeultimine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局配置，保存在 config/fakeultimine.json。
 *
 * <p>所有字段都可以直接手改；改完在游戏里执行 {@code /fakeultimine reload} 即可热重载。
 */
public final class UltimineConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("FakePlayerUltimine");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	/** 使用 /fakeultimine 所需的最低权限等级（2 = OP）。 */
	public int permissionLevel = 2;

	/** 单次连锁最多破坏多少方块（含被挖掉的那一个）。 */
	public int maxBlocks = 64;

	/** 全服每游戏刻最多额外破坏多少方块，防止同一刻多个假人一起连锁把服务器卡死。 */
	public int maxBlocksPerTick = 512;

	/** 同一个假人两次连锁之间至少间隔多少刻（0 = 每次挖掘都连锁）。 */
	public int cooldownTicks = 4;

	/** 新假人默认使用的形状，可选值见 /fakeultimine shapes。 */
	public String defaultShape = "cube3";

	/** 是否要求手上工具适合挖该方块（true 时用镐连锁挖泥土会失败）。 */
	public boolean requireCorrectTool = false;

	/** 连锁破坏是否掉物品 / 经验。 */
	public boolean dropItems = true;

	/** 连锁破坏是否消耗工具耐久。 */
	public boolean damageTool = true;

	/** 是否也允许给真实玩家开启（默认只有假人能开）。 */
	public boolean allowRealPlayers = false;

	/** 是否遵守本模组的 blacklist 方块标签。 */
	public boolean respectBlacklist = true;

	/** 是否遵守 FTB Ultimine 的 ftbultimine:excluded_blocks 方块标签。 */
	public boolean respectUltimineTags = true;

	/** 调试模式：每次连锁都往日志里写一行。 */
	public boolean debug = false;

	/** 每个假人的独立设置，key 为假人名字。 */
	public Map<String, BotSettings> bots = new LinkedHashMap<>();

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("fakeultimine.json");
	}

	public static UltimineConfig load() {
		Path path = path();
		if (!Files.exists(path)) {
			UltimineConfig fresh = new UltimineConfig();
			fresh.save();
			return fresh;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			UltimineConfig loaded = GSON.fromJson(reader, UltimineConfig.class);
			if (loaded == null) {
				return new UltimineConfig();
			}
			if (loaded.bots == null) {
				loaded.bots = new LinkedHashMap<>();
			}
			loaded.sanitize();
			return loaded;
		} catch (Exception e) {
			LOGGER.error("[FakePlayerUltimine] 读取配置失败，已使用默认配置: {}", e.toString());
			return new UltimineConfig();
		}
	}

	public void save() {
		sanitize();
		Path path = path();
		try {
			Files.createDirectories(path.getParent());
			Path tmp = path.resolveSibling("fakeultimine.json.tmp");
			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			LOGGER.error("[FakePlayerUltimine] 保存配置失败: {}", e.toString());
		}
	}

	private void sanitize() {
		permissionLevel = clamp(permissionLevel, 0, 4);
		maxBlocks = clamp(maxBlocks, 1, 4096);
		maxBlocksPerTick = clamp(maxBlocksPerTick, 1, 65536);
		cooldownTicks = clamp(cooldownTicks, 0, 72000);
		if (defaultShape == null || ShapeType.byId(defaultShape) == null) {
			defaultShape = ShapeType.CUBE3.id();
		}
		for (Map.Entry<String, BotSettings> entry : bots.entrySet()) {
			if (entry.getValue() == null) {
				entry.setValue(new BotSettings());
			}
			entry.getValue().sanitize();
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
