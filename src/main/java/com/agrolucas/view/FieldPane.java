package com.agrolucas.view;

import com.agrolucas.model.Capture;
import com.agrolucas.model.Field;
import com.agrolucas.model.FieldDecoder;
import com.agrolucas.model.FieldDisplay;
import com.agrolucas.model.HexPacket;
import com.agrolucas.model.MessageType;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Manages message types: creates and deletes them, and shows the Fields of whichever one is
 * selected in the capture section. Every Field can be edited in place or deleted.
 */
public class FieldPane extends VBox {

    private final CaptureState state;
    private final TableView<Field> fieldTableView;

    public FieldPane(CaptureState state) {
        super(12);
        this.state = state;
        getStyleClass().add("card");

        Label sectionTitle = new Label("MESSAGE TYPES");
        sectionTitle.getStyleClass().add("card-title");

        Label separator = new Label("›");
        separator.getStyleClass().add("inline-label");

        Label viewedTypeName = new Label();
        viewedTypeName.getStyleClass().add("viewed-type-name");

        fieldTableView = buildFieldTable();

        TextField newTypeNameTextField = new TextField();
        newTypeNameTextField.setPromptText("New message type…");
        newTypeNameTextField.setPrefWidth(160);
        newTypeNameTextField.setOnAction(e -> createMessageType(newTypeNameTextField)); // ENTER while typing the name

        Button createTypeButton = new Button("Create");
        createTypeButton.getStyleClass().add("ghost"); // quiet, it sits among the destructive actions
        createTypeButton.setTooltip(new Tooltip("Create a new message type and switch to it"));
        createTypeButton.setOnAction(e -> createMessageType(newTypeNameTextField));

        Button deleteFieldButton = new Button("Delete fields");
        deleteFieldButton.getStyleClass().add("danger");
        deleteFieldButton.setTooltip(new Tooltip("Delete the fields selected in the table below"));
        deleteFieldButton.setOnAction(e -> deleteFields(fieldTableView.getSelectionModel().getSelectedItems()));

        Button deleteTypeButton = new Button("Delete type");
        deleteTypeButton.getStyleClass().add("danger");
        deleteTypeButton.setTooltip(new Tooltip("Delete the message type being shown, along with its fields"));
        deleteTypeButton.setOnAction(e -> state.deleteMessageType(state.getViewedMessageType()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // both the shown name and whether the type may be deleted follow whichever message type is being viewed
        state.viewedMessageTypeProperty().addListener((obs, oldVal, newVal) -> refreshViewedType(viewedTypeName, deleteTypeButton));
        refreshViewedType(viewedTypeName, deleteTypeButton);

        HBox titleRow = new HBox(10, sectionTitle, separator, viewedTypeName, spacer,
                newTypeNameTextField, createTypeButton, deleteFieldButton, deleteTypeButton);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label hint = new Label("Double-click a cell to edit it. Switch between message types from the capture section above.");
        hint.getStyleClass().add("card-hint");

        getChildren().addAll(titleRow, hint, fieldTableView);
    }

    /**
     * Create the message type named in the given field and switch to it.
     * A name already in use simply switches to that existing message type instead of duplicating it.
     */
    private void createMessageType(TextField newTypeNameTextField) {
        MessageType created = state.findOrCreateMessageType(newTypeNameTextField.getText());
        if (created == null) // blank name
            return;

        state.viewedMessageTypeProperty().set(created);
        newTypeNameTextField.clear();
    }

    /**
     * Show the name of the message type being viewed, and only allow deleting it when it is not the default one
     */
    private void refreshViewedType(Label viewedTypeName, Button deleteTypeButton) {
        MessageType viewed = state.getViewedMessageType();
        viewedTypeName.setText(viewed == null ? "-" : viewed.getName());
        deleteTypeButton.setDisable(viewed == null || state.isDefaultMessageType(viewed));
    }

    /**
     * The table of Fields, every column editable in place
     */
    private TableView<Field> buildFieldTable() {
        TableView<Field> table = new TableView<>(state.getViewedFields());
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setPrefHeight(150); // secondary to the capture grid, which should get most of the window
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No fields yet. Select columns in the grid above to create one."));
        table.setEditable(true);

        TableColumn<Field, String> nameColumn = new TableColumn<>("FIELD NAME");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setOnEditCommit(event -> {
            String newName = event.getNewValue();
            if (newName != null && !newName.isBlank())
                event.getRowValue().setName(newName.trim());
            table.refresh(); // the model is a plain POJO, nothing notifies the table on its own
        });

        TableColumn<Field, Integer> startColumn = new TableColumn<>("START BIT");
        startColumn.setCellValueFactory(new PropertyValueFactory<>("startPosition"));
        startColumn.setCellFactory(TextFieldTableCell.forTableColumn(integerConverter()));
        startColumn.setMaxWidth(130);
        startColumn.setOnEditCommit(event -> {
            Integer newStart = event.getNewValue();
            Field field = event.getRowValue();
            if (newStart != null && newStart >= 0 && newStart <= field.getEndPosition())
                field.setStartPosition(newStart);
            table.refresh(); // also puts the old value back on screen when the entry was rejected
            state.notifyFieldsChanged(); // the field now covers different columns of the grid
        });

        TableColumn<Field, Integer> endColumn = new TableColumn<>("END BIT");
        endColumn.setCellValueFactory(new PropertyValueFactory<>("endPosition"));
        endColumn.setCellFactory(TextFieldTableCell.forTableColumn(integerConverter()));
        endColumn.setMaxWidth(130);
        endColumn.setOnEditCommit(event -> {
            Integer newEnd = event.getNewValue();
            Field field = event.getRowValue();
            if (newEnd != null && newEnd >= field.getStartPosition())
                field.setEndPosition(newEnd);
            table.refresh();
            state.notifyFieldsChanged(); // the field now covers different columns of the grid
        });

        TableColumn<Field, FieldDisplay> displayColumn = new TableColumn<>("DISPLAY");
        displayColumn.setCellValueFactory(new PropertyValueFactory<>("fieldDisplay"));
        displayColumn.setCellFactory(ComboBoxTableCell.forTableColumn(FieldDisplay.values()));
        displayColumn.setMaxWidth(150);
        displayColumn.setOnEditCommit(event -> {
            if (event.getNewValue() != null)
                event.getRowValue().setFieldDisplay(event.getNewValue());
            table.refresh();
            state.notifyFieldsChanged();
        });

        TableColumn<Field, Field> colorColumn = new TableColumn<>("COLOUR");
        // the whole row is handed to the cell, since the picker has to write straight back to the Field
        colorColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        colorColumn.setCellFactory(column -> new ColorPickerCell());
        colorColumn.setMaxWidth(120);
        colorColumn.setSortable(false);

        TableColumn<Field, Field> rulesColumn = new TableColumn<>("DECODE RULES");
        rulesColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        rulesColumn.setCellFactory(column -> new DecodeRulesCell());
        rulesColumn.setMaxWidth(190);
        rulesColumn.setSortable(false);

        TableColumn<Field, String> valueColumn = new TableColumn<>("VALUE IN REFERENCE");
        valueColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(describeValue(cellData.getValue())));
        valueColumn.setMaxWidth(230);
        valueColumn.setSortable(false);
        // the monospace font belongs on the cells only. Putting it on the column would style the
        // header too, leaving it in a different font from every other header.
        valueColumn.setCellFactory(column -> {
            TableCell<Field, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String value, boolean empty) {
                    super.updateItem(value, empty);
                    setText(empty ? null : value);
                }
            };
            cell.getStyleClass().add("value-cell");
            return cell;
        });

