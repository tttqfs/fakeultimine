package dev.fakeultimine;

import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 假人识别。
 *
 * <p>地毯 1.21 的假人类是 {@code carpet.patches.EntityPlayerMPFake}，GCA 生成的机器人同样是这个类
 * （GCA 只是给它加了额外的功能混合），其它模组的假人（如某些机器人模组）通常也会在类名里带
 * FakePlayer。这里沿继承链按类名特征判断，避免编译期依赖地毯 / GCA，也不会因为它们换版本而崩。
 */
public final class CarpetCompat {
	/** 已知的假人类名精确匹配。 */
	private static final String CARPET_FAKE = "carpet.patches.EntityPlayerMPFake";

	private static final Map<Class<?>, Optional<String>> CACHE = new HashMap<>();

	private CarpetCompat() {
	}

	/** 返回假人来源描述（carpet / gca / fake），不是假人则返回 empty。 */
	public static Optional<String> fakeKind(PlayerEntity player) {
		if (player == null) {
			return Optional.empty();
		}
		return CACHE.computeIfAbsent(player.getClass(), CarpetCompat::detect);
	}

	public static boolean isFakePlayer(PlayerEntity player) {
		return fakeKind(player).isPresent();
	}

	private static Optional<String> detect(Class<?> type) {
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			String name = current.getName();
			if (CARPET_FAKE.equals(name)) {
				// GCA 的机器人就是地毯假人，两者共用同一个类
				return Optional.of(FakeUltimine.loaded("gca") ? "Carpet/GCA" : "Carpet");
			}
			if (name.startsWith("dev.dubhe.gugle.carpet.")) {
				return Optional.of("GCA");
			}
			String simple = current.getSimpleName();
			if (simple.contains("FakePlayer") || simple.contains("FakeServerPlayer") || simple.contains("PlayerFake")) {
				return Optional.of("FakePlayer");
			}
		}
		return Optional.empty();
	}
}
