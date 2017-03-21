package org.tvrenamer.controller;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.util.FileUtilities;
import org.tvrenamer.model.FileEpisode;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileMover {
    private static Logger logger = Logger.getLogger(FileMover.class.getName());

    private final FileEpisode episode;
    private final Path destRoot;
    private final String destBaseName;
    private final String destSuffix;
    private Integer destIndex;

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

        String filename = (destIndex == null)
            ? destBaseName + destSuffix
            : destBaseName + VERSION_SEPARATOR_STRING + destIndex + destSuffix;
        Path destPath = destRoot.resolve(filename);

        if (Files.exists(destPath)) {
            if (destPath.equals(srcPath)) {
                logger.info("nothing to be done to " + srcPath);
                return true;
            }
            logger.severe("*** already exists:\n  " + destPath);
            throw new IllegalStateException("dest file exists for file move");
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

    private static void addToMapList(Map<String, List<FileMover>> table,
                                     String key, FileMover newVal)
    {
        List<FileMover> totalList = table.get(key);
        if (totalList == null) {
            totalList = new LinkedList<FileMover>();
        }
        totalList.add(newVal);
        table.put(key, totalList);
    }

    private static void addToMapList(Map<Path, List<FileMover>> table,
                                     Path key, FileMover newVal)
    {
        List<FileMover> totalList = table.get(key);
        if (totalList == null) {
            totalList = new LinkedList<FileMover>();
        }
        totalList.add(newVal);
        table.put(key, totalList);
    }

    private static Set<Path> existingConflicts(Path destDir,
                                               String basename,
                                               List<FileMover> moves)
    {
        Set<Path> hits = new HashSet<>();
        if (Files.exists(destDir) && Files.isDirectory(destDir)) {
            try (DirectoryStream<Path> contents
                 = Files.newDirectoryStream(destDir, basename + "*"))
            {
                Iterator<Path> it = contents.iterator();
                while (it.hasNext()) {
                    hits.add(it.next());
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "IO Exception descending " + destDir, ioe);
            }
        }
        if (!hits.isEmpty()) {
            Set<Path> toMove = new HashSet<>();
            for (FileMover move : moves) {
                toMove.add(move.episode.getPath());
            }
            try {
                for (Path pathToMove : toMove) {
                    Set<Path> newHits = new HashSet<>();
                    for (Path hit : hits) {
                        logger.info("comparing " + pathToMove + " and " + hit);
                        if (Files.isSameFile(pathToMove, hit)) {
                            logger.info("*** removing " + hit);
                        } else {
                            logger.info("*** keeping " + hit);
                            newHits.add(hit);
                        }
                    }
                    hits = newHits;
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "IO Exception comparing files", ioe);
            }
        }
        return hits;
    }

    /**
     * @param moves the files which we want to move to the destination
     * @param existing the files which are already at the destination, and
     *        which the user has not specifically asked to move
     * @return nothing; modifies the entries of "moves" in-place
     *
     * There are many different ways of renaming.  Some questions we might
     * deal with in the future:
     * - can we rename the files that are already in the destination?
     * - assuming two files refer to the same episode, is it still a conflict if:
     *   - they are different resolution?
     *   - they are different file formats (e.g., avi, mp4)?
     * - what do we do with identical files?
     *   - could treat as any other "conflict", or move into a special folder
     *   - but if we verify they are byte-for-byte duplicates, really no point
     *   - when we log all moves, for undo-ability, need to keep track of
     *     multiple file names that mapped to the same result
     * - do we prioritize by file type?  file size?  resolution?
     *     source (dvdrip, etc.)?
     * - can we integrate with a library that gives us information about the
     *   content (actual video quality, length, etc.)?
     *
     * As a first pass, we:
     * - leave existing files as they are
     * - add indexes to conflicting files in the files we're moving
     * note if move is in existing
     *
     */
    private static void addIndices(List<FileMover> moves, Set<Path> existing) {
        int index = existing.size();
        moves.sort(new Comparator<FileMover>() {
                public int compare(FileMover m1, FileMover m2) {
                    return (int) (m2.episode.getFileSize() - m1.episode.getFileSize());
                }
            });
        for (FileMover move : moves) {
            index++;
            if (index > 1) {
                move.destIndex = index;
            }
        }
    }

    private static void resolveConflicts(List<FileMover> fileList, Path destDir) {
        Map<String, List<FileMover>> basenames = new HashMap<>();
        for (FileMover move : fileList) {
            addToMapList(basenames, move.destBaseName, move);
        }
        for (String basename : basenames.keySet()) {
            List<FileMover> moves = basenames.get(basename);
            Set<Path> existing = existingConflicts(destDir, basename, moves);
            int nFiles = existing.size() + moves.size();
            if (nFiles > 1) {
                addIndices(moves, existing);
            }
        }
    }

    public static Map<Path, List<FileMover>> listOfFileToMove(List<FileEpisode> episodes) {
        final Map<Path, List<FileMover>> toMove = new HashMap<>();

        for (final FileEpisode episode : episodes) {
            // Skip files not successfully downloaded and ready to be moved
            if (!episode.isReady()) {
                logger.info("selected but not ready: " + episode.getFilepath());
                continue;
            }

            Path destDir = episode.getDestinationDirectory();
            addToMapList(toMove, destDir, new FileMover(episode));
        }

        for (Path dest : toMove.keySet()) {
            resolveConflicts(toMove.get(dest), dest);
        }

        return toMove;
    }
}
