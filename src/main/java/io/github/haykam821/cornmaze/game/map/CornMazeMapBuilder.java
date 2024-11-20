package io.github.haykam821.cornmaze.game.map;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.github.haykam821.cornmaze.game.CornMazeConfig;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;

public class CornMazeMapBuilder {
	private static final BlockState BARRIER_STATE = Blocks.BARRIER.getDefaultState();
	private static final BlockState SIDEWAYS_BARRIER_STATE = Blocks.BLACK_STAINED_GLASS.getDefaultState();

	public static final BlockState LADDER_STATE = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH);
	private static final BlockState END_LADDER_STATE = Blocks.VINE.getDefaultState().with(VineBlock.NORTH, true);

	public static final BlockState AIR_STATE = Blocks.AIR.getDefaultState();

	private static final float VERTICAL_START_YAW = 180;
	private static final float VERTICAL_START_PITCH = 20;

	private final CornMazeConfig config;

	public CornMazeMapBuilder(CornMazeConfig config) {
		this.config = config;
	}

	public CornMazeMap create() {
		MapTemplate template = MapTemplate.createEmpty();
		CornMazeMapConfig mapConfig = this.config.getMapConfig();

		BlockBounds bounds = this.getMazeBounds(mapConfig);

		// Make maze 2D array with default walls
		MazeState[][] maze = new MazeState[mapConfig.getZ()][mapConfig.getX()];
		for (int z = 0; z < maze.length; z++) {
			for (int x = 0; x < maze[z].length; x++) {
				this.setMazeState(x, z, MazeState.WALL, maze);
			}
		}

		Random random = Random.createLocal();
		int startX = (random.nextInt((mapConfig.getX() - 1) / 2) + 1) * 2 - 1;
		int startZ = (random.nextInt((mapConfig.getZ() - 1) / 2) + 1) * 2 - 1;

		Object2IntOpenHashMap<MazeCoordinate> targets = new Object2IntOpenHashMap<MazeCoordinate>();
		this.formMaze(startX, startZ, maze, targets, 0);

		MazeCoordinate endCoordinate = this.getFurthest(targets);
		this.setMazeState(endCoordinate.getX(), endCoordinate.getZ(), MazeState.END, maze);

		this.build(bounds, template, mapConfig, maze, random);

		Direction startDirection = this.getStartDirection(startX, startZ, maze);
		Direction startFacingDirection = startDirection; 

		BlockBounds startBounds = this.getBounds(startX, startZ, mapConfig);
		Vec3d startPos = startBounds.centerBottom();

		if (mapConfig.isSideways()) {
			startFacingDirection = startFacingDirection.rotateClockwise(Direction.Axis.X);
		} else {
			startPos = startPos.offset(Direction.UP, 1);
		}

		float startYaw = getStartYaw(startFacingDirection);
		float startPitch = getStartPitch(startFacingDirection);

		BlockBounds barrierBounds = this.getBounds(startX + startDirection.getOffsetX(), startZ + startDirection.getOffsetZ(), mapConfig, true);

		for (BlockPos pos : barrierBounds) {
			template.setBlockState(pos, mapConfig.isSideways() ? SIDEWAYS_BARRIER_STATE : BARRIER_STATE);
		}

		return new CornMazeMap(template, bounds, startBounds, this.getBounds(endCoordinate.getX(), endCoordinate.getZ(), mapConfig), barrierBounds, startPos, startYaw, startPitch);
	}

	private MazeCoordinate getFurthest(Object2IntOpenHashMap<MazeCoordinate> targets) {
		int furthest = Collections.max(targets.values());
		for (Object2IntMap.Entry<MazeCoordinate> entry : targets.object2IntEntrySet()) {
			if (furthest == entry.getIntValue()) {
				return entry.getKey();
			}
		}
		return null;
	}

	private void setMazeState(int x, int z, MazeState state, MazeState[][] maze) {
		maze[z][x] = state;
	}

	private MazeState getMazeState(int x, int z, MazeState[][] maze) {
		return maze[z][x];
	}

	private BlockBounds getMazeBounds(CornMazeMapConfig mapConfig) {
		int x = mapConfig.getX() * mapConfig.getXScale() - 1;
		int y = mapConfig.getHeight();
		int z = mapConfig.getZ() * mapConfig.getZScale() - 1;

		if (mapConfig.isSideways()) {
			return BlockBounds.of(0, 0, 0, x, z, y);
		}

		return BlockBounds.of(0, 0, 0, x, y, z);
	}

	private BlockBounds getBounds(int x, int z, CornMazeMapConfig mapConfig, boolean inner) {
		int shrinkY = inner ? 1 : 0;

		int startX = x * mapConfig.getXScale();
		int startY = shrinkY;
		int startZ = z * mapConfig.getZScale();

		int endX = startX + (mapConfig.getXScale() - 1);
		int endY = startY + (mapConfig.getHeight() - shrinkY * 2);
		int endZ = startZ + (mapConfig.getZScale() - 1);

		if (mapConfig.isSideways()) {
			return BlockBounds.of(startX, startZ, startY, endX, endZ, endY);
		}

		return BlockBounds.of(startX, startY, startZ, endX, endY, endZ);
	}

	private BlockBounds getBounds(int x, int z, CornMazeMapConfig mapConfig) {
		return this.getBounds(x, z, mapConfig, false);
	}

	private boolean isWall(int x, int z, MazeState[][] maze) {
		if (x <= 0) return false;
		if (x >= maze[0].length) return false;

		if (z <= 0) return false;
		if (z >= maze.length) return false;

		return this.getMazeState(x, z, maze) == MazeState.WALL;
	}

	private void formMaze(int x, int z, MazeState[][] maze, Object2IntOpenHashMap<MazeCoordinate> targets, int distance) {
		this.setMazeState(x, z, targets.size() == 0 ? MazeState.START : MazeState.PATH, maze);
		targets.put(new MazeCoordinate(x, z), distance);

		List<Direction> shuffledDirections = Direction.Type.HORIZONTAL.stream().collect(Collectors.toList());
		Collections.shuffle(shuffledDirections);

		for (Direction direction : shuffledDirections) {
			int targetX = x + direction.getOffsetX() * 2;
			int targetZ = z + direction.getOffsetZ() * 2;

			if (this.isWall(targetX, targetZ, maze)) {
				int linkX = x + direction.getOffsetX();
				int linkZ = z + direction.getOffsetZ();
	
				this.setMazeState(linkX, linkZ, MazeState.PATH, maze);
				this.formMaze(targetX, targetZ, maze, targets, distance + 2);
			}
		}
	}

	private boolean isDecayed(MazeState state, CornMazeMapConfig mapConfig, Random random) {
		if (!state.isDecayable()) return false;

		if (mapConfig.getDecay() <= 0) return false;
		if (mapConfig.getDecay() >= 1) return true;

		return random.nextDouble() < mapConfig.getDecay();
	}

	private void build(BlockBounds bounds, MapTemplate template, CornMazeMapConfig mapConfig, MazeState[][] maze, Random random) {
		for (BlockPos pos : bounds) {
			int x = pos.getX();
			int y = mapConfig.isSideways() ? pos.getZ() : pos.getY();
			int z = mapConfig.isSideways() ? pos.getY() : pos.getZ();

			MazeState state = this.getMazeState(x / mapConfig.getXScale(), z / mapConfig.getZScale(), maze);
			
			if ((state.isTall() || y == 0) && !this.isDecayed(state, mapConfig, random)) {
				template.setBlockState(pos, state.getState());
			} else if (!state.isTall() && y == 1 && mapConfig.isSideways()) {
				template.setBlockState(pos, state == MazeState.END ? END_LADDER_STATE : LADDER_STATE);
			} else if (y == mapConfig.getHeight()) {
				template.setBlockState(pos, BARRIER_STATE);
			}
		}
	}

	private Direction getStartDirection(int startX, int startZ, MazeState[][] maze) {
		for (Direction direction : Direction.Type.HORIZONTAL) {
			int x = startX + direction.getOffsetX();
			if (x <= 0 || x >= maze[0].length) continue;

			int z = startZ + direction.getOffsetZ();
			if (z <= 0 || z >= maze.length) continue;

			if (!this.getMazeState(x, z, maze).isTall()) {
				return direction;
			}
		}

		return null;
	}

	private static float getStartYaw(Direction direction) {
		return direction.getAxis().isVertical() ? VERTICAL_START_YAW : direction.asRotation();
	}

	private static float getStartPitch(Direction direction) {
		return direction.getOffsetY() * -VERTICAL_START_PITCH;
	}
}