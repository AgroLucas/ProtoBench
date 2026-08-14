package com.agrolucas.view;

import com.agrolucas.model.Capture;
import com.agrolucas.model.HexPacket;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class MainView extends BorderPane {

    private final ObservableList<Capture> captures = FXCollections.observableArrayList();
    private final List<TableColumn<Capture, String>> hexColumns = new ArrayList<>();
    private TableView<Capture> captureTableView;

    public MainView() {
        setPadding(new Insets(10));
        VBox captureInput = buildCaptureInput();
        VBox captureList = buildCaptureList();
        setTop(captureInput);
        setCenter(captureList);
    }


    // ============================================================
    // COMPONENTS BUILDING
    // ============================================================

    private VBox buildCaptureInput() {
        TextField captureNameTextField = new TextField();
        captureNameTextField.setPromptText("Enter a capture name");

        TextField captureHexTextField = new TextField();
        captureHexTextField.setPromptText("Enter the hexadecimal data of the capture");

        Label hint = new Label();

        Button addCaptureButton = new Button("Add");
        addCaptureButton.setOnAction(e -> addCapture(captureNameTextField, captureHexTextField, hint));

        HBox row1 = new HBox(10, captureNameTextField, captureHexTextField, addCaptureButton);

        return new VBox(10, row1, hint);
    }


    private VBox buildCaptureList() {
        Label sectionTitle = new Label("List of capture");

        Button deleteCaptureButton = new Button("Delete");

        captureTableView = new TableView<>(captures);
        captureTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<Capture, String> captureNameColumn = new TableColumn<>("Capture name");
        captureNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        captureTableView.getColumns().add(captureNameColumn);

        deleteCaptureButton.setOnAction(e -> deleteCapture(captureTableView.getSelectionModel().getSelectedItems()));
        captures.addListener((ListChangeListener<Capture>) change -> refreshHexColumns());
        refreshHexColumns();

        return new VBox(10, sectionTitle, deleteCaptureButton, captureTableView);
    }


    // ============================================================
    // EVENT HANDLING
    // ============================================================

    private void addCapture(TextField captureNameTextField, TextField captureHexTextField, Label hint) {
        String captureName = captureNameTextField.getText();
        String captureHex = captureHexTextField.getText();

        if (!isCorrectHexadecimal(captureHex)) {
            hint.setText("Enter a valid hexadecimal value");
            return;
        }

        if (captureName == null || captureName.isBlank())
            captureName = "Capture " + captures.size(); // we don't care if the name is not unique

        captures.add(new Capture(captureName, new HexPacket(captureHex)));
        captureNameTextField.setText(null);
        captureHexTextField.setText(null);
        hint.setText(null);
    }


    private void deleteCapture(ObservableList<Capture> selectedItems) {
        captures.removeAll(selectedItems);
    }


    private void refreshHexColumns() {
        int maxLength = captures.stream()
                .mapToInt(c -> c.getHexPacket().toHexString().length())
                .max()
                .orElse(0);

        if (maxLength == hexColumns.size()) // avoid rebuilding all the hex columns when the max hex size has not changed
            return;

        captureTableView.getColumns().removeAll(hexColumns);
        hexColumns.clear();

        for (int i = 0; i < maxLength; i++) {
            TableColumn<Capture, String> column = createHexColumn(i);
            hexColumns.add(column);
        }

        captureTableView.getColumns().addAll(hexColumns);
    }


    // ============================================================
    // HELPER FUNCTIONS
    // ============================================================

    private boolean isCorrectHexadecimal(String hex) {
        if (hex == null)
            return false;

        String stripped = hex.replaceAll("\\s+", ""); // Replace all white spaces

        if (stripped.isBlank())
            return false;

        return stripped.matches("^[0-9A-Fa-f]+$");
    }

    private static TableColumn<Capture, String> createHexColumn(int i) {
        int charIndex = i; // the lambda needs a final variable, the value of i changes so it will also change in the lambda
        TableColumn<Capture, String> column = new TableColumn<>(String.valueOf(i));
        column.setCellValueFactory(cellData -> {
            String hex = cellData.getValue().getHexPacket().toHexString();
            String value = charIndex < hex.length() ? String.valueOf(hex.charAt(charIndex)) : "";
            return new SimpleStringProperty(value);
        });
        return column;
    }

}
