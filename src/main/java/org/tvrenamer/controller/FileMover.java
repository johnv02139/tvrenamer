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
    private final String destBaseName;
    private final String destSuffix;

    public FileMover(FileEpisode episode) {
        this.episode = episode;

        destRoot = episode.getDestinationDirectory();
        destBaseName = episode.getFileBasename();
        destSuffix = episode.getFilenameSuffix();
    }

    private boolean doActualMove(Path srcPath, Path destPath) {
        logger.fine("Going to move\n  '" + srcPath + "'\n  '" + destPath + "'");
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
        if (Files.notExists(destRoot)) {
            try {
                Files.createDirectories(destRoot);
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Unable to create directory " + destRoot, ioe);
                return false;
            }
        }
        if (!Files.exists(destRoot)) {
            logger.warning("could not create destination directory " + destRoot
                           + "; not attempting to move " + srcPath);
            return false;
        }
        if (!Files.isDirectory(destRoot)) {
            logger.warning("cannot use specified destination " + destRoot
                           + "because it is not a directory; not attempting to move "
                           + srcPath);
            return false;
        }

        String filename = destBaseName + destSuffix;
        Path destPath = destRoot.resolve(filename);

        if (destPath.equals(srcPath)) {
            logger.info("nothing to be done to " + srcPath);
            return false;
        }

        while (Files.exists(destPath)) {
            logger.warning("already exists:\n  " + destPath);
            filename = "z" + filename;
            destPath = destRoot.resolve(filename);
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
