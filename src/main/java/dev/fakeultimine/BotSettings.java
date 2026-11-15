package dev.fakeultimine;

/**
 * 单个假人的连锁设置。值为 {@code null} 表示沿用全局配置。
 */
public final class BotSettings {
	/** 是否已开启连锁破坏。 */
	public boolean enabled = false;

	/** 形状 id，空字符串 = 用全局默认。 */
	public String shape = "";

	/** 单次连锁上限，0 = 用全局默认。 */
	public int maxBlocks = 0;

	public Boolean dropItems;
	public Boolean damageTool;
	public Boolean requireCorrectTool;

	public ShapeType shapeType() {
		ShapeType type = ShapeType.byId(shape);
		return type != null ? type : ShapeType.byId(FakeUltimine.config().defaultShape);
	}

	public int maxOrDefault() {
		UltimineConfig cfg = FakeUltimine.config();
		int value = maxBlocks > 0 ? maxBlocks : cfg.maxBlocks;
		return Math.max(1, Math.min(value, 4096));
	}

	public boolean drops() {
		return dropItems != null ? dropItems : FakeUltimine.config().dropItems;
	}

	public boolean damagesTool() {
		return damageTool != null ? damageTool : FakeUltimine.config().damageTool;
	}

	public boolean needsCorrectTool() {
		return requireCorrectTool != null ? requireCorrectTool : FakeUltimine.config().requireCorrectTool;
	}

	public void sanitize() {
		if (shape == null) {
			shape = "";
		}
		if (!shape.isEmpty() && ShapeType.byId(shape) == null) {
			shape = "";
		}
		if (maxBlocks < 0) {
			maxBlocks = 0;
		}
		if (maxBlocks > 4096) {
			maxBlocks = 4096;
		}
	}
}
