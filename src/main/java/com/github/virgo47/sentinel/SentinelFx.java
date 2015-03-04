package com.github.virgo47.sentinel;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.AmbientLight;
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
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public class SentinelFx extends Application {

	private static final Logger log = Logger.getLogger(SentinelFx.class.getName());

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

		PointLight pointLight = new PointLight(Color.GRAY);
		pointLight.setTranslateX(200);
		pointLight.setTranslateY(300);
		pointLight.setTranslateZ(1000);
		root3d.getChildren().add(pointLight);

		AmbientLight aLight = new AmbientLight(Color.color(0.1, 0.1, 0.1));
		root3d.getChildren().add(aLight);

		PerspectiveCamera camera = new PerspectiveCamera(true);
		camera.setFarClip(Double.MAX_VALUE);
		scene3d.setCamera(camera);
		scene3d.setFocusTraversable(true);

		CameraXform cameraNode = new CameraXform();
		cameraNode.getChildren().add(camera);

		// TODO can be replaced with Timeline?
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
			debugButton,
			debugOutput
		);
		controls.setSpacing(5);
		controls.setStyle("-fx-background-color: whitesmoke;" +
			"-fx-border-color: transparent transparent transparent gray;" +
			"-fx-border-width: 2px;" +
			"-fx-padding: 5px;");

		buildLandscape(root3d);

		cameraNode.invertMouse = true;

		cameraNode.pos.xProperty().bindBidirectional(xControl.valueProperty());
		cameraNode.pos.yProperty().bindBidirectional(yControl.valueProperty());
		cameraNode.pos.zProperty().bindBidirectional(zControl.valueProperty());

		cameraNode.yaw.angleProperty().bindBidirectional(yawControl.valueProperty());
		cameraNode.pitch.angleProperty().bindBidirectional(pitchControl.valueProperty());
		cameraNode.roll.angleProperty().bindBidirectional(rollControl.valueProperty());

		camera.fieldOfViewProperty().bindBidirectional(fovControl.valueProperty());

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
				log.info("pickResult = " + pickResult);
			}
		});
		mainPane.setOnMouseMoved(e -> {
			double deltaX = e.getScreenX() - crosshair.getResetX();
			double deltaY = e.getScreenY() - crosshair.getResetY();

//			log.finest("Mouse MOVE: " + deltaX + ", " + deltaY);
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

	private void buildLandscape(Group group3d) throws FileNotFoundException {
		Landscape landscape = new Landscape(32, 24, new Landscape.Config(1, 1, 30, 30));
		landscape.generate(0);

		LandscapeMeshView landscapeMeshView = new LandscapeMeshView();
		landscapeMeshView.setLandscape(landscape);

		group3d.getChildren().add(landscapeMeshView);
	}

	private Media playSound() throws URISyntaxException {
		URI oggUri = new URI("file", "some.mp3", null);
		String source = oggUri.toString();
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
		Logger topLogger = Logger.getLogger("com.github.virgo47");
		topLogger.setLevel(Level.ALL);
		ConsoleHandler handler = new ConsoleHandler() {
			protected void setOutputStream(OutputStream out) throws SecurityException {
				super.setOutputStream(System.out);
			}
		};
		handler.setLevel(Level.ALL);
		topLogger.addHandler(handler);
		handler.setFormatter(new NormalSingleLineFormatter());

		launch(args);
	}

	private static class NormalSingleLineFormatter extends Formatter {
		@Override
		public String format(LogRecord record) {
			String timestamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
				LocalDateTime.ofInstant(Instant.ofEpochMilli(record.getMillis()), ZoneId.systemDefault()));
			return timestamp + ' ' + record.getLevel() + ' ' + record.getMessage() + System.lineSeparator();
		}
	}
}
