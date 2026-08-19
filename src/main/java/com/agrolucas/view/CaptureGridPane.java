package com.agrolucas.view;

import com.agrolucas.model.Capture;
import com.agrolucas.model.FieldDisplay;
import com.agrolucas.model.HexPacket;
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

    private final CaptureState state;
    private final GridPane grid = new GridPane();
    // Contains for each column, the label of the column header and all the data label of the column
    // [ [Label("cap 1"), Label("A"), Label("B")], [Label("cap 2"), Label("C"), Label("D")], ... ]
    // This is used to update the style of the column when it is being selected
    private final List<List<Label>> dataColumnLabels = new ArrayList<>();
    private int selectionAnchorIndex = -1; // the column where a drag-select started

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
        HBox.setHgrow(spacer, Priority.ALWAYS); // pushes the display selector to the right edge of the card

        HBox titleRow = new HBox(10, sectionTitle, spacer, fieldDisplayComboBox);
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

        state.getCaptures().addListener((ListChangeListener<Capture>) change -> rebuildGrid());
        state.displayModeProperty().addListener((obs, oldVal, newVal) -> rebuildGrid());
        state.referenceCaptureProperty().addListener((obs, oldVal, newVal) -> rebuildGrid()); // every row's diff highlighting depends on it
        state.selectionStartProperty().addListener((obs, oldVal, newVal) -> updateColumnHighlight());
        state.selectionEndProperty().addListener((obs, oldVal, newVal) -> updateColumnHighlight());
        rebuildGrid();

        getChildren().addAll(titleRow, hint, selectionBar, scrollPane);
    }

    /**
     * Rebuild the whole grid from scratch
     */
    private void rebuildGrid() {
        grid.getChildren().clear();
        dataColumnLabels.clear();
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

            List<Label> labelsForColumn = new ArrayList<>();
            labelsForColumn.add(header);
            dataColumnLabels.add(labelsForColumn);
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

                Label cell = new Label(hasValue ? String.valueOf(display.charAt(col)) : "·");
                cell.getStyleClass().add("data-cell");
                if (!hasValue)
                    cell.getStyleClass().add("empty-cell"); // this capture is shorter than the longest one
                else if (differsFromReference(referenceDisplay, isReference, display, col))
                    cell.getStyleClass().add("diff");
                if ((col + 1) % charsPerByte == 0)
                    cell.getStyleClass().add("byte-end"); // small gap after each complete byte
                wireColumnSelection(cell, col);
                grid.add(cell, col + 2, row);
                dataColumnLabels.get(col).add(cell);
            }
            row++;
        }
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

    /**
     * Refresh the "selected-column" style class on every Label (header and data cells) of every selected data column
     * Allow the user to see what columns are selected
     */
    private void updateColumnHighlight() {
        for (int col = 0; col < dataColumnLabels.size(); col++) {
            boolean selected = !state.isSelectionEmpty() && col >= state.getSelectionStart() && col <= state.getSelectionEnd();
            for (Label label : dataColumnLabels.get(col)) {
                label.getStyleClass().remove("selected-column");
                if (selected)
                    label.getStyleClass().add("selected-column");
            }
        }
    }
}
