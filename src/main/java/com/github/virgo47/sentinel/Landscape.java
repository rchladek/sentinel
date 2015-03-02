package com.github.virgo47.sentinel;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Logger;

public class Landscape {

	private static final Logger log = Logger.getLogger(Landscape.class.getName());

	private static final int SQUARE_UNPLAYABLE = Integer.MAX_VALUE;

	private static final int MIN_PATCH_SIZE = 5;

	private static final int WALK_NORTH = 1; // y++
	private static final int WALK_WEST = 2; // x--
	private static final int WALK_SOUTH = 4; // y--
	private static final int WALK_EAST = 8; // x++

	public final int sizeX;
	public final int sizeY;

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

	public Landscape(int sizeX, int sizeY) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		initializeFlatGameplan();
	}

	// - height is in "units of change", which has nothing to do with block height or final scale of the landscape
	// - ratio between block and height step should be around 2, but different values can provide funny results ;-)
	// - maxHeight is used both for + and - heights
	// - 0 height is "middle ground" with highest probability of occurrance
	public void generate(Config config) {
		Random random = new Random(0);

		int changes = random.nextInt(config.changesCount) + config.changesCount / 2;
		log.fine("Requested changes " + config.changesCount + ", planned changes " + changes);

		/*
		There are many options here:
		1. randomly fill patches and eventually the whole area (lower)
		2. go row by row with some probability of change in height (based on both previous column and row)
		...?
		 */
		while (changes > 0) {
			int x = random.nextInt(sizeX);
			int y = random.nextInt(sizeY);
			int height = random.nextInt(config.maxHeight) + 1;
			height = random.nextBoolean() ? height : -height;
			int patchSize = random.nextInt(config.maxPatchSize) / Math.abs(height) + minPatchSize(height);
			log.finer("Patch plan - height=" + height + ", patchSize=" + patchSize + ", x,y=" + x + ',' + y);

			Queue<Position> todoQueue = new ArrayDeque<>();
			todoQueue.add(new Position(x, y));

			// from now on we use pos, not x,y
			while (patchSize > 0) {
				Position pos = todoQueue.poll();
				log.finest("Found " + pos + ", left size: " + todoQueue.size());
				if (pos == null) {
					log.finer("Finishing patch prematurely with left patchSize " + patchSize);
					break;
				}

				if (setGameplanSquare(pos, height, config.maxHeightDifference)) {
					patchSize -= 1;
				}

				int whereNext = random.nextInt(16);
				if ((whereNext & WALK_NORTH) > 0 && pos.y < sizeY - 1) {
					todoQueue.add(pos.north());
				}
				if ((whereNext & WALK_WEST) > 0 && pos.x > 0) {
					todoQueue.add(pos.west());
				}
				if ((whereNext & WALK_SOUTH) > 0 && pos.y > 0) {
					todoQueue.add(pos.south());
				}
				if ((whereNext & WALK_EAST) > 0 && pos.x < sizeX - 1) {
					todoQueue.add(pos.east());
				}
			}

			changes -= 1;
		}
	}

	/**
	 * Higher level change of the gameplan, that also fixes maximal requested height difference, fixes
	 * slopes across more than a single square, joins squares with the same height across vertical
	 * or horizontal gap (not diagonal), and also chooses candidates for sentinel/sentry/player position.
	 */
	private boolean setGameplanSquare(Position pos, int height, int maxHeightDifference) {
		boolean changed = setGameplanSquare(pos.x, pos.y, height);
		if (changed) {
			// TODO stuff we promised in javadoc
			// TODO don't forget about landscape update with proper values included UNPLAYABLE
		}
		return changed;
	}

	/** Low level change of the gameplan and its geometry. */
	private boolean setGameplanSquare(int x, int y, int height) {
		if (gameplan[x][y] == height) return false;

		gameplan[x][y] = height;
		points[x][y] = height;
		points[x + 1][y] = height;
		points[x + 1][y + 1] = height;
		points[x][y + 1] = height;
		log.finer("Square (" + x + ',' + y + ") set to height: " + height);
		return true;
	}

	private void initializeFlatGameplan() {
		gameplan = new int[sizeX][sizeY];
		points = new int[sizeX + 1][sizeY + 1];
		for (int i = 0; i < sizeX; i += 1) {
			for (int j = 0; j < sizeY; j += 1) {
				setGameplanSquare(i, j, 0);
			}
		}
	}

	private int minPatchSize(int height) {
		return MIN_PATCH_SIZE;
	}

	public int pointHeight(int x, int y) {
		return points[x][y];
	}

	private static class Position {

		public int x, y;

		Position(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString() {
			return "(" + x + ',' + y + ')';
		}

		public Position north() {
			return new Position(x, y + 1);
		}

		public Position west() {
			return new Position(x - 1, y);
		}

		public Position south() {
			return new Position(x, y - 1);
		}

		public Position east() {
			return new Position(x + 1, y);
		}
	}

	/** Configuration for landscape generation. */
	public static class Config {

		public final int maxHeight;
		public final int maxHeightDifference;
		public final int maxPatchSize;
		public final int changesCount;

		public Config(int maxHeight, int maxHeightDifference, int maxPatchSize, int changesCount) {
			this.maxHeight = maxHeight;
			this.maxHeightDifference = maxHeightDifference;
			this.maxPatchSize = maxPatchSize;
			this.changesCount = changesCount;
		}
	}
}
