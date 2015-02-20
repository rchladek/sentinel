package com.github.virgo47.sentinel;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;

public class Crosshair extends Group {

	private static final int CROSSHAIR_RADIUS = 10;
	private static final double CROSSHAIR_THICKNESS = 1.5;

	private final Robot robot;

	private int resetX;
	private int resetY;

	public Crosshair() {
		Line yLine = new Line(-CROSSHAIR_RADIUS, 0, CROSSHAIR_RADIUS, 0);
		yLine.setStrokeWidth(CROSSHAIR_THICKNESS);
		yLine.setStrokeLineCap(StrokeLineCap.ROUND);
		Line xLine = new Line(0, -CROSSHAIR_RADIUS, 0, CROSSHAIR_RADIUS);
		xLine.setStrokeWidth(CROSSHAIR_THICKNESS);
		xLine.setStrokeLineCap(StrokeLineCap.ROUND);
		getChildren().addAll(yLine, xLine);
		setMouseTransparent(true);

		try {
			robot = new Robot();
		} catch (AWTException e) {
			throw new RuntimeException(e);
		}
	}

	public void resetMouse() {
		robot.mouseMove(resetX, resetY);
	}

	public int getResetX() {
		return resetX;
	}

	public int getResetY() {
		return resetY;
	}

	public void click() {
		robot.mousePress(InputEvent.BUTTON1_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
	}

	public void refreshResetPosition() {
		Point2D screenPoint = localToScreen(0, 0);
		resetX = (int) screenPoint.getX();
		resetY = (int) screenPoint.getY();
		resetMouse();
		System.out.println("REFRESH: " + resetX + ", " + resetY);
	}
}
