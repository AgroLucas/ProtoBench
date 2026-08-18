package com.agrolucas.view;

import com.agrolucas.model.Field;
import com.agrolucas.model.MessageType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.util.ArrayList;

/**
 * Display bar that appears above the capture grid only when data columns are selected.
 * It shows the current bit range and allows for a Field to be created from it.
 */
public class SelectionBar extends HBox {

    private final CaptureState state;
    private final Label selectionLabel = new Label();
    private final ComboBox<MessageType> messageTypeComboBox;
    private final TextField fieldNameTextField = new TextField();

    public SelectionBar(CaptureState state) {
        super(10);
        this.state = state;

        messageTypeComboBox = new ComboBox<>(state.getMessageTypes());
        messageTypeComboBox.setEditable(true);
        messageTypeComboBox.setPromptText("Message type");
        messageTypeComboBox.valueProperty().bindBidirectional(state.viewedMessageTypeProperty()); // defaults to whichever MessageType is currently viewed, see CaptureState's default MessageType

        // An editable ComboBox<T> needs a StringConverter for any T that isn't String itself
        messageTypeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(MessageType messageType) {
                return messageType == null ? "" : messageType.getName();
            }

            @Override
            public MessageType fromString(String name) {
                if (name == null || name.isBlank())
                    return null;

                return state.getMessageTypes().stream()
                        .filter(mt -> mt.getName().equals(name))
                        .findFirst()
                        .orElseGet(() -> {
                            MessageType newMessageType = new MessageType(name, new ArrayList<>());
                            state.getMessageTypes().add(newMessageType);
                            return newMessageType;
                        });
            }
        });

        fieldNameTextField.setPromptText("Field name");

        Button createFieldButton = new Button("Create field");
        createFieldButton.setOnAction(e -> createField());

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> state.clearSelection());

        getChildren().addAll(selectionLabel, messageTypeComboBox, fieldNameTextField, createFieldButton, cancelButton);

        setVisible(false);
        setManaged(false); // fully collapses the space when hidden, setVisible alone would leave a gap

        state.selectionStartProperty().addListener((obs, oldVal, newVal) -> refresh());
        state.selectionEndProperty().addListener((obs, oldVal, newVal) -> refresh());
    }

    /**
     * Show / hide this bar and update the bit-range label to match the current selection
     */
    private void refresh() {
        boolean hasSelection = !state.isSelectionEmpty();
        setVisible(hasSelection);
        setManaged(hasSelection);

        if (!hasSelection)
            return;

        int bitsPerColumn = state.bitsPerColumn();
        int fromBit = state.getSelectionStart() * bitsPerColumn;
        int toBit = (state.getSelectionEnd() + 1) * bitsPerColumn - 1;
        int bitCount = toBit - fromBit + 1;
        selectionLabel.setText("Selection: bits " + fromBit + "-" + toBit + " (" + bitCount + " bits)");
    }

    /**
     * Create a Field from the currently selected data columns, and attach it to the MessageType
     * typed / selected in the MessageType ComboBox (creating that MessageType if it doesn't exist yet)
     */
    private void createField() {
        String fieldName = fieldNameTextField.getText();
        MessageType messageType = messageTypeComboBox.getValue();

        if (fieldName == null || fieldName.isBlank() || messageType == null || state.isSelectionEmpty())
            return;

        int bitsPerColumn = state.bitsPerColumn();
        int startBit = state.getSelectionStart() * bitsPerColumn;
        int endBit = (state.getSelectionEnd() + 1) * bitsPerColumn - 1;

        Field field = new Field(fieldName, startBit, endBit, state.getDisplayMode());
        state.addField(messageType, field);
        state.viewedMessageTypeProperty().set(messageType); // switches the view to a newly created MessageType, a no-op otherwise

        state.clearSelection();
        fieldNameTextField.setText(null);
        // the message type combo is intentionally left as-is, so the next field defaults to the same MessageType
    }
}
