package com.agrolucas.view;

import com.agrolucas.persistence.ProjectStorage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class MainView extends BorderPane {

    private final CaptureState state = new CaptureState();

    public MainView() {
        getStyleClass().add("main-view");
        setPadding(new Insets(18));

        VBox top = new VBox(16, buildHeader(), new CaptureInputPane(state));
        CaptureGridPane capturePane = new CaptureGridPane(state);
        FieldPane fieldPane = new FieldPane(state);

        // BorderPane has no spacing of its own, so the gaps between the three regions are set here
        BorderPane.setMargin(capturePane, new Insets(16, 0, 16, 0));

        setTop(top);
        setCenter(capturePane);
        setBottom(fieldPane);
    }

    public CaptureState getState() {
        return state;
    }

    /**
     * The application title block with the project actions, and a thin accent rule under it
     */
    private Node buildHeader() {
        Label eyebrow = new Label("// PROTOCOL REVERSE ENGINEERING BENCH");
        eyebrow.getStyleClass().add("app-eyebrow");

        Label title = new Label("ProtoBench");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Paste captured packets, line them up byte by byte, and map out the fields of each message type.");
        subtitle.getStyleClass().add("app-subtitle");

        VBox titleBlock = new VBox(4, eyebrow, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button importButton = new Button("Import project…");
        importButton.getStyleClass().add("ghost");
        importButton.setOnAction(e -> importProject());

        Button exportButton = new Button("Export project…");
        exportButton.getStyleClass().add("ghost");
        exportButton.setOnAction(e -> exportProject());

        HBox titleRow = new HBox(10, titleBlock, spacer, importButton, exportButton);
        titleRow.setAlignment(Pos.BOTTOM_RIGHT);

        Region rule = new Region();
        rule.getStyleClass().add("header-rule");

        VBox header = new VBox(4, titleRow, rule);
        VBox.setMargin(rule, new Insets(12, 0, 0, 0));
        return header;
    }

    private void importProject() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import project");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ProtoBench project", "*.json"));

        File chosen = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (chosen == null)
            return;

        try {
            state.loadProjectData(ProjectStorage.load(chosen.toPath()));
        } catch (IOException e) {
            showError("Could not import the project", e.getMessage());
        }
    }

    private void exportProject() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export project");
        chooser.setInitialFileName("protobench-project.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ProtoBench project", "*.json"));

        File chosen = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (chosen == null)
            return;

        try {
            ProjectStorage.save(chosen.toPath(), state.toProjectData());
        } catch (IOException e) {
            showError("Could not export the project", e.getMessage());
        }
    }

    /**
     * Load the project the app was last working on, when there is one.
     * A missing file is the normal first-run case, and a broken one must not stop the app from opening,
     * so both simply leave the app empty.
     */
    public void loadLastProject(Path file) {
        if (!java.nio.file.Files.exists(file))
            return;

        try {
            state.loadProjectData(ProjectStorage.load(file));
        } catch (IOException e) {
            System.err.println("Ignoring the last project, it could not be read: " + e.getMessage());
        }
    }

    /**
     * Remember the current project so the next launch reopens it
     */
    public void saveLastProject(Path file) {
        try {
            ProjectStorage.save(file, state.toProjectData());
        } catch (IOException e) {
            System.err.println("Could not save the last project: " + e.getMessage());
        }
    }

    private void showError(String header, String detail) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ProtoBench");
        alert.setHeaderText(header);
        alert.setContentText(detail);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("rules-dialog");
        alert.showAndWait();
    }
}
