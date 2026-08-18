package com.agrolucas.view;

import com.agrolucas.model.Field;
import com.agrolucas.model.FieldDisplay;
import com.agrolucas.model.MessageType;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Shows the Fields of whichever MessageType is currently selected, and lets some be deleted.
 * Also holds the ComboBox used to switch which MessageType is being viewed.
 */
public class FieldPane extends VBox {

    private final CaptureState state;

    public FieldPane(CaptureState state) {
        super(12);
        this.state = state;
        getStyleClass().add("card");

        Label sectionTitle = new Label("MESSAGE TYPE FIELDS");
        sectionTitle.getStyleClass().add("card-title");

        ComboBox<MessageType> messageTypeComboBox = new ComboBox<>(state.getMessageTypes());
        messageTypeComboBox.setPromptText("Select a message type");
        messageTypeComboBox.setPrefWidth(210);
        messageTypeComboBox.valueProperty().bindBidirectional(state.viewedMessageTypeProperty());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // pushes the delete button to the right edge of the card

        TableView<Field> fieldTableView = new TableView<>(state.getViewedFields());
        fieldTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fieldTableView.setPrefHeight(150); // secondary to the capture grid, which should get most of the window
        fieldTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        fieldTableView.setPlaceholder(new Label("No fields yet. Select columns in the grid above to create one."));

        TableColumn<Field, String> fieldNameColumn = new TableColumn<>("FIELD NAME");
        fieldNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Field, Integer> startColumn = new TableColumn<>("START BIT");
        startColumn.setCellValueFactory(new PropertyValueFactory<>("startPosition"));
        startColumn.setMaxWidth(130);

        TableColumn<Field, Integer> endColumn = new TableColumn<>("END BIT");
        endColumn.setCellValueFactory(new PropertyValueFactory<>("endPosition"));
        endColumn.setMaxWidth(130);

        TableColumn<Field, FieldDisplay> displayColumn = new TableColumn<>("DISPLAY");
        displayColumn.setCellValueFactory(new PropertyValueFactory<>("fieldDisplay"));
        displayColumn.setMaxWidth(150);

        fieldTableView.getColumns().addAll(fieldNameColumn, startColumn, endColumn, displayColumn);

        Button deleteFieldButton = new Button("Delete selected");
        deleteFieldButton.getStyleClass().add("danger");
        deleteFieldButton.setOnAction(e -> deleteFields(fieldTableView.getSelectionModel().getSelectedItems()));

        HBox titleRow = new HBox(10, sectionTitle, messageTypeComboBox, spacer, deleteFieldButton);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(titleRow, fieldTableView);
    }

    private void deleteFields(ObservableList<Field> selectedItems) {
        MessageType selected = state.getViewedMessageType();
        if (selected != null)
            state.removeFields(selected, selectedItems);
    }
}
