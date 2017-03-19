package org.tvrenamer.controller;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.model.except.TVRenamerIOException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateChecker {
    private static Logger logger = Logger.getLogger(UpdateChecker.class.getName());

    /**
     * Checks if a newer version is available.
     *
     * @return the new version number as a string if available, empty string if no new version
     *         or null if an error has occurred
     */
    public static boolean isUpdateAvailable() {
        String latestVersion;
        try {
            latestVersion = new HttpConnectionHandler().downloadUrl(TVRENAMER_VERSION_URL);
        } catch (TVRenamerIOException e) {
            // Do nothing when an exception is thrown, just don't update display
            logger.log(Level.SEVERE, "Exception when downloading version file "
                       + TVRENAMER_VERSION_URL, e);
            return false;
        }

        boolean newVersionAvailable = latestVersion.compareToIgnoreCase(TVRENAMER_VERSION_URL) > 0;

        if (newVersionAvailable) {
            logger.info("There is a new version available, running " + TVRENAMER_VERSION_URL
                        + ", new version is " + latestVersion);
            return true;
        }
        return false;
    }
}
