/**
 * UIStarter -- creates and manages the table of episodes.
 *
 * This is the process the code executes when adding files to the table:
 * - verify that the file exists, is not hidden, and is not already in the table
 * - create a TableItem to go in the table
 * - create a FileEpisode to hold all the information about the file
 *   - upon creation, the FileEpisode will have its filename parsed
 *   - we pass the TableItem to the FileEpisode constructor, which establishes
 *     pointers back and forth
 *   - we also pass the FileEpisode the instance of the UIStarter, which implements
 *     EpisodeInformationListener.  The FileEpisode will register the listener, and
 *     notify us when it gets more information about the episode.
 *   - assuming the filename parsed correctly (basically meaning that we were able
 *     to find a season number and episode number in the filename), the FileEpisode
 *     constructor will spawn a thread to look up the show and episode information
 * - once the FileEpisode constructor returns, we add the table item to the table;
 *   for the "Proposed File Name" field, we have a status text rather than an actual
 *   new filename.  Files start out un-selected, and with a status that indicates
 *   they have not been processed yet.
 * - we also add the FileEpisode to a filename-to-object mapping in case the same
 *   file is added again later
 * - then, when the spawned thread gets information about the TV series that the
 *   file is an episode of, it updates the listener, which updates the information
 *   displayed in the table
 * - after we know which series it is, information about the particular episode is
 *   looked up; when that information becomes available, we again get a callback
 *   and update the table again
 * - at that point, we know the "new" name of the file, and it is ready to be renamed
 *   on demand
 *
 */

package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TaskBar;
import org.eclipse.swt.widgets.TaskItem;
import org.eclipse.swt.widgets.Text;

import org.tvrenamer.controller.EpisodeInformationListener;
import org.tvrenamer.controller.FileMover;
import org.tvrenamer.controller.UpdateChecker;
import org.tvrenamer.controller.UpdateCompleteHandler;
import org.tvrenamer.model.EpisodeDb;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.Series;
import org.tvrenamer.model.ShowStore;
import org.tvrenamer.model.UnresolvedShow;
import org.tvrenamer.model.UserPreference;
import org.tvrenamer.model.UserPreferences;
import org.tvrenamer.model.util.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

public class UIStarter implements Observer, EpisodeInformationListener {

    private static Logger logger = Logger.getLogger(UIStarter.class.getName());

    private static final int SELECTED_COLUMN = 0;
    private static final int CURRENT_FILE_COLUMN = 1;
    private static final int NEW_FILENAME_COLUMN = 2;
    private static final int STATUS_COLUMN = 3;

    private static final int ITEM_NOT_IN_TABLE = -1;

    private boolean isUIRunning = false;
    private List<String> ignoreKeywords;

    private Shell shell;
    private Display display;
    private Button addFilesButton;
    private Button addFolderButton;
    private Button clearFilesButton;
    private Button selectAllButton;
    private Button deselectAllButton;
    private Link updatesAvailableLink;
    private Button renameSelectedButton;
    private TableColumn destinationColumn;
    private Table resultsTable;
    private Font italicFont;
    private ProgressBar totalProgressBar;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final UserPreferences prefs;
    private final Path preloadFolder;
    private final EpisodeDb episodeMap;

    private enum FileMoveIcon {
        ADDED("/icons/SweetieLegacy/16-circle-blue.png"),
        DOWNLOADING("/icons/SweetieLegacy/16-clock.png"),
        SUCCESS("/icons/SweetieLegacy/16-em-check.png"),
        FAIL("/icons/SweetieLegacy/16-em-cross.png"),
        GOT_SHOW("/icons/SweetieLegacy/16-em-down.png"),
        RENAMING("/icons/SweetieLegacy/16-em-pencil.png"),
        NOPARSE("/icons/SweetieLegacy/16-message-warn.png");

        final Image icon;

        FileMoveIcon(String path) {
            InputStream stream = UIStarter.class.getResourceAsStream(path);
            if (stream != null) {
                icon = new Image(Display.getCurrent(), stream);
            } else {
                icon = new Image(Display.getCurrent(),
                                 ICON_PARENT_DIRECTORY + path);
            }
        }
    }

    private static String getFailMessage(final FileEpisode ep) {
        if (ep.isFailToParse()) {
            return CANT_PARSE_FILENAME;
        }

        if (ep.isFailed()) {
            String failMsg = DOWNLOADING_FAILED_MESSAGE;
            // BROKEN_PLACEHOLDER_FILENAME;

            final Series epSeries = ep.getSeries();
            if (epSeries instanceof UnresolvedShow) {
                UnresolvedShow f = (UnresolvedShow) epSeries;
                if (f.getException() == null) {
                    failMsg = NO_SUCH_SHOW_MESSAGE + f.getName();
                }
            }
            return failMsg;
        }

        return FAIL_MSG_FOR_NONFAIL;
    }

