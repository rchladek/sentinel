package com.github.virgo47.sentinel;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class CameraXform extends Group {

	public static final double PITCH_MAX = 85;
	public static final double PITCH_MIN = -85;
	public static final double ROLL_MAX = 85;
	public static final double ROLL_MIN = -85;
	public static final double WRAP_YAW_AT = 180;
	public static final double WRAP_YAW_COMPLEMENT = 360 - WRAP_YAW_AT;

	private static final Rotate PITCH_OFFSET = new Rotate(-90, Rotate.X_AXIS);

	public Rotate yaw = new Rotate(0, new Point3D(0, 0, -1)); // Z axis, but clockwise, so that it goes like compass
	public Rotate pitch = new Rotate(0, Rotate.X_AXIS);
	public Rotate roll = new Rotate(0, Rotate.Y_AXIS);
	public Translate pos = new Translate();

	public double sensitivityYaw = 0.1;
	public double sensitivityPitch = 0.1;
	public boolean invertMouse = false;

	public CameraXform() throws NonInvertibleTransformException {
		getTransforms().addAll(
			pos,
			yaw,
			pitch,
			roll,
			PITCH_OFFSET);
	}

	public void moveTo(double x, double y, double z) {
		pos.setX(x);
		pos.setY(y);
		pos.setZ(z);
	}

	public void move(double x, double y, double z) {
		pos.setX(x + pos.getX());
		pos.setY(y + pos.getY());
		pos.setZ(z + pos.getZ());
	}

	public void moveWithYaw(double forwardSpeed, double rightSpeed) {
		if (forwardSpeed == 0 && rightSpeed == 0) return;

		double yawRadian = Math.toRadians(yaw.getAngle());
		double pitchRadian = Math.toRadians(pitch.getAngle());

		double cosPitch = Math.cos(pitchRadian);
		double sinYaw = Math.sin(yawRadian);
		double cosYaw = Math.cos(yawRadian);

		double xForwardFactor = sinYaw * cosPitch * forwardSpeed;
		double xRightFactor = cosYaw * rightSpeed;
		double yForwardFactor = cosYaw * cosPitch * forwardSpeed;
		double yRightFactor = sinYaw * rightSpeed;

		double deltaX = xForwardFactor + xRightFactor;
		double deltaY = yForwardFactor - yRightFactor;
		double deltaZ = Math.sin(pitchRadian) * forwardSpeed;
		move(deltaX, deltaY, deltaZ);
	}

	public void setRotate(double yw, double ptch, double rll) {
		yaw.setAngle(yw);
		pitch.setAngle(ptch);
		roll.setAngle(rll);
	}

	public void setYaw(double yw) {
		yaw.setAngle(((yw - WRAP_YAW_AT) % 360 + 360) % 360 - WRAP_YAW_COMPLEMENT);
	}

	public void setPitch(double ptch) {
		pitch.setAngle(Math.max(Math.min(ptch, PITCH_MAX), PITCH_MIN));
	}

	public void setRoll(double rll) {
		roll.setAngle(Math.max(Math.min(rll, ROLL_MAX), ROLL_MIN));
	}

	public void resetLook() {
		yaw.setAngle(0);
		pitch.setAngle(0);
		roll.setAngle(0);
	}

	public void resetMove() {
		pos.setX(0.0);
		pos.setY(0.0);
		pos.setZ(0.0);
	}

	public void reset() {
		resetMove();
		resetLook();
	}

	public void rotate(double yawDelta, double pitchDelta) {
		setYaw(yaw.getAngle() + yawDelta * sensitivityYaw);
		setPitch(pitch.getAngle() + pitchDelta * sensitivityPitch * (invertMouse ? 1 : -1));
	}

	@Override
	public String toString() {
		return "CameraXform{" +
			"pos=(" + pos.getX() + ',' + pos.getY() + ',' + pos.getZ() + ')' +
			", yaw=" + yaw.getAngle() +
			", pitch=" + pitch.getAngle() +
			", roll=" + roll.getAngle() +
			", sensitivity=(" + sensitivityYaw + ',' + sensitivityPitch + ')' +
			", invertMouse=" + invertMouse +
			'}';
	}
}