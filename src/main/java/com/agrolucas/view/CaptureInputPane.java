package com.agrolucas.view;

import com.agrolucas.model.Capture;
import com.agrolucas.model.HexPacket;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Builds the form to add a new Capture
 */
public class CaptureInputPane extends VBox {

    private final CaptureState state;

    public CaptureInputPane(CaptureState state) {
        super(10);
        this.state = state;
        getStyleClass().add("card");

        Label sectionTitle = new Label("ADD A CAPTURE");
        sectionTitle.getStyleClass().add("card-title");

        TextField captureNameTextField = new TextField();
        captureNameTextField.setPromptText("Capture name (e.g. Power button)");
        captureNameTextField.setPrefWidth(230);

        TextField captureHexTextField = new TextField();
        captureHexTextField.setPromptText("Paste hex, e.g. C4 3D 54 00 1B");
        captureHexTextField.getStyleClass().add("mono-field");
        HBox.setHgrow(captureHexTextField, Priority.ALWAYS); // the hex is the long one, let it take the leftover width

        Label hint = new Label();
        hint.getStyleClass().add("card-hint");
        // only takes up space while it actually says something, otherwise it leaves a gap under the row
        hint.visibleProperty().bind(hint.textProperty().isNotEmpty());
        hint.managedProperty().bind(hint.visibleProperty());

        Button addCaptureButton = new Button("Add capture");
        addCaptureButton.getStyleClass().add("accent");
        addCaptureButton.setDefaultButton(true); // pressing ENTER anywhere in the form adds the capture
        addCaptureButton.setOnAction(e -> addCapture(captureNameTextField, captureHexTextField, hint));

        HBox inputRow = new HBox(10, captureNameTextField, captureHexTextField, addCaptureButton);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(sectionTitle, inputRow, hint);
    }

    private void addCapture(TextField captureNameTextField, TextField captureHexTextField, Label hint) {
        String captureName = captureNameTextField.getText();
        String captureHex = captureHexTextField.getText();

        if (!isCorrectHexadecimal(captureHex)) {
            hint.setText("Enter a valid hexadecimal value");
            return;
        }

        if (captureName == null || captureName.isBlank())
            captureName = "Capture " + state.getCaptures().size(); // we don't care if the name is not unique

        state.getCaptures().add(new Capture(captureName, new HexPacket(captureHex)));
        captureNameTextField.setText(null);
        captureHexTextField.setText(null);
        hint.setText(null);
    }

    /**
     * Check that the given String is a correct hexadecimal value
     * Allow spaces in the hexadecimal string
     * @param hex, the string to check
     * @return true if the string is a correct hexadecimal value, false otherwise
     */
    private boolean isCorrectHexadecimal(String hex) {
        if (hex == null)
            return false;

        String stripped = hex.replaceAll("\\s+", ""); // Replace all white spaces

        if (stripped.isBlank())
            return false;

        return stripped.matches("^[0-9A-Fa-f]+$");
    }
}
