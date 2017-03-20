package org.tvrenamer.controller;

import org.tvrenamer.controller.util.FileUtilities;
import org.tvrenamer.model.FileEpisode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileMover {
    private static Logger logger = Logger.getLogger(FileMover.class.getName());

    private final FileEpisode episode;
    private final Path destRoot;
    private final Path destPath;

    public FileMover(FileEpisode episode, Path destRoot, Path destPath) {
        this.episode = episode;
        this.destRoot = destRoot;
        this.destPath = destPath;
    }

    private boolean doActualMove(Path srcPath, Path destPath) {
        Path actualDest = null;
        try {
            actualDest = Files.move(srcPath, destPath);
            long timestamp = episode.getAirDate();
            if (timestamp > 0) {
                Files.setLastModifiedTime(actualDest, FileTime.fromMillis(timestamp));
            }
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Unable to move " + srcPath, ioe);
            // TODO: there used to be a facility for moving files to a different disk,
            // and monitoring the progress, but I didn't like the library it used.
            // Look into a replacement.
            return false;
        }
        episode.setPath(actualDest);
        boolean same = destPath.equals(actualDest);
        if (!same) {
            logger.warning("actual destination did not match intended:\n  "
                           + actualDest + "\n  " + destPath);
            return false;
        }
        return true;
    }

    private boolean tryToMoveFile() {
        Path srcPath = episode.getPath();
        if (Files.notExists(srcPath)) {
            logger.info("Path no longer exists: " + srcPath);
            episode.setDoesNotExist();
            return false;
        }
        Path destDir = destPath.getParent();
        if (Files.notExists(destDir)) {
            try {
                Files.createDirectories(destDir);
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Unable to create directory " + destDir, ioe);
                return false;
            }
        }
        if (!Files.exists(destDir)) {
            logger.warning("could not create destination directory " + destDir
                           + "; not attempting to move " + srcPath);
            return false;
        }
        if (!Files.isDirectory(destDir)) {
            logger.warning("cannot use specified destination " + destDir
                           + "because it is not a directory; not attempting to move "
                           + srcPath);
            return false;
        }
        if (Files.exists(destPath)) {
            String message ="already exists:\n  " + destPath + "\n"
                + srcPath + " was not renamed!";
            logger.warning(message);
            // UIUtils.showErrorMessageBox(RENAME_FAILED_LABEL, message, null);
            return false;
        }

        episode.setMoving();
        Path srcDir = srcPath.getParent();

        if (false == doActualMove(srcPath, destPath)) {
            return false;
        }

        episode.setRenamed();
        episode.setPath(destPath);
        logger.info("successful:\n  " + srcPath.toAbsolutePath().toString()
                    + "\n  " + destPath.toAbsolutePath().toString());
        FileUtilities.removeWhileEmpty(srcDir.toFile());
        return true;
    }

    public boolean moveFile() {
        // There are numerous reasons why the move would fail.  Instead of
        // calling setFailToMove on the episode in each individual case,
        // make the functionality into a subfunction, and set the episode
        // here for any of the failure cases.
        boolean success = tryToMoveFile();
        if (!success) {
            episode.setFailToMove();
        }
        return success;
    }
}
