package com.github.virgo47.sentinel;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Logger;

public class Landscape {

	static final int SQUARE_UNPLAYABLE = Integer.MAX_VALUE;

	private static final Logger log = Logger.getLogger(Landscape.class.getName());

	private static final int MIN_PATCH_SIZE = 5;

	private static final int WALK_NORTH = 1; // y++
	private static final int WALK_WEST = 2; // x--
	private static final int WALK_SOUTH = 4; // y--
	private static final int WALK_EAST = 8; // x++

	public final int sizeX;
	public final int sizeY;
	public final Config config;

	/** Describes heights for flat squares, {@link #SQUARE_UNPLAYABLE} for slopes. */
	private int[][] gameplan;

	/** Describes heights at points (square corners) - size must be +1 in both directions compared to {@link #gameplan}. */
	private int[][] points;
	/** Sentinel's position - height is determined from gameplan. It should be one of the highest squares, not to mention his base-block. */
	private Position sentinel;
	/** Sentries' positions - generally some above average squares. Sentines stand on the ground directly. */
	private List<Position> sentries;

	/** Starting position for player. */
	private Position playerStart;

	private Random random;

	public Landscape(int sizeX, int sizeY, Config config) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.config = config;

		initializeFlatGameplan();
		random = new Random(0);
	}

	public void generate(int seed) {
		random = new Random(seed);

		int changes = random.nextInt(config.changesCount) + config.changesCount / 2;
		log.fine("Requested changes " + config.changesCount + ", planned changes " + changes);

		while (changes > 0) {
			if (createNextChange(changes)) {
				changes -= 1;
			}
		}
	}

	private boolean createNextChange(int changesLeft) {
		int x = random.nextInt(sizeX);
		int y = random.nextInt(sizeY);
		int height = random.nextInt(config.maxHeight) + 1;
		height = random.nextBoolean() ? height : -height;
		int patchSize = random.nextInt(config.maxPatchSize) / Math.abs(height) + minPatchSize(height) + changesLeft;
		return performChange(new Position(x, y), height, patchSize);
	}

	// callable from the package for testing purposes
	boolean performChange(Position initPosition, int height, int patchSize) {
		log.finer("Patch plan - height=" + height + ", patchSize=" + patchSize + ", x,y=" + initPosition);

		if (gameplan(initPosition) == height) {
			return false; // let's try some other random change
		}

		Queue<Position> todoQueue = new ArrayDeque<>();
		todoQueue.add(initPosition);

		// from now on we use pos, not x,y
		while (patchSize > 0) {
			Position pos = todoQueue.poll();
			log.finest("Found " + pos + ", left size: " + todoQueue.size());
			if (pos == null) {
				log.finer("Finishing patch prematurely with left patchSize " + patchSize);
				break;
			}

			if (setSquare(pos, height)) {
				patchSize -= 1;
			}

			int whereNext = random.nextInt(16);
			if ((whereNext & WALK_NORTH) > 0) {
				addToQueue(todoQueue, pos.north(), height);
			}
			if ((whereNext & WALK_WEST) > 0) {
				addToQueue(todoQueue, pos.west(), height);
			}
			if ((whereNext & WALK_SOUTH) > 0) {
				addToQueue(todoQueue, pos.south(), height);
			}
			if ((whereNext & WALK_EAST) > 0) {
				addToQueue(todoQueue, pos.east(), height);
			}
		}
		return true;
	}

	private void addToQueue(Queue<Position> todoQueue, Position pos, int height) {
		if (isValidPosition(pos) && gameplan(pos) != height) {
			todoQueue.add(pos);
		}
	}

	// not private for testing purposes
	boolean setSquare(int x, int y, int height) {
		return setSquare(new Position(x, y), height);
	}

	/**
	 * Higher level change of the landscape, that also fixes maximal requested height difference, fixes
	 * slopes across more than a single square, joins squares with the same height across vertical
	 * or horizontal gap (not diagonal), and also chooses candidates for sentinel/sentry/player position.
	 */
	private boolean setSquare(Position pos, int height) {
		boolean changed = setGameplan(pos, height);
		if (changed) {
			checkSurrounding(pos.east(), pos.east(2), height);
			checkSurrounding(pos.north(), pos.north(2), height);
			checkSurrounding(pos.west(), pos.west(2), height);
			checkSurrounding(pos.south(), pos.south(2), height);
		}
		return changed;
	}

	/** Low level change of the gameplan and its geometry. */
	private boolean setGameplan(Position pos, int height) {
		return setGameplan(pos.x, pos.y, height);
	}

	/** Low level change of the gameplan and its geometry. */
	private boolean setGameplan(int x, int y, int height) {
		if (gameplan[x][y] == height) return false;

		gameplan[x][y] = height;
		log.finer("Square (" + x + ',' + y + ") set to height: " + (height == SQUARE_UNPLAYABLE ? "unplayable" : height));
		// we don't update geometry for unplayable - it is updated properly by surrounded playable squares
		if (height == SQUARE_UNPLAYABLE) {
			// but we check that it is not flat, which indicates problem
			if (points[x][y] == points[x + 1][y] && points[x][y] == points[x + 1][y + 1] && points[x][y] == points[x][y + 1]) {
				throw new IllegalStateException("Unplayable square " + x + ',' + y + " has flat geometry with height " + points[x][y]);
			}
			return true;
		}

		points[x][y] = height;
		points[x + 1][y] = height;
		points[x + 1][y + 1] = height;
		points[x][y + 1] = height;

		setSurroundingUnplayable(x, y, height);
		return true;
	}

	private void setSurroundingUnplayable(int x, int y, int height) {
		setGameplanUnplayable(x + 1, y, height);
		setGameplanUnplayable(x + 1, y + 1, height);
		setGameplanUnplayable(x, y + 1, height);
		setGameplanUnplayable(x - 1, y + 1, height);
		setGameplanUnplayable(x - 1, y, height);
		setGameplanUnplayable(x - 1, y - 1, height);
		setGameplanUnplayable(x, y - 1, height);
		setGameplanUnplayable(x + 1, y - 1, height);
	}

	/** Sets the gameplan square to uplayable if the position is valid (not out of bounds) and it does not already have the specified height. */
	private void setGameplanUnplayable(int x, int y, int height) {
		if (isValidPosition(x, y) && gameplan(x, y) != height) {
			setGameplan(x, y, SQUARE_UNPLAYABLE);
		}
	}

	private void checkSurrounding(Position next, Position nextNext, int height) {
		if (!isValidPosition(next) || !isValidPosition(nextNext)) return;
		// no need to fix the direction that is already of requested height
		if (gameplan(next) == height) return;

		int nextNextHeight = gameplan(nextNext);
		if (nextNextHeight != SQUARE_UNPLAYABLE) {
			// fixes maximum height difference
			if ((nextNextHeight - height) > config.maxHeightDifference) {
				setSquare(nextNext, height + config.maxHeightDifference);
			} else if ((height - nextNextHeight) > config.maxHeightDifference) {
				setSquare(nextNext, height - config.maxHeightDifference);
			}
		}
		// joins patches of the same height
		if (nextNextHeight == height) {
			setSquare(next, height);
		}
	}

	private int gameplan(Position pos) {
		return gameplan(pos.x, pos.y);
	}

	// not private for testing purposes
	int gameplan(int x, int y) {
		return gameplan[x][y];
	}

	private boolean isValidPosition(Position pos) {
		return isValidPosition(pos.x, pos.y);
	}

	private boolean isValidPosition(int x, int y) {
		return x >= 0 && x < sizeX
			&& y >= 0 && y < sizeY;
	}

	private void initializeFlatGameplan() {
		gameplan = new int[sizeX][sizeY];
		points = new int[sizeX + 1][sizeY + 1];
		for (int i = 0; i < sizeX; i += 1) {
			for (int j = 0; j < sizeY; j += 1) {
				setGameplan(i, j, 0);
			}
		}
	}

	private int minPatchSize(int height) {
		return MIN_PATCH_SIZE;
	}

	public int pointHeight(int x, int y) {
		return points[x][y];
	}

	public static final class Position {

		public final int x;
		public final int y;

		Position(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString() {
			return "(" + x + ',' + y + ')';
		}

		public Position north() {
			return north(1);
		}

		public Position west() {
			return west(1);
		}

		public Position south() {
			return south(1);
		}

		public Position east() {
			return east(1);
		}

		public Position north(int distance) {
			return new Position(x, y + distance);
		}

		public Position west(int distance) {
			return new Position(x - distance, y);
		}

		public Position south(int distance) {
			return new Position(x, y - distance);
		}

		public Position east(int distance) {
			return new Position(x + distance, y);
		}
	}

	/**
	 * Configuration for landscape generation. Height is in "units of change", which has nothing to do
	 * with the block height or final scale of the landscape. Ratio between block and height step should
	 * be around 2, but different values can provide funny results.
	 */
	public static class Config {

		/** Height extremes, both to + and - difference. That is for value 2 landscape can go from -2 to +2. */
		public final int maxHeight;
		public final int maxHeightDifference;
		public final int maxPatchSize;
		public final int changesCount;

		public Config(int maxHeight, int maxHeightDifference, int maxPatchSize, int changesCount) {
			if (maxHeight < 0) throw new IllegalArgumentException("maxHeight must be higher than 0, is " + maxHeight);
			if (maxHeightDifference < 0) throw new IllegalArgumentException("maxHeightDifference must be higher than 0, is " + maxHeightDifference);
			if (maxPatchSize < 0) throw new IllegalArgumentException("maxPatchSize must be higher than 0, is " + maxPatchSize);
			if (changesCount < 0) throw new IllegalArgumentException("changesCount must be higher than 0, is " + changesCount);

			this.maxHeight = maxHeight;
			this.maxHeightDifference = maxHeightDifference;
			this.maxPatchSize = maxPatchSize;
			this.changesCount = changesCount;
		}
	}
}
