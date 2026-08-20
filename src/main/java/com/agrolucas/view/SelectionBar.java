package com.agrolucas.view;

import com.agrolucas.model.Field;
import com.agrolucas.model.FieldDisplay;
import com.agrolucas.model.MessageType;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Display bar that appears above the capture grid only when data columns are selected.
 * It shows the current bit range and allows for a Field to be created from it.
 * The Field is added to the message type currently selected in the capture section.
 */
public class SelectionBar extends HBox {

    private final CaptureState state;
    private final Label selectionLabel = new Label();
    private final TextField fieldNameTextField = new TextField();
    private final ComboBox<FieldDisplay> fieldDisplayComboBox = new ComboBox<>();
    private final ColorPicker fieldColorPicker = new ColorPicker();

    public SelectionBar(CaptureState state) {
        super(10);
        this.state = state;
        getStyleClass().add("selection-bar");
        setAlignment(Pos.CENTER_LEFT);

        selectionLabel.getStyleClass().add("selection-label");

        Region spacer = new Region();
        spacer.setMinWidth(4);

        fieldNameTextField.setPromptText("Field name");
        HBox.setHgrow(fieldNameTextField, Priority.ALWAYS);

        // how this Field will be displayed in the field section, independent of how the grid is currently viewed
        Label displayLabel = new Label("shown as");
        displayLabel.getStyleClass().add("inline-label");
        fieldDisplayComboBox.getItems().addAll(FieldDisplay.values());
        fieldDisplayComboBox.setValue(state.getDisplayMode()); // by default has the same value as the capture grid's current view type

        // the color of the field in the capture grid
        fieldColorPicker.getStyleClass().add("field-color-picker");
        fieldColorPicker.setTooltip(new Tooltip("Colour used for this field in the capture grid"));
        fieldColorPicker.setValue(Color.web(state.nextFieldColor()));

        Button createFieldButton = new Button("Create field");
        createFieldButton.getStyleClass().add("accent");
        createFieldButton.setOnAction(e -> createField());

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("ghost");
        cancelButton.setOnAction(e -> state.clearSelection());

        getChildren().addAll(selectionLabel, spacer, fieldNameTextField,
                displayLabel, fieldDisplayComboBox, fieldColorPicker, createFieldButton, cancelButton);

        setVisible(false); // hide the node
        setManaged(false); // fully collapses the space when hidden, setVisible alone would leave a gap

        state.selectionStartProperty().addListener((obs, oldVal, newVal) -> refresh());
        state.selectionEndProperty().addListener((obs, oldVal, newVal) -> refresh());
    }

    /**
     * Show / hide this bar and update the bit-range label to match the current selection
     */
    private void refresh() {
        boolean hasSelection = !state.isSelectionEmpty();
        boolean wasVisible = isVisible();

        setVisible(hasSelection);
        setManaged(hasSelection);

        if (!hasSelection)
            return;

        if (!wasVisible) {
            fieldDisplayComboBox.setValue(state.getDisplayMode());
            fieldColorPicker.setValue(Color.web(state.nextFieldColor()));
        }

        int bitsPerColumn = state.bitsPerColumn();
        int fromBit = state.getSelectionStart() * bitsPerColumn;
        int toBit = (state.getSelectionEnd() + 1) * bitsPerColumn - 1;
        int bitCount = toBit - fromBit + 1;
        selectionLabel.setText("Selection: bits " + fromBit + "-" + toBit + " (" + bitCount + " bits)");
    }

    /**
     * Create a Field from the currently selected data columns, and add it to the message type
     * currently selected in the capture section
     */
    private void createField() {
        String fieldName = fieldNameTextField.getText();
        MessageType messageType = state.getViewedMessageType();

        if (fieldName == null || fieldName.isBlank() || messageType == null || state.isSelectionEmpty())
            return;

        int bitsPerColumn = state.bitsPerColumn();
        int startBit = state.getSelectionStart() * bitsPerColumn;
        int endBit = (state.getSelectionEnd() + 1) * bitsPerColumn - 1;

        state.addField(messageType, new Field(fieldName, startBit, endBit,
                fieldDisplayComboBox.getValue(), ColorUtils.toHex(fieldColorPicker.getValue())));

        state.clearSelection();
        fieldNameTextField.setText(null);
    }
}
