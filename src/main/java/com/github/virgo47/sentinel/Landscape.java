package com.github.virgo47.sentinel;

import java.util.Random;

public class Landscape {

	private static final int SQUARE_UNINITIALIZED = 0;
	private static final int SQUARE_UNPLAYABLE = -1;

	private final int sizeX;
	private final int sizeY;

	// describes heights for flat fields, 0 for sloped/in-between stuff
	private int[][] landscape;

	public Landscape(int sizeX, int sizeY) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
	}

	public void generate(int maxHeight, int maxPatchSize) {
		int uninitialized = sizeX * sizeY;
		landscape = new int[sizeX][sizeY];
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
			int height = random.nextInt(maxHeight) + 1;
			int patchSize = random.nextInt(maxPatchSize);

			// find first uninitalized
			while (landscape[x][y] != SQUARE_UNINITIALIZED) {
				y += 1;
				if (y >= sizeY) {
					y = 0;
					x += 1;
				}
				if (x >= sizeX) {
					x = 0;
				}
			}

			while (patchSize > 0 && uninitialized > 0) {
				landscape[x][y] = height;
				// walk randomly
			}

			// frame it with UNPLAYABLEs
		}
	}
}
