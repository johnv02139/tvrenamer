package org.tvrenamer.controller;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.util.FileUtilities;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.ProgressObserver;
import org.tvrenamer.model.UserPreferences;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileMover implements Callable<Boolean> {
    static final Logger logger = Logger.getLogger(FileMover.class.getName());
    static final UserPreferences userPrefs = UserPreferences.getInstance();

    private final FileEpisode episode;
    private final Path destRoot;
    private final String destBasename;
    private final String destSuffix;
    private ProgressObserver observer = null;
    Integer destIndex = null;

    /**
     * Constructs a FileMover to move the given episode.
     *
     * @param episode
     *    the FileEpisode we intend to move
     */
    public FileMover(FileEpisode episode) {
        this.episode = episode;

        destRoot = episode.getMoveToPath();
        destBasename = episode.getDestinationBasename();
        destSuffix = episode.getFilenameSuffix();
    }

    /**
     * Sets the progress observer for this FileMover
     *
     * @param observer
     *   the observer to add
     */
    public void addObserver(ProgressObserver observer) {
        this.observer = observer;
    }

    /**
     * Gets the current location of the file to be moved
     *
     * @return the Path where the file is currently located
     */
    Path getCurrentPath() {
        return episode.getPath();
    }

    /**
     * Gets the size (in bytes) of the file to be moved
     *
     * @return the size of the file
     */
    long getFileSize() {
        return episode.getFileSize();
    }

    /**
     * The filename of the destination we want to move the file to.
     * We may not be able to actually use this filename due to a conflict,
     * in which case, we will probably add an index and use a subdirectory.
     * But this is the name we WANT to change it to.
     *
     * @return the filename that we want to move the file to
     */
    String getDesiredDestName() {
        return destBasename + destSuffix;
    }

    /**
     * Gets the name of the directory we should move the file to, as a string.
     *
     * We call it the "moveToDirectory" because "destinationDirectory" is used more
     * to refer to the top-level directory: the one the user specified in the dialog
     * for user preferences.  This is the subdirectory of that folder that the file
     * should actually be placed in.
     *
     * @return the name of the directory we should move the file to, as a string.
     */
    String getMoveToDirectory() {
        return destRoot.toString();
    }

    /**
     * Try to clean up, and log errors, after a move attempt has failed.
     * This is more likely to be useful in the case where the "move" was
     * actually a copy-and-delete, but it's possible a straightforward
     * move also could have partially completed before failing.
     *
     * @param source
     *            The source file we had wanted to move.
     * @param dest
     *            The destination where we tried to move the file.
     *
     */
    private void failToCopy(final Path source, final Path dest) {
        if (Files.exists(dest)) {
            // An incomplete copy was done.  Try to clean it up.
            FileUtilities.deleteFile(dest);
            if (Files.exists(dest)) {
                logger.warning("made incomplete copy of \"" + source
                               + "\" to \"" + dest
                               + "\" and could not clean it up");
                return;
            }
        }
        logger.warning("failed to move " + source);
        if (Files.notExists(dest)) {
            Path outDir = userPrefs.getDestinationDirectory();
            if (outDir != null) {
                Path parent = dest.getParent();
                while ((parent != null)
                       && !FileUtilities.isSameFile(parent, outDir))
                {
                    boolean rmdired = FileUtilities.rmdir(parent);
                    if (rmdired) {
                        logger.info("removing empty directory " + parent);
                    } else {
                        break;
                    }
                    parent = parent.getParent();
                }
            }
        }
    }

    /**
     * Copies the source file to the destination, and deletes the source.
     *
     * <p>If the destination cannot be created or is a read-only file, the
     * method returns <code>false</code>.  Otherwise, the contents of the
     * source are copied to the destination, the source is deleted, and
     * <code>true</code> is returned.
     *
     * @param source
     *            The source file to move.
     * @param dest
     *            The destination where to move the file.
     * @return true on success, false otherwise.
     *
     */
    private boolean copyAndDelete(final Path source, final Path dest) {
        if (observer != null) {
            observer.initializeProgress(episode.getFileSize());
        }
        boolean ok = FileUtilities.copyWithUpdates(source, dest, observer);
        if (ok) {
            ok = FileUtilities.deleteFile(source);
        } else {
            failToCopy(source, dest);
        }
        return ok;
    }

    private boolean finishMove(final Path actualDest) {
        // TODO: why do we set the file modification time to "now"?  Would like to
        // at least make this behavior configurable.
        try {
            FileTime now = FileTime.fromMillis(System.currentTimeMillis());
            Files.setLastModifiedTime(actualDest, now);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Unable to set modification time " + actualDest, ioe);
            // Well, the file got moved to the right place already.  One could argue
            // for returning true.  But, true is only if *everything* worked.
            return false;
        }

        return true;
    }

    /**
     * Execute the file move action.  This method assumes that all sanity checks have been
     * completed and that everything is ready to go: source file and destination directory
     * exist, destination file doesn't, etc.
     *
     * At the end, if the move was successful, it sets the file modification time.
     *
     * @param srcPath
     *    the Path to the file to be moved
     * @param destPath
     *    the Path to which the file should be moved
     * @param tryRename
     *    if false, do not try to simply rename the file; always do a "copy-and-delete"
     * @return true on complete success, false otherwise.
     */
    private boolean doActualMove(final Path srcPath, final Path destPath, final boolean tryRename) {
        logger.fine("Going to move\n  '" + srcPath + "'\n  '" + destPath + "'");
        Path actualDest = destPath;
        if (tryRename) {
            actualDest = FileUtilities.renameFile(srcPath, destPath);
            if (actualDest == null) {
                logger.severe("Unable to move " + srcPath);
                failToCopy(srcPath, destPath);
                return false;
            }
        } else {
            logger.info("different disks: " + srcPath + " and " + destPath);
            boolean success = copyAndDelete(srcPath, destPath);
            if (!success) {
                return false;
            }
        }
        episode.setPath(actualDest);
        boolean same = destPath.equals(actualDest);
        if (!same) {
            logger.warning("actual destination did not match intended:\n  "
                           + actualDest + "\n  " + destPath);
            return false;
        }

        return finishMove(actualDest);
    }

    /**
     * Execute the move using real paths.  Also does side-effects, like
     * updating the FileEpisode.
     *
     * @param realSrc
     *    the "real" Path of the source file to be moved
     * @param destPath
     *    the "real" destination where the file should be moved; can contain
     *    non-existent directories, which will be created
     * @param destDir
     *    an existent ancestor of destPath
     * @return true on success, false otherwise.
     */
    private boolean tryToMoveRealPaths(Path realSrc, Path destPath, Path destDir) {
        boolean tryRename = FileUtilities.areSameDisk(realSrc, destDir);
        Path srcDir = realSrc.getParent();

        episode.setMoving();
        boolean success = doActualMove(realSrc, destPath, tryRename);
        if (observer != null) {
            observer.finishProgress(success);
        }
        if (!success) {
            logger.info("failed to move " + realSrc);
            return false;
        }

        logger.info("successful:\n  " + realSrc + "\n  " + destPath);
        if (userPrefs.isRemoveEmptiedDirectories()) {
            FileUtilities.removeWhileEmpty(srcDir);
        }
        return true;
    }

    /**
     * Add a version string to the destination filename.
     *
     * @return destination filename with a version added
     */
    private String addVersionString() {
        return destBasename + " (" + destIndex + ")" + destSuffix;
    }

    /**
     * Check/verify numerous things, and if everything is as it should be,
     * execute the move.
     *
     * This sanity-checks the move: the source file must exist, the destination
     * file should not, etc.  It may actually change things, e.g., if the
     * destination directory doesn't exist, it will try to create it.  It also
     * gathers information, like whether the source and destination are on the
     * same file store.  And it does side-effects, like updating the FileEpisode.
     *
     * @return true on success, false otherwise.
     */
    private boolean tryToMoveFile() {
        Path srcPath = episode.getPath();
        if (Files.notExists(srcPath)) {
            logger.info("Path no longer exists: " + srcPath);
            episode.setDoesNotExist();
            return false;
        }
        Path realSrc;
        try {
            realSrc = srcPath.toRealPath();
        } catch (IOException ioe) {
            logger.warning("could not get real path of " + srcPath);
            return false;
        }
        Path destDir = destRoot;
        String filename = destBasename + destSuffix;
        if (destIndex != null) {
            if (userPrefs.isMoveEnabled()) {
                destDir = destRoot.resolve(DUPLICATES_DIRECTORY);
            }
            filename = addVersionString();
        }

        if (!FileUtilities.ensureWritableDirectory(destDir)) {
            logger.warning("not attempting to move " + srcPath);
            return false;
        }

        try {
            destDir = destDir.toRealPath();
        } catch (IOException ioe) {
            logger.warning("could not get real path of " + destDir);
            return false;
        }

        Path destPath = destDir.resolve(filename);
        if (Files.exists(destPath)) {
            if (destPath.equals(realSrc)) {
                logger.info("nothing to be done to " + srcPath);
                return true;
            }
            logger.warning("cannot move; destination exists:\n  " + destPath);
            return false;
        }

        return tryToMoveRealPaths(realSrc, destPath, destDir);
    }

    /**
     * Do the move.
     *
     * Using the attributes set in this instance, execute the move functionality.
     * In reality, this method is little more than a wrapper for getting the return
     * value right.
     *
     * @return true on success, false otherwise.
     */
    @Override
    public Boolean call() {
        boolean success = false;
        try {
            // There are numerous reasons why the move would fail.  Instead of calling
            // setFailToMove on the episode in each individual case, make the functionality
            // into a subfunction, and set the episode here for any of the failure cases.
            success = tryToMoveFile();
        } catch (Exception e) {
            logger.log(Level.WARNING, "exception caught doing file move", e);
        }
        if (observer != null) {
            observer.finishProgress(success);
        }
        if (success) {
            episode.setRenamed();
        } else {
            episode.setFailToMove();
        }
        return success;
    }
}
