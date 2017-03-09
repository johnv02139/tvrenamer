package org.tvrenamer.controller.util;

import java.io.File;
import java.util.logging.Logger;

public class FileUtilities {
    private static Logger logger = Logger.getLogger(FileUtilities.class.getName());

    public static boolean areSameDisk(String pathA, String pathB) {
        File[] roots = File.listRoots();
        if (roots.length < 2) {
            return true;
        }
        for (File root : roots) {
            String rootPath = root.getAbsolutePath();
            if (pathA.startsWith(rootPath)) {
                return pathB.startsWith(rootPath);
            }
        }
        return false;
    }

    public static boolean removeWhileEmpty(File dir) {
        if (dir == null) {
            return false;
        }
        if (!dir.exists()) {
            return false;
        }
        if (!dir.isDirectory()) {
            return false;
        }

        String[] files = dir.list(null);
        if (files == null) {
            // This shouldn't happen.
            return false;
        }
        if (files.length > 0) {
            // If the directory is not empty, then doing nothing is correct,
            // and we have succeeded.
            return true;
        }
        File parent = dir.getParentFile();
        boolean success = dir.delete();
        if (success) {
            logger.info("removed empty directory " + dir);
            if (parent != null) {
                return removeWhileEmpty(parent);
            }
        }
        return success;
    }
}