    private static String renameButtonText(boolean isMoveEnabled) {
        if (isMoveEnabled) {
            return RENAME_AND_MOVE;
        } else {
            return RENAME_LABEL;
        }
    }

    private static String renameButtonToolTip(boolean isMoveEnabled,
                                              String destDirectory)
    {
        if (isMoveEnabled) {
            return MOVE_TOOLTIP_1 + destDirectory + MOVE_TOOLTIP_2;
        } else {
            return RENAME_TOOLTIP;
        }
    }

    private static String destColumnText(boolean isMoveEnabled) {
        if (isMoveEnabled) {
            return MOVE_HEADER;
        } else {
            return RENAME_HEADER;
        }
    }

    private TableItem[] getTableItems() {
        return resultsTable.getItems();
    }

    private TableItem getTableItem(int index) {
        return resultsTable.getItem(index);
    }

    private FileEpisode getTableItemEpisode(final TableItem item) {
        FileEpisode episode = null;
        Object data = item.getData();
        if ((data != null) && (data instanceof FileEpisode)) {
            return (FileEpisode) data;
        }
        throw new IllegalStateException("TableItem does not contain FileEpisode: " + item);
    }

    private String getFilenameRowText(TableItem item) {
        return item.getText(CURRENT_FILE_COLUMN);
    }

    private boolean isNameIgnored(String fileName) {
        for (int i = 0; i < ignoreKeywords.size(); i++) {
            if (fileName.toLowerCase().contains(ignoreKeywords.get(i))) {
                return true;
            }
        }
        return false;
    }

    private void setEpisodeIsChecked(final TableItem item, final FileEpisode episode) {
        String fileName = episode.getFilepath();
        episodeMap.put(fileName, episode);

        if (episode.wasNotParsed()) {
            logger.severe("Couldn't parse file: " + fileName);
        }
        // Set if the item is checked or not according
        // to a list of banned keywords
        item.setChecked(!isNameIgnored(fileName) && episode.isReady());
    }

    private void setEpisodeFilenameText(final TableItem item, final FileEpisode episode) {
        item.setText(CURRENT_FILE_COLUMN, episode.getFilepath());
    }

    private void setEpisodeNewFilenameText(final TableItem item, final FileEpisode episode) {
        String valueForNewFilename;
        if (episode.isFailed()) {
            valueForNewFilename = getFailMessage(episode);
        } else if (episode.isReady()) {
            valueForNewFilename = episode.getProposedFilename();
        } else if (episode.isSeriesReady()) {
            valueForNewFilename = PROCESSING_EPISODES;
        } else {
            valueForNewFilename = ADDED_PLACEHOLDER_FILENAME;
        }

        item.setText(NEW_FILENAME_COLUMN, valueForNewFilename);
    }

    private void setEpisodeStatusImage(final TableItem item, final FileEpisode episode) {
        if (episode.isNewlyAdded()) {
            item.setImage(STATUS_COLUMN, FileMoveIcon.ADDED.icon);
        } else if (episode.isInvestigating()) {
            item.setImage(STATUS_COLUMN, FileMoveIcon.DOWNLOADING.icon);
        } else if (episode.isReady()) {
            item.setImage(STATUS_COLUMN, FileMoveIcon.SUCCESS.icon);
        } else if (episode.isFailed()) {
            item.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
            item.setFont(italicFont);
            item.setImage(STATUS_COLUMN, FileMoveIcon.FAIL.icon);
        } else if (episode.isRenameInProgress()) {
            item.setImage(STATUS_COLUMN, FileMoveIcon.RENAMING.icon);
        } else {
            item.setGrayed(true); // makes checkbox use a dot; very weird
            item.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
            item.setFont(italicFont);
            item.setImage(STATUS_COLUMN, FileMoveIcon.NOPARSE.icon);
        }
    }

    private void updateTableItemText(final TableItem item, final FileEpisode episode) {
        setEpisodeIsChecked(item, episode);
        setEpisodeFilenameText(item, episode);
        setEpisodeNewFilenameText(item, episode);
        setEpisodeStatusImage(item, episode);
    }

    private TableItem addRowCopy(TableItem oldItem, FileEpisode oldEpisode, int index) {
        boolean wasChecked = oldItem.getChecked();
        int oldStyle = oldItem.getStyle();

        TableItem newItem = new TableItem(resultsTable, oldStyle, index);
        newItem.setChecked(wasChecked);
        newItem.setText(CURRENT_FILE_COLUMN, getFilenameRowText(oldItem));
        newItem.setText(NEW_FILENAME_COLUMN, oldItem.getText(NEW_FILENAME_COLUMN));
        newItem.setImage(STATUS_COLUMN, oldItem.getImage(STATUS_COLUMN));
        newItem.setForeground(oldItem.getForeground());
        newItem.setFont(oldItem.getFont());
        newItem.setData(oldEpisode);

        return newItem;
    }

