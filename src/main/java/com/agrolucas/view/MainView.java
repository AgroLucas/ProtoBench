package com.agrolucas.view;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MainView extends BorderPane {

    public MainView() {
        getStyleClass().add("main-view");
        setPadding(new Insets(18));

        CaptureState state = new CaptureState();

        VBox top = new VBox(16, buildHeader(), new CaptureInputPane(state));
        CaptureGridPane capturePane = new CaptureGridPane(state);
        FieldPane fieldPane = new FieldPane(state);

        // BorderPane has no spacing of its own, so the gaps between the three regions are set here
        BorderPane.setMargin(capturePane, new Insets(16, 0, 16, 0));

        setTop(top);
        setCenter(capturePane);
        setBottom(fieldPane);
    }

    /**
     * The application title block, with a thin accent rule under it
     */
    private Node buildHeader() {
        Label eyebrow = new Label("// PROTOCOL REVERSE ENGINEERING BENCH");
        eyebrow.getStyleClass().add("app-eyebrow");

        Label title = new Label("ProtoBench");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Paste captured packets, line them up byte by byte, and map out the fields of each message type.");
        subtitle.getStyleClass().add("app-subtitle");

        Region rule = new Region();
        rule.getStyleClass().add("header-rule");

        VBox header = new VBox(4, eyebrow, title, subtitle, rule);
        VBox.setMargin(rule, new Insets(12, 0, 0, 0));
        return header;
    }
}
