package org.tvrenamer.controller;

import org.tvrenamer.model.UserPreferences;
import org.tvrenamer.view.ResultsTable;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Launcher {
    private static Logger logger = Logger.getLogger(Launcher.class.getName());

    // Static initalisation block
    static {
        // Find logging.properties file inside jar
        InputStream loggingConfigStream = Launcher.class.getResourceAsStream("/etc/logging.properties");

        if (loggingConfigStream != null) {
            try {
                LogManager.getLogManager().readConfiguration(loggingConfigStream);
            } catch (IOException e) {
                System.err.println("Exception thrown while loading logging config");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        UserPreferences prefs = UserPreferences.getInstance();

        // It doesn't make much sense for the launcher to look up the preload
        // folder, when the ResultsTable could do so itself, but the idea is that
        // really, loading the files and looking up the info should be outside
        // of the UI.  This is just a first tiny step in that direction.
        ResultsTable ui = new ResultsTable(prefs.getPreloadFolder());
        ui.run();
    }
}
