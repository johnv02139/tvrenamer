package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import static org.tvrenamer.view.UIUtils.showMessageBox;

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
import org.tvrenamer.model.SWTMessageBoxType;
import org.tvrenamer.model.Series;
import org.tvrenamer.model.ShowStore;
import org.tvrenamer.model.UnresolvedShow;
import org.tvrenamer.model.UserPreference;
import org.tvrenamer.model.UserPreferences;
import org.tvrenamer.model.except.NotFoundException;
import org.tvrenamer.model.util.Environment;

import java.io.File;
import java.io.InputStream;
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
    private final File preloadFolder;
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
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream != null) {
                icon = new Image(Display.getCurrent(), stream);
            } else {
                icon = new Image(Display.getCurrent(),
                                 ICON_PARENT_DIRECTORY
                                 + File.separator
                                 + path);
            }
        }
    }

    private static Image getFileMoveIcon(final FileEpisode episode) {
        if (episode.isNewlyAdded()) {
            return FileMoveIcon.ADDED.icon;
        } else if (episode.isInvestigating()) {
            return FileMoveIcon.DOWNLOADING.icon;
        } else if (episode.isReady()) {
            return FileMoveIcon.SUCCESS.icon;
        } else if (episode.isFailed()) {
            return FileMoveIcon.FAIL.icon;
        } else if (episode.isRenameInProgress()) {
            return FileMoveIcon.RENAMING.icon;
        } else {
            return FileMoveIcon.NOPARSE.icon;
        }
    }

    private static String getFailMessage(final FileEpisode ep) {
        if (ep.isFailed()) {
            final Series epSeries = ep.getSeries();
            // BROKEN_PLACEHOLDER_FILENAME;
            String failMsg = DOWNLOADING_FAILED_MESSAGE;
            if (epSeries instanceof UnresolvedShow) {
                UnresolvedShow f = (UnresolvedShow) epSeries;
                if (f.getException() == null) {
                    failMsg = NO_SUCH_SHOW_MESSAGE + f.getName();
                }
            }
            return failMsg;
        } else {
            return "Internal error: fail message for non-failure";
        }
    }

    private static String valueForNewFilename(FileEpisode episode) {
        if (episode.isFailed()) {
            return getFailMessage(episode);
        } else if (episode.isReady()) {
            return episode.getProposedFilename();
        } else if (episode.isSeriesReady()) {
            return "Processing episodes...";
        } else {
            return ADDED_PLACEHOLDER_FILENAME;
        }
    }

    private static String renameButtonText(boolean isMoveEnabled) {
        if (isMoveEnabled) {
            return "Rename && Move Selected";
        } else {
            return "Rename Selected";
        }
    }

    private static String renameButtonToolTip(boolean isMoveEnabled,
                                              String destDirectory)
    {
        if (isMoveEnabled) {
            return "Clicking this button will rename and move "
                + "the selected files to the directory set "
                + "in preferences (currently "
                + destDirectory
                + ").";
        } else {
            return "Clicking this button will rename the selected "
                + "files but leave them where they are.";
        }
    }

    private static String destColumnText(boolean isMoveEnabled) {
        if (isMoveEnabled) {
            return "Proposed File Path";
        } else {
            return "Proposed File Name";
        }
    }

    private TableItem[] getTableItems() {
        return resultsTable.getItems();
    }

    private TableItem getTableItem(int index) {
        return resultsTable.getItem(index);
    }

    public void onEpisodeUpdate(final FileEpisode ep) {
        if (!isUIRunning || display.isDisposed() || (ep == null)) {
            return;
        }
        final TableItem item = ep.getViewItem();
        if (item == null) {
            logger.info("episode not associated with table item: " + ep);
            return;
        }
        display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    synchronized (ep) {
                        final TableItem item = ep.getViewItem();
                        if ((item == null) || item.isDisposed()) {
                            logger.info("episode not associated with table item: " + ep);
                            ep.setViewItem(null);
                            return;
                        }
                        item.setText(NEW_FILENAME_COLUMN, valueForNewFilename(ep));
                        item.setImage(STATUS_COLUMN, getFileMoveIcon(ep));
                        item.setChecked(ep.isReady());
                    }
                }
            });
    }

    private TableItem failToParseTableItem(TableItem item, String fileName) {
        logger.severe("Couldn't parse file: " + fileName);
        item.setImage(STATUS_COLUMN, FileMoveIcon.NOPARSE.icon);
        item.setGrayed(true); // makes checkbox use a dot; very weird
        item.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
        item.setFont(italicFont);
        item.setText(NEW_FILENAME_COLUMN, "filename not parsed");
        return item;
    }

    private void refreshTable() {
        logger.info("Refreshing table");
        for (TableItem item : getTableItems()) {
            String fileName = item.getText(CURRENT_FILE_COLUMN);
            FileEpisode episode = episodeMap.remove(fileName);
            if (episode == null) {
                failToParseTableItem(item, fileName);
            } else {
                String newFileName = episode.getFile().toAbsolutePath().toString();
                episodeMap.put(newFileName, episode);
                item.setText(CURRENT_FILE_COLUMN, newFileName);
                item.setText(NEW_FILENAME_COLUMN, valueForNewFilename(episode));
            }
        }
    }

    private boolean isNameIgnored(String fileName) {
        for (int i = 0; i < ignoreKeywords.size(); i++) {
            if (fileName.toLowerCase().contains(ignoreKeywords.get(i))) {
                return true;
            }
        }
        return false;
    }

    private TableItem createTableItem(FileEpisode episode) {
        TableItem item = new TableItem(resultsTable, SWT.NONE);
        item.setData(episode);
        episode.setViewItem(item);

        String fileName = episode.getFilename();
        item.setText(CURRENT_FILE_COLUMN, fileName);

        if (episode.wasNotParsed()) {
            return failToParseTableItem(item, fileName);
        }
        String newFilename = fileName;
        try {
            // Set if the item is checked or not according
            // to a list of banned keywords
            item.setChecked(!isNameIgnored(newFilename));
            // TODO: this used to get just the basename (no directory), even if
            // move was enabled.  Why?
            newFilename = valueForNewFilename(episode);
            item.setImage(STATUS_COLUMN, FileMoveIcon.DOWNLOADING.icon);
        } catch (NotFoundException e) {
            item.setChecked(false);
            newFilename = e.getMessage();
            item.setImage(STATUS_COLUMN, FileMoveIcon.FAIL.icon);
            item.setForeground(display.getSystemColor(SWT.COLOR_RED));
        }
        item.setText(NEW_FILENAME_COLUMN, newFilename);
        return item;
    }

    private int getTableItemIndex(TableItem item) {
        try {
            return resultsTable.indexOf(item);
        } catch (IllegalArgumentException | SWTException ignored) {
            // We'll just fall through and return the sentinel.
        }
        return ITEM_NOT_IN_TABLE;
    }

    private void addListOfFiles(final List<String> fileNames) {
        // Update the list of ignored keywords
        ignoreKeywords = prefs.getIgnoreKeywords();

        for (final String fileName : fileNames) {
            final FileEpisode episode = episodeMap.add(fileName);
            episode.listen(this);
            // We add the file to the table even if we couldn't parse the filename
            final TableItem item = createTableItem(episode);
            episode.lookupSeries();
        }
    }

    private void addFilesToList(File file, List<String> fileList, boolean descend) {
        if (file.exists()) {
            if (file.isHidden()) {
                logger.finer("ignoring hidden file " + file);
            } else if (file.isDirectory()) {
                if (descend) {
                    String[] fileNames = file.list();
                    if (fileNames != null) {
                        for (final String fileName : fileNames) {
                            // recursive call
                            addFilesToList(new File(file, fileName), fileList, true);
                        }
                    }
                }
            } else {
                fileList.add(file.getAbsolutePath());
            }
        }
    }

    private void initiateRenamer(final String[] fileNames) {
        final List<String> files = new LinkedList<>();
        for (final String fileName : fileNames) {
            addFilesToList(new File(fileName), files,
                           prefs.isRecursivelyAddFolders());
        }
        addListOfFiles(files);
    }

    private void initiateRenamer(final File file) {
        String[] fileArray = new String[1];
        fileArray[0] = file.getAbsolutePath();
        initiateRenamer(fileArray);
    }

    private void addSelectedFiles(FileDialog fd) {
        String pathPrefix = fd.open();
        if (pathPrefix != null) {
            File file = new File(pathPrefix);
            String parent = file.getParent();

            String[] fileNames = fd.getFileNames();
            for (int i = 0; i < fileNames.length; i++) {
                fileNames[i] = parent + File.separatorChar + fileNames[i];
            }

            initiateRenamer(fileNames);
        }
    }

    private void addSelectedFolder(DirectoryDialog dd) {
        String directory = dd.open();
        if (directory != null) {
            // load all of the files in the dir
            File file = new File(directory);
            initiateRenamer(file);
        }
    }

    private FileEpisode getTableItemEpisode(final TableItem item) {
        FileEpisode episode = null;
        Object data = item.getData();
        if ((data != null) && (data instanceof FileEpisode)) {
            return (FileEpisode) data;
        }
        throw new IllegalStateException("TableItem does not contain FileEpisode: " + item);
    }

    private void setSortedItem(int i, int j) {
        TableItem oldItem = getTableItem(i);
        FileEpisode oldEpisode = getTableItemEpisode(oldItem);

        boolean wasChecked = oldItem.getChecked();
        int oldStyle = oldItem.getStyle();

        TableItem item = new TableItem(resultsTable, oldStyle, j);
        item.setChecked(wasChecked);
        item.setText(CURRENT_FILE_COLUMN, oldItem.getText(CURRENT_FILE_COLUMN));
        item.setText(NEW_FILENAME_COLUMN, oldItem.getText(NEW_FILENAME_COLUMN));
        item.setImage(STATUS_COLUMN, oldItem.getImage(STATUS_COLUMN));
        item.setChecked(wasChecked);
        item.setData(oldEpisode);

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

            String filename = item.getText(CURRENT_FILE_COLUMN);
            episodeMap.remove(filename);

            resultsTable.remove(index);
            item.dispose();
        }
        resultsTable.deselectAll();
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

    private void renameFiles() {
        final Queue<Future<Boolean>> futures = new LinkedList<>();

        for (final TableItem item : getTableItems()) {
            if (item.getChecked()) {
                final String fileName = item.getText(CURRENT_FILE_COLUMN);
                final File currentFile = new File(fileName);
                final FileEpisode episode = episodeMap.get(fileName);

                // Skip files not successfully downloaded
                if (!episode.isDownloaded()) {
                    continue;
                }

                String newName = item.getText(NEW_FILENAME_COLUMN);
                File newFile = null;
                if (prefs != null && prefs.isMoveEnabled()) {
                    // If move is enabled, the full path is in the table already
                    newFile = new File(newName);
                } else {
                    // Else we need to build it
                    String newFilePath = currentFile.getParent() + File.separatorChar + newName;
                    newFile = new File(newFilePath);
                }

                logger.info("Going to move '" + currentFile.getAbsolutePath()
                            + "' to '" + newFile.getAbsolutePath() + "'");

                String currentName = currentFile.getName();
                if (newName.equals(currentName)) {
                    logger.info("nothing to be done to " + currentName);
                } else {
                    Callable<Boolean> moveCallable = new FileMover(episode, newFile);
                    futures.add(executor.submit(moveCallable));
                    item.setChecked(false);
                }
            }
        }

        final TaskItem taskItem = getTaskItem();
        // There is no task bar on linux
        if (taskItem != null) {
            taskItem.setProgressState(SWT.NORMAL);
            taskItem.setOverlayImage(FileMoveIcon.RENAMING.icon);

            Thread progressThread =
                new Thread(new ProgressBarUpdater(new ProgressProxy() {
                        @Override
                        public void setProgress(final float progress) {
                            if (display.isDisposed()) {
                                return;
                            }

                            display.asyncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (totalProgressBar.isDisposed()) {
                                            return;
                                        }
                                        totalProgressBar.setSelection(Math.round(progress * totalProgressBar.getMaximum()));
                                        if (taskItem.isDisposed()) {
                                            return;
                                        }
                                        taskItem.setProgress(Math.round(progress * 100));
                                    }
                                });
                        }
                    },
                        futures,
                        new UpdateCompleteHandler() {
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
                        }));
            progressThread.setName("ProgressBarThread");
            progressThread.setDaemon(true);
            progressThread.start();
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
        if (prefs.isMoveEnabled()) {
            renameSelectedButton.setText("Rename && Move Selected");
            shell.changed(new Control[] {renameSelectedButton});
            shell.layout(false, true);
        } else {
            renameSelectedButton.setText("Rename Selected");
            shell.changed(new Control[] {renameSelectedButton});
            shell.layout(false, true);
        }
    }

    private void setupMoveButtonText() {
        setRenameButtonText();
        renameSelectedButton.setToolTipText("Clicking this button will rename and move "
                                            + "the selected files to the directory set "
                                            + "in preferences (currently "
                                            + prefs.getDestinationDirectory().getAbsolutePath()
                                            + ").");
    }

    private void setupRenameButtonText() {
        setRenameButtonText();
        renameSelectedButton.setToolTipText("Clicking this button will rename the selected "
                                            + "files but leave them where they are.");
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
        setupTableColumn(SELECTED_COLUMN, 60, "Selected");
        setupTableColumn(CURRENT_FILE_COLUMN, 550, "Current File");
        destinationColumn = setupTableColumn(NEW_FILENAME_COLUMN, 550,
                                             destColumnText(isMoveEnabled));
        setupTableColumn(STATUS_COLUMN, 60, "Status");

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
                            initiateRenamer(fileList);
                        }
                    }
                });
    }

    private void setupMainWindow() {
        final Composite topButtonsComposite = new Composite(shell, SWT.FILL);
        topButtonsComposite.setLayout(new RowLayout());

        addFilesButton = new Button(topButtonsComposite, SWT.PUSH);
        addFilesButton.setText("Add files");

        addFolderButton = new Button(topButtonsComposite, SWT.PUSH);
        addFolderButton.setText("Add Folder");

        clearFilesButton = new Button(topButtonsComposite, SWT.PUSH);
        clearFilesButton.setText("Clear List");

        selectAllButton = new Button(topButtonsComposite, SWT.PUSH);
        selectAllButton.setText("Select All");

        deselectAllButton = new Button(topButtonsComposite, SWT.PUSH);
        deselectAllButton.setText("Deselect All");

        updatesAvailableLink = new Link(topButtonsComposite, SWT.VERTICAL);
        //updatesAvailableLink.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
        updatesAvailableLink.setVisible(false);
        updatesAvailableLink.setText("There is an update available. <a href=\""
                                     + TVRENAMER_DOWNLOAD_URL
                                     + "\">Click here to download</a>");
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
        GridData bottomButtonsCompositeGridData =
                new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        bottomButtonsComposite.setLayoutData(bottomButtonsCompositeGridData);

        final Button quitButton = new Button(bottomButtonsComposite, SWT.PUSH);
        GridData quitButtonGridData =
                new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        quitButtonGridData.minimumWidth = 70;
        quitButtonGridData.widthHint = 70;
        quitButton.setLayoutData(quitButtonGridData);
        quitButton.setText("Quit");

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
                    addSelectedFiles(fd);
                }
            });

        final DirectoryDialog dd = new DirectoryDialog(shell, SWT.SINGLE);
        addFolderButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    addSelectedFolder(dd);
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
        helpMenuHeader.setText("Help");

        Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
        helpMenuHeader.setMenu(helpMenu);

        MenuItem helpHelpItem = new MenuItem(helpMenu, SWT.PUSH);
        helpHelpItem.setText("Help");

        MenuItem helpVisitWebpageItem = new MenuItem(helpMenu, SWT.PUSH);
        helpVisitWebpageItem.setText("Visit Webpage");
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
            fileMenuItem.setText("File");

            Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
            fileMenuItem.setMenu(fileMenu);

            MenuItem filePreferencesItem = new MenuItem(fileMenu, SWT.PUSH);
            filePreferencesItem.setText("Preferences");
            filePreferencesItem.addListener(SWT.Selection, preferencesListener);

            MenuItem fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
            fileExitItem.setText("Exit");
            fileExitItem.addListener(SWT.Selection, quitListener);

            helpMenu = setupHelpMenuBar(menuBarMenu);

            // The About item is added to the OSX bar, so we need to add it manually here
            MenuItem helpAboutItem = new MenuItem(helpMenu, SWT.PUSH);
            helpAboutItem.setText("About");
            helpAboutItem.addListener(SWT.Selection, aboutListener);
        }

        shell.setMenuBar(menuBarMenu);
    }

    private void setupIcons() {
        try {
            InputStream icon = getClass().getResourceAsStream("/icons/tvrenamer.png");
            if (icon != null) {
                shell.setImage(new Image(display, icon));
            } else {
                shell.setImage(new Image(display, "res/icons/tvrenamer.png"));
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
        new UIUtils(shell);

        // Add controls to main shell
        setupMainWindow();
        setupAddFilesDialog();
        setupClearFilesButton();
        setupSelectButtons();
        setupMenuBar();

        setupIcons();

        shell.pack(true);
    }

    private void launch() {
        try {
            // place the window in the centre of the primary monitor
            Monitor primary = display.getPrimaryMonitor();
            Rectangle bounds = primary.getBounds();
            Rectangle rect = shell.getBounds();
            int x = bounds.x + (bounds.width - rect.width) / 2;
            int y = bounds.y + (bounds.height - rect.height) / 2;
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

            if (preloadFolder != null) {
                initiateRenamer(preloadFolder);
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
            showMessageBox(SWTMessageBoxType.ERROR, "Error",
                           UNKNOWN_EXCEPTION, exception);
            logger.log(Level.SEVERE, UNKNOWN_EXCEPTION, exception);
            System.exit(2);
        }
    }

    public void run() {
        init();
        launch();
    }

    public UIStarter(File preloadFolder) {
        prefs = UserPreferences.getInstance();
        this.preloadFolder = preloadFolder;
        episodeMap = new EpisodeDb();
    }
}
