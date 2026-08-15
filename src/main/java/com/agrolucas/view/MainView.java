package com.agrolucas.view;

import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;

public class MainView extends BorderPane {

    public MainView() {
        setPadding(new Insets(10));

        CaptureState state = new CaptureState();

        setTop(new CaptureInputPane(state));
        setCenter(new CaptureGridPane(state));
        setBottom(new FieldPane(state));
    }
}