    private FileEpisode verifyEpisode(final TableItem item) {
        FileEpisode data = (FileEpisode) item.getData();
        String fileName;

        if (data != null) {
            fileName = data.getFilepath();
        } else {
            fileName = getFilenameRowText(item);
        }
        if (fileName == null) {
            throw new IllegalStateException("unrecoverable table corruption");
        }

        FileEpisode mapped = episodeMap.get(fileName);

        // TODO: check that file still exists?

        // This is the success case, and very much normal and expected.
        if ((data != null) && (data == mapped)) {
            return data;
        }

        // Everything below here is handling internal (programming) errors.
        // There's nothing the user can do that should cause any of this.
        if (mapped == null) {
            if (data == null) {
                logger.warning("table item without FileEpisode: " + fileName);
            } else {
                logger.warning("table item with no episodeMap mapping: " + fileName);
            }
        } else {
            if (!episodeMap.remove(fileName, mapped)) {
                throw new IllegalStateException("unrecoverable episode DB corruption");
            }
            if (data == null) {
                logger.warning("table item with no FileEpisode data: " + fileName);
            } else {
                logger.warning("table item mapped to two different episodes: " + fileName);
            }
        }

        // Again, we're in an internal error case, by this point.  Perhaps just throwing
        // an exception and exiting would be better.  But to try to debug any internal
        // errors that come up, it's better for now to whip up a new object and continue.
        data = new FileEpisode(Paths.get(fileName), item, this);
        episodeMap.put(fileName, data);

        return data;
    }

    private void refreshTable() {
        logger.info("Refreshing table");
        for (TableItem item : getTableItems()) {
            FileEpisode episode = verifyEpisode(item);
            updateTableItemText(item, episode);
        }
    }

