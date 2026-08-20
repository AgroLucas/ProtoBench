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
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.ArrayList;
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
    private final ObjectProperty<Capture> referenceCapture = new SimpleObjectProperty<>(); // every other Capture is compared against this one
    private final MessageType defaultMessageType = new MessageType("Default", new ArrayList<>()); // always present, cannot be deleted

    // a column selection is always a contiguous range, -1 means "no selection"
    private final IntegerProperty selectionStart = new SimpleIntegerProperty(-1);
    private final IntegerProperty selectionEnd = new SimpleIntegerProperty(-1);

    // bumped whenever a Field is added, removed, or edited. Field is a plain POJO, so editing one
    // notifies nothing on its own, this is what lets the capture grid know its coloring is stale
    private final IntegerProperty fieldsRevision = new SimpleIntegerProperty(0);

    /** Default colors handed out to new Fields, so consecutive fields are visually distinct */
    private static final String[] FIELD_COLOR_PALETTE = {
            "#e0b341", "#4aa3f0", "#c678dd", "#2dd4a7", "#f0803c", "#6cc24a", "#e06c9f", "#5bc8d6"
    };

    public CaptureState() {
        viewedMessageType.addListener((obs, oldVal, newVal) -> refreshViewedFields());

        // there is always a reference as long as there is at least one Capture: the first one added becomes it,
        // and deleting the current reference hands the role over to whichever Capture is now first
        captures.addListener((ListChangeListener<Capture>) change -> {
            if (!captures.contains(referenceCapture.get()))
                referenceCapture.set(captures.isEmpty() ? null : captures.get(0));
        });

        messageTypes.add(defaultMessageType);
        viewedMessageType.set(defaultMessageType);
    }

    public ObservableList<Capture> getCaptures() {
        return captures;
    }

    /**
     * The Capture every other Capture is compared against, to highlight what differs.
     * Null only while there is no Capture at all.
     */
    public Capture getReferenceCapture() {
        return referenceCapture.get();
    }

    public ObjectProperty<Capture> referenceCaptureProperty() {
        return referenceCapture;
    }

    public boolean isReference(Capture capture) {
        return capture != null && capture == referenceCapture.get();
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
     * Whether a MessageType is the built-in default one, which always exists and cannot be deleted
     */
    public boolean isDefaultMessageType(MessageType messageType) {
        return messageType == defaultMessageType;
    }

    /**
     * Look up a MessageType by name, creating and registering it if no MessageType has that name yet.
     * This is what lets the message type ComboBox double as a "create a new type" field.
     * @param name, the name to look for
     * @return the existing or newly created MessageType, null if the name is blank
     */
    public MessageType findOrCreateMessageType(String name) {
        if (name == null || name.isBlank())
            return null;

        return messageTypes.stream()
                .filter(mt -> mt.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    MessageType newMessageType = new MessageType(name, new ArrayList<>());
                    messageTypes.add(newMessageType);
                    return newMessageType;
                });
    }

    /**
     * Delete a MessageType along with its Fields, and fall back to viewing the default one.
     * The default MessageType itself is never deleted.
     * @param messageType, the MessageType to delete
     */
    public void deleteMessageType(MessageType messageType) {
        if (messageType == null || isDefaultMessageType(messageType))
            return;

        messageTypes.remove(messageType);
        // removing the viewed item from the list nulls the bound ComboBox, so the view is put back on the default
        viewedMessageType.set(defaultMessageType);
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
        notifyFieldsChanged();
    }

    /**
     * Remove Fields from a MessageType, refreshing the viewed Fields mirror if that MessageType is the one currently viewed
     */
    public void removeFields(MessageType messageType, Collection<Field> fields) {
        messageType.getFields().removeAll(fields);
        if (messageType == getViewedMessageType())
            refreshViewedFields();
        notifyFieldsChanged();
    }

    /**
     * Signal that a Field was added, removed, or had one of its values edited, so anything drawing
     * Fields (the capture grid colouring) can catch up
     */
    public void notifyFieldsChanged() {
        fieldsRevision.set(fieldsRevision.get() + 1);
    }

    public IntegerProperty fieldsRevisionProperty() {
        return fieldsRevision;
    }

    /**
     * Walking the default color palette to avoid the creation of consecutive Fields having the same color
     */
    public String nextFieldColor() {
        MessageType viewed = getViewedMessageType();
        int fieldCount = viewed == null ? 0 : viewed.getFields().size();
        return FIELD_COLOR_PALETTE[fieldCount % FIELD_COLOR_PALETTE.length];
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

    /**
     * Reset the column selection range
     */
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
