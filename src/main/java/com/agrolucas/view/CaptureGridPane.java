package com.agrolucas.view;

import com.agrolucas.model.Capture;
import com.agrolucas.model.Field;
import com.agrolucas.model.FieldDisplay;
import com.agrolucas.model.HexPacket;
import com.agrolucas.model.MessageType;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the following:
 *  - the view selection (hex / bin / ...)
 *  - the 'field add' section in case data is selected
 *  - every Capture in a Grid:
 *      The first column of the Grid (starting from row 1) contains the Delete Buttons.
 *      The second column of the Grid (starting from row 1) contains the Capture name.
 *      The first row (starting from column 2) contains the bit offset ruler.
 *      The remaining rows contains (starting from column 2) contains the packet data.
 */
public class CaptureGridPane extends VBox {

    // Cell colours are computed in Java rather than set through style classes, because a field's colour is
    // arbitrary and a stylesheet cannot hold a rule per possible colour. These mirror the palette in style.css.
    private static final String SELECTED_BACKGROUND = "#123a30";
    private static final String ACCENT_TEXT = "#2dd4a7";
    private static final String DIFF_TEXT = "#ff9d96";
    private static final String DIFF_BORDER = "#7d3b3b";
    private static final String EMPTY_TEXT = "#3a4552";
    private static final String DEFAULT_TEXT = "#e6edf3";
    private static final String MUTED_TEXT = "#66758a";
    private static final double FIELD_TINT_ALPHA = 0.22;

    private final CaptureState state;
    private final GridPane grid = new GridPane();
    private final List<Label> columnHeaders = new ArrayList<>();       // the bit offset ruler, one entry per data column
    private final List<List<DataCell>> columnCells = new ArrayList<>(); // every data cell, grouped by data column
    private final List<Field> columnFields = new ArrayList<>();         // the Field covering each data column, null when none
    private int selectionAnchorIndex = -1; // the column where a drag-select started

    /**
     * One character of one capture, remembering the states that decide how it is coloured
     * @param label, the Label showing the character
     * @param diff, whether it differs from the reference capture at that position
     * @param empty, whether this capture is too short to reach that position
     */
    private record DataCell(Label label, boolean diff, boolean empty) {
    }

