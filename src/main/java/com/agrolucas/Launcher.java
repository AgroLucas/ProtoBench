package com.agrolucas;

import javafx.application.Application;

/**
 * Entry point of the runnable jar.
 * <p>
 * It exists only so that the jar's main class is <b>not</b> a subclass of {@link Application}.
 * When the main class extends Application, the JavaFX runtime insists on being loaded from the
 * module path and refuses to start with "JavaFX runtime components are missing", which is exactly
 * what happens inside a single shaded jar. Going through a plain class sidesteps that check.
 */
public class Launcher {

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
