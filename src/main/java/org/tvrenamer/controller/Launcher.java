package org.tvrenamer.controller;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.model.ShowStore;
import org.tvrenamer.view.UIStarter;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

class Launcher {
    private static final Logger logger = Logger.getLogger(Launcher.class.getName());

    private static void initializeLogger() {
        // Find logging.properties file inside jar
        try (InputStream loggingConfigStream
             = Launcher.class.getResourceAsStream(LOGGING_PROPERTIES))
        {
            if (loggingConfigStream == null) {
                System.err.println("Warning: logging properties not found.");
            } else {
                LogManager.getLogManager().readConfiguration(loggingConfigStream);
            }
        } catch (IOException e) {
            System.err.println("Exception thrown while loading logging config");
            e.printStackTrace();
        }
    }

    /**
     * Shut down any threads that we know might be running.  Sadly hard-coded.
     */
    private static void tvRenamerThreadShutdown() {
        MoveRunner.shutDown();
        ShowStore.cleanUp();
        ListingsLookup.cleanUp();
    }

    /**
     * All this application does is run the UI, with no arguments.  Configuration
     * comes from {@link org.tvrenamer.model.util.Constants#PREFERENCES_FILE}.
     * But in the future, it might be able to do different things depending on
     * command-line arguments.
     *
     * @return the status we get back from {@link org.tvrenamer.view.UIStarter#run}
     */
    public static int launchUi() {
        logger.info("starting UI...");
        UIStarter ui = new UIStarter();
        return ui.run();
    }

    /**
     * Run a program; currently hard-coded to launchUi(), but can be expanded.
     *
     * @param args
     *    not actually processed, at this time
     */
    public static void main(String[] args) {
        initializeLogger();
        int status = launchUi();
        tvRenamerThreadShutdown();
        System.exit(status);
    }
}
