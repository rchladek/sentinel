package com.github.virgo47.sentinel;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

public class Landscape {

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
	private int[][] points;

	// current number of uninitialized squares
	private int uninitialized;
	// current height to be set
	private int height;

	public static void main(String[] args) {
		Landscape landscape = new Landscape(50, 30);
		landscape.generate(1, 100);
		System.out.println("LANDSCAPE:\n" + landscape);
	}

	public Landscape(int sizeX, int sizeY) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		initializeFlatGameplan();
	}

	// - height is in "units of change", which has nothing to do with block height or final scale of the landscape
	// - ratio between block and height step should be around 2, but different values can provide funny results ;-)
	// - maxHeight is used both for + and - heights
	// - 0 height is "middle ground" with highest probability of occurrance
	public void generate(int maxHeight, int maxPatchSize) {
		// middle ground will be offset for height 0 (internally 0 is minimum height)
		int middleGround = maxHeight;
		// we will use maxHeight as maximum random int (never rached)
		maxHeight = maxHeight * 2 + 1;

		Random random = new Random(0);

		/*
		There are many options here:
		1. randomly fill patches and eventually the whole area (lower)
		2. go row by row with some probability of change in height (based on both previous column and row)
		...?
		 */
		while (uninitialized > 0) {
			int x = random.nextInt(sizeX);
			int y = random.nextInt(sizeY);
			height = random.nextInt(maxHeight) - middleGround;
			// +1 to avoid division by zero
			int patchSize = random.nextInt(maxPatchSize) / (Math.abs(height) + 1) + minPatchSize(height);
			System.out.println("new patch plan - height=" + height + ", patchSize=" + patchSize + ", x,y=" + x + ',' + y);

			Queue<Position> todoQueue = new ArrayDeque<>();
			addWork(todoQueue, new Position(x, y));

			// from now on we use pos, not x,y
			while (patchSize > 0 && uninitialized > 0) {
				Position pos = todoQueue.poll();
				System.out.println("Found " + pos + ", todo size: " + todoQueue.size());
				if (pos == null) break;

				int whereNext = random.nextInt(16);
				if ((whereNext & WALK_NORTH) > 0 && pos.y < sizeY - 1) {
					addWork(todoQueue, pos.north());
				}
				if ((whereNext & WALK_WEST) > 0 && pos.x > 0) {
					addWork(todoQueue, pos.west());
				}
				if ((whereNext & WALK_SOUTH) > 0 && pos.y > 0) {
					addWork(todoQueue, pos.south());
				}
				if ((whereNext & WALK_EAST) > 0 && pos.x < sizeX - 1) {
					addWork(todoQueue, pos.east());
				}
			}

			// frame it with UNPLAYABLEs
		}
	}

	private void addWork(Queue<Position> todoQueue, Position pos) {
		todoQueue.add(pos);
		setGameplanSquare(pos, height);
	}

	private void setGameplanSquare(Position pos, int height) {
		setGameplanSquare(pos.x, pos.y, height);
	}

	private void setGameplanSquare(int x, int y, int height) {
		gameplan[x][y] = height;
		points[x][y] = height;
		points[x + 1][y] = height;
		points[x + 1][y + 1] = height;
		points[x][y + 1] = height;
		System.out.println("Initialized (" + x + ',' + y + ") to height: " + height);
	}

	private void initializeFlatGameplan() {
		gameplan = new int[sizeX][sizeY];
		points = new int[sizeX + 1][sizeY + 1];
		for (int i = 0; i < sizeX; i += 1) {
			for (int j = 0; j < sizeY; j += 1) {
				setGameplanSquare(i, j, 0);
			}
		}
		uninitialized = sizeX * sizeY;
	}

	private int minPatchSize(int height) {
		return MIN_PATCH_SIZE;
	}

	public int pointHeight(int x, int y) {
		return points[x][y];
	}

	private class Position {

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
}
