import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;

/**
 * DoubleControl combines label, text field input and slider. You can add it to the scene
 * in its default form (HBox) or you can use {@link #addAllTo(Pane)} to add its three internal
 * nodes to any other pane (e.g. grid).
 */
public class DoubleControl extends HBox {

	private final Label label;
	private final TextField input;
	private final Slider slider;

	public DoubleControl(String labelText, double min, double max, double initialValue) {
		label = new Label(labelText);
		input = new TextField();
		slider = new Slider(min, max, initialValue);
		slider.setShowTickMarks(true);
//		slider.setShowTickLabels(true);
		slider.setMajorTickUnit(max / 2);
		slider.setMinorTickCount(9);
		slider.setSnapToTicks(true);

		// TODO this will throw Runtime/ParseExceptions
		StringConverter<Number> converter = new NumberStringConverter();
		Bindings.bindBidirectional(input.textProperty(), slider.valueProperty(), converter);

		HBox labelInputBox = new HBox(label, input);
		labelInputBox.setSpacing(3);
		labelInputBox.setAlignment(Pos.BASELINE_LEFT);
		getChildren().addAll(labelInputBox, slider);
		setSpacing(3);
	}

	public void addAllTo(Pane pane) {
		pane.getChildren().addAll(label, input, slider);
	}

	public DoubleProperty valueProperty() {
		return slider.valueProperty();
	}
}
