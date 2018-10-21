package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.UpdateListener;
import org.tvrenamer.controller.UserPreferencesPersistence;
import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.view.UIStarter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.logging.Logger;

public class UserPreferences extends Observable {
    private static Logger logger = Logger.getLogger(UserPreferences.class.getName());

    private File doneDir;
    private File preloadFolder;
    private String seasonPrefix;
    private boolean seasonPrefixLeadingZero;
    private boolean moveEnabled;
    private String renameReplacementMask;
    private ProxySettings proxy;
    private boolean recursivelyAddFolders;
    private List<String> ignoreKeywords;

    private static final UserPreferences INSTANCE = load();

    /**
     * UserPreferences constructor which uses the defaults from {@link Constants}
     */
    private UserPreferences() {
        super();

        this.doneDir = DEFAULT_DESTINATION_DIRECTORY;
        this.preloadFolder = null;
        this.seasonPrefix = DEFAULT_SEASON_PREFIX;
        this.seasonPrefixLeadingZero = false;
        this.moveEnabled = false;
        this.renameReplacementMask = DEFAULT_REPLACEMENT_MASK;
        this.proxy = new ProxySettings();
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
            logger.finer("Sucessfully read preferences from: " + PREFERENCES_FILE.getAbsolutePath());
            // logger.info("Sucessfully read preferences: " + prefs.toString());
        } else {
            prefs = new UserPreferences();
        }

        // apply the proxy configuration
        if (prefs.getProxy() != null) {
            prefs.getProxy().apply();
        }

        prefs.ensurePath();

        // add observer
        prefs.addObserver(new UpdateListener());

        return prefs;
    }

    public static void store(UserPreferences prefs) {
        UserPreferencesPersistence.persist(prefs, PREFERENCES_FILE);
        logger.fine("Sucessfully saved/updated preferences");
    }

    private void preferenceChanged(UserPreference preference, Object newValue) {
        notifyObservers(preference);
    }

    /**
     * Sets the directory to move renamed files to. The entire path will be created if it doesn't exist.
     *
     * @param dir the directory to use as destination
     */
    public void setDestinationDirectory(File dir) throws GenericException {
        if (hasChanged(this.doneDir, dir)) {
            this.doneDir = dir;
            ensurePath();

            setChanged();
            preferenceChanged(UserPreference.DEST_DIR, dir);
        }
    }

    /**
     * Sets the directory to move renamed files to. Must be an absolute path, and the entire path will be created if it
     * doesn't exist.
     *
     * @param dir the path to the directory
     */
    public void setDestinationDirectory(String dir) throws GenericException {
        if (hasChanged(this.doneDir.getAbsolutePath(), dir)) {
            this.doneDir = new File(dir);
            ensurePath();

            setChanged();
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
        return this.doneDir;
    }

    public void setMoveEnabled(boolean moveEnabled) {
        if (hasChanged(this.moveEnabled, moveEnabled)) {
            this.moveEnabled = moveEnabled;

            setChanged();
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

    public void setRecursivelyAddFolders(boolean recursivelyAddFolders) {
        if (hasChanged(this.recursivelyAddFolders, recursivelyAddFolders)) {
            this.recursivelyAddFolders = recursivelyAddFolders;

            setChanged();
            preferenceChanged(UserPreference.ADD_SUBDIRS,
                                 recursivelyAddFolders);
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
        if (hasChanged(this.ignoreKeywords, ignoreKeywords)) {
            // Convert commas into pipes for proper regex, remove periods
            this.ignoreKeywords = ignoreKeywords;

            setChanged();
            preferenceChanged(UserPreference.IGNORE_REGEX, ignoreKeywords);

        }
    }

    public List<String> getIgnoreKeywords() {
        return this.ignoreKeywords;
    }

    public void setSeasonPrefix(String prefix) {
        // Remove the displayed "
        prefix = prefix.replaceAll("\"", "");

        if (hasChanged(this.seasonPrefix, prefix)) {
            this.seasonPrefix = StringUtils.sanitiseTitle(prefix);

            setChanged();
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
        if (hasChanged(this.seasonPrefixLeadingZero, seasonPrefixLeadingZero)) {
            this.seasonPrefixLeadingZero = seasonPrefixLeadingZero;

            setChanged();
            preferenceChanged(UserPreference.LEADING_ZERO,
                                 seasonPrefixLeadingZero);

        }
    }

    public void setRenameReplacementString(String renameReplacementMask) {
        if (hasChanged(this.renameReplacementMask, renameReplacementMask)) {
            this.renameReplacementMask = renameReplacementMask;

            setChanged();
            preferenceChanged(UserPreference.REPLACEMENT_MASK,
                                 renameReplacementMask);
        }
    }

    public String getRenameReplacementString() {
        return renameReplacementMask;
    }

    public ProxySettings getProxy() {
        return proxy;
    }

    public void setProxy(ProxySettings proxy) {
        if (hasChanged(this.proxy, proxy)) {
            this.proxy = proxy;
            proxy.apply();

            setChanged();
            preferenceChanged(UserPreference.PROXY, proxy);
        }
    }

    /**
     * Create the directory if it doesn't exist.
     */
    public void ensurePath() {
        if (this.moveEnabled && !this.doneDir.mkdirs()) {
            if (!this.doneDir.exists()) {
                this.moveEnabled = false;
                String message = "Couldn't create path: '"
                    + this.doneDir.getAbsolutePath()
                    + "'. Move is now disabled";
                logger.warning(message);
                UIStarter.showMessageBox(SWTMessageBoxType.ERROR, "Error", message);
            }
        }
    }

    @Override
    public String toString() {
        return "UserPreferences [doneDir="
                + doneDir
                + ", seasonPrefix="
                + seasonPrefix
                + ", moveEnabled="
                + moveEnabled
                + ", renameReplacementMask="
                + renameReplacementMask
                + ", proxy="
                + proxy
                + ", setRecursivelyAddFolders="
                + recursivelyAddFolders
                + "]";
    }

    private boolean hasChanged(Object originalValue, Object newValue) {
        return !originalValue.equals(newValue);
    }
}
