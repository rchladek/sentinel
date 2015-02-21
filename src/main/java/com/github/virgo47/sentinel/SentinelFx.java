package com.github.virgo47.sentinel;

import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.Camera;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.PickResult;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

public class SentinelFx extends Application {

	private static final KeyCodeCombination KEY_COMBINATION_FULLSCREEN = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN);

	public static final double AXIX_LEN = 500;

	public static final Color X_COLOR = Color.RED;
	public static final Color Y_COLOR = Color.BLUE;
	public static final Color Z_COLOR = Color.GREEN;
	public static final double FLY_SPEED = 3;
	public static final double WALK_SPEED = 0.5;

	public static final int STILL = 0;
	public static final int MOVE = 1;

	// could be booleans, but we want to subtract them
	private int movingForward;
	private int movingBack;
	private int movingRight;
	private int movingLeft;

	private double speed = FLY_SPEED;

	private boolean spaceClick = false;
	private Crosshair crosshair;

	@Override
	public void start(final Stage stage) throws Exception {
		stage.setTitle("Sentinel Java Remake");

		stage.setFullScreen(true);
		stage.setFullScreenExitHint("");
		stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH); // handled via menu

		BorderPane layout2d = new BorderPane();

		MenuBar menuBar = new MenuBar();
		Menu mainMenu = new Menu("Sentinel");
		MenuItem anyAction = new MenuItem("Any Test Action");
		MenuItem fullscreenCmd = new MenuItem("Toggle Fullscreen");
		MenuItem exitCmd = new MenuItem("Exit");
		mainMenu.getItems().addAll(anyAction, fullscreenCmd, exitCmd);
		menuBar.getMenus().add(mainMenu);

		exitCmd.setOnAction(e -> stage.close());
		fullscreenCmd.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
		fullscreenCmd.setAccelerator(KEY_COMBINATION_FULLSCREEN);

		anyAction.setAccelerator(new KeyCodeCombination(KeyCode.ESCAPE));
		anyAction.setOnAction(e -> Platform.runLater(() -> layout2d.setTop(layout2d.getTop() != null ? null : menuBar)));

		// 2D
		StackPane group2d = new StackPane();

		Text text = new Text("Press ESC to toggle menu\nPress Alt+Enter to toggle fullscreen\n" +
			"3D is " + (Platform.isSupported(ConditionalFeature.SCENE3D) ? "fully" : "NOT") + " supported");
		text.setTextAlignment(TextAlignment.CENTER);
		text.setTranslateY(-100);
		group2d.getChildren().add(text);

		// 3D
		Group root3d = new Group();
		SubScene scene3d = new SubScene(root3d, 1600, 900, true, SceneAntialiasing.BALANCED);
		root3d.setRotationAxis(Rotate.X_AXIS);
		root3d.setRotate(90);

		buildAxes(root3d);

		PointLight pointLight = new PointLight(Color.WHITE);
		pointLight.setTranslateZ(1000);
		root3d.getChildren().add(pointLight);

