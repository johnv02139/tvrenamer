package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.UserPreferencesChangeListener;
import org.tvrenamer.controller.UserPreferencesPersistence;
import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.except.TVRenamerIOException;
import org.tvrenamer.view.UIUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.logging.Logger;

public class UserPreferences extends Observable {
    private static Logger logger = Logger.getLogger(UserPreferences.class.getName());

    private File destDir;
    private File preloadFolder;
    private String seasonPrefix;
    private boolean seasonPrefixLeadingZero;
    private boolean moveEnabled;
    private boolean renameEnabled;
    private String renameReplacementMask;
    private ProxySettings proxy;
    private boolean checkForUpdates;
    private boolean recursivelyAddFolders;
    private List<String> ignoreKeywords;

    private static final UserPreferences INSTANCE = load();

    /**
     * UserPreferences constructor which uses the defaults from {@link Constants}
     */
    private UserPreferences() {
        super();

        this.destDir = DEFAULT_DESTINATION_DIRECTORY;
        this.preloadFolder = null;
        this.seasonPrefix = DEFAULT_SEASON_PREFIX;
        this.seasonPrefixLeadingZero = false;
        this.moveEnabled = false;
        this.renameEnabled = true;
        this.renameReplacementMask = DEFAULT_REPLACEMENT_MASK;
        this.proxy = new ProxySettings();
        this.checkForUpdates = true;
        this.recursivelyAddFolders = true;
        this.ignoreKeywords = new ArrayList<>();
        this.ignoreKeywords.add("sample");

        ensurePath();
    }

    public static UserPreferences getInstance() {
        return INSTANCE;
    }

    /**
     * Deal with legacy files and set up
     */
    public static void initialize() {
        File temp = null;
        logger.warning("configuration directory = "
                       + CONFIGURATION_DIRECTORY.getAbsolutePath());
        if (CONFIGURATION_DIRECTORY.exists()) {
            // Older versions used the same name as a preferences file
            if (!CONFIGURATION_DIRECTORY.isDirectory()) {
                try {
                    temp = File.createTempFile(APPLICATION_NAME, null, null);
                } catch (IOException ioe) {
                    temp = null;
                }
                if ((temp == null) || !temp.exists()) {
                    throw new RuntimeException("Could not create temp file");
                }
                temp.delete();
                CONFIGURATION_DIRECTORY.renameTo(temp);
            }
        }
        if (!CONFIGURATION_DIRECTORY.exists()) {
            boolean success = CONFIGURATION_DIRECTORY.mkdir();
            if (!success) {
                throw new RuntimeException("Could not create configuration directory");
            }
        }
        if (temp != null) {
            boolean success = temp.renameTo(PREFERENCES_FILE);
            if (!success) {
                throw new RuntimeException("Could not rename old prefs file from "
                                           + temp.getPath()
                                           + " to " + PREFERENCES_FILE.getPath());
            }
        }
        if (PREFERENCES_FILE_LEGACY.exists()) {
            if (PREFERENCES_FILE.exists()) {
                throw new RuntimeException("Found two legacy preferences files!!");
            } else {
                PREFERENCES_FILE_LEGACY.renameTo(PREFERENCES_FILE);
            }
        }
        if (OVERRIDES_FILE_LEGACY.exists()) {
            OVERRIDES_FILE_LEGACY.renameTo(OVERRIDES_FILE);
        } else if (!OVERRIDES_FILE.exists()) {
            // Previously the GlobalOverrides class was hard-coded to write some
            // overrides to the file.  I don't think that's right, but to try to
            // preserve the default behavior, if the user doesn't have any other
            // overrides file, we'll try to copy one from the source code into
            // place.  If it doesn't work, so be it.
            File defOver = new File(DEVELOPER_DEFAULT_OVERRIDES_FILENAME);
            if (defOver.exists()) {
                try {
                    Files.copy(defOver.toPath(), OVERRIDES_FILE.toPath());
                } catch (IOException ioe) {
                    logger.info("unable to copy default overrides file.");
                }
            }
        }
        if (!THETVDB_CACHE.exists()) {
            THETVDB_CACHE.mkdir();
        }
    }

