package org.tvrenamer.controller;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.tvrenamer.model.TVRenamerIOException;
import org.tvrenamer.model.UserPreferences;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateChecker {
    private static final Logger logger = Logger.getLogger(UpdateChecker.class.getName());

    /**
     * Checks if a newer version is available.
     *
     * @return the new version number as a string if available, empty string if no new version
     *          or null if an error has occurred
     */
    public static boolean isUpdateAvailable() {
        String latestVersion;
        try {
            latestVersion = new HttpConnectionHandler().downloadUrl(TVRENAMER_VERSION_URL);
        } catch (TVRenamerIOException e) {
            // Do nothing when an exception is thrown, just don't update display
            logger.log(Level.SEVERE, "Exception when downloading version file " + TVRENAMER_VERSION_URL, e);
            return false;
        }

        boolean newVersionAvailable = latestVersion.compareToIgnoreCase(VERSION_NUMBER) > 0;

        if (newVersionAvailable) {
            logger.info("There is a new version available, running " + VERSION_NUMBER + ", new version is "
                + latestVersion);
            return true;
        }
        logger.finer("You have the latest version!");
        return false;
    }

    /**
     * Lets the UI update itself with information about a new version, if one is available
     *
     * @param display the display where the UI is running
     * @param control the control to make visible if an update is available
     */
    public static void checkForUpdates(final Display display, final Control control) {
        Thread updateCheckThread = new Thread(() -> {
            if (UserPreferences.getInstance().checkForUpdates()) {
                final boolean updatesAvailable = isUpdateAvailable();

                if (updatesAvailable) {
                    display.asyncExec(() -> control.setVisible(true));
                }
            }
        });
        updateCheckThread.start();
    }
}
