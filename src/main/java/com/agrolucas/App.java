package com.agrolucas;

import com.agrolucas.persistence.ProjectPaths;
import com.agrolucas.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private MainView mainView;

    @Override
    public void start(Stage stage) {
        mainView = new MainView();

        Scene scene = new Scene(mainView, 1280, 840);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("ProtoBench");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();

        // reopen whatever was being worked on last time, staying empty when there is nothing to reopen
        mainView.loadLastProject(ProjectPaths.lastProjectFile());
    }

    /**
     * Called by JavaFX as the application closes, which is where the current project is remembered
     */
    @Override
    public void stop() {
        if (mainView != null)
            mainView.saveLastProject(ProjectPaths.lastProjectFile());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