    public CaptureGridPane(CaptureState state) {
        super(12);
        this.state = state;
        getStyleClass().add("card");

        Label sectionTitle = new Label("CAPTURES");
        sectionTitle.getStyleClass().add("card-title");

        ComboBox<FieldDisplay> fieldDisplayComboBox = new ComboBox<>();
        fieldDisplayComboBox.getItems().addAll(FieldDisplay.values());
        fieldDisplayComboBox.valueProperty().bindBidirectional(state.displayModeProperty());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // pushes the selectors to the right edge of the card

        Label messageTypeLabel = new Label("Message type");
        messageTypeLabel.getStyleClass().add("inline-label");

        Label displayLabel = new Label("View as");
        displayLabel.getStyleClass().add("inline-label");

        HBox titleRow = new HBox(10, sectionTitle, spacer,
                messageTypeLabel, buildMessageTypeComboBox(), displayLabel, fieldDisplayComboBox);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label hint = new Label("Click a column, or drag across several, to select a bit range and turn it into a field. "
                + "Click a star to make that capture the reference, everything that differs from it is highlighted.");
        hint.getStyleClass().add("card-hint");
        hint.setWrapText(true);

        SelectionBar selectionBar = new SelectionBar(state);

        grid.getStyleClass().add("capture-grid");
        grid.setHgap(1);
        grid.setVgap(3);

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.getStyleClass().add("capture-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setMinHeight(150); // the grid is the point of the app, never let the other cards squeeze it away
        VBox.setVgrow(scrollPane, Priority.ALWAYS); // Is the most important view of the app, so give priority

        // anything that changes which cells exist rebuilds the grid
        state.getCaptures().addListener((ListChangeListener<Capture>) change -> rebuildGrid());
        state.displayModeProperty().addListener((obs, oldVal, newVal) -> rebuildGrid());
        state.referenceCaptureProperty().addListener((obs, oldVal, newVal) -> rebuildGrid()); // every row's diff highlighting depends on it

        // anything that only changes how existing cells look just repaints them, so the current selection survives
        state.selectionStartProperty().addListener((obs, oldVal, newVal) -> applyCellStyles());
        state.selectionEndProperty().addListener((obs, oldVal, newVal) -> applyCellStyles());
        state.getViewedFields().addListener((ListChangeListener<Field>) change -> refreshFieldStyling());
        state.fieldsRevisionProperty().addListener((obs, oldVal, newVal) -> refreshFieldStyling());

        rebuildGrid();

        getChildren().addAll(titleRow, hint, selectionBar, scrollPane);
    }

    /**
     * Picks the message type new Fields are added to, and whose Fields the message type section shows.
     * Only selects among existing message types, creating one is done from the message type section.
     * The items are displayed through MessageType.toString().
     */
    private ComboBox<MessageType> buildMessageTypeComboBox() {
        ComboBox<MessageType> comboBox = new ComboBox<>(state.getMessageTypes());
        comboBox.setPromptText("Message type");
        comboBox.setPrefWidth(170);
        comboBox.valueProperty().bindBidirectional(state.viewedMessageTypeProperty());
        return comboBox;
    }

    /**
     * Rebuild the whole grid from scratch
     */
    private void rebuildGrid() {
        grid.getChildren().clear();
        columnHeaders.clear();
        columnCells.clear();
        state.clearSelection();

        if (state.getCaptures().isEmpty()) {
            Label empty = new Label("No captures yet. Add one above to start comparing packets.");
            empty.getStyleClass().add("empty-state");
            grid.add(empty, 0, 0);
            return;
        }

        int columnCount = computeMaxColumnCount();
        int charsPerByte = 8 / state.bitsPerColumn(); // 2 chars per byte in hex, 8 in binary, 1 in ascii

        Label cornerHeader = new Label("PACKET");
        cornerHeader.getStyleClass().add("grid-corner-header");
        grid.add(cornerHeader, 1, 0);

        // create the header for each data column
        for (int col = 0; col < columnCount; col++) {
            // only every few columns is labelled, otherwise the ruler is unreadable
            // the number shown is the bit offset, matching what the selection bar reports
            boolean isTick = col % tickInterval() == 0;
            Label header = new Label(isTick ? String.valueOf(col * state.bitsPerColumn()) : "");
            header.getStyleClass().add("col-header");
            if ((col + 1) % charsPerByte == 0)
                header.getStyleClass().add("byte-end");
            wireColumnSelection(header, col); // set event handler when clicking and dragging column header
            grid.add(header, col + 2, 0); // starts from the data column

            columnHeaders.add(header);
            columnCells.add(new ArrayList<>());
        }

        // the reference is what every other row is compared against, null only when there is no capture at all
        Capture reference = state.getReferenceCapture();
        String referenceDisplay = reference == null ? null : getDisplayValue(reference.getHexPacket());

        // create one row for each capture
        int row = 1;
        for (Capture capture : state.getCaptures()) {
            Button deleteButton = new Button("✕");
            deleteButton.getStyleClass().add("icon-button");
            deleteButton.setOnAction(e -> state.getCaptures().remove(capture));
            grid.add(deleteButton, 0, row);

            boolean isReference = state.isReference(capture);
            grid.add(buildNameCell(capture, isReference), 1, row);

            String display = getDisplayValue(capture.getHexPacket());
            for (int col = 0; col < columnCount; col++) {
                boolean hasValue = col < display.length();
                boolean isDiff = hasValue && differsFromReference(referenceDisplay, isReference, display, col);

                Label cell = new Label(hasValue ? String.valueOf(display.charAt(col)) : "·");
                cell.getStyleClass().add("data-cell");
                if ((col + 1) % charsPerByte == 0)
                    cell.getStyleClass().add("byte-end"); // small gap after each complete byte
                wireColumnSelection(cell, col);
                grid.add(cell, col + 2, row);
                columnCells.get(col).add(new DataCell(cell, isDiff, !hasValue));
            }
            row++;
        }

        refreshFieldStyling();
    }

    /**
     * Whether a given position of a capture should be flagged as differing from the reference capture.
     * Positions the reference does not reach are never flagged, there is nothing to compare them against.
     * @param referenceDisplay, the reference capture as displayed, null when there is no reference
     * @param isReference, whether the row being built is the reference itself (never compared against itself)
     * @param display, the current capture as displayed
     * @param col, the position being checked
     */
    private boolean differsFromReference(String referenceDisplay, boolean isReference, String display, int col) {
        if (isReference || referenceDisplay == null || col >= referenceDisplay.length())
            return false;

        return referenceDisplay.charAt(col) != display.charAt(col);
    }

    /**
     * Work out which Field covers each data column, then repaint. Called when the Fields themselves change,
     * which never changes the shape of the grid, only its colours, so the current selection is left alone.
     */
    private void refreshFieldStyling() {
        columnFields.clear();

        int bitsPerColumn = state.bitsPerColumn();
        for (int col = 0; col < columnCells.size(); col++) {
            int columnStartBit = col * bitsPerColumn;
            int columnEndBit = columnStartBit + bitsPerColumn - 1;
            columnFields.add(findFieldCovering(columnStartBit, columnEndBit));
        }

        applyCellStyles();
    }

    /**
     * The first Field of the viewed message type overlapping a bit range, null when none does.
     * A column belongs to a field as soon as they overlap at all, since one column can be narrower
     * than a field (a nibble in hex) or wider than one (a whole byte in ascii).
     */
    private Field findFieldCovering(int startBit, int endBit) {
        for (Field field : state.getViewedFields()) {
            if (field.getStartPosition() <= endBit && field.getEndPosition() >= startBit)
                return field;
        }
        return null;
    }

    /**
     * Repaint every header and data cell, combining the three things that can colour a cell:
     * which field it belongs to, whether it differs from the reference, and whether it is selected
     */
    private void applyCellStyles() {
        for (int col = 0; col < columnCells.size(); col++) {
            boolean selected = !state.isSelectionEmpty() && col >= state.getSelectionStart() && col <= state.getSelectionEnd();
            Field field = col < columnFields.size() ? columnFields.get(col) : null;

            styleHeader(columnHeaders.get(col), selected, field);
            for (DataCell cell : columnCells.get(col))
                styleDataCell(cell, selected, field);
        }
    }

    /**
     * The ruler follows the same colours as the data underneath it
     */
    private void styleHeader(Label header, boolean selected, Field field) {
        String textFill = MUTED_TEXT;
        if (selected)
            textFill = ACCENT_TEXT;
        else if (field != null)
            textFill = field.getColor();

        header.setStyle("-fx-text-fill: " + textFill + ";"
                + "-fx-font-weight: " + (selected ? "bold" : "normal") + ";");
    }

    /**
     * Decide the final look of one data cell.
     * The background says whether the cell is selected, otherwise which field it belongs to.
     * The text says whether it differs from the reference, otherwise which field it belongs to.
     * Keeping those on separate channels is what lets a cell stay readable as a difference even
     * while it is selected, and while it belongs to a coloured field.
     */
    private void styleDataCell(DataCell cell, boolean selected, Field field) {
        String background = "transparent";
        if (selected)
            background = SELECTED_BACKGROUND;
        else if (field != null)
            background = ColorUtils.toRgba(field.getColor(), FIELD_TINT_ALPHA);

        String borderColor = "transparent";
        String textFill;
        if (cell.diff()) {
            textFill = DIFF_TEXT;
            borderColor = DIFF_BORDER;
        } else if (cell.empty()) {
            textFill = EMPTY_TEXT;
        } else if (field != null) {
            textFill = field.getColor();
        } else if (selected) {
            textFill = ACCENT_TEXT;
        } else {
            textFill = DEFAULT_TEXT;
        }

        cell.label().setStyle("-fx-background-color: " + background + ";"
                + "-fx-text-fill: " + textFill + ";"
                + "-fx-border-color: " + borderColor + ";"
                + "-fx-font-weight: " + (selected || cell.diff() ? "bold" : "normal") + ";");
    }

    /**
     * Build the name cell of a capture row: a star to make it the reference, its name, and its size in bytes
     * @param capture, the capture the row belongs to
     * @param isReference, whether that capture is the current reference
     */
    private Node buildNameCell(Capture capture, boolean isReference) {
        Button star = new Button(isReference ? "★" : "☆");
        star.getStyleClass().add("star-button");
        if (isReference)
            star.getStyleClass().add("is-reference");
        star.setTooltip(new Tooltip(isReference ? "Reference capture" : "Compare the others against this capture"));
        star.setOnAction(e -> state.referenceCaptureProperty().set(capture));

        Label name = new Label(capture.getName());
        name.getStyleClass().add("capture-name");
        if (isReference)
            name.getStyleClass().add("is-reference");

        Label size = new Label(capture.getHexPacket().length() + "B");
        size.getStyleClass().add("capture-size");

        HBox nameCell = new HBox(star, name, size);
        nameCell.setAlignment(Pos.CENTER_LEFT);
        return nameCell;
    }

    /**
     * How often the ruler shows a bit offset, kept low enough to stay readable in every display mode
     */
    private int tickInterval() {
        return switch (state.getDisplayMode()) {
            case HEX -> 4;      // every 2 bytes
            case BINARY -> 8;   // every byte
            case ASCII -> 4;    // every 4 bytes
        };
    }

    /**
     * Compute the maximum capture size, in the current display mode
     */
    private int computeMaxColumnCount() {
        return state.getCaptures().stream()
                .mapToInt(c -> getDisplayValue(c.getHexPacket()).length())
                .max()
                .orElse(0);
    }

    /**
     * Transform a HexPacket into a String, based on the current display mode
     */
    private String getDisplayValue(HexPacket hexPacket) {
        return switch (state.getDisplayMode()) {
            case HEX -> hexPacket.toHexString();
            case BINARY -> hexPacket.toBinaryString(false);
            case ASCII -> hexPacket.toAsciiString();
        };
    }

    /**
     * Attach the click / press-and-drag column selection gesture to a header or a data cell Label
     * A single click selects just that column, dragging into another Label (header or cell, any row) extends the range
     */
    private void wireColumnSelection(Node node, int index) {
        node.setOnMousePressed(e -> {
            selectionAnchorIndex = index;
            state.setSelection(index, index);
        });
        node.setOnDragDetected(e -> node.startFullDrag());
        node.setOnMouseDragEntered(e -> state.setSelection(
                Math.min(selectionAnchorIndex, index),
                Math.max(selectionAnchorIndex, index)
        ));
    }
}
