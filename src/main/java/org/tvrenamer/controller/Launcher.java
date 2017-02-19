package org.tvrenamer.controller;

import org.tvrenamer.model.UserPreferences;
import org.tvrenamer.view.UIStarter;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Launcher {
    private static Logger logger = Logger.getLogger(Launcher.class.getName());

    // Static initalisation block
    static {
        // Find logging.properties file inside jar
        InputStream loggingConfigStream = Launcher.class.getResourceAsStream("/logging.properties");

        if (loggingConfigStream == null) {
            System.err.println("Warning: logging properties not found.");
        } else {
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
        // folder, when the UIStarter could do so itself, but the idea is that
        // really, loading the files and looking up the info should be outside
        // of the UI.  This is just a first tiny step in that direction.
        UIStarter ui = new UIStarter(prefs.getPreloadFolder());
        ui.run();
        // The application doesn't seem to exit without this line.  I assume
        // this means that there are other threads that have not been shut
        // down.  It would be better to shut them down explicitly and let
        // the program exit on its own.  TODO: investigate threads.
        System.exit(0);
    }
}
