package org.tvrenamer.devel;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.util.FileUtilities;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/* DummyFiles -- create "fake" video files based on the real ones.
 *
 *
 */
public class DummyFiles {
    private static Logger logger = Logger.getLogger(DummyFiles.class.getName());

    private static final String[] VIDEO_SUFFIXES = { "mp4",
                                                     "mkv",
                                                     "mpg",
                                                     "mpeg",
                                                     "rm",
                                                     "asf",
                                                     "m4v",
                                                     "mov",
                                                     "qt",
                                                     "divx",
                                                     "wmv" };

    private static String makeGlobPattern() {
        int nSuffixes = VIDEO_SUFFIXES.length;
        String globPattern = "*.{";
        for (int i=0; i<nSuffixes; i++) {
            if (i > 0) {
                globPattern += ",";
            }
            globPattern += VIDEO_SUFFIXES[i];
        }
        globPattern += "}";
        return globPattern;
    }

    private static final String GLOB_PATTERN = makeGlobPattern();


    private static void processDirectory(Path outPath, Path basePath, Path current) {
        if (Files.isDirectory(current)) {
            try (DirectoryStream<Path> dirfiles = Files.newDirectoryStream(current)) {
                if (dirfiles != null) {
                    // recursive call
                    dirfiles.forEach(pth -> processDirectory(outPath, basePath, pth));
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "IO Exception descending " + current, ioe);
            }
            // FileUtilities.removeWhileEmpty(current);
        } else {
            Path abs = current.toAbsolutePath();
            Path rel =  basePath.relativize(abs);
            String content = abs.toString();

            Path dest = outPath.resolve(rel).toAbsolutePath();
            Path destDir = dest.getParent();
            FileUtilities.mkdirs(destDir);

            try {
                Files.write(dest, content.getBytes());
                logger.info("created:\n  " + dest);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "error writing file " + dest, ioe);
            }
        }
    }

    private static void createFiles(String basedir, String outdir) {
        Path basepath = Paths.get(basedir);
        Path outpath = Paths.get(outdir);

        processDirectory(outpath, basepath, basepath);
    }

    public static void createFiles(String[] args) {
        logger.info("made glob pattern: " + GLOB_PATTERN);
        // TODO: process flags, use third-party package
        createFiles(args[0], args[1]);
    }
}
