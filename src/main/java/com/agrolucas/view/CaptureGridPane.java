package com.agrolucas.view;

import com.agrolucas.model.Capture;
import com.agrolucas.model.FieldDisplay;
import com.agrolucas.model.HexPacket;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
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
 *      The first row (starting from column 2) contains the packet index (from 0 to the last packet index).
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
        super(10);
        this.state = state;

        Label sectionTitle = new Label("List of capture");

        ComboBox<FieldDisplay> fieldDisplayComboBox = new ComboBox<>();
        fieldDisplayComboBox.getItems().addAll(FieldDisplay.values());
        fieldDisplayComboBox.valueProperty().bindBidirectional(state.displayModeProperty());

        SelectionBar selectionBar = new SelectionBar(state);

        grid.setHgap(6);
        grid.setVgap(4);

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS); // Is the most important view of the app, so give priority

        state.getCaptures().addListener((ListChangeListener<Capture>) change -> rebuildGrid());
        state.displayModeProperty().addListener((obs, oldVal, newVal) -> rebuildGrid());
        state.selectionStartProperty().addListener((obs, oldVal, newVal) -> updateColumnHighlight());
        state.selectionEndProperty().addListener((obs, oldVal, newVal) -> updateColumnHighlight());
        rebuildGrid();

        getChildren().addAll(sectionTitle, fieldDisplayComboBox, selectionBar, scrollPane);
    }

    /**
     * Rebuild the whole grid from scratch
     */
    private void rebuildGrid() {
        grid.getChildren().clear();
        dataColumnLabels.clear();
        state.clearSelection();

        int columnCount = computeMaxColumnCount();

        grid.add(new Label(), 1, 0); // The header for the capture name column

        // create the header for each data column
        for (int col = 0; col < columnCount; col++) {
            Label header = new Label(String.valueOf(col)); // column header is column index (e.g. "12")
            wireColumnSelection(header, col); // set event handler when clicking and dragging column header
            grid.add(header, col + 2, 0); // starts from the data column

            List<Label> labelsForColumn = new ArrayList<>();
            labelsForColumn.add(header);
            dataColumnLabels.add(labelsForColumn);
        }

        // create one row for each capture
        int row = 1;
        for (Capture capture : state.getCaptures()) {
            Button deleteButton = new Button("x");
            deleteButton.setOnAction(e -> state.getCaptures().remove(capture));
            grid.add(deleteButton, 0, row);

            grid.add(new Label(capture.getName()), 1, row);

            String display = getDisplayValue(capture.getHexPacket());
            for (int col = 0; col < columnCount; col++) {
                String charValue = col < display.length() ? String.valueOf(display.charAt(col)) : "";
                Label cell = new Label(charValue);
                wireColumnSelection(cell, col);
                grid.add(cell, col + 2, row);
                dataColumnLabels.get(col).add(cell);
            }
            row++;
        }
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
