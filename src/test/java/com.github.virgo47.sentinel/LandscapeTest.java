package com.github.virgo47.sentinel;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LandscapeTest {

	@Test
	public void testNoChange() {
		Landscape landscape = new Landscape(3, 1, new Landscape.Config(1, 1, 1, 1));
		boolean changed = landscape.setSquare(0, 0, 0);

		Assert.assertFalse(changed);
		Assert.assertEquals(landscape.gameplan(0, 0), 0);
	}

	@Test
	public void testSingleChangeOnStrip() {
		Landscape landscape = new Landscape(3, 1, new Landscape.Config(1, 1, 1, 1));

		Assert.assertTrue(landscape.setSquare(2, 0, 1));

		Assert.assertEquals(landscape.gameplan(0, 0), 0);
		Assert.assertEquals(landscape.gameplan(2, 0), 1);
		Assert.assertEquals(landscape.gameplan(1, 0), Landscape.SQUARE_UNPLAYABLE);
	}

	@Test
	public void testSingleChangeOn3Square() {
		Landscape landscape = new Landscape(3, 3, new Landscape.Config(1, 1, 1, 1));

		Assert.assertTrue(landscape.setSquare(1, 1, 1));

		Assert.assertEquals(landscape.gameplan(1, 1), 1);
		Assert.assertEquals(landscape.gameplan(0, 0), Landscape.SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(0, 1), Landscape.SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(0, 2), Landscape.SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(1, 2), Landscape.SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(2, 2), Landscape.SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(2, 1), Landscape.SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(2, 0), Landscape.SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(1, 0), Landscape.SQUARE_UNPLAYABLE);
	}

	@Test
	public void testTwoChangeSameHeightNeigboursOnStrip() {
		Landscape landscape = new Landscape(3, 1, new Landscape.Config(1, 1, 1, 1));

		Assert.assertTrue(landscape.setSquare(2, 0, 1));
		Assert.assertTrue(landscape.setSquare(1, 0, 1));

		Assert.assertEquals(landscape.gameplan(0, 0), Landscape.SQUARE_UNPLAYABLE);
		Assert.assertEquals(landscape.gameplan(1, 0), 1);
		Assert.assertEquals(landscape.gameplan(2, 0), 1);
	}

}
