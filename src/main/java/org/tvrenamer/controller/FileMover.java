package org.tvrenamer.controller;

import org.tvrenamer.controller.util.FileUtilities;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.UserPreferences;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class FileMover implements Callable<Boolean> {
    private static Logger logger = Logger.getLogger(FileMover.class.getName());

    private final File destFile;

    private final FileEpisode episode;

    public FileMover(FileEpisode episode, File destFile) {
        this.episode = episode;
        this.destFile = destFile;
    }

    private void updateFileModifiedDate(File file, long timestamp) {
        // update the modified time on the file, the parent, and the grandparent
        file.setLastModified(timestamp);
        if (UserPreferences.getInstance().isMoveEnabled()) {
            file.getParentFile().setLastModified(timestamp);
            file.getParentFile().getParentFile().setLastModified(timestamp);
        }
    }

    @Override
    public Boolean call() {
        File srcFile = episode.getFile();
        if (!srcFile.exists()) {
            logger.info("File no longer exists: " + srcFile);
            episode.setDoesNotExist();
            return false;
        }
        File destDir = destFile.getParentFile();
        String destFileName = destFile.getAbsolutePath();
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        if (destDir.exists() && destDir.isDirectory()) {
            if (destFile.exists()) {
                String message = "File " + destFile + " already exists.\n" + srcFile + " was not renamed!";
                logger.warning(message);
                // showMessageBox(SWTMessageBoxType.ERROR, "Rename Failed", message);
                return false;
            }
            episode.setMoving();
            boolean succeeded = false;
            if (FileUtilities.areSameDisk(srcFile.getAbsolutePath(), destFileName)) {
                succeeded = srcFile.renameTo(destFile);
            }
            if (succeeded) {
                long timestamp = System.currentTimeMillis();
                episode.setRenamed();
                logger.info("Moved " + srcFile.getAbsolutePath() + " to " + destFileName);
                episode.setFile(destFile);
                updateFileModifiedDate(destFile, timestamp);
                return true;
            } else {
                // TODO: there used to be a facility for moving files to a different disk,
                // and monitoring the progress, but I didn't like the library it used.
                // Look into a replacement.
                logger.severe("Unable to move " + srcFile.getAbsolutePath()
                              + " to " + destFileName);
                episode.setFailToMove();
                return false;
            }
        } else {
            logger.severe("Unable to use " + destDir + " as destination directory");
            episode.setFailToMove();
        }
        return false;
    }
}