    public void onEpisodeUpdate(final FileEpisode ep) {
        if (ep == null) {
            logger.severe("got update for a null episode (makes no sense)");
            return;
        }
        if (!isUIRunning || display.isDisposed()) {
            return;
        }
        final TableItem item = ep.getViewItem();
        if (item == null) {
            logger.warning("episode not associated with table item: " + ep);
            return;
        }
        display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    synchronized (ep) {
                        final TableItem item = ep.getViewItem();
                        if (item == null) {
                            logger.info("item has disappeared from episode: " + ep);
                            return;
                        }
                        if (item.isDisposed()) {
                            logger.info("item has been displosed from episode: " + ep);
                            ep.setViewItem(null);
                            return;
                        }
                        updateTableItemText(item, ep);
                    }
                }
            });
    }

    private void addFileToRenamer(final Path path) {
        final Path absPath = path.toAbsolutePath();
        final String key = absPath.toString();
        if (episodeMap.containsKey(key)) {
            logger.info("already in table: " + key);
        } else {
            final TableItem item = new TableItem(resultsTable, SWT.NONE);
            final FileEpisode episode = new FileEpisode(absPath, item, this);
            // We add the file to the table even if we couldn't parse the filename
            updateTableItemText(item, episode);
            episodeMap.add(episode);
        }
    }

    private boolean fileIsVisible(Path path) {
        boolean isVisible = false;
        try {
            if (Files.exists(path)) {
                if (Files.isHidden(path)) {
                    logger.finer("ignoring hidden file " + path);
                } else {
                    isVisible = true;
                }
            }
        } catch (IOException | SecurityException e) {
            logger.finer("could not access file; treating as hidden: " + path);
        }
        return isVisible;
    }

    private void addFileIfVisible(Path path) {
        if (fileIsVisible(path) && Files.isRegularFile(path)) {
            addFileToRenamer(path);
        }
    }

    private void addFilesRecursively(final Path path) {
        if (fileIsVisible(path)) {
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> contents = Files.newDirectoryStream(path)) {
                    if (contents != null) {
                        // recursive call
                        contents.forEach(pth -> addFilesRecursively(pth));
                    }
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "IO Exception descending " + path, ioe);
                }
            } else {
                addFileToRenamer(path);
            }
        }
    }

    private void addFolderToRenamer(final Path path) {
        if (prefs.isRecursivelyAddFolders()) {
            addFilesRecursively(path);
        } else {
            addFileIfVisible(path);
        }
    }

    private void addArrayOfStringsToRenamer(final String[] fileNames) {
        boolean descend = prefs.isRecursivelyAddFolders();
        for (final String fileName : fileNames) {
            Path path = Paths.get(fileName);
            if (descend) {
                addFilesRecursively(path);
            } else {
                addFileIfVisible(path);
            }
        }
    }

    private void setSortedItem(int i, int j) {
        TableItem oldItem = getTableItem(i);
        FileEpisode oldEpisode = getTableItemEpisode(oldItem);

        TableItem item = addRowCopy(oldItem, oldEpisode, j);

        oldEpisode.setViewItem(item);

        oldItem.dispose();
    }

    private void sortResultsTable(TableColumn col, int position) {
        int newSortDirection = SWT.DOWN;
        TableColumn previouslySortedBy = resultsTable.getSortColumn();
        if (col == previouslySortedBy) {
            int oldSortDirection = resultsTable.getSortDirection();
            newSortDirection = (oldSortDirection == SWT.DOWN) ? SWT.UP : SWT.DOWN;
        }

        // Get the items
        TableItem[] items = getTableItems();
        Collator collator = Collator.getInstance(THIS_LOCALE);

        // Go through the item list and
        for (int i = 1; i < items.length; i++) {
            String value1 = items[i].getText(position);
            for (int j = 0; j < i; j++) {
                String value2 = items[j].getText(position);
                // Compare the two values and order accordingly
                if (resultsTable.getSortDirection() == SWT.DOWN) {
                    if (collator.compare(value1, value2) < 0) {
                        setSortedItem(i, j);
                        // the snippet replaces the items with the new items, we
                        // do the same
                        items = getTableItems();
                        break;
                    }
                } else {
                    if (collator.compare(value1, value2) > 0) {
                        setSortedItem(i, j);
                        // the snippet replaces the items with the new items, we
                        // do the same
                        items = getTableItems();
                        break;
                    }
                }
            }
        }
        resultsTable.setSortColumn(col);
        resultsTable.setSortDirection(newSortDirection);
    }

    private int getTableItemIndex(TableItem item) {
        try {
            return resultsTable.indexOf(item);
        } catch (IllegalArgumentException | SWTException ignored) {
            // We'll just fall through and return the sentinel.
        }
        return ITEM_NOT_IN_TABLE;
    }

    private void deleteSelectedTableItems() {
        int index = ITEM_NOT_IN_TABLE;
        for (final TableItem item : resultsTable.getSelection()) {
            index = getTableItemIndex(item);
            if (ITEM_NOT_IN_TABLE == index) {
                logger.info("error: somehow selected item not found in table");
                continue;
            }

            FileEpisode ep = getTableItemEpisode(item);
            ep.setViewItem(null);

            item.setData(null);

            String filename = ep.getFilepath();
            episodeMap.remove(filename);

            resultsTable.remove(index);
            item.dispose();
        }
        resultsTable.deselectAll();
    }

    private Queue<Future<Boolean>> listOfFileMoves() {
        final Queue<Future<Boolean>> futures = new LinkedList<>();

        for (final TableItem item : getTableItems()) {
            if (item.getChecked()) {
                final FileEpisode episode = (FileEpisode) item.getData();

                // Skip files not successfully downloaded
                if (!episode.isDownloaded()) {
                    continue;
                }

                Path currentPath = episode.getPath();
                String newName = item.getText(NEW_FILENAME_COLUMN);
                Path newPath = Paths.get(newName);

                if (!prefs.isMoveEnabled()) {
                    // If move is enabled, the full path is in the table already,
                    // but if not, we need to build it
                    Path currentParent = currentPath.getParent();
                    newPath = currentParent.resolve(newName);
                }

                if (currentPath.equals(newPath)) {
                    logger.info("nothing to be done to " + currentPath);
                    continue;
                }

                logger.info("Going to move\n  '" + currentPath + "'\nto\n  '" + newPath + "'");

                Callable<Boolean> moveCallable = new FileMover(episode, newPath);
                futures.add(executor.submit(moveCallable));
                item.setChecked(false);
            }
        }

        return futures;
    }

    private TaskItem getTaskItem() {
        TaskItem taskItem = null;
        TaskBar taskBar = display.getSystemTaskBar();
        if (taskBar != null) {
            taskItem = taskBar.getItem(shell);
            if (taskItem == null) {
                taskItem = taskBar.getItem(null);
            }
        }
        return taskItem;
    }

    private void updateProgressBar(final float progress, final TaskItem taskItem) {
        if (totalProgressBar.isDisposed()) {
            return;
        }
        totalProgressBar.setSelection(Math.round(progress * totalProgressBar.getMaximum()));
        if (taskItem.isDisposed()) {
            return;
        }
        taskItem.setProgress(Math.round(progress * 100));
    }


    private void doRenamesWithProgressBar(final Queue<Future<Boolean>> futures,
                                          final TaskItem taskItem)
    {
        taskItem.setProgressState(SWT.NORMAL);
        taskItem.setOverlayImage(FileMoveIcon.RENAMING.icon);

        ProgressProxy proxy = new ProgressProxy() {
                @Override
                public void setProgress(final float progress) {
                    if (display.isDisposed()) {
                        return;
                    }

                    display.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                updateProgressBar(progress, taskItem);
                            }
                        });
                }
            };

        UpdateCompleteHandler updateHandler = new UpdateCompleteHandler() {
                @Override
                public void onUpdateComplete() {
                    display.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                taskItem.setOverlayImage(null);
                                taskItem.setProgressState(SWT.DEFAULT);
                                refreshTable();
                            }
                        });
                }
            };

        Runnable updater = new ProgressBarUpdater(proxy, futures, updateHandler);
        Thread progressThread = new Thread(updater);
        progressThread.setName(PROGRESS_THREAD_LABEL);
        progressThread.setDaemon(true);
        progressThread.start();
    }

    private void renameFiles() {
        final Queue<Future<Boolean>> futures = listOfFileMoves();
        TaskItem taskItem = getTaskItem();
        if (taskItem == null) {
            // There is no task bar on linux
            // In this case, we should execute the futures without the task bar
            // (TODO)
            logger.info("not moving files becasue no task item");
        } else {
            doRenamesWithProgressBar(futures, taskItem);
        }
    }

    private void setColumnDestText() {
        if (!isUIRunning) {
            return;
        }
        destinationColumn.setText(destColumnText(prefs.isMoveEnabled()));
    }

    private void setRenameButtonText() {
        if (!isUIRunning) {
            return;
        }
        renameSelectedButton.setText(renameButtonText(prefs.isMoveEnabled()));
        shell.changed(new Control[] {renameSelectedButton});
        shell.layout(false, true);
    }

    private void setupMoveButtonText() {
        setRenameButtonText();
        renameSelectedButton.setToolTipText(MOVE_TOOLTIP_1
                                            + prefs.getDestinationDirectory().getAbsolutePath()
                                            + MOVE_TOOLTIP_2);
    }

    private void setupRenameButtonText() {
        setRenameButtonText();
        renameSelectedButton.setToolTipText(RENAME_TOOLTIP);
    }

    private void updateUserPreferences(UserPreferences observed, UserPreference upref) {
        logger.info("Preference change event: " + upref);

        if ((upref == UserPreference.MOVE_ENABLED)
            || (upref == UserPreference.RENAME_ENABLED))
        {
            boolean isMoveEnabled = observed.isMoveEnabled();
            destinationColumn.setText(destColumnText(isMoveEnabled));
            renameSelectedButton.setText(renameButtonText(isMoveEnabled));
            shell.changed(new Control[] {renameSelectedButton});
            shell.layout(false, true);
        }
        if ((upref == UserPreference.REPLACEMENT_MASK)
            || (upref == UserPreference.MOVE_ENABLED)
            || (upref == UserPreference.RENAME_ENABLED)
            || (upref == UserPreference.DEST_DIR)
            || (upref == UserPreference.SEASON_PREFIX)
            || (upref == UserPreference.LEADING_ZERO))
        {
            refreshTable();
        }

        if (upref == UserPreference.IGNORE_REGEX) {
            ignoreKeywords = observed.getIgnoreKeywords();
        }
    }

    /* (non-Javadoc)
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(Observable observable, Object value) {
        if (!isUIRunning) {
            return;
        }
        if (observable instanceof UserPreferences && value instanceof UserPreference) {
            updateUserPreferences((UserPreferences) observable,
                                  (UserPreference) value);
        }
    }

    private void doCleanup() {
        executor.shutdownNow();
        // TODO: may not be necessary if they're daemon threads
        ShowStore.cleanUp();
        shell.dispose();
        display.dispose();
    }

    private void showPreferencesPane() {
        PreferencesDialog preferencesDialog = new PreferencesDialog(shell);
        preferencesDialog.open();
    }

    /**
     * Create the 'About' dialog.
     */
    private void showAboutPane() {
        AboutDialog aboutDialog = new AboutDialog(shell);
        aboutDialog.open();
    }

    private void setupEditableTableListener() {
        // editable table
        final TableEditor editor = new TableEditor(resultsTable);
        editor.horizontalAlignment = SWT.CENTER;
        editor.grabHorizontal = true;

        Listener tblEditListener =
            new Listener() {
                @Override
                public void handleEvent(Event event) {
                    Rectangle clientArea = resultsTable.getClientArea();
                    Point pt = new Point(event.x, event.y);
                    int index = resultsTable.getTopIndex();
                    while (index < resultsTable.getItemCount()) {
                        boolean visible = false;
                        final TableItem item = getTableItem(index);
                        for (int i = 0; i < resultsTable.getColumnCount(); i++) {
                            Rectangle rect = item.getBounds(i);
                            if (rect.contains(pt)) {
                                final int column = i;
                                final Text text = new Text(resultsTable, SWT.NONE);
                                Listener textListener =
                                    new Listener() {
                                        @Override
                                        @SuppressWarnings("fallthrough")
                                        public void handleEvent(final Event e) {
                                            switch (e.type) {
                                            case SWT.FocusOut:
                                                item.setText(column, text.getText());
                                                text.dispose();
                                                break;
                                            case SWT.Traverse:
                                                switch (e.detail) {
                                                case SWT.TRAVERSE_RETURN:
                                                    item.setText(column, text.getText());
                                                    // fall through
                                                case SWT.TRAVERSE_ESCAPE:
                                                    text.dispose();
                                                    e.doit = false;
                                                }
                                                break;
                                            }
                                        }
                                    };
                                text.addListener(SWT.FocusOut, textListener);
                                text.addListener(SWT.FocusIn, textListener);
                                editor.setEditor(text, item, i);
                                text.setText(item.getText(i));
                                text.selectAll();
                                text.setFocus();
                                return;
                            }
                            if (!visible && rect.intersects(clientArea)) {
                                visible = true;
                            }
                        }
                        if (!visible) {
                            return;
                        }
                        index++;
                    }
                }
            };
        resultsTable.addListener(SWT.MouseDown, tblEditListener);
    }

    private void setupSelectionListener() {
        resultsTable.addListener(
                SWT.Selection,
                new Listener() {
                    public void handleEvent(Event event) {
                        if (event.detail == SWT.CHECK) {
                            TableItem eventItem = (TableItem) event.item;
                            // This assumes that the current status of the TableItem
                            // already reflects its toggled state, which appears to
                            // be the case.
                            boolean checked = eventItem.getChecked();
                            boolean isSelected = false;

                            for (final TableItem item : resultsTable.getSelection()) {
                                if (item == eventItem) {
                                    isSelected = true;
                                    break;
                                }
                            }
                            if (isSelected) {
                                for (final TableItem item : resultsTable.getSelection()) {
                                    item.setChecked(checked);
                                }
                            } else {
                                resultsTable.deselectAll();
                            }
                        }
                        // else, it's a SELECTED event, which we just don't care about
                    }
                });
    }

    private TableColumn setupTableColumn(final int position,
                                         final int width,
                                         final String text)
    {
        final TableColumn col = new TableColumn(resultsTable, SWT.LEFT);
        col.setText(text);
        col.setWidth(width);
        col.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    sortResultsTable(col, position);
                }
            });
        return col;
    }

    private void setupResultsTable() {
        resultsTable = new Table(shell, SWT.CHECK | SWT.FULL_SELECTION | SWT.MULTI);
        resultsTable.setHeaderVisible(true);
        resultsTable.setLinesVisible(true);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        // gridData.widthHint = 780;
        gridData.heightHint = 350;
        gridData.horizontalSpan = 3;
        resultsTable.setLayoutData(gridData);

        boolean isMoveEnabled = prefs.isMoveEnabled();
        setupTableColumn(SELECTED_COLUMN, 60, CHECKBOX_LABEL);
        setupTableColumn(CURRENT_FILE_COLUMN, 550, FILENAME_LABEL);
        destinationColumn = setupTableColumn(NEW_FILENAME_COLUMN, 550,
                                             destColumnText(isMoveEnabled));
        setupTableColumn(STATUS_COLUMN, 60, STATUS_LABEL);

        // Allow deleting of elements
        resultsTable.addKeyListener(
                new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent e) {
                        super.keyReleased(e);

                        switch (e.keyCode) {

                        case '\u0008':
                            // backspace
                            deleteSelectedTableItems();
                            break;

                        case '\u007F':
                            // delete
                            deleteSelectedTableItems();
                            break;
                        }
                    }
                });

        // setupEditableTableListener();
        setupSelectionListener();
    }

    private void setupTableDragDrop() {
        DropTarget dt = new DropTarget(resultsTable, DND.DROP_DEFAULT | DND.DROP_MOVE);
        dt.setTransfer(new Transfer[] {FileTransfer.getInstance()});
        dt.addDropListener(
                new DropTargetAdapter() {

                    @Override
                    public void drop(DropTargetEvent e) {
                        String[] fileList;
                        FileTransfer ft = FileTransfer.getInstance();
                        if (ft.isSupportedType(e.currentDataType)) {
                            fileList = (String[]) e.data;
                            addArrayOfStringsToRenamer(fileList);
                        }
                    }
                });
    }

    private void setupMainWindow() {
        final Composite topButtonsComposite = new Composite(shell, SWT.FILL);
        topButtonsComposite.setLayout(new RowLayout());

        addFilesButton = new Button(topButtonsComposite, SWT.PUSH);
        addFilesButton.setText(ADD_FILES_LABEL);

        addFolderButton = new Button(topButtonsComposite, SWT.PUSH);
        addFolderButton.setText(ADD_FOLDER_LABEL);

        clearFilesButton = new Button(topButtonsComposite, SWT.PUSH);
        clearFilesButton.setText(CLEAR_LIST_LABEL);

        selectAllButton = new Button(topButtonsComposite, SWT.PUSH);
        selectAllButton.setText(SELECT_ALL_LABEL);

        deselectAllButton = new Button(topButtonsComposite, SWT.PUSH);
        deselectAllButton.setText(DESELECT_ALL_LABEL);

        updatesAvailableLink = new Link(topButtonsComposite, SWT.VERTICAL);
        //updatesAvailableLink.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
        updatesAvailableLink.setVisible(false);
        updatesAvailableLink.setText(UPDATE_IS_AVAILABLE_1
                                     + TVRENAMER_DOWNLOAD_URL
                                     + UPDATE_IS_AVAILABLE_2);
        updatesAvailableLink.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent arg0) {
                        Program.launch(TVRENAMER_DOWNLOAD_URL);
                    }
                });

        // Show the label if updates are available (in a new thread)
        Thread updateCheckThread =
            new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (prefs.checkForUpdates()) {
                            final boolean updatesAvailable =
                                UpdateChecker.isUpdateAvailable();

                            if (updatesAvailable) {
                                display.asyncExec(new Runnable() {
                                        @Override
                                        public void run() {
                                            updatesAvailableLink.setVisible(true);
                                        }
                                    });
                            }
                        }
                    }
                });
        updateCheckThread.start();

        setupResultsTable();
        setupTableDragDrop();

        FontData fontData = resultsTable.getFont().getFontData()[0];
        italicFont = new Font(display,
                              new FontData(fontData.getName(),
                                           fontData.getHeight(),
                                           SWT.ITALIC));
        // TODO: note, we never explicitly dispose of this font

        Composite bottomButtonsComposite = new Composite(shell, SWT.FILL);
        bottomButtonsComposite.setLayout(new GridLayout(3, false));
        GridData bottomButtonsCompositeGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        bottomButtonsComposite.setLayoutData(bottomButtonsCompositeGridData);

        final Button quitButton = new Button(bottomButtonsComposite, SWT.PUSH);
        GridData quitButtonGridData =
                new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        quitButtonGridData.minimumWidth = 70;
        quitButtonGridData.widthHint = 70;
        quitButton.setLayoutData(quitButtonGridData);
        quitButton.setText(QUIT_LABEL);

        totalProgressBar = new ProgressBar(bottomButtonsComposite, SWT.SMOOTH);
        totalProgressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        boolean isMoveEnabled = prefs.isMoveEnabled();
        renameSelectedButton = new Button(bottomButtonsComposite, SWT.PUSH);
        GridData renameSelectedButtonGridData =
                new GridData(GridData.END, GridData.CENTER, false, false);
        renameSelectedButton.setLayoutData(renameSelectedButtonGridData);
        renameSelectedButton.setText(renameButtonText(isMoveEnabled));
        renameSelectedButton.setToolTipText(renameButtonToolTip(isMoveEnabled,
                                                                prefs.getDestinationDirectory()
                                                                .getAbsolutePath()));
        renameSelectedButton.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        renameFiles();
                    }
                });

        quitButton.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        doCleanup();
                    }
                });
    }

    private void setupAddFilesDialog() {
        final FileDialog fd = new FileDialog(shell, SWT.MULTI);
        addFilesButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String pathPrefix = fd.open();
                    if (pathPrefix != null) {
                        Path path = Paths.get(pathPrefix);
                        Path parent = path.getParent();

                        String[] fileNames = fd.getFileNames();
                        for (int i = 0; i < fileNames.length; i++) {
                            path = parent.resolve(fileNames[i]);
                            addFileIfVisible(path);
                        }
                    }
                }
            });
    }

    private void setupAddFolderDialog() {
        final DirectoryDialog dd = new DirectoryDialog(shell, SWT.SINGLE);
        addFolderButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String directory = dd.open();
                    if (directory != null) {
                        // load all of the files in the dir
                        addFolderToRenamer(Paths.get(directory));
                    }
                }
            });
    }

    private void setupClearFilesButton() {

        clearFilesButton.addSelectionListener(
                new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        resultsTable.removeAll();
                    }
                });
    }

    private void setupSelectButtons() {
        selectAllButton.addSelectionListener(
                new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        for (final TableItem item : getTableItems()) {
                            item.setChecked(true);
                        }
                    }
                });
        deselectAllButton.addSelectionListener(
                new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        for (final TableItem item : getTableItems()) {
                            item.setChecked(false);
                        }
                    }
                });
    }

    private Menu setupHelpMenuBar(Menu menuBar) {
        MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        helpMenuHeader.setText(HELP_LABEL);

        Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
        helpMenuHeader.setMenu(helpMenu);

        MenuItem helpHelpItem = new MenuItem(helpMenu, SWT.PUSH);
        helpHelpItem.setText(HELP_LABEL);

        MenuItem helpVisitWebpageItem = new MenuItem(helpMenu, SWT.PUSH);
        helpVisitWebpageItem.setText(VISIT_WEBPAGE);
        helpVisitWebpageItem.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Program.launch(TVRENAMER_PROJECT_URL);
                    }
                });

        return helpMenu;
    }

    private void setupMenuBar() {
        Menu menuBarMenu = new Menu(shell, SWT.BAR);
        Menu helpMenu;

        Listener preferencesListener = new Listener() {
                @Override
                public void handleEvent(Event e) {
                    showPreferencesPane();
                }
            };

        Listener aboutListener = new Listener() {
                @Override
                public void handleEvent(Event e) {
                    showAboutPane();
                }
            };

        Listener quitListener = new Listener() {
                @Override
                public void handleEvent(Event e) {
                    doCleanup();
                }
            };

        if (Environment.IS_MAC_OSX) {
            // Add the special Mac OSX Preferences, About and Quit menus.
            CocoaUIEnhancer enhancer = new CocoaUIEnhancer(APPLICATION_NAME);
            enhancer.hookApplicationMenu(display, quitListener,
                                         aboutListener, preferencesListener);
            setupHelpMenuBar(menuBarMenu);
        } else {
            // Add the normal Preferences, About and Quit menus.
            MenuItem fileMenuItem = new MenuItem(menuBarMenu, SWT.CASCADE);
            fileMenuItem.setText(FILE_MENU_LABEL);

            Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
            fileMenuItem.setMenu(fileMenu);

            MenuItem filePreferencesItem = new MenuItem(fileMenu, SWT.PUSH);
            filePreferencesItem.setText(PREFERENCES_LABEL);
            filePreferencesItem.addListener(SWT.Selection, preferencesListener);

            MenuItem fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
            fileExitItem.setText(EXIT_LABEL);
            fileExitItem.addListener(SWT.Selection, quitListener);

            helpMenu = setupHelpMenuBar(menuBarMenu);

            // The About item is added to the OSX bar, so we need to add it manually here
            MenuItem helpAboutItem = new MenuItem(helpMenu, SWT.PUSH);
            helpAboutItem.setText(ABOUT_MENU_LABEL);
            helpAboutItem.addListener(SWT.Selection, aboutListener);
        }

        shell.setMenuBar(menuBarMenu);
    }

    private void setupIcons() {
        try {
            InputStream icon = getClass().getResourceAsStream(TVRENAMER_ICON_PATH);
            if (icon != null) {
                shell.setImage(new Image(display, icon));
            } else {
                shell.setImage(new Image(display, ICON_PARENT_DIRECTORY + TVRENAMER_ICON_PATH));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() {
        prefs.addObserver(this);

        // Setup display and shell
        GridLayout shellGridLayout = new GridLayout(3, false);
        Display.setAppName(APPLICATION_NAME);
        display = new Display();

        shell = new Shell(display);

        shell.setText(APPLICATION_NAME);
        shell.setLayout(shellGridLayout);

        // Setup the util class
        UIUtils.setShell(shell);

        // Add controls to main shell
        setupMainWindow();
        setupAddFilesDialog();
        setupAddFolderDialog();
        setupClearFilesButton();
        setupSelectButtons();
        setupMenuBar();

        setupIcons();

        shell.pack(true);
    }

    private void launch() {
        try {
            // place the window in the bottom right of the primary monitor
            Monitor primary = display.getPrimaryMonitor();
            Rectangle bounds = primary.getBounds();
            Rectangle rect = shell.getBounds();
            int x = bounds.x + bounds.width - rect.width - 5;
            int y = bounds.y + bounds.height - rect.height - 35;
            shell.setLocation(x, y);

            // Start the shell
            shell.pack();
            shell.open();
            isUIRunning = true;

            // TODO: trying to get the UI thread to be more responsive.
            // So far, this doesn't seem to have much, if any, effect.
            // But it's probably part of the solution, even if it's not
            // sufficient, so leave it in here.
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            // Update the list of ignored keywords
            ignoreKeywords = prefs.getIgnoreKeywords();

            if (preloadFolder != null) {
                addFolderToRenamer(preloadFolder);
            }

            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            doCleanup();
        } catch (IllegalArgumentException argumentException) {
            logger.log(Level.SEVERE, NO_DND, argumentException);
            JOptionPane.showMessageDialog(null, NO_DND);
            System.exit(1);
        } catch (Exception exception) {
            UIUtils.showErrorMessageBox(ERROR_LABEL, UNKNOWN_EXCEPTION, exception);
            logger.log(Level.SEVERE, UNKNOWN_EXCEPTION, exception);
            System.exit(2);
        }
    }

    public void run() {
        init();
        launch();
    }

    public UIStarter(Path preloadFolder) {
        prefs = UserPreferences.getInstance();
        this.preloadFolder = preloadFolder;
        episodeMap = new EpisodeDb();
    }
}
