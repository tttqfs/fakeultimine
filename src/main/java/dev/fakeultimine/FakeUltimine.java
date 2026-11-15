package dev.fakeultimine;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FakePlayer Ultimine —— 假人连锁破坏附属模组。
 *
 * <p>本体只做三件事：
 * <ol>
 *     <li>识别地毯（Carpet）/ GCA 生成的假人；</li>
 *     <li>监听假人“真正挖掉一个方块”的瞬间，按所选形状把周围同类方块一并挖掉；</li>
 *     <li>提供 /fakeultimine 指令来开关与调参。</li>
 * </ol>
 *
 * <p>本模组不依赖地毯 / GCA / FTB Ultimine 的编译期 API：
 * 假人通过类名特征识别，连锁规则参考 FTB Ultimine 的方块标签，
 * 因此三者任意一个缺失都不会导致加载失败。
 */
public class FakeUltimine implements ModInitializer {
	public static final String MOD_ID = "fakeultimine";
	public static final Logger LOGGER = LoggerFactory.getLogger("FakePlayerUltimine");

	/** 本模组自己的连锁黑名单方块标签：data/fakeultimine/tags/block/blacklist.json */
	public static final TagKey<Block> BLACKLIST =
			TagKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "blacklist"));

	/** FTB Ultimine 自带的不参与连锁的方块标签，装了 Ultimine 时自动同步生效。 */
	public static final TagKey<Block> ULTIMINE_EXCLUDED =
			TagKey.of(RegistryKeys.BLOCK, Identifier.of("ftbultimine", "excluded_blocks"));

	private static UltimineConfig config = new UltimineConfig();

	public static UltimineConfig config() {
		return config;
	}

	@Override
	public void onInitialize() {
		config = UltimineConfig.load();
		ChainBreaker.register();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				FakeUltimineCommand.register(dispatcher));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> config.save());

		String version = FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
		LOGGER.info("[FakePlayerUltimine] 假人连锁破坏 v{} 已加载（carpet: {}, gca: {}, ftbultimine: {}）",
				version, loaded("carpet"), loaded("gca"), loaded("ftbultimine"));
	}

	public static boolean loaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}
}
