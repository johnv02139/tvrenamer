package org.tvrenamer.controller;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.model.GroupedMoveList;
import org.tvrenamer.model.Moves;
import org.tvrenamer.model.ProgressUpdater;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MoveRunner implements Runnable {
    private static final Logger logger = Logger.getLogger(MoveRunner.class.getName());

    private static final int DEFAULT_TIMEOUT = 120;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final Thread progressThread = new Thread(this);
    private final Queue<Future<Boolean>> futures = new LinkedList<>();
    private final int numMoves;
    private final int timeout;
    private ProgressUpdater updater = null;

    /**
     * Does the activity of the thread, which is to dequeue a move task, and block
     * until it returns, then update the progress bar and repeat the whole thing,
     * until the queue is empty.
     */
    @Override
    public void run() {
        while (true) {
            int remaining = futures.size();
            if (updater != null) {
                updater.setProgress(numMoves, remaining);
            }

            if (remaining > 0) {
                final Future<Boolean> future = futures.remove();
                try {
                    Boolean success = future.get(timeout, TimeUnit.SECONDS);
                    logger.finer("future returned: " + success);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    future.cancel(true);
                    logger.warning("exception executing move: " + e.getClass().getName());
                }
            } else {
                if (updater != null) {
                    updater.finish();
                }
                return;
            }
        }
    }

    /**
     * Runs the thread for this FileMover, to move all the files.
     *
     * This actually could be done right in the constructor, as that is, in fact, the only
     * way it's only currently used.  But it's nice to let it be more explicit.
     *
     */
    public void runThread() {
        progressThread.start();
    }

    /**
     * Adds an index to files that would otherwise conflict with other files.
     *
     * There are a lot of ways to approach the indexing, as discussed in the
     * doc of resolveConflicts, below; but as a first pass, we:
     * - consider only the basename for a conflict
     * - leave existing files as they are
     * - add indexes to conflicting files in the files we're moving
     *
     * @param moves the files which we want to move to the destination
     * @param existing the files which are already at the destination, and
     *        which the user has not specifically asked to move
     *
     * Returns nothing; modifies the entries of "moves" in-place
     */
    private static void addIndices(final Moves moves,
                                   final Set<Path> existing)
    {
        int index = existing.size();
        moves.sortBy(FileMover::getFileSize);
        for (FileMover move : moves) {
            index++;
            if (index > 1) {
                move.destIndex = index;
            }
        }
    }

    /**
     * Adds any files that match the given glob pattern, in the given directory,
     * to the given set.
     *
     * @param pathSet
     *   the Set to add any matches to
     * @param glob
     *   the glob pattern to match
     * @param dir
     *   the directory to look in for matching files
     *
     * Returns nothing; will modify the given Set if any matches are found.
     */
    private static void addPathMatchesToSet(final Set<Path> pathSet,
                                            final String glob,
                                            final Path dir)
    {
        if (Files.exists(dir) && Files.isDirectory(dir)) {
            // TODO: there are better ways of filtering files than globbing,
            // especially when we're dealing with files that may have literal
            // brackets in their names.
            try (DirectoryStream<Path> contents
                 = Files.newDirectoryStream(dir, glob))
            {
                for (Path content : contents) {
                    pathSet.add(content);
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "IO Exception descending " + dir, ioe);
            }
        }
    }

    /**
     * Finds existing conflicts; that is, files that are already in the
     * destination that have an episode which conflicts with one (or
     * more) that we want to move into the destination.
     *
     * It should be noted that we don't expect these conflicts to be
     * common.  Nevertheless, they can happen, and we are prepared to
     * deal with them.
     *
     * @param destDirName
     *    the specific directory into which we'll be moving files
     * @param basename
     *     the base portion of a the source files; this means, the part
     *     of their filepath without:<ul>
     *     <li>the filename extension</li>
     *     <li>the final dot (that precedes the filename extension</li>
     *     <li>the directory</li></ul>
     * So, for example, for "/Users/me/TV/Lost.S06E05.Lighthouse.avi",
     * the basename would be "Lost.S06E05.Lighthouse".
     * @param moves
     *     a list of moves, all of which must have a destination directory
     *     equivalent to destDirName, and all of which must have a source
     *     basename equal to the given basename; very often will be a list
     *     with just a single element
     * @return a set of paths that have conflicts; may be empty, and
     *         in fact almost always would be.
     */
    private static Set<Path> existingConflicts(final String destDirName,
                                               final String basename,
                                               final Moves moves)
    {
        Set<Path> hits = new HashSet<>();
        String glob = basename.replaceAll("\\[", "\\\\[") + "*";
        Path destDir = Paths.get(destDirName);
        addPathMatchesToSet(hits, glob, destDir);
        if (!hits.isEmpty()) {
            Set<Path> toMove = moves.stream()
                .map(FileMover::getCurrentPath)
                .collect(Collectors.toSet());
            try {
                for (Path pathToMove : toMove) {
                    Set<Path> newHits = new HashSet<>();
                    for (Path hit : hits) {
                        logger.info("comparing " + pathToMove + " and " + hit);
                        if (!Files.isSameFile(pathToMove, hit)) {
                            logger.fine("conflict: " + hit);
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
     * Resolves conflicts between episode names
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
     * @param basenames
     *   a mapping of desired destination to a group of FileMovers that all want
     *   to move a file to that destination; in the normal case, this "group"
     *   will have just one element, but it is possible to have more
     */
    private static void resolveConflicts(final GroupedMoveList basenames) {
        final String destDir = basenames.getUserData();
        for (String basename : basenames.keys()) {
            Moves moves = basenames.getMoves(basename);
            Set<Path> existing = existingConflicts(destDir, basename, moves);
            int nFiles = existing.size() + moves.size();
            if (nFiles > 1) {
                addIndices(moves, existing);
            }
        }
    }

    /**
     * Creates a MoveRunner to move all the episodes in the list, and update the progress
     * bar, using the specified timeout.
     *
     * @param episodes a list of FileMovers to execute
     * @param updater a ProgressUpdater to be informed of our progress
     * @param timeout the number of seconds to allow each FileMover to run, before killing it
     *
     */
    @SuppressWarnings("SameParameterValue")
    private MoveRunner(final Moves episodes,
                       final ProgressUpdater updater,
                       final int timeout)
    {
        this.updater = updater;
        this.timeout = timeout;

        progressThread.setName(FILE_MOVE_THREAD_LABEL);
        progressThread.setDaemon(true);

        final GroupedMoveList mappings
            = new GroupedMoveList((m) -> m.getMoveToDirectory().toString(),
                                  episodes);
        for (String destDir : mappings.keys()) {
            resolveConflicts(mappings.subGroup(FileMover::getDesiredDestName, destDir));
        }

        int count = 0;
        for (Moves moves : mappings.moveLists()) {
            for (FileMover move : moves) {
                futures.add(EXECUTOR.submit(move));
                count++;
            }
        }
        numMoves = count;
        logger.fine("have " + numMoves + " files to move");
    }

    /**
     * Creates a MoveRunner to move all the episodes in the list, using the default timeout.
     *
     * @param episodes a list of FileMovers to execute
     *
     */
    public MoveRunner(final List<FileMover> episodes) {
        this(new Moves(episodes), null, DEFAULT_TIMEOUT);
    }

    /**
     * Set the progress updater for this MoveRunner.
     *
     * @param updater a ProgressUpdater to be informed of our progress
     *
     */
    public void setUpdater(final ProgressUpdater updater) {
        this.updater = updater;
    }

    /**
     * Shut down all the threads.
     *
     * This is intended for usage just in case the program wants to shut down while the
     * moves are still running.
     *
     */
    public static void shutDown() {
        EXECUTOR.shutdownNow();
    }
}
