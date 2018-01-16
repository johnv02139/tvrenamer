package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.UserPreferencesPersistence;
import org.tvrenamer.controller.util.FileUtilities;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserPreferences extends Observable {
    private static final Logger logger = Logger.getLogger(UserPreferences.class.getName());

    private static final UserPreferences INSTANCE = load();

    private final String preloadFolder;
    private transient Path destDirPath;
    private String destDir;
    private String seasonPrefix;
    private boolean seasonPrefixLeadingZero;
    private boolean moveSelected;
    private boolean renameSelected;
    private boolean removeEmptiedDirectories;
    private boolean deleteRowAfterMove;
    private String renameReplacementMask;
    private boolean checkForUpdates;
    private boolean recursivelyAddFolders;

    // For the ignore keywords, we do some processing.  So we also preserve exactly what the user specified.
    private transient String specifiedIgnoreKeywords;
    private final List<String> ignoreKeywords;

    private transient boolean destDirProblem = false;
    private transient String lastError = null;

    /**
     * UserPreferences constructor which uses the defaults from {@link org.tvrenamer.model.util.Constants}
     */
    private UserPreferences() {
        super();

        preloadFolder = null;
        destDirPath = DEFAULT_DESTINATION_DIRECTORY;
        destDir = destDirPath.toString();
        seasonPrefix = DEFAULT_SEASON_PREFIX;
        seasonPrefixLeadingZero = false;
        moveSelected = false;
        renameSelected = true;
        removeEmptiedDirectories = true;
        deleteRowAfterMove = false;
        renameReplacementMask = DEFAULT_REPLACEMENT_MASK;
        checkForUpdates = true;
        recursivelyAddFolders = true;
        ignoreKeywords = new ArrayList<>();
        ignoreKeywords.add(DEFAULT_IGNORED_KEYWORD);
        destDirProblem = false;
    }

    /**
     * @return the singleton UserPreferences instance for this application
     */
    public static UserPreferences getInstance() {
        return INSTANCE;
    }

    /**
     * Save preferences to xml file
     *
     * @param prefs the instance to export to XML
     */
    @SuppressWarnings("SameParameterValue")
    public static void store(UserPreferences prefs) {
        UserPreferencesPersistence.persist(prefs, PREFERENCES_FILE);
        logger.fine("Successfully saved/updated preferences");
    }

    /**
     * Deal with legacy files and set up
     */
    private static void initialize() {
        Path temp = null;
        logger.fine("configuration directory = " + CONFIGURATION_DIRECTORY.toAbsolutePath().toString());
        if (Files.exists(CONFIGURATION_DIRECTORY)) {
            // Older versions used the same name as a preferences file
            if (!Files.isDirectory(CONFIGURATION_DIRECTORY)) {
                try {
                    temp = Files.createTempDirectory(APPLICATION_NAME);
                } catch (Exception ioe) {
                    temp = null;
                }
                if ((temp == null) || Files.notExists(temp)) {
                    throw new RuntimeException("Could not create temp file");
                }
                try {
                    Files.delete(temp);
                    Files.move(CONFIGURATION_DIRECTORY, temp);
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }
        if (!FileUtilities.ensureWritableDirectory(CONFIGURATION_DIRECTORY)) {
            throw new RuntimeException("Could not create configuration directory");
        }
        if (temp != null) {
            try {
                Files.move(temp, PREFERENCES_FILE);
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                throw new RuntimeException("Could not rename old prefs file from "
                                           + temp + " to " + PREFERENCES_FILE);
            }
        }
        if (Files.exists(PREFERENCES_FILE_LEGACY)) {
            if (Files.exists(PREFERENCES_FILE)) {
                throw new RuntimeException("Found two legacy preferences files!!");
            } else {
                try {
                    Files.move(PREFERENCES_FILE_LEGACY, PREFERENCES_FILE);
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }
        FileUtilities.ensureWritableDirectory(LEGACY_TVDB_DIR);
    }

    /*
     * Clean and sanity-check a newly created instance.
     */
    private void sanitise() {
        buildIgnoredKeywordsString();
        if (!moveSelected && !renameSelected) {
            logger.warning("if move is not selected, then rename must be selected.");
            renameSelected = true;
            store(this);
        } else if (moveSelected) {
            ensureDestDir();
        }
    }

    /**
     * Load preferences from xml file
     *
     * @return an instance of UserPreferences, expected to be used as the singleton instance
     *         for the class
     */
    private static UserPreferences load() {
        initialize();

        // retrieve from file and update in-memory copy
        UserPreferences prefs = UserPreferencesPersistence.retrieve(PREFERENCES_FILE);

        if (prefs != null) {
            prefs.destDirPath = Paths.get(prefs.destDir);
            prefs.buildIgnoredKeywordsString();
            logger.finer("Successfully read preferences from: " + PREFERENCES_FILE.toAbsolutePath());
            logger.fine("Successfully read preferences: " + prefs.toString());
        } else {
            prefs = new UserPreferences();
        }
        prefs.sanitise();

        return prefs;
    }

    /**
     * Get the text of the last error.
     *
     * @return the text of the last error; null if there is no last error
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Clear the last error.
     *
     */
    public void clearLastError() {
        lastError = null;
    }

    /**
     * A private helper method we call for each preference that gets change.
     * When any attribute of this object changes, the object itself has changed.
     * Set the flag, notify the observers, and then clear the flag.
     *
     * @param preference the user preference that has changed
     */
    private void preferenceChanged(UserPreference preference) {
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
     * Gets the name of the directory to preload into the table.
     *
     * @return String naming the directory.
     */
    public String getPreloadFolder() {
        return preloadFolder;
    }

    /**
     * Create the directory if it doesn't exist and we need it.
     *
     * @return true if the destination directory exists -- at the time this method
     *              returns.  That is, it's true whether the directory was already
     *              there, or if we successfully created it.  Returns false if the
     *              directory does not exist and could not be created.
     */
    public boolean ensureDestDir() {
        if (!moveSelected) {
            // It doesn't matter if the directory exists or not if move is not selected.
            return true;
        }

        boolean canCreate = FileUtilities.checkForCreatableDirectory(destDirPath);
        destDirProblem = !canCreate;

        if (destDirProblem) {
            Path destPath = Paths.get(destDir);

            if (Files.exists(destPath)) {
                // destDir exists but is not a directory.
                lastError = DEST_NOT_DIR + ": '"
                    + destDir + "'. " + MOVE_NOT_POSSIBLE;
            } else {
                lastError = CANT_CREATE_DEST + ": '"
                    + destDir + "'. " + MOVE_NOT_POSSIBLE;
            }

            logger.warning(lastError);
        }

        return canCreate;
    }

    /**
     * Sets the directory to move renamed files to.  Must be an absolute path, and the entire path
     * will be created if it doesn't exist.
     *
     * @param dir the path to the directory
     */
    public void setDestinationDirectory(String dir) {
        // TODO: Our javadoc says it must be an absolute path, but how can we enforce that?
        // Should we create the path, convert it to absolute, then back to a String, and
        // then compare?  Also, what happens if ensureDestDir fails?
        if (valuesAreDifferent(destDir, dir)) {
            destDir = dir;
            destDirPath = Paths.get(destDir);

            preferenceChanged(UserPreference.DEST_DIR);
        }
    }

    /**
     * Gets the directory the user last chose to move renamed files to.
     *
     * Note that this returns a directory name even if "move" is disabled.
     * Therefore, this is NOT necessarily "where files should be moved to".
     * Callers need to check isMoveSelected() separately.
     *
     * @return name of the directory.
     */
    public String getDestinationDirectoryName() {
        // This method is called by the preferences dialog, to fill in the
        // field of the dialog.  If "move" is disabled, the dialog should
        // show this text greyed out, but it still needs to know what it
        // is, in order to disable it.
        return destDir;
    }

    /**
     * Gets the directory that files should be moved into; if "move" is
     * disabled, returns null.
     *
     * @return the directory if move is enabled, null if not.
     */
    public Path getDestinationDirectory() {
        if (moveSelected) {
            return destDirPath;
        } else {
            return null;
        }
    }

    /**
     * Sets whether or not we want the FileMover to move files to a destination directory
     *
     * @param moveSelected whether or not we want the FileMover to move files to a
     *           destination directory
     */
    public void setMoveSelected(boolean moveSelected) {
        if (valuesAreDifferent(this.moveSelected, moveSelected)) {
            this.moveSelected = moveSelected;
            ensureDestDir();
            preferenceChanged(UserPreference.MOVE_SELECTED);
        }
    }

    /**
     * Get whether or not the user has requested that the FileMover move files to
     * a destination directory.  This can be true even if the destination directory
     * is invalid.
     *
     * @return true if the user requested that the FileMover move files to a
     *    destination directory
     */
    public boolean isMoveSelected() {
        return moveSelected;
    }

    /**
     * Get whether or the FileMover should try to move files to a destination directory.
     * For this to be true, the following BOTH must be true:
     *  - the user has requested we move files
     *  - the user has supplied a valid place to move them to.
     *
     * @return true if the FileMover should try to move files to a destination directory.
     */
    public boolean isMoveEnabled() {
        return moveSelected && !destDirProblem;
    }

    /**
     * Sets whether or not we want the FileMover to rename files based on the show,
     * season, and episode we find.
     *
     * @param renameSelected whether or not we want the FileMover to rename files
     */
    public void setRenameSelected(boolean renameSelected) {
        if (valuesAreDifferent(this.renameSelected, renameSelected)) {
            this.renameSelected = renameSelected;

            preferenceChanged(UserPreference.RENAME_SELECTED);
        }
    }

    /**
     * Get whether or not we want the FileMover to rename files based on the show,
     * season, and episode we find.
     *
     * @return true if we want the FileMover to rename files
     */
    public boolean isRenameSelected() {
        return renameSelected;
    }

    /**
     * Sets whether or not we want the FileMover to delete directories when their last
     * remaining contents have been moved away.
     *
     * @param removeEmptiedDirectories whether or not we want the FileMover to delete
     *               directories when their last remaining contents have been moved away.
     */
    public void setRemoveEmptiedDirectories(boolean removeEmptiedDirectories) {
        if (valuesAreDifferent(this.removeEmptiedDirectories, removeEmptiedDirectories)) {
            this.removeEmptiedDirectories = removeEmptiedDirectories;

            preferenceChanged(UserPreference.REMOVE_EMPTY);
        }
    }

    /**
     * Get whether or not we want the FileMover to delete directories when their last
     * remaining contents have been moved away.
     *
     * @return true if we want the FileMover to delete directories when their last
     *         remaining contents have been moved away.
     */
    public boolean isRemoveEmptiedDirectories() {
        return removeEmptiedDirectories;
    }

    /**
     * Sets whether or not we want the UI to automatically delete rows after the
     * files have been successfully moved/renamed.
     *
     * @param deleteRowAfterMove whether or not we want the UI to automatically
     *     delete rows after the files have been successfully moved/renamed.
     */
    public void setDeleteRowAfterMove(boolean deleteRowAfterMove) {
        if (valuesAreDifferent(this.deleteRowAfterMove, deleteRowAfterMove)) {
            this.deleteRowAfterMove = deleteRowAfterMove;

            preferenceChanged(UserPreference.DELETE_ROWS);
        }
    }

    /**
     * Get whether or not we want the UI to automatically delete rows after the
     * files have been successfully moved/renamed.
     *
     * @return true if we want the UI to automatically delete rows after the
     *     files have been successfully moved/renamed.
     */
    public boolean isDeleteRowAfterMove() {
        return deleteRowAfterMove;
    }

    /**
     * Sets whether or not we want "Add Folder" to descend into subdirectories.
     *
     * @param recursivelyAddFolders whether or not we want "Add Folder" to descend
     *               into subdirectories.
     */
    public void setRecursivelyAddFolders(boolean recursivelyAddFolders) {
        if (valuesAreDifferent(this.recursivelyAddFolders, recursivelyAddFolders)) {
            this.recursivelyAddFolders = recursivelyAddFolders;

            preferenceChanged(UserPreference.ADD_SUBDIRS);
        }
    }

    /**
     * Get the status of recursively adding files within a directory
     *
     * @return true if we want "Add Folder" to descend into subdirectories,
     *         false if we want it to just consider the files at the top level of
     *               the folder
     */
    public boolean isRecursivelyAddFolders() {
        return recursivelyAddFolders;
    }

    /**
     * @return a list of strings that indicate that the presence of that string in
     *         a filename means that we should ignore that file
     */
    public List<String> getIgnoreKeywords() {
        return ignoreKeywords;
    }

    /**
     * @return a string containing the list of ignored keywords, separated by commas
     */
    public String getIgnoredKeywordsString() {
        return specifiedIgnoreKeywords;
    }

    /**
     * Turn the "ignore keywords" list into a String.  This is only necessary when we are restoring
     * the user preferences from XML.  When the keywords are modified by the user via the preferences
     * dialog, we maintain the actual string the user entered.
     *
     */
    private void buildIgnoredKeywordsString() {
        StringBuilder ignoreWords = new StringBuilder();
        String sep = "";
        for (String s : ignoreKeywords) {
            ignoreWords.append(sep);
            ignoreWords.append(s);
            sep = ",";
        }
        specifiedIgnoreKeywords = ignoreWords.toString();
    }

    /**
     * Sets the ignore keywords, given a string
     *
     * @param ignoreWordsString a string which, when parsed, indicate the files
     *           that should be ignored.  To be acceptable as an "ignore keyword",
     *           a string must be at least two characters long.
     */
    public void setIgnoreKeywords(String ignoreWordsString) {
        if (valuesAreDifferent(specifiedIgnoreKeywords, ignoreWordsString)) {
            specifiedIgnoreKeywords = ignoreWordsString;
            ignoreKeywords.clear();
            String[] ignoreWords = ignoreWordsString.split(IGNORE_WORDS_SPLIT_REGEX);
            for (String ignorable : ignoreWords) {
                // Be careful not to allow empty string as a "keyword."
                if (ignorable.length() > 1) {
                    // TODO: Convert commas into pipes for proper regex, remove periods
                    ignoreKeywords.add(ignorable);
                } else {
                    logger.warning("keywords to ignore must be at least two characters.");
                    logger.warning("not adding \"" + ignorable + "\"");
                }
            }

            // Technically, we could end up with an identical array of strings despite the
            // fact that the input was not precisely identical to the previous input.  But
            // not worth it to check.
            preferenceChanged(UserPreference.IGNORE_REGEX);
        }
    }

    /**
     * Sets the season prefix
     *
     * @param prefix the prefix for subfolders we would create to hold individual
     *         seasons of a show
     */
    public void setSeasonPrefix(String prefix) {
        if (valuesAreDifferent(seasonPrefix, prefix)) {
            seasonPrefix = prefix;

            preferenceChanged(UserPreference.SEASON_PREFIX);
        }
    }

    /**
     * @return the prefix for subfolders we would create to hold individual
     *         seasons of a show
     */
    public String getSeasonPrefix() {
        return seasonPrefix;
    }

    /**
     * Get whether or not we want the season subfolder to be numbered with a
     * leading zero.
     *
     * @return true if we want want the season subfolder to be numbered with
     *            a leading zero
     */
    public boolean isSeasonPrefixLeadingZero() {
        return seasonPrefixLeadingZero;
    }

    /**
     * Sets whether or not we want the season subfolder to be numbered with a
     * leading zero.
     *
     * @param seasonPrefixLeadingZero whether or not we want the season subfolder
     *               to be numbered with a leading zero
     */
    public void setSeasonPrefixLeadingZero(boolean seasonPrefixLeadingZero) {
        if (valuesAreDifferent(this.seasonPrefixLeadingZero, seasonPrefixLeadingZero)) {
            this.seasonPrefixLeadingZero = seasonPrefixLeadingZero;

            preferenceChanged(UserPreference.LEADING_ZERO);

        }
    }

    /**
     * Sets the rename replacement mask
     *
     * @param renameReplacementMask the rename replacement mask
     */
    public void setRenameReplacementString(String renameReplacementMask) {
        if (valuesAreDifferent(this.renameReplacementMask, renameReplacementMask)) {
            this.renameReplacementMask = renameReplacementMask;

            preferenceChanged(UserPreference.REPLACEMENT_MASK);
        }
    }

    /**
     * @return the rename replacement mask
     */
    public String getRenameReplacementString() {
        return renameReplacementMask;
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

            preferenceChanged(UserPreference.UPDATE_CHECK);
        }
    }

    /**
     * @return a string displaying attributes of this object
     */
    @Override
    public String toString() {
        return "UserPreferences\n [destDir=" + destDir + ",\n  seasonPrefix=" + seasonPrefix
            + ",\n  moveSelected=" + moveSelected + ",\n  renameSelected=" + renameSelected
            + ",\n  renameReplacementMask=" + renameReplacementMask
            + ",\n  checkForUpdates=" + checkForUpdates
            + ",\n  deleteRowAfterMove=" + deleteRowAfterMove
            + ",\n  setRecursivelyAddFolders=" + recursivelyAddFolders + "]";
    }

    public static class Builder {
        String destinationDirectory = null;
        String seasonPrefix = null;
        Boolean seasonPrefixLeadingZero = null;
        Boolean moveEnabled = null;
        Boolean renameEnabled = null;
        Boolean removeEmptiedDirectories = null;
        Boolean deleteRowAfterMove = null;
        String renameReplacementMask = null;
        Boolean checkForUpdates = null;
        Boolean recursivelyAddFolders = null;
        String ignoreKeywordsString = null;

        public Builder() {
        }

        public Builder destinationDirectory(String val) {
            if (destinationDirectory == null) {
                destinationDirectory = val;
            } else {
                throw new IllegalStateException("cannot re-set destinationDirectory");
            }
            return this;
        }

        public String getDestinationDirectory() {
            if (destinationDirectory == null) {
                return DEFAULT_DESTINATION_DIRECTORY.toString();
            } else {
                return destinationDirectory;
            }
        }

        public Builder seasonPrefix(String val) {
            if (seasonPrefix == null) {
                seasonPrefix = val;
            } else {
                throw new IllegalStateException("cannot re-set seasonPrefix");
            }
            return this;
        }

        public String getSeasonPrefix() {
            if (seasonPrefix == null) {
                return DEFAULT_SEASON_PREFIX;
            } else {
                return seasonPrefix;
            }
        }

        public Builder seasonPrefixLeadingZero(boolean val) {
            if (seasonPrefixLeadingZero == null) {
                seasonPrefixLeadingZero = val;
            } else {
                throw new IllegalStateException("cannot re-set seasonPrefixLeadingZero");
            }
            return this;
        }

        public boolean getSeasonPrefixLeadingZero() {
            if (seasonPrefixLeadingZero == null) {
                return false;
            } else {
                return seasonPrefixLeadingZero;
            }
        }

        public Builder moveEnabled(boolean val) {
            if (moveEnabled == null) {
                moveEnabled = val;
            } else {
                throw new IllegalStateException("cannot re-set moveEnabled");
            }
            return this;
        }

        public boolean getMoveEnabled() {
            if (moveEnabled == null) {
                return false;
            } else {
                return moveEnabled;
            }
        }

        public Builder renameEnabled(boolean val) {
            if (renameEnabled == null) {
                renameEnabled = val;
            } else {
                throw new IllegalStateException("cannot re-set renameEnabled");
            }
            return this;
        }

        public boolean getRenameEnabled() {
            if (renameEnabled == null) {
                return true;
            } else {
                return renameEnabled;
            }
        }

        public Builder removeEmptiedDirectories(boolean val) {
            if (removeEmptiedDirectories == null) {
                removeEmptiedDirectories = val;
            } else {
                throw new IllegalStateException("cannot re-set removeEmptiedDirectories");
            }
            return this;
        }

        public boolean getRemoveEmptiedDirectories() {
            if (removeEmptiedDirectories == null) {
                return true;
            } else {
                return removeEmptiedDirectories;
            }
        }

        public Builder deleteRowAfterMove(boolean val) {
            if (deleteRowAfterMove == null) {
                deleteRowAfterMove = val;
            } else {
                throw new IllegalStateException("cannot re-set deleteRowAfterMove");
            }
            return this;
        }

        public boolean getDeleteRowAfterMove() {
            if (deleteRowAfterMove == null) {
                return false;
            } else {
                return deleteRowAfterMove;
            }
        }

        public Builder renameReplacementMask(String val) {
            if (renameReplacementMask == null) {
                renameReplacementMask = val;
            } else {
                throw new IllegalStateException("cannot re-set renameReplacementMask");
            }
            return this;
        }

        public String getRenameReplacementMask() {
            if (renameReplacementMask == null) {
                return DEFAULT_REPLACEMENT_MASK;
            } else {
                return renameReplacementMask;
            }
        }

        public Builder checkForUpdates(boolean val) {
            if (checkForUpdates == null) {
                checkForUpdates = val;
            } else {
                throw new IllegalStateException("cannot re-set checkForUpdates");
            }
            return this;
        }

        public boolean getCheckForUpdates() {
            if (checkForUpdates == null) {
                return true;
            } else {
                return checkForUpdates;
            }
        }

        public Builder recursivelyAddFolders(boolean val) {
            if (recursivelyAddFolders == null) {
                recursivelyAddFolders = val;
            } else {
                throw new IllegalStateException("cannot re-set recursivelyAddFolders");
            }
            return this;
        }

        public boolean getRecursivelyAddFolders() {
            if (recursivelyAddFolders == null) {
                return true;
            } else {
                return recursivelyAddFolders;
            }
        }

        public Builder ignoreKeywordsString(String val) {
            if (ignoreKeywordsString == null) {
                ignoreKeywordsString = val;
            } else {
                throw new IllegalStateException("cannot re-set ignoreKeywordsString");
            }
            return this;
        }

        public String getIgnoreKeywordsString() {
            if (ignoreKeywordsString == null) {
                return DEFAULT_IGNORED_KEYWORD;
            } else {
                return ignoreKeywordsString;
            }
        }
    }

    /**
     * Use the Builder to set all the values of INSTANCE at once
     *
     * @param builder
     *   the Builder holding the values to slam in
     */
    public synchronized void slam(Builder builder) {
        // Update the preferences object from the UI control values
        setMoveSelected(builder.getMoveEnabled());
        setSeasonPrefix(builder.getSeasonPrefix());
        setSeasonPrefixLeadingZero(builder.getSeasonPrefixLeadingZero());
        setRenameReplacementString(builder.getRenameReplacementMask());
        setIgnoreKeywords(builder.getIgnoreKeywordsString());
        setRenameSelected(builder.getRenameEnabled());

        setCheckForUpdates(builder.getCheckForUpdates());
        setRecursivelyAddFolders(builder.getRecursivelyAddFolders());
        setRemoveEmptiedDirectories(builder.getRemoveEmptiedDirectories());
        setDeleteRowAfterMove(builder.getDeleteRowAfterMove());
        setDestinationDirectory(builder.getDestinationDirectory());

        UserPreferences.store(this);
    }
}
