package org.tvrenamer.controller;

import org.tvrenamer.controller.util.FileUtilities;
import org.tvrenamer.model.FileEpisode;

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

    @Override
    public Boolean call() {
        File srcFile = episode.getPath().toFile();
        if (!srcFile.exists()) {
            logger.info("File no longer exists: " + srcFile);
            episode.setDoesNotExist();
            return false;
        }
        File srcDir = srcFile.getParentFile();
        File destDir = destFile.getParentFile();
        String destFileName = destFile.getAbsolutePath();
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        if (destDir.exists() && destDir.isDirectory()) {
            if (destFile.exists()) {
                String message = "File " + destFile + " already exists.\n" + srcFile + " was not renamed!";
                logger.warning(message);
                // UIUtils.showErrorMessageBox(RENAME_FAILED_LABEL, message, null);
                return false;
            }
            episode.setMoving();
            boolean succeeded = srcFile.renameTo(destFile);
            if (succeeded) {
                long timestamp = episode.getAirDate();
                if (timestamp > 0) {
                    destFile.setLastModified(timestamp);
                }
                episode.setRenamed();
                logger.info("Moved " + srcFile.getAbsolutePath() + " to " + destFileName);
                episode.setPath(destFile.toPath());
                FileUtilities.removeWhileEmpty(srcDir);
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
