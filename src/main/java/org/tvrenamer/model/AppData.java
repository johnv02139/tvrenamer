package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.AppDataPersistence;

import java.util.logging.Logger;

public class AppData {
    private static final Logger logger = Logger.getLogger(AppData.class.getName());

    private int heightHint = DEFAULT_TABLE_HEIGHT;
    private int widthChecked = SELECTED_COLUMN_DEFAULT_WIDTH;
    private int widthSource = SOURCE_COLUMN_DEFAULT_WIDTH;
    private int widthDest = DEST_COLUMN_DEFAULT_WIDTH;
    private int widthStatus = STATUS_COLUMN_DEFAULT_WIDTH;

    private static final AppData INSTANCE = load();

    /**
     * AppData constructor which uses the defaults from {@link org.tvrenamer.model.util.Constants}
     *
     * Prevent instantiation from outside the class.
     */
    private AppData() {
    }

    /**
     * @return the singleton AppData instance for this application
     */
    public static AppData getInstance() {
        return INSTANCE;
    }

    /**
     * Load app data from xml file
     *
     * @return an instance of AppData, expected to be used as the singleton instance
     *         for the class
     */
    private static AppData load() {
        // retrieve from file and update in-memory copy
        AppData data = AppDataPersistence.retrieve(APPDATA_FILE);

        if (data != null) {
            logger.fine("Successfully read app data from: " + APPDATA_FILE.toAbsolutePath());
            logger.finer("Successfully read app data: " + data.toString());
        } else {
            data = new AppData();
        }

        return data;
    }

    /**
     * Save app data to xml file
     *
     * @param data the instance to export to XML
     */
    public void store() {
        AppDataPersistence.persist(this, APPDATA_FILE);
        logger.fine("Successfully saved/updated app data");
    }

    /**
     * Gets the preferred height of the results table
     *
     * @return int giving the preferred height
     */
    public int getHeightHint() {
        return heightHint;
    }

    /**
     * Gets the preferred width of the "Active" column
     *
     * @return int giving the preferred width
     */
    public int getWidthChecked() {
        return widthChecked;
    }

    /**
     * Gets the preferred width of the "source" column
     *
     * @return int giving the preferred width
     */
    public int getWidthSource() {
        return widthSource;
    }

    /**
     * Gets the preferred width of the "dest" column
     *
     * @return int giving the preferred width
     */
    public int getWidthDest() {
        return widthDest;
    }

    /**
     * Gets the preferred width of the "status" column
     *
     * @return int giving the preferred width
     */
    public int getWidthStatus() {
        return widthStatus;
    }

    /**
     * @return a string displaying attributes of this object
     */
    @Override
    public String toString() {
        return "AppData["
            + "\n heightHint=" + heightHint
            + "\n widthChecked=" + widthChecked
            + "\n widthSource=" + widthSource
            + "\n widthDest=" + widthDest
            + "\n widthStatus=" + widthStatus
            + "]";
    }
}
