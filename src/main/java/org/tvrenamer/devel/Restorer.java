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

/* Restorer -- restore renamed, "fake" video files to their original names and locations.
 *
 * This file assumes a certain test scenario, where there are files with filename
 * extensions indicating they are video files, but in fact the content of the files is a
 * single line of text indicating an original pathname.  We can move these files around
 * arbitrarily, and always get back to our original state, by reading the content of the
 * file and using it to restore the file back to a location similar to the original.
 *
 * The content is assumed to be full pathnames, and this class can "restore" them to a
 * different relative location.  The basedir is the part of the original paths that
 * should not be considered, and the outdir is where to put them.  For example:
 *   content = /home/eprenamer/Videos/test/Frasier/S10E01.The.Ring.Cycle.avi
 *   basedir = /home/eprenamer/Videos/test
 *   outdir  = /tmp/testdir
 *   restored: /tmp/testdir/Frasier/S10E01.The.Ring.Cycle.avi
 *
 */
public class Restorer {
    private static Logger logger = Logger.getLogger(Restorer.class.getName());

    static Path outpath;

    private static boolean isIgnoreFile(String content) {
        // These are the test files that are created by
        //   <tvrenamer>/etc/run-scripts/tvrtest.sh
        return content.startsWith("content");
    }

    private static void processDirectory(Path path) {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> dirfiles = Files.newDirectoryStream(path)) {
                if (dirfiles != null) {
                    // recursive call
                    dirfiles.forEach(pth -> processDirectory(pth));
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "IO Exception descending " + path, ioe);
            }
            FileUtilities.removeWhileEmpty(path);
        } else {
            try {
                String content = new String(Files.readAllBytes(path)).trim();
                if (isIgnoreFile(content)) {
                    logger.info("ignoring " + path);
                } else {
                    // logger.info("processing " + path);
                    Path resolved = outpath.resolve(content);
                    Path newPath = resolved.normalize();
                    if (Files.exists(newPath)) {
                        logger.warning("already exists: " + newPath);
                    } else {
                        Path dstParent = newPath.getParent();
                        Files.createDirectories(dstParent);
                        Files.move(path, newPath);
                    }
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "I/O Exception descending " + path, ioe);
            }
        }
    }

    private static Path verifyPath(String pathname) {
        Path asPath = Paths.get(pathname);
        if (Files.notExists(asPath)) {
            logger.warning("cannot use \"" + pathname + "\", does not exist");
            return null;
        }
        if (Files.isDirectory(asPath)) {
            return asPath;
        }
        logger.warning("cannot use \"" + pathname + "\", not a directory");
        return null;
    }

    private static Path verifyOrCreatePath(String pathname) {
        Path asPath = Paths.get(pathname);
        if (Files.exists(asPath)) {
            if (Files.isDirectory(asPath)) {
                return asPath;
            } else {
                logger.warning("cannot use \"" + pathname + "\", not a directory");
            }
        } else {
            boolean created = FileUtilities.mkdirs(asPath);
            if (created) {
                return asPath;
            }
        }
        return null;
    }

    public static void restoreToOriginalNames(String basedir, String outdir) {
        Path basepath = verifyPath(basedir);
        if (basepath == null) {
            return;
        }
        outpath = verifyOrCreatePath(outdir);
        if (outpath == null) {
            return;
        }

        processDirectory(basepath);
    }

    public static void restoreToOriginalNames(String[] args) {
        restoreToOriginalNames(args[0], args[1]);
    }
}