//		AmbientLight aLight = new AmbientLight(new Color(0.9, 0.9, 0.95, 1));
//		root3d.getChildren().add(aLight);

		PerspectiveCamera camera = new PerspectiveCamera(true);
		camera.setFarClip(Double.MAX_VALUE);
		scene3d.setCamera(camera);
		scene3d.setFocusTraversable(true);

		CameraXform cameraNode = new CameraXform();
		cameraNode.getChildren().add(camera);

		ScheduledService<Void> svc = new ScheduledService<Void>() {
			@Override
			protected Task<Void> createTask() {
				return new Task<Void>() {
					@Override
					protected Void call() throws Exception {
						Platform.runLater(() -> cameraNode.moveWithYaw(
							(movingForward - movingBack) * speed,
							(movingRight - movingLeft) * speed));
						return null;
					}
				};
			}
		};
		svc.setPeriod(Duration.millis(20));
		svc.start();

		DoubleControl xControl = new DoubleControl("X", -1000, 1000, 0);
		DoubleControl yControl = new DoubleControl("Y", -1000, 1000, 0);
		DoubleControl zControl = new DoubleControl("Z", -1000, 1000, 0);
		Button resetPosition = new Button("Reset");
		resetPosition.setOnAction(e -> {
			xControl.valueProperty().set(100);
			yControl.valueProperty().set(-400);
			zControl.valueProperty().set(100);
		});

		DoubleControl yawControl = new DoubleControl("Yaw", -200, 200, 0);
		DoubleControl pitchControl = new DoubleControl("Pitch", -200, 200, 0);
		DoubleControl rollControl = new DoubleControl("Roll", -200, 200, 0);
		Button resetRotation = new Button("Reset");
		resetRotation.setOnAction(e -> {
			yawControl.valueProperty().set(0);
			pitchControl.valueProperty().set(0);
			rollControl.valueProperty().set(0);
		});
		DoubleControl fovControl = new DoubleControl("FOV", 1, 180, 45);

		DoubleControl sxControl = new DoubleControl("X", -1000, 1000, -20);
		DoubleControl syControl = new DoubleControl("Y", -1000, 1000, 10);
		DoubleControl szControl = new DoubleControl("Z", -1000, 1000, 0);
		Button resetSphere = new Button("Reset");
		resetSphere.setOnAction(e -> {
			sxControl.valueProperty().set(0);
			syControl.valueProperty().set(0);
			szControl.valueProperty().set(0);
		});

		root3d.getChildren().add(cameraNode);

		Button debugButton = new Button("Debug output");
		TextArea debugOutput = new TextArea();
		debugOutput.setWrapText(true);
		debugOutput.setMaxWidth(320);
		debugOutput.setPrefHeight(500);
		debugOutput.setStyle("-fx-font-size: 90%;");
		debugButton.setOnAction(e ->
			debugOutput.setText(debugNode(root3d)));
		VBox controls = new VBox(
			new Label("Camera position"),
			xControl, yControl, zControl,
			resetPosition,
			new Separator(Orientation.HORIZONTAL),
			new Label("Camera rotation"),
			yawControl, pitchControl, rollControl,
			resetRotation,
			new Label("Field of view (degrees)"),
			fovControl,
			new Separator(Orientation.HORIZONTAL),
			new Label("Sphere position"),
			sxControl, syControl, szControl,
			resetSphere,
			new Separator(Orientation.HORIZONTAL),
			debugButton,
			debugOutput
		);
		controls.setSpacing(5);
		controls.setStyle("-fx-background-color: whitesmoke;" +
			"-fx-border-color: transparent transparent transparent gray;" +
			"-fx-border-width: 2px;" +
			"-fx-padding: 5px;");

		Sphere sphere = new Sphere(10);
		sphere.setMaterial(new PhongMaterial(Color.BROWN));
		root3d.getChildren().addAll(sphere);

		addPlane(root3d);

//		Box box = new Box(200, 200, 1);
//		box.setTranslateZ(-1);
//		root3d.getChildren().addAll(box);

		cameraNode.invertMouse = true;

		cameraNode.pos.xProperty().bindBidirectional(xControl.valueProperty());
		cameraNode.pos.yProperty().bindBidirectional(yControl.valueProperty());
		cameraNode.pos.zProperty().bindBidirectional(zControl.valueProperty());

		cameraNode.yaw.angleProperty().bindBidirectional(yawControl.valueProperty());
		cameraNode.pitch.angleProperty().bindBidirectional(pitchControl.valueProperty());
		cameraNode.roll.angleProperty().bindBidirectional(rollControl.valueProperty());

		camera.fieldOfViewProperty().bindBidirectional(fovControl.valueProperty());

		sphere.translateXProperty().bindBidirectional(sxControl.valueProperty());
		sphere.translateYProperty().bindBidirectional(syControl.valueProperty());
		sphere.translateZProperty().bindBidirectional(szControl.valueProperty());

		layout2d.setRight(controls);
		layout2d.setTop(menuBar);

		crosshair = new Crosshair();
		StackPane mainPane = new StackPane(scene3d, group2d, layout2d, crosshair);
		scene3d.widthProperty().bind(layout2d.widthProperty());
		scene3d.heightProperty().bind(layout2d.heightProperty());

		Scene rootAppScene = new Scene(mainPane);
		stage.setScene(rootAppScene);
		stage.show();

		crosshair.resetMouse();

		mainPane.setOnMouseClicked(e -> {
			scene3d.requestFocus();
			if (spaceClick) {
				PickResult pickResult = e.getPickResult();
				Node intersectedNode = pickResult.getIntersectedNode();
				if (intersectedNode instanceof Sphere) {
					System.out.println("BINGO!");
				}
				System.out.println("pickResult = " + pickResult);
			}
		});
		mainPane.setOnMouseMoved(e -> {
			double deltaX = e.getScreenX() - crosshair.getResetX();
			double deltaY = e.getScreenY() - crosshair.getResetY();

//			System.out.println("MOVE: " +deltaX + ", " + deltaY);
			cameraNode.rotate(deltaX, deltaY);
			crosshair.resetMouse();
		});
		mainPane.setCursor(Cursor.NONE);

		group2d.setMouseTransparent(true);
		layout2d.setPickOnBounds(false);

		scene3d.setOnKeyPressed(e -> {
			KeyCode keycode = e.getCode();
			modifyDirection(keycode, MOVE); // any 1 constant will do
			if (keycode == KeyCode.SPACE) {
				spaceClick = true;
				// if we could get pick result here easily... now we move to mouse click handler
				crosshair.click();
			}
		});

		scene3d.setOnKeyReleased(e -> {
			KeyCode keycode = e.getCode();
			modifyDirection(keycode, STILL);
			if (keycode == KeyCode.SPACE) {
				spaceClick = false;
			}
		});