    /**
     * Load preferences from xml file
     */
    public static UserPreferences load() {
        initialize();

        // retrieve from file and update in-memory copy
        UserPreferences prefs = UserPreferencesPersistence.retrieve(PREFERENCES_FILE);

        if (prefs != null) {
            logger.fine("Sucessfully read preferences from: " + PREFERENCES_FILE.getAbsolutePath());
            logger.fine("Sucessfully read preferences: " + prefs.toString());
        } else {
            prefs = new UserPreferences();
        }

        // apply the proxy configuration
        if (prefs.getProxy() != null) {
            prefs.getProxy().apply();
        }

        prefs.ensurePath();

        // add observer
        // TODO: why do we do this?
        prefs.addObserver(new UserPreferencesChangeListener());

        return prefs;
    }

    public static void store(UserPreferences prefs) {
        UserPreferencesPersistence.persist(prefs, PREFERENCES_FILE);
        logger.fine("Sucessfully saved/updated preferences");
    }

    private void preferenceChanged(UserPreference preference, Object newValue) {
        setChanged();
        notifyObservers(preference);
        clearChanged();
    }


    /**
     * Simply the complement of equals(), but with the specific purpose of detecting
     * if the value of a preference has been changed.
     *
     * @param originalValue the value of the UserPreference before the dialog was opened
     * @param newValue the value of the UserPreference as set in the dialog
     * @return true if the values are different
     */
    private boolean valuesAreDifferent(Object originalValue, Object newValue) {
        return !originalValue.equals(newValue);
    }

    /**
     * Sets the directory to move renamed files to. The entire path will be created if it doesn't exist.
     *
     * @param dir the directory to use as destination
     */
    public void setDestinationDirectory(File dir) throws TVRenamerIOException {
        if (valuesAreDifferent(this.destDir, dir)) {
            this.destDir = dir;
            ensurePath();

            preferenceChanged(UserPreference.DEST_DIR, dir);
        }
    }

    /**
     * Sets the directory to move renamed files to. Must be an absolute path, and the entire path will be created if it
     * doesn't exist.
     *
     * @param dir the path to the directory
     */
    public void setDestinationDirectory(String dir) throws TVRenamerIOException {
        if (valuesAreDifferent(this.destDir.getAbsolutePath(), dir)) {
            this.destDir = new File(dir);
            ensurePath();

            preferenceChanged(UserPreference.DEST_DIR, dir);
        }
    }

    /**
     * Gets the directory to preload into the table.
     *
     * @return File object representing the directory.
     */
    public File getPreloadFolder() {
        return this.preloadFolder;
    }

    /**
     * Gets the directory set to move renamed files to.
     *
     * @return File object representing the directory.
     */
    public File getDestinationDirectory() {
        return this.destDir;
    }

    public void setMoveEnabled(boolean moveEnabled) {
        if (valuesAreDifferent(this.moveEnabled, moveEnabled)) {
            this.moveEnabled = moveEnabled;

            preferenceChanged(UserPreference.MOVE_ENABLED, moveEnabled);
        }
    }

    /**
     * Get the status of of move support
     *
     * @return true if selected destination exists, false otherwise
     */
    public boolean isMoveEnabled() {
        return this.moveEnabled;
    }

    public void setRenameEnabled(boolean renameEnabled) {
        if (valuesAreDifferent(this.renameEnabled, renameEnabled)) {
            this.renameEnabled = renameEnabled;

            preferenceChanged(UserPreference.RENAME_ENABLED, renameEnabled);
        }
    }

    /**
     * Get the status of of rename support
     *
     * @return true if selected destination exists, false otherwise
     */
    public boolean isRenameEnabled() {
        return this.renameEnabled;
    }

    public void setRecursivelyAddFolders(boolean recursivelyAddFolders) {
        if (valuesAreDifferent(this.recursivelyAddFolders, recursivelyAddFolders)) {
            this.recursivelyAddFolders = recursivelyAddFolders;

            preferenceChanged(UserPreference.ADD_SUBDIRS, recursivelyAddFolders);
        }
    }

    /**
     * Get the status of recursively adding files within a directory
     *
     * @return true if adding subdirectories, false otherwise
     */
    public boolean isRecursivelyAddFolders() {
        return this.recursivelyAddFolders;
    }

