package com.agrolucas.view;

import com.agrolucas.model.Capture;
import com.agrolucas.model.Field;
import com.agrolucas.model.FieldDisplay;
import com.agrolucas.model.MessageType;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Collection;
import java.util.List;

/**
 * Shared reactive state for the capture / field editing screen.
 */
public class CaptureState {

    private final ObservableList<Capture> captures = FXCollections.observableArrayList();
    private final ObservableList<MessageType> messageTypes = FXCollections.observableArrayList();
    private final ObjectProperty<FieldDisplay> displayMode = new SimpleObjectProperty<>(FieldDisplay.HEX);
    private final ObjectProperty<MessageType> viewedMessageType = new SimpleObjectProperty<>();
    private final ObservableList<Field> viewedFields = FXCollections.observableArrayList(); // mirrors viewedMessageType's plain field list

    // a column selection is always a contiguous range, -1 means "no selection"
    private final IntegerProperty selectionStart = new SimpleIntegerProperty(-1);
    private final IntegerProperty selectionEnd = new SimpleIntegerProperty(-1);

    public CaptureState() {
        // fires no matter how the value changes: direct set(), bindBidirectional from a ComboBox, ...
        viewedMessageType.addListener((obs, oldVal, newVal) -> refreshViewedFields());
    }

    public ObservableList<Capture> getCaptures() {
        return captures;
    }

    public ObservableList<MessageType> getMessageTypes() {
        return messageTypes;
    }

    public FieldDisplay getDisplayMode() {
        return displayMode.get();
    }

    public ObjectProperty<FieldDisplay> displayModeProperty() {
        return displayMode;
    }

    public MessageType getViewedMessageType() {
        return viewedMessageType.get();
    }

    public ObjectProperty<MessageType> viewedMessageTypeProperty() {
        return viewedMessageType;
    }

    /**
     * The Fields of the currently viewed MessageType, kept in sync with it automatically.
     * A separate ObservableList mirror, since MessageType.getFields() is a plain List
     * (the model package does not depend on JavaFX)
     */
    public ObservableList<Field> getViewedFields() {
        return viewedFields;
    }

    private void refreshViewedFields() {
        MessageType messageType = getViewedMessageType();
        viewedFields.setAll(messageType == null ? List.of() : messageType.getFields());
    }

    /**
     * Add a Field to a MessageType, refreshing the viewed Fields mirror if that MessageType is the one currently viewed
     */
    public void addField(MessageType messageType, Field field) {
        messageType.getFields().add(field);
        if (messageType == getViewedMessageType())
            refreshViewedFields();
    }

    /**
     * Remove Fields from a MessageType, refreshing the viewed Fields mirror if that MessageType is the one currently viewed
     */
    public void removeFields(MessageType messageType, Collection<Field> fields) {
        messageType.getFields().removeAll(fields);
        if (messageType == getViewedMessageType())
            refreshViewedFields();
    }

    public int getSelectionStart() {
        return selectionStart.get();
    }

    public int getSelectionEnd() {
        return selectionEnd.get();
    }

    public IntegerProperty selectionStartProperty() {
        return selectionStart;
    }

    public IntegerProperty selectionEndProperty() {
        return selectionEnd;
    }

    public boolean isSelectionEmpty() {
        return selectionStart.get() < 0;
    }

    /**
     * Set the current column selection to a contiguous range (replacing any previous selection)
     * @param fromIndex, the first column index of the range (inclusive)
     * @param toIndex, the last column index of the range (inclusive)
     */
    public void setSelection(int fromIndex, int toIndex) {
        selectionStart.set(Math.min(fromIndex, toIndex));
        selectionEnd.set(Math.max(fromIndex, toIndex));
    }

    public void clearSelection() {
        selectionStart.set(-1);
        selectionEnd.set(-1);
    }

    /**
     * The number of bits represented by a single data column in the current display mode:
     * one hex digit is a nibble, one binary digit is a single bit, one ASCII character is a whole byte
     */
    public int bitsPerColumn() {
        return switch (getDisplayMode()) {
            case HEX -> 4;
            case BINARY -> 1;
            case ASCII -> 8;
        };
    }
}
