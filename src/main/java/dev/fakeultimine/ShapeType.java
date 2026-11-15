package dev.fakeultimine;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 连锁破坏的形状。每个形状只负责“生成候选坐标”，是否真的能挖由 {@link ChainBreaker} 的过滤器决定。
 */
public enum ShapeType {
	/** 3×3×3 立方体（默认，挖矿最常用）。 */
	CUBE3("cube3", "3×3×3 立方体"),
	/** 5×5×5 立方体（很暴力，注意上限）。 */
	CUBE5("cube5", "5×5×5 立方体"),
	/** 与挖掘方向垂直的 3×3 平面（像 Ultimine 的小方块）。 */
	PLANE3("plane3", "3×3 平面"),
	/** 与挖掘方向垂直的 5×5 平面。 */
	PLANE5("plane5", "5×5 平面"),
	/** 3×3 截面的隧道，沿挖掘方向延伸。 */
	TUNNEL("tunnel", "3×3 隧道"),
	/** 1×1 的一条直线。 */
	LINE("line", "1×1 直线"),
	/** 同类方块连通矿脉（挖钻石/木头很好用）。 */
	VEIN("vein", "同类连通矿脉");

	private final String id;
	private final String displayName;

	ShapeType(String id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public static ShapeType byId(String id) {
		if (id == null) {
			return null;
		}
		String trimmed = id.trim();
		for (ShapeType type : values()) {
			if (type.id.equalsIgnoreCase(trimmed)) {
				return type;
			}
		}
		// 常见别名
		return switch (trimmed.toLowerCase()) {
			case "3x3x3", "cube", "3x3" -> CUBE3;
			case "5x5x5", "cube5x5x5" -> CUBE5;
			case "plane", "square", "3x3plane" -> PLANE3;
			case "vein", "ore", "log", "shapeless" -> VEIN;
			case "straight", "1x1" -> LINE;
			default -> null;
		};
	}

	public static List<String> ids() {
		List<String> list = new ArrayList<>();
		for (ShapeType type : values()) {
			list.add(type.id);
		}
		return list;
	}

	/**
	 * 生成候选坐标（不含原方块自身，按距离由近到远排序，已被 max 截断）。
	 *
	 * @param origin   刚刚被挖掉的方块坐标
	 * @param dir      从玩家眼睛指向该方块的方向，用作隧道/直线/平面的朝向
	 * @param max      最多返回多少个坐标
	 * @param canBreak 单个坐标是否可被连锁破坏
	 */
	public List<BlockPos> collect(BlockPos origin, Direction dir, int max, Predicate<BlockPos> canBreak) {
		int limit = Math.max(1, max);
		return switch (this) {
			case CUBE3 -> cube(origin, 1, limit, canBreak);
			case CUBE5 -> cube(origin, 2, limit, canBreak);
			case PLANE3 -> plane(origin, dir, 1, limit, canBreak);
			case PLANE5 -> plane(origin, dir, 2, limit, canBreak);
			case TUNNEL -> tunnel(origin, dir, limit, canBreak);
			case LINE -> line(origin, dir, limit, canBreak);
			case VEIN -> vein(origin, limit, canBreak);
		};
	}

	private static List<BlockPos> cube(BlockPos origin, int radius, int limit, Predicate<BlockPos> canBreak) {
		List<BlockPos> candidates = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) {
						continue;
					}
					candidates.add(origin.add(dx, dy, dz));
				}
			}
		}
		return trim(origin, candidates, limit, canBreak);
	}

	private static List<BlockPos> plane(BlockPos origin, Direction dir, int radius, int limit, Predicate<BlockPos> canBreak) {
		Direction u = horizontalAxis(dir);
		Direction v = verticalAxis(dir);
		List<BlockPos> candidates = new ArrayList<>();
		for (int a = -radius; a <= radius; a++) {
			for (int b = -radius; b <= radius; b++) {
				if (a == 0 && b == 0) {
					continue;
				}
				candidates.add(origin.offset(u, a).offset(v, b));
			}
		}
		return trim(origin, candidates, limit, canBreak);
	}

	private static List<BlockPos> tunnel(BlockPos origin, Direction dir, int limit, Predicate<BlockPos> canBreak) {
		Direction u = horizontalAxis(dir);
		Direction v = verticalAxis(dir);
		int length = Math.max(1, (limit + 8) / 9);
		List<BlockPos> out = new ArrayList<>();
		for (int d = 0; d <= length; d++) {
			for (int a = -1; a <= 1; a++) {
				for (int b = -1; b <= 1; b++) {
					if (d == 0 && a == 0 && b == 0) {
						continue;
					}
					BlockPos pos = origin.offset(dir, d).offset(u, a).offset(v, b);
					if (canBreak.test(pos)) {
						out.add(pos);
						if (out.size() >= limit) {
							return out;
						}
					}
				}
			}
		}
		return out;
	}

	private static List<BlockPos> line(BlockPos origin, Direction dir, int limit, Predicate<BlockPos> canBreak) {
		List<BlockPos> out = new ArrayList<>();
		for (int d = 1; d <= limit; d++) {
			BlockPos pos = origin.offset(dir, d);
			if (canBreak.test(pos)) {
				out.add(pos);
			}
		}
		return out;
	}

	private static List<BlockPos> vein(BlockPos origin, int limit, Predicate<BlockPos> canBreak) {
		List<BlockPos> out = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		seen.add(origin);
		queue.add(origin);
		int guard = 0;
		int maxVisits = Math.max(64, limit * 12);
		while (!queue.isEmpty() && out.size() < limit && guard++ < maxVisits) {
			BlockPos current = queue.poll();
			for (Direction face : Direction.values()) {
				BlockPos next = current.offset(face);
				if (!seen.add(next)) {
					continue;
				}
				guard++;
				if (canBreak.test(next)) {
					out.add(next);
					if (out.size() >= limit) {
						return out;
					}
					queue.add(next);
				}
				if (seen.size() > maxVisits * 4) {
					return out;
				}
			}
		}
		return out;
	}

	private static List<BlockPos> trim(BlockPos origin, List<BlockPos> candidates, int limit, Predicate<BlockPos> canBreak) {
		candidates.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(origin)));
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos pos : candidates) {
			if (canBreak.test(pos)) {
				out.add(pos);
				if (out.size() >= limit) {
					break;
				}
			}
		}
		return out;
	}

	/** 与方向垂直的“水平”轴。 */
	private static Direction horizontalAxis(Direction dir) {
		return switch (dir.getAxis()) {
			case Y -> Direction.EAST;
			case X -> Direction.NORTH;
			case Z -> Direction.EAST;
		};
	}

	/** 与方向垂直的“竖直”轴。 */
	private static Direction verticalAxis(Direction dir) {
		return switch (dir.getAxis()) {
			case Y -> Direction.SOUTH;
			case X, Z -> Direction.UP;
		};
	}
}