//		playSound();
//		playVideo();
//		playYoutube();

		Window window = rootAppScene.getWindow();
		window.heightProperty().addListener(this::changedSceneSize);
		window.widthProperty().addListener(this::changedSceneSize);
		window.xProperty().addListener(this::changedSceneSize);
		window.yProperty().addListener(this::changedSceneSize);

		crosshair.refreshResetPosition();
		resetPosition.fire();
		debugButton.fire();
	}

	private void modifyDirection(KeyCode keycode, int move) {
		if (keycode == KeyCode.E) {
			movingForward = move;
		}
		if (keycode == KeyCode.D) {
			movingBack = move;
		}
		if (keycode == KeyCode.F) {
			movingRight = move;
		}
		if (keycode == KeyCode.S) {
			movingLeft = move;
		}
		if (keycode == KeyCode.A) {
			speed = move == MOVE ? WALK_SPEED : FLY_SPEED;
		}
	}

	public void changedSceneSize(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
		// without runLater it does not reflect position after the actual resize
		Platform.runLater(crosshair::refreshResetPosition);
	}

	private void buildAxes(Group group3d) {
		final PhongMaterial xMaterial = new PhongMaterial();
		xMaterial.setDiffuseColor(X_COLOR.darker());
		xMaterial.setSpecularColor(X_COLOR);

		final PhongMaterial yMaterial = new PhongMaterial();
		yMaterial.setDiffuseColor(Y_COLOR.darker());
		yMaterial.setSpecularColor(Y_COLOR);

		final PhongMaterial zMaterial = new PhongMaterial();
		zMaterial.setDiffuseColor(Z_COLOR.darker());
		zMaterial.setSpecularColor(Z_COLOR);

		final Box xAxis = new Box(AXIX_LEN, 1, 1);
		xAxis.setTranslateX(AXIX_LEN / 2);
		final Box yAxis = new Box(1, AXIX_LEN, 1);
		yAxis.setTranslateY(AXIX_LEN / 2);
		final Box zAxis = new Box(1, 1, AXIX_LEN);
		zAxis.setTranslateZ(AXIX_LEN / 2);

		xAxis.setMaterial(xMaterial);
		yAxis.setMaterial(yMaterial);
		zAxis.setMaterial(zMaterial);

		group3d.getChildren().addAll(xAxis, yAxis, zAxis);
	}

	private void addPlane(Group group3d) throws FileNotFoundException {
		TriangleMesh landscapeMesh = new TriangleMesh();
		landscapeMesh.getPoints().addAll(
			0, 0, 1,
			1, 0, 1,
			2, 0, 0,
			3, 0, 0,
			4, 0, 1,
			0, 1, 1,
			1, 1, 1,
			2, 1, 0,
			3, 1, 0,
			4, 1, 0,
			0, 2, 0,
			1, 2, 0,
			2, 2, 1,
			3, 2, 1,
			4, 2, 0,
			0, 3, 0,
			1, 3, 0,
			2, 3, 2,
			3, 3, 0,
			4, 3, 0,
			0, 4, 0,
			1, 4, 0,
			2, 4, 0,
			3, 4, 0,
			4, 4, 0
		);
		landscapeMesh.getTexCoords().addAll(
			0.0f, 0.0f,
			0.5f, 0.0f,
			0.5f, 0.5f,
			0.0f, 0.5f,
			0.5f, 0.0f,
			1.0f, 0.0f,
			1.0f, 0.5f,
			0.5f, 0.5f,
			0.0f, 0.5f,
			0.5f, 0.5f,
			0.5f, 1.0f,
			0.0f, 1.0f
		);

		int sizeX = 4;
		int sizeY = 4;
		for (int i = 0; i < sizeX; i++) {
			for (int j = 0; j < sizeY; j++) {
				int arraySizeX = sizeX + 1;
				int pointLeftBottom = i * arraySizeX + j;
				int pointRightBottom = i * arraySizeX + j + 1;
				int pointRightTop = (i + 1) * arraySizeX + j + 1;
				int pointLeftTop = (i + 1) * arraySizeX + j;

				// by default we make triangles: LB-RB-RT and LB-RT-LT

				float zlb = landscapeMesh.getPoints().get(pointLeftBottom * 3 + 2);
				float zrb = landscapeMesh.getPoints().get(pointRightBottom * 3 + 2);
				float zrt = landscapeMesh.getPoints().get(pointRightTop * 3 + 2);
				float zlt = landscapeMesh.getPoints().get(pointLeftTop * 3 + 2);

				boolean triangle1IsFlat = zlb == zrb && zlb == zrt;
				boolean triangle2IsFlat = zlb == zrt && zlb == zlt;
				boolean triangleInverse1IsFlat = zlb == zrb && zlb == zlt;
				boolean triangleInverse2IsFlat = zlt == zrt && zrb == zrt;

				// if the whole square is not flat, but any of default triangles are flat, we want to inverse the triangulation
				boolean plainSqure = triangle1IsFlat && triangle2IsFlat;
				boolean invertBecauseOfPartialFlatness = !plainSqure && (triangle1IsFlat || triangle2IsFlat);
				boolean invertedTrianglesSumIsLower = (2 * zlb + zrb + 2 * zrt + zlt) > (zlb + 2 * zrb + zrt + 2 * zlt);

				boolean inverseTriangulation = invertBecauseOfPartialFlatness
					// we also want to prefer valleys instead of high ridges IF it doesn't create new partially-flat squares
					|| invertedTrianglesSumIsLower && !triangleInverse1IsFlat && !triangleInverse2IsFlat;

				System.out.println("Height(" + i + ',' + j + "): " + zlb + ", " + zrb + ", " + zrt + ", " + zlt +
					" - triangulation " + (inverseTriangulation ? "inverse" : "default") +
					", t1flat " + triangle1IsFlat + ", t2flat " + triangle2IsFlat +
					", ti1flat " + triangleInverse1IsFlat + ", ti2flat " + triangleInverse2IsFlat +
					", pflat " + invertBecauseOfPartialFlatness + ", isum " + invertedTrianglesSumIsLower);

				int textureIndex = plainSqure ? (i + j) % 2 * 4 : 8;

				if (inverseTriangulation) {
					landscapeMesh.getFaces().addAll(
						pointLeftBottom, textureIndex, pointRightBottom, textureIndex + 1, pointLeftTop, textureIndex + 3,
						pointRightBottom, textureIndex + 1, pointRightTop, textureIndex + 2, pointLeftTop, textureIndex + 3
					);
				} else {
					landscapeMesh.getFaces().addAll(
						pointLeftBottom, textureIndex, pointRightBottom, textureIndex + 1, pointRightTop, textureIndex + 2,
						pointLeftBottom, textureIndex, pointRightTop, textureIndex + 2, pointLeftTop, textureIndex + 3
					);
				}
			}
		}
		MeshView meshView = new MeshView(landscapeMesh);
		double scale = 100;
		meshView.setScaleX(scale);
		meshView.setScaleY(scale);
		meshView.setScaleZ(scale / 5);

		meshView.setCullFace(CullFace.BACK);
		meshView.setDrawMode(DrawMode.FILL);

		PhongMaterial mat = new PhongMaterial();
		Image simpleTexture = new Image(getClass().getClassLoader().getResourceAsStream("textures-wood.jpg"));
		mat.setDiffuseMap(simpleTexture);
		meshView.setMaterial(mat);

		group3d.getChildren().add(meshView);
	}

	private Media playSound() throws URISyntaxException {
		URI oggUri = new URI("file", "/f:/music/Dream Theater/(1993) Live At The Marquee\\04 Dream Theater - Surrounded.mp3", null);
		String source = oggUri.toString();
		System.out.println("source = " + source);
		Media media = new Media(source);

		MediaPlayer mediaPlayer = new MediaPlayer(media);
		mediaPlayer.setStartTime(Duration.seconds(60));
		mediaPlayer.setStopTime(Duration.seconds(66));
		mediaPlayer.setVolume(0.5);
		mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
		mediaPlayer.play();
		return media;
	}

	private String debugNode(Node node) {
		StringBuilder sb = new StringBuilder();
		debugNode(node, sb, 0);
		return sb.toString();
	}

	private static final int LEVEL_INDENT = 4;

	private void debugNode(Node node, StringBuilder sb, int level) {
		int indent = level * LEVEL_INDENT;
		for (int i = 0; i < indent; i++) sb.append(' ');
		debugOutput(sb, node);
		sb.append('\n');
		if (node instanceof Parent) {
			for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
				debugNode(child, sb, level + 1);
			}
		}
	}

	private void debugOutput(StringBuilder sb, Node node) {
		sb.append(node.toString()).append(" - at: ").append(node.localToScene(0, 0, 0));
		if (node instanceof PerspectiveCamera) {
			Camera camera = (Camera) node;
			sb.append(", nearClip=").append(camera.getNearClip())
				.append(", farClip=").append(camera.getFarClip());
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