    public void setIgnoreKeywords(List<String> ignoreKeywords) {
        if (valuesAreDifferent(this.ignoreKeywords, ignoreKeywords)) {
            this.ignoreKeywords.clear();
            for (String ignorable : ignoreKeywords) {
                // Be careful not to allow empty string as a "keyword."
                if (ignorable.length() > 1) {
                    // TODO: Convert commas into pipes for proper regex, remove periods
                    this.ignoreKeywords.add(ignorable);
                } else {
                    logger.warning("keywords to ignore must be at least two characters.");
                    logger.warning("not adding \"" + ignorable + "\"");
                }
            }

            preferenceChanged(UserPreference.IGNORE_REGEX, ignoreKeywords);
        }
    }

    public List<String> getIgnoreKeywords() {
        return this.ignoreKeywords;
    }

    public void setSeasonPrefix(String prefix) {
        // Remove the displayed "
        prefix = prefix.replaceAll("\"", "");

        if (valuesAreDifferent(this.seasonPrefix, prefix)) {
            this.seasonPrefix = StringUtils.sanitiseTitle(prefix);

            preferenceChanged(UserPreference.SEASON_PREFIX, prefix);
        }
    }

    public String getSeasonPrefix() {
        return this.seasonPrefix;
    }

    public String getSeasonPrefixForDisplay() {
        return ("\"" + this.seasonPrefix + "\"");
    }

    public boolean isSeasonPrefixLeadingZero() {
        return this.seasonPrefixLeadingZero;
    }

    public void setSeasonPrefixLeadingZero(boolean seasonPrefixLeadingZero) {
        if (valuesAreDifferent(this.seasonPrefixLeadingZero, seasonPrefixLeadingZero)) {
            this.seasonPrefixLeadingZero = seasonPrefixLeadingZero;

            preferenceChanged(UserPreference.LEADING_ZERO, seasonPrefixLeadingZero);

        }
    }

    public void setRenameReplacementString(String renameReplacementMask) {
        if (valuesAreDifferent(this.renameReplacementMask, renameReplacementMask)) {
            this.renameReplacementMask = renameReplacementMask;

            preferenceChanged(UserPreference.REPLACEMENT_MASK, renameReplacementMask);
        }
    }

    public String getRenameReplacementString() {
        return renameReplacementMask;
    }

    public ProxySettings getProxy() {
        return proxy;
    }

    public void setProxy(ProxySettings proxy) {
        if (valuesAreDifferent(this.proxy, proxy)) {
            this.proxy = proxy;
            proxy.apply();

            preferenceChanged(UserPreference.PROXY, proxy);
        }
    }

    /**
     * @return the checkForUpdates
     */
    public boolean checkForUpdates() {
        return checkForUpdates;
    }

    /**
     * @param checkForUpdates the checkForUpdates to set
     */
    public void setCheckForUpdates(boolean checkForUpdates) {
        if (valuesAreDifferent(this.checkForUpdates, checkForUpdates)) {
            this.checkForUpdates = checkForUpdates;

            preferenceChanged(UserPreference.UPDATE_CHECK, checkForUpdates);
        }
    }

    /**
     * Create the directory if it doesn't exist.
     */
    public void ensurePath() {
        if (this.moveEnabled && !this.destDir.mkdirs()) {
            if (!this.destDir.exists()) {
                this.moveEnabled = false;
                String message = "Couldn't create path: '" + this.destDir.getAbsolutePath() + "'. Move is now disabled";
                logger.warning(message);
                UIUtils.showMessageBox(SWTMessageBoxType.ERROR, "Error", message);
            }
        }
    }

    @Override
    public String toString() {
        return "UserPreferences [destDir=" + destDir + ", seasonPrefix=" + seasonPrefix
            + ", moveEnabled=" + moveEnabled + ", renameEnabled=" + renameEnabled
            + ", renameReplacementMask=" + renameReplacementMask + ", proxy=" + proxy
            + ", checkForUpdates=" + checkForUpdates + ", setRecursivelyAddFolders=" + recursivelyAddFolders + "]";
    }
}
