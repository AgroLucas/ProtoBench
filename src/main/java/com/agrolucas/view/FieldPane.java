package com.agrolucas.view;

import com.agrolucas.model.Field;
import com.agrolucas.model.FieldDisplay;
import com.agrolucas.model.MessageType;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

/**
 * Shows the Fields of whichever MessageType is currently selected, and lets some be deleted.
 * Also holds the ComboBox used to switch which MessageType is being viewed.
 */
public class FieldPane extends VBox {

    private final CaptureState state;

    public FieldPane(CaptureState state) {
        super(10);
        this.state = state;

        Label sectionTitle = new Label("Fields");

        ComboBox<MessageType> messageTypeComboBox = new ComboBox<>(state.getMessageTypes());
        messageTypeComboBox.setPromptText("Select a message type");
        messageTypeComboBox.valueProperty().bindBidirectional(state.viewedMessageTypeProperty());

        TableView<Field> fieldTableView = new TableView<>(state.getViewedFields());
        fieldTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fieldTableView.setPrefHeight(160); // secondary to the capture grid, which should get most of the window

        TableColumn<Field, String> fieldNameColumn = new TableColumn<>("Field name");
        fieldNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Field, Integer> startColumn = new TableColumn<>("Start");
        startColumn.setCellValueFactory(new PropertyValueFactory<>("startPosition"));

        TableColumn<Field, Integer> endColumn = new TableColumn<>("End");
        endColumn.setCellValueFactory(new PropertyValueFactory<>("endPosition"));

        TableColumn<Field, FieldDisplay> displayColumn = new TableColumn<>("Display");
        displayColumn.setCellValueFactory(new PropertyValueFactory<>("fieldDisplay"));

        fieldTableView.getColumns().addAll(fieldNameColumn, startColumn, endColumn, displayColumn);

        Button deleteFieldButton = new Button("Delete field");
        deleteFieldButton.setOnAction(e -> deleteFields(fieldTableView.getSelectionModel().getSelectedItems()));

        getChildren().addAll(sectionTitle, messageTypeComboBox, fieldTableView, deleteFieldButton);
    }

    private void deleteFields(ObservableList<Field> selectedItems) {
        MessageType selected = state.getViewedMessageType();
        if (selected != null)
            state.removeFields(selected, selectedItems);
    }
}
