package org.tvrenamer.controller.util;

import java.io.File;

public class FileUtilities {

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
}
