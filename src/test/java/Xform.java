
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

import java.util.StringJoiner;

public class Xform extends Group {

	private static final Rotate PITCH_OFFSET = new Rotate(-90, Rotate.X_AXIS);

	public Rotate yaw = new Rotate(0, new Point3D(0, 0, -1)); // Z axis, but clockwise, so that it goes like compass
	public Rotate pitch = new Rotate(0, Rotate.X_AXIS);
	public Rotate roll = new Rotate(0, Rotate.Y_AXIS);
	public Translate pos = new Translate();

	public Xform() throws NonInvertibleTransformException {
		getTransforms().addAll(
			pos,
			yaw,
			pitch,
			roll,
			PITCH_OFFSET);
	}

	public void move(double x, double y, double z) {
		pos.setX(x);
		pos.setY(y);
		pos.setZ(z);
	}

	public void setRotate(double yw, double ptch, double rll) {
		yaw.setAngle(yw);
		pitch.setAngle(ptch);
		roll.setAngle(rll);
	}

	public void setYaw(double yw) {
		yaw.setAngle(yw);
	}

	public void setPitch(double ptch) {
		pitch.setAngle(ptch);
	}

	public void setRoll(double rll) {
		roll.setAngle(rll);
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

	public String toString() {
		StringJoiner sj = new StringJoiner(", ", "Xform: ", "");
		for (Transform transform : getTransforms()) {
			sj.add(transform.toString());
		}
		return sj.toString();
	}
}