package org.tvrenamer.controller.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUtilities {
    private static Logger logger = Logger.getLogger(FileUtilities.class.getName());

    public static boolean areSameDisk(String pathA, String pathB) {
        FileSystem fsA = Paths.get(pathA).getFileSystem();
        if (fsA == null) {
            return false;
        }
        FileSystem fsB = Paths.get(pathB).getFileSystem();
        if (fsB == null) {
            return false;
        }
        return fsA.equals(fsB);
    }

    public static boolean differentFiles(final Path pathA, final Path pathB) {
        if (Files.notExists(pathA)) {
            return true;
        }
        if (Files.notExists(pathB)) {
            return true;
        }
        try {
            return !Files.isSameFile(pathA, pathB);
        } catch (IOException ioe) {
            logger.log(Level.FINER, "exception inspecting files", ioe);
            return true;
        }
    }

    public static boolean isDirEmpty(final Path dir) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "exception checking directory " + dir, ioe);
            return false;
        }
    }

    public static boolean rmdir(final Path dir) {
        try {
            Files.delete(dir);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "exception trying to remove directory " + dir, ioe);
            return false;
        }
        if (Files.notExists(dir)) {
            return true;
        }
        return false;
    }

    public static boolean removeWhileEmpty(final Path dir) {
        if (dir == null) {
            return false;
        }
        if (Files.notExists(dir)) {
            return false;
        }
        if (!Files.isDirectory(dir)) {
            return false;
        }
        if (!isDirEmpty(dir)) {
            // If the directory is not empty, then doing nothing is correct,
            // and we have succeeded.
            return true;
        }

        Path parent = dir.getParent();
        boolean success = rmdir(dir);
        if (success) {
            logger.info("removed empty directory " + dir);
            if (parent != null) {
                return removeWhileEmpty(parent);
            }
        }
        return success;
    }
}
