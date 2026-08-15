package com.agrolucas.view;

import com.agrolucas.model.Capture;
import com.agrolucas.model.HexPacket;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Builds the form to add a new Capture
 */
public class CaptureInputPane extends VBox {

    private final CaptureState state;

    public CaptureInputPane(CaptureState state) {
        super(10);
        this.state = state;

        TextField captureNameTextField = new TextField();
        captureNameTextField.setPromptText("Enter a capture name");

        TextField captureHexTextField = new TextField();
        captureHexTextField.setPromptText("Enter the hexadecimal data of the capture");

        Label hint = new Label();

        Button addCaptureButton = new Button("Add");
        addCaptureButton.setOnAction(e -> addCapture(captureNameTextField, captureHexTextField, hint));

        HBox row1 = new HBox(10, captureNameTextField, captureHexTextField, addCaptureButton);

        getChildren().addAll(row1, hint);
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
