package com.agrolucas.persistence;

import java.nio.file.Path;

/**
 * Where the application keeps its own files.
 * <p>
 * Under the user's home directory rather than next to the jar, so it works the same whether the app
 * is started from the IDE or from a jar sitting in a folder the user may not be able to write to.
 */
public final class ProjectPaths {

    private ProjectPaths() {
    }

    public static Path applicationDirectory() {
        return Path.of(System.getProperty("user.home"), ".protobench");
    }

    /**
     * The project reloaded at startup, written automatically when the app closes
     */
    public static Path lastProjectFile() {
        return applicationDirectory().resolve("last-project.json");
    }
}
