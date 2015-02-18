import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Camera;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Robot;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

public class JavaFx3dTest extends Application {

	private static final KeyCodeCombination KEY_COMBINATION_FULLSCREEN = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN);

	public static final double AXIX_LEN = 500;

	public static final Color X_COLOR = Color.RED;
	public static final Color Y_COLOR = Color.BLUE;
	public static final Color Z_COLOR = Color.GREEN;
	public static final double SPEED_WALKING = 1;
	public static final double SPEED_RUNNING = 3;

	boolean walk = false;
	double speed = SPEED_WALKING;

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

		anyAction.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN));
		anyAction.setOnAction(e -> menuBar.setVisible(!menuBar.visibleProperty().getValue()));

		// 2D
		StackPane group2d = new StackPane();

		Text text = new Text("Press ESC to toggle menu\nPress Alt+Enter to toggle fullscreen\n" +
			"3D is " + (Platform.isSupported(ConditionalFeature.SCENE3D) ? "fully" : "NOT") + " supported");
		text.setTextAlignment(TextAlignment.CENTER);
		group2d.getChildren().add(text);

		// 3D
		Group root3d = new Group();
		SubScene scene3d = new SubScene(root3d, 1600, 900, true, SceneAntialiasing.BALANCED);
		root3d.setRotationAxis(Rotate.X_AXIS);
		root3d.setRotate(90);

		buildAxes(root3d);

//		AmbientLight aLight = new AmbientLight(new Color(0.6, 0.7, 0.8, 1));
//		root3d.getChildren().add(aLight);

		PerspectiveCamera camera = new PerspectiveCamera(true);
		camera.setFarClip(Double.MAX_VALUE);
		scene3d.setCamera(camera);
		scene3d.setFocusTraversable(true);

		CameraXform cameraNode = new CameraXform();
		cameraNode.getChildren().add(camera);

		scene3d.setOnKeyPressed(e -> {
			KeyCode keycode = e.getCode();
			if (keycode == KeyCode.E) {
				walk = true;
			}
			if (keycode == KeyCode.A) {
				speed = SPEED_RUNNING;
			}
		});

		scene3d.setOnKeyReleased(e -> {
			KeyCode keycode = e.getCode();
			if (keycode == KeyCode.E) {
				walk = false;
			}
			if (keycode == KeyCode.A) {
				speed = SPEED_WALKING;
			}
		});

		ScheduledService<Void> svc = new ScheduledService<Void>() {
			@Override
			protected Task<Void> createTask() {
				return new Task<Void>() {
					@Override
					protected Void call() throws Exception {
						Platform.runLater(() -> cameraNode.moveWithYaw(0, walk ? speed : 0));
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

		DoubleControl sxControl = new DoubleControl("X", -1000, 1000, 0);
		DoubleControl syControl = new DoubleControl("Y", -1000, 1000, 0);
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

//		addPlane(root3d);
		Box box = new Box(200, 200, 1);
		box.setTranslateZ(-1);
		root3d.getChildren().addAll(sphere, box);

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

		layout2d.setCenter(group2d);
		layout2d.setRight(controls);
		layout2d.setTop(menuBar);

		StackPane mainPane = new StackPane(scene3d, layout2d);
		scene3d.widthProperty().bind(layout2d.widthProperty());
		scene3d.heightProperty().bind(layout2d.heightProperty());

		mainPane.setCursor(Cursor.NONE);

		Scene rootAppScene = new Scene(mainPane);
		stage.setScene(rootAppScene);
		stage.show();

		double width = mainPane.getWidth();
		double height = mainPane.getHeight();
		Point2D mouseResetPoint = mainPane.localToScreen(width / 2, height / 2);
		double resetX = mouseResetPoint.getX();
		double resetY = mouseResetPoint.getY();

//		BaseFXRobot robot = new BaseFXRobot(rootAppScene);
		Robot robot = new Robot();
		robot.mouseMove((int) resetX, (int) resetY);

		mainPane.setOnMouseClicked(e -> {
			scene3d.requestFocus();
			System.out.println("e = " + e);
		});
		mainPane.setOnMouseMoved(e -> {
			double deltaX = e.getScreenX() - resetX;
			double deltaY = e.getScreenY() - resetY;

			cameraNode.rotate(deltaX, deltaY);
			robot.mouseMove((int) resetX, (int) resetY);
		});

//		playSound();
//		playVideo();
//		playYoutube();

		resetPosition.fire();
		debugButton.fire();
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
		Image diffuseMap = new Image(new FileInputStream("c:\\Users\\Virgo\\Pictures\\crescent-wallpaper.jpg"));
		TriangleMesh planeMesh = new TriangleMesh();

		float[] points = {
			-5, 5, 0,
			-5, -5, 0,
			5, 5, 0,
			5, -5, 0
		};
		float[] texCoords = {
			1, 1,
			1, 0,
			0, 1,
			0, 0
		};
		int[] faces = {
			2, 2, 1, 1, 0, 0,
			2, 2, 3, 3, 1, 1
		};
		planeMesh.getPoints().addAll(points);
		planeMesh.getTexCoords().addAll(texCoords);
		planeMesh.getFaces().addAll(faces);
		MeshView meshView = new MeshView(planeMesh);
		meshView.setMaterial(new PhongMaterial(Color.BLACK, diffuseMap, null, null, null));
		int scale = 1000;
		meshView.setScaleX(scale);
		meshView.setScaleY(scale);
		meshView.setScaleZ(scale);

		meshView.setCullFace(CullFace.NONE);

		group3d.getChildren().add(new MeshView(planeMesh));
	}

	/*
	private void playVideo() throws URISyntaxException {
		URI videoUri = new URI("file", "/f:/video/Byzantium (2012)/Byzantium.2013.mp4", null);
//		String uri = "http://www.youtube.com/watch?v=VWkmz-hPnEE";
		Media media = new Media(videoUri.toString());
		MediaPlayer mediaPlayer = new MediaPlayer(media);
		mediaPlayer.play();
		MediaView mediaView = new MediaView(mediaPlayer);
		rootLayout.setCenter(mediaView);
	}

	// loads the page, but video does not play
	private void playYoutube() throws URISyntaxException {
		String uri = "https://www.youtube.com/watch?v=VWkmz-hPnEE";
		WebView webView = new WebView();
		webView.getEngine().load(uri);
		rootLayout.setCenter(webView);
	}
	*/

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
