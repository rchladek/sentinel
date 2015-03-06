package com.github.virgo47.sentinel;

import static com.github.virgo47.sentinel.Landscape.SQUARE_UNPLAYABLE;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LandscapeTest {

	/** Tests that setting the square to the same value does not change anything. */
	@Test
	public void testNoChange() {
		Landscape landscape = new Landscape(3, 1, new Landscape.Config(1, 1, 1, 1));
		boolean changed = landscape.setSquare(0, 0, 0);

		Assert.assertFalse(changed);
		Assert.assertEquals(landscape.gameplan(0, 0), 0);
	}

	/**
	 * Tests that the square next to raised/lowered square gets unplayable ("rock") and the one further
	 * stays at the same height (because it is within maxHeightDifference limits).
	 * This is tested on the strip, only one direction.
	 */
	@Test
	public void testSingleChangeOnStrip() {
		Landscape landscape = new Landscape(3, 1, new Landscape.Config(1, 1, 1, 1));

		Assert.assertTrue(landscape.setSquare(2, 0, 1));

		Assert.assertEquals(landscape.gameplan(0, 0), 0);
		Assert.assertEquals(landscape.gameplan(2, 0), 1);
		Assert.assertEquals(landscape.gameplan(1, 0), SQUARE_UNPLAYABLE);
	}

	/** Tests that all surrounding squares (in all eight directions) get unplayable. */
	@Test
	public void testSingleChangeOn3Square() {
		Landscape landscape = new Landscape(3, 3, new Landscape.Config(1, 1, 1, 1));

		Assert.assertTrue(landscape.setSquare(1, 1, 1));

		Assert.assertEquals(landscape.gameplan(1, 1), 1);
		Assert.assertEquals(landscape.gameplan(0, 0), SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(0, 1), SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(0, 2), SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(1, 2), SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(2, 2), SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(2, 1), SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(2, 0), SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(1, 0), SQUARE_UNPLAYABLE);
	}

	/** Tests two subsequent changes on a strip, second one to the same height like adjacent square. */
	@Test
	public void testTwoChangeSameHeightNeigboursOnStrip() {
		Landscape landscape = new Landscape(3, 1, new Landscape.Config(1, 1, 1, 1));

		Assert.assertTrue(landscape.setSquare(2, 0, 1));
		Assert.assertTrue(landscape.setSquare(1, 0, 1));

		Assert.assertEquals(landscape.gameplan(0, 0), SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(1, 0), 1);
		Assert.assertEquals(landscape.gameplan(2, 0), 1);
	}

	/**
	 * Tests (on the strip) that maxHeightDifference is respected - that is next to the changed square
	 * there is rock, and next square has playable square of height +- maxHeightDifference (here 2).
	 * This also tests that the check cascade from the "fixed" field (position +2) further (+3 rock, +4 fixed)
	 * if necessary.
	 */
	@Test
	public void testMaxHeightDifferenceOnStrip() {
		Landscape landscape = new Landscape(7, 1, new Landscape.Config(5, 2, 1, 1));

		Assert.assertTrue(landscape.setSquare(6, 0, 5));

		Assert.assertEquals(landscape.gameplan(6, 0), 5);
		Assert.assertEquals(landscape.gameplan(5, 0), SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(4, 0), 3);
		Assert.assertEquals(landscape.gameplan(3, 0), SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(2, 0), 1);
		Assert.assertEquals(landscape.gameplan(1, 0), SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(0, 0), 0);
	}

	/**
	 * TODO: this one is questionable... in original Sentinel, +2 square can be unplayable, but all slopes are +1 max difference.
	 * Maybe we need another flag, that says, whether +2 square must be playable or can be sloped. Then maxHeightDifference would
	 * check geometry (points) in the first place.
	 */
	@Test
	public void testChangesWithTwoRocksBetween() {
		Landscape landscape = new Landscape(4, 1, new Landscape.Config(2, 1, 1, 1));

		Assert.assertTrue(landscape.setSquare(0, 0, 1)); // this does not change +2 square (is within maxHeightDiff)
		Assert.assertTrue(landscape.setSquare(3, 0, 3)); // this does not change -2 square, because it is unplayable

		Assert.assertEquals(landscape.gameplan(0, 0), 1);
		Assert.assertEquals(landscape.gameplan(1, 0), SQUARE_UNPLAYABLE); // not changed by 3,0,3
		Assert.assertEquals(landscape.gameplan(2, 0), SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(3, 0), 3);
	}
}