        table.getColumns().addAll(nameColumn, startColumn, endColumn, displayColumn, colorColumn, rulesColumn, valueColumn);

        // the decoded value is read off the reference capture, so it has to be recomputed whenever the
        // reference changes, the captures change, or any rule / position of a field is edited
        state.referenceCaptureProperty().addListener((obs, oldVal, newVal) -> table.refresh());
        state.getCaptures().addListener((ListChangeListener<Capture>) change -> table.refresh());
        state.fieldsRevisionProperty().addListener((obs, oldVal, newVal) -> table.refresh());

        return table;
    }

    /**
     * The value this Field holds in the reference capture, with the CRC verdict appended when it is a CRC field
     */
    private String describeValue(Field field) {
        Capture reference = state.getReferenceCapture();
        if (reference == null)
            return "no reference";

        HexPacket packet = reference.getHexPacket();
        if (!FieldDecoder.fitsInPacket(packet, field))
            return "-"; // the reference is too short to hold this field

        String value = FieldDecoder.formattedValue(packet, field);

        FieldDecoder.CrcCheck crcCheck = FieldDecoder.checkCrc(packet, field);
        if (crcCheck == null)
            return value;

        return crcCheck.valid()
                ? value + "  ✓ valid"
                : value + String.format("  ✗ expected 0x%X", crcCheck.expected());
    }

    /**
     * A table cell with a button opening the decode rules editor for its Field, and summarising
     * the rules currently on it
     */
    private class DecodeRulesCell extends TableCell<Field, Field> {

        private final Button editButton = new Button();

        private DecodeRulesCell() {
            editButton.getStyleClass().add("ghost");
            editButton.setOnAction(event -> openRulesDialog(getItem()));
        }

        @Override
        protected void updateItem(Field field, boolean empty) {
            super.updateItem(field, empty);

            if (empty || field == null) {
                setGraphic(null); // recycled cells must be cleared, otherwise buttons show on blank rows
                return;
            }

            editButton.setText(summarise(field));
            setGraphic(editButton);
        }

        private String summarise(Field field) {
            int ruleCount = field.getDecodeRules().size();
            if (ruleCount == 0)
                return "Add rules…";

            return field.hasCrc() && ruleCount == 1
                    ? "CRC…"
                    : ruleCount + " rule" + (ruleCount > 1 ? "s" : "") + "…";
        }
    }

    /**
     * Open the rules editor and refresh everything once it closes, since rules change both the
     * decoded values here and the CRC verdicts in the capture section
     */
    private void openRulesDialog(Field field) {
        if (field == null)
            return;

        new DecodeRulesDialog(field).showAndWait();
        state.notifyFieldsChanged();
        fieldTableView.refresh();
    }

    /**
     * A table cell holding a ColorPicker that writes the chosen colour straight onto its Field.
     * Unlike the other columns this needs no double-click to edit, the picker is always live,
     * which also makes each field's colour visible at a glance.
     */
    private class ColorPickerCell extends TableCell<Field, Field> {

        private final ColorPicker colorPicker = new ColorPicker();

        private ColorPickerCell() {
            colorPicker.getStyleClass().add("field-color-picker");
            colorPicker.setOnAction(event -> {
                Field field = getItem();
                if (field == null)
                    return;

                field.setColor(ColorUtils.toHex(colorPicker.getValue()));
                state.notifyFieldsChanged(); // repaints the capture grid with the new colour
            });
        }

        @Override
        protected void updateItem(Field field, boolean empty) {
            super.updateItem(field, empty);

            if (empty || field == null) {
                setGraphic(null); // recycled cells must be cleared, otherwise pickers show on blank rows
                return;
            }

            colorPicker.setValue(ColorUtils.parse(field.getColor()));
            setGraphic(colorPicker);
        }
    }

    /**
     * Converts the text typed into a bit position cell, yielding null when it is not a number.
     * The edit commit handlers treat null as "leave the value alone", so bad input is simply ignored.
     */
    private StringConverter<Integer> integerConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : value.toString();
            }

            @Override
            public Integer fromString(String text) {
                try {
                    return Integer.valueOf(text.trim());
                } catch (NumberFormatException | NullPointerException e) {
                    return null;
                }
            }
        };
    }

    private void deleteFields(ObservableList<Field> selectedItems) {
        MessageType selected = state.getViewedMessageType();
        if (selected != null)
            state.removeFields(selected, selectedItems);
    }
}
