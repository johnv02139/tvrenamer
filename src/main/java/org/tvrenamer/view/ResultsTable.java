package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;
import static org.tvrenamer.view.Fields.*;

import org.eclipse.swt.SWT;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TaskBar;
import org.eclipse.swt.widgets.TaskItem;

import org.tvrenamer.controller.AddEpisodeListener;
import org.tvrenamer.controller.FileMover;
import org.tvrenamer.controller.MoveRunner;
import org.tvrenamer.controller.UpdateChecker;
import org.tvrenamer.controller.UrlLauncher;
import org.tvrenamer.model.AppData;
import org.tvrenamer.model.EpisodeDb;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.UserPreference;
import org.tvrenamer.model.UserPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class ResultsTable implements Observer, AddEpisodeListener {
    private static final Logger logger = Logger.getLogger(ResultsTable.class.getName());
    // load preferences
    private static final UserPreferences prefs = UserPreferences.getInstance();
    private static final AppData appData = AppData.getInstance();

    private final UIStarter ui;
    private final Shell shell;
    private final Display display;
    private final Table swtTable;
    private final List<EpisodeView> views = new ArrayList<>(1000);
    private final Set<TableItem> lastKnownSelection = new HashSet<>();
    final EpisodeDb episodeMap = new EpisodeDb();

    private Button actionButton;
    private ProgressBar totalProgressBar;
    private TaskItem taskItem = null;

    private boolean apiDeprecated = false;

    private synchronized void checkDestinationDirectory() {
        boolean success = prefs.ensureDestDir();
        if (!success) {
            String errMsg = prefs.getLastError();
            if (errMsg == null) {
                errMsg = CANT_CREATE_DEST;
            } else {
                prefs.clearLastError();
            }
            logger.warning(errMsg);
            ui.showMessageBox(SWTMessageBoxType.DLG_ERR, ERROR_LABEL, errMsg);
        }
    }

    void ready() {
        prefs.addObserver(this);
        swtTable.setFocus();

        checkDestinationDirectory();

        // Load the preload folder into the episode map, which will call
        // us back with the list of files once they've been loaded.
        episodeMap.subscribe(this);
        episodeMap.preload();
    }

    Display getDisplay() {
        return display;
    }

    ProgressBar getProgressBar() {
        return totalProgressBar;
    }

    TaskItem getTaskItem() {
        return taskItem;
    }

    Combo newComboBox() {
        if (swtTable.isDisposed()) {
            return null;
        }
        return new Combo(swtTable, SWT.DROP_DOWN | SWT.READ_ONLY);
    }

    TableItem newTableItem() {
        return new TableItem(swtTable, SWT.NONE);
    }

    private String itemText(final TableItem item) {
        try {
            if (item == null) {
                return "null TableItem";
            }
            final String currentFile = CURRENT_FILE_FIELD.getCellText(item);
            if (currentFile == null) {
                return "TableItem with null \"current file\"";
            }
            if (currentFile.length() == 0) {
                return "TableItem with empty \"current file\"";
            }
            return currentFile;
        } catch (Exception e) {
            return "TableItem that had exception accessing: " + e.getMessage();
        }
    }

    synchronized void noteApiFailure() {
        boolean showDialogBox = !apiDeprecated;
        apiDeprecated = true;
        if (showDialogBox) {
            boolean updateIsAvailable = UpdateChecker.isUpdateAvailable();
            ui.showMessageBox(SWTMessageBoxType.DLG_ERR, ERROR_LABEL,
                              updateIsAvailable ? GET_UPDATE_MESSAGE : NEED_UPDATE);
        }
    }

    private void renderEpisodeView(final EpisodeView epview) {
        final TableItem item = epview.getItem();
        if (item == null) {
            logger.severe("could not refresh " + epview);
            return;
        }
        epview.refreshTableItem();
    }

    @Override
    public void addEpisodes(final Queue<FileEpisode> episodes) {
        final List<EpisodeView> newViews = new ArrayList<>(episodes.size());
        for (final FileEpisode episode : episodes) {
            final EpisodeView epview = new EpisodeView(this, episode);
            newViews.add(epview);
            renderEpisodeView(epview);
        }

        synchronized (this) {
            if (apiDeprecated) {
                newViews.forEach(EpisodeView::setFail);
            }
        }

        synchronized (views) {
            views.addAll(newViews.stream().collect(Collectors.toList()));
        }

        newViews.forEach(EpisodeView::lookupShow);
    }

    /**
     * Returns (and, really, creates) a progress label for the given item.
     * This is used to display progress while the item's file is being copied.
     * (We don't actually support "copying" the file, only moving it, but when
     * the user chooses to "move" it across filesystems, that becomes a copy-
     * and-delete operation.)
     *
     * @param item
     *    the item to create a progress label for
     * @return
     *    a Label which is set as an editor for the status field of the given item
     */
    public Label getProgressLabel(final TableItem item) {
        Label progressLabel = new Label(swtTable, SWT.SHADOW_NONE | SWT.CENTER);
        TableEditor editor = new TableEditor(swtTable);
        editor.grabHorizontal = true;
        STATUS_FIELD.setEditor(item, editor, progressLabel);

        return progressLabel;
    }

    private EpisodeView getEpisodeView(final TableItem item) {
        if (item == null) {
            logger.severe("null table item!");
            return null;
        }
        final Object itemData = item.getData();
        if (itemData != null) {
            if (itemData instanceof EpisodeView) {
                final EpisodeView epview = (EpisodeView) itemData;
                if (epview.getItem() == item) {
                    return epview;
                }
            }
        }
        final String itemText = itemText(item);
        if (itemData == null) {
            logger.severe("table item with no episode view: " + itemText);
            return null;
        }
        if (itemData instanceof EpisodeView) {
            final EpisodeView epview = (EpisodeView) itemData;
            logger.severe("inconsistent table item state: user data of "
                          + itemText + " is " + epview);
            final TableItem epviewItem = epview.getItem();
            logger.severe("but the item for that view is " + itemText(epviewItem));
            return null;
        }
        logger.severe("serious internal error: table item data for \"" + itemText
                      + "\" is of wrong type: " + itemData);
        return null;
    }

    void renameFiles() {
        if (!prefs.isMoveEnabled() && !prefs.isRenameSelected()) {
            logger.info("move and rename both disabled, nothing to be done.");
            return;
        }

        final List<FileMover> pendingMoves = new LinkedList<>();
        for (final TableItem item : swtTable.getItems()) {
            if (item.getChecked()) {
                final EpisodeView epview = getEpisodeView(item);
                if (epview == null) {
                    logger.severe("(checked) item with no episode view: " + itemText(item));
                    continue;
                }
                final FileEpisode episode = epview.getEpisode();
                // Skip files not successfully downloaded and ready to be moved
                if (episode.optionCount() == 0) {
                    logger.info("checked but not ready: " + itemText(item));
                    continue;
                }
                FileMover pendingMove = new FileMover(episode);
                pendingMove.addObserver(epview);
                pendingMoves.add(pendingMove);
            }
        }

        MoveRunner mover = new MoveRunner(pendingMoves);
        mover.setUpdater(new ProgressBarUpdater(this));
        mover.runThread();

        for (final TableItem item : swtTable.getItems()) {
            item.setChecked(false);
        }
        swtTable.setFocus();
    }

    /**
     * Sort the table by the given column in the given direction.
     *
     * @param column
     *    the Column to sort by
     * @param sortDirection
     *    the direction to sort by; SWT.UP means sort A-Z, while SWT.DOWN is Z-A
     */
    void sortTable(final Column column, final int sortDirection) {
        Collections.sort(views, new EpisodeView.Comparator(column.field, sortDirection));
        int nViews = views.size();

        // Get the items
        TableItem[] items = swtTable.getItems();
        int nRows = items.length;

        if (nRows != nViews) {
            String msg = "mismatch between UI items (" + nRows
                + ") and internal views (" + nViews + ")";
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }

        int i = 0;
        for (EpisodeView view : views) {
            view.replaceTableItem(items[i++]);
        }

        swtTable.setSortDirection(sortDirection);
        swtTable.setSortColumn(column.swtColumn);
    }

    /**
     * Refreshes the "destination" and "status" field of all items in the table.
     *
     * This is intended to be called after something happens which changes what the
     * proposed destination would be.  The destination is determined partly by how
     * we parse the filename, of course, but also based on numerous fields that the
     * user sets in the Preferences Dialog.  When the user closes the dialog and
     * saves the changes, we want to immediately update the table for the new choices
     * specified.  This method iterates over each item, makes sure the model is
     * updated ({@link FileEpisode}), and then updates the relevant fields.
     *
     * (Doesn't bother updating other fields, because we know nothing in the
     * Preferences Dialog can cause them to need to be changed.)
     */
    public void refreshDestinations() {
        logger.info("Refreshing destinations");
        for (TableItem item : swtTable.getItems()) {
            final EpisodeView epview = getEpisodeView(item);
            if (epview == null) {
                logger.severe("disposing of table row that had no episode view: "
                              + itemText(item));
                // TODO: could do getEditor()?
                item.dispose();
            } else {
                epview.refreshProposedDest();
            }
        }
    }

    private void setActionButtonText(final Button b) {
        String label = JUST_MOVE_LABEL;
        if (prefs.isRenameSelected()) {
            if (prefs.isMoveSelected()) {
                label = RENAME_AND_MOVE;
            } else {
                label = RENAME_LABEL;
            }
            // In the unlikely and erroneous case where neither is selected,
            // we'll still stick with JUST_MOVE_LABEL for the label.
        }
        b.setText(label);

        // Enable the button, in case it had been disabled before.  But we may
        // disable it again, below.
        b.setEnabled(true);

        String tooltip = RENAME_TOOLTIP;
        if (prefs.isMoveSelected()) {
            if (prefs.isMoveEnabled()) {
                tooltip = INTRO_MOVE_DIR + prefs.getDestinationDirectoryName()
                    + FINISH_MOVE_DIR;
                if (prefs.isRenameSelected()) {
                    tooltip = MOVE_INTRO + AND_RENAME + tooltip;
                } else {
                    tooltip = MOVE_INTRO + tooltip;
                }
            } else {
                b.setEnabled(false);
                tooltip = CANT_CREATE_DEST + ". " + MOVE_NOT_POSSIBLE;
            }
        } else if (!prefs.isRenameSelected()) {
            // This setting, "do not move and do not rename", really makes no sense.
            // But for now, we're not taking the effort to explicitly disable it.
            b.setEnabled(false);
            tooltip = NO_ACTION_TOOLTIP;
        }
        b.setToolTipText(tooltip);

        shell.changed(new Control[] {b});
        shell.layout(false, true);
    }

    private void setColumnDestText() {
        final TableColumn destinationColumn = NEW_FILENAME_FIELD.getTableColumn();
        if (destinationColumn == null) {
            logger.warning("could not get destination column");
        } else if (prefs.isMoveSelected()) {
            destinationColumn.setText(MOVE_HEADER);
        } else {
            destinationColumn.setText(RENAME_HEADER);
        }
    }

    private void deleteTableItem(final TableItem item) {
        final EpisodeView epview = getEpisodeView(item);
        String currentFile = itemText(item);
        if (epview == null) {
            logger.warning("deleting table item that lacks episode view: " + currentFile);
        } else {
            boolean removed = views.remove(epview);
            if (!removed) {
                logger.warning("deleting episode view that was not in list: "
                           + epview);
            }
            epview.delete();
        }
        episodeMap.remove(currentFile);
        item.dispose();
    }

    private void deleteSelectedTableItems() {
        for (final TableItem item : swtTable.getSelection()) {
            deleteTableItem(item);
        }
        swtTable.deselectAll();
    }

    private void updateUserPreferences(final UserPreference userPref) {
        logger.info("Preference change event: " + userPref);

        switch (userPref) {
            case RENAME_SELECTED:
            case MOVE_SELECTED:
            case DEST_DIR:
                checkDestinationDirectory();
                setColumnDestText();
                setActionButtonText(actionButton);
                // Note: NO break!  We WANT to fall through.
            case REPLACEMENT_MASK:
            case SEASON_PREFIX:
            case LEADING_ZERO:
                refreshDestinations();
            // Also note, no default case.  We know there are other types of
            // UserPreference events that we might be notified of.  We're
            // just not interested.
        }
    }

    /* (non-Javadoc)
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(final Observable observable, final Object value) {
        if (observable instanceof UserPreferences && value instanceof UserPreference) {
            updateUserPreferences((UserPreference) value);
        }
    }

    void finishAllMoves() {
        ui.setAppIcon();
    }

    /*
     * The table displays various data; a lot of it changes during the course of the
     * program.  As we get information from the provider, we automatically update the
     * status, the proposed destination, even whether the row is checked or not.
     *
     * The one thing we don't automatically update is the location.  That's something
     * that doesn't change, no matter how much information comes flowing in.  EXCEPT...
     * that's kind of the whole point of the program, to move files.  So when we actually
     * do move a file, we need to update things in some way.
     *
     * The program now has the "deleteRowAfterMove" option, which I recommend.  But if
     * we do not delete the row, then we need to update it.
     *
     * We also need to update the internal model we have of which files we're working with.
     *
     * So, here's what we do:
     *  1) find the text that is CURRENTLY being displayed as the file's location
     *  2) ask EpisodeDb to look up that file, figure out where it now resides, update its
     *     own internal model, and then return to us the current location
     *  3) assuming the file was found, check to see if it was really moved
     *  4) if it actually was moved, update the row with the most current information
     *
     * We do all this only after checking the row is still valid, and then we do it
     * with the item locked, so it can't change out from under us.
     *
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private void updateTableItemAfterMove(final TableItem item) {
        synchronized (item) {
            if (item.isDisposed()) {
                return;
            }
            final EpisodeView epview = getEpisodeView(item);
            String fileName = CURRENT_FILE_FIELD.getCellText(item);
            if (epview == null) {
                logger.severe("disposing of table row that had no episode view: "
                              + fileName);
                // TODO: could do getEditor()?
                item.dispose();
            } else {
                String newLocation = episodeMap.currentLocationOf(fileName);
                if (newLocation == null) {
                    // Not expected, but could happen, primarily if some other,
                    // unrelated program moves the file out from under us.
                    deleteTableItem(item);
                    return;
                }
                if (!fileName.equals(newLocation)) {
                    epview.onFileMoved(newLocation);
                    renderEpisodeView(epview);
                }
            }
        }
    }

    /**
     * A callback that indicates that the {@link FileMover} has finished moving
     * a file, the one displayed in the given item.  We want to take an action
     * when the move has been finished.
     *
     * The specific action depends on the user preference, "deleteRowAfterMove".
     * As its name suggests, when it's true, and we successfully move the file,
     * we delete the TableItem from the table.
     *
     * If "deleteRowAfterMove" is false, then the moved file remains in the
     * table.  There's no reason why its proposed destination should change;
     * nothing that is used to create the proposed destination has changed.
     * But one thing that has changed is the file's current location.  We call
     * helper method updateTableItemAfterMove to update the table.
     *
     * @param item
     *   the item representing the file that we've just finished trying to move
     */
    void successfulMove(final TableItem item) {
        if (prefs.isDeleteRowAfterMove()) {
            deleteTableItem(item);
        } else {
            updateTableItemAfterMove(item);
        }
    }

    private void setupUpdateStuff(final Composite parentComposite) {
        Link updatesAvailableLink = new Link(parentComposite, SWT.VERTICAL);
        // updatesAvailableLink.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
        updatesAvailableLink.setVisible(false);
        updatesAvailableLink.setText(UPDATE_AVAILABLE);
        updatesAvailableLink.addSelectionListener(new UrlLauncher(TVRENAMER_DOWNLOAD_URL));

        // Show the label if updates are available (in a new thread)
        UpdateChecker.notifyOfUpdate(updateIsAvailable -> {
            if (updateIsAvailable) {
                display.asyncExec(() -> updatesAvailableLink.setVisible(true));
            }
        });
    }

    private void setupTopButtons() {
        final Composite topButtonsComposite = new Composite(shell, SWT.FILL);
        topButtonsComposite.setLayout(new RowLayout());

        final FileDialog fd = new FileDialog(shell, SWT.MULTI);
        final Button addFilesButton = new Button(topButtonsComposite, SWT.PUSH);
        addFilesButton.setText("Add files");
        addFilesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String pathPrefix = fd.open();
                if (pathPrefix != null) {
                    episodeMap.addFilesToQueue(pathPrefix, fd.getFileNames());
                }
            }
        });

        final DirectoryDialog dd = new DirectoryDialog(shell, SWT.SINGLE);
        final Button addFolderButton = new Button(topButtonsComposite, SWT.PUSH);
        addFolderButton.setText("Add Folder");
        addFolderButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String directory = dd.open();
                if (directory != null) {
                    // load all of the files in the dir
                    episodeMap.addFolderToQueue(directory);
                }
            }

        });

        final Button clearFilesButton = new Button(topButtonsComposite, SWT.PUSH);
        clearFilesButton.setText("Clear List");
        clearFilesButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                for (final TableItem item : swtTable.getItems()) {
                    deleteTableItem(item);
                }
            }
        });

        setupUpdateStuff(topButtonsComposite);
    }

    private void setupBottomComposite() {
        Composite bottomButtonsComposite = new Composite(shell, SWT.FILL);
        bottomButtonsComposite.setLayout(new GridLayout(3, false));

        GridData bottomButtonsCompositeGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        bottomButtonsComposite.setLayoutData(bottomButtonsCompositeGridData);

        final Button quitButton = new Button(bottomButtonsComposite, SWT.PUSH);
        GridData quitButtonGridData = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        quitButtonGridData.minimumWidth = 70;
        quitButtonGridData.widthHint = 70;
        quitButton.setLayoutData(quitButtonGridData);
        quitButton.setText(QUIT_LABEL);
        quitButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ui.quit();
            }
        });

        totalProgressBar = new ProgressBar(bottomButtonsComposite, SWT.SMOOTH);
        totalProgressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        actionButton = new Button(bottomButtonsComposite, SWT.PUSH);
        GridData actionButtonGridData = new GridData(GridData.END, GridData.CENTER, false, false);
        actionButton.setLayoutData(actionButtonGridData);
        setActionButtonText(actionButton);
        actionButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                renameFiles();
            }
        });
    }

    private void setupTableDragDrop() {
        DropTarget dt = new DropTarget(swtTable, DND.DROP_DEFAULT | DND.DROP_MOVE);
        dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
        dt.addDropListener(new DropTargetAdapter() {

            @Override
            public void drop(DropTargetEvent e) {
                FileTransfer ft = FileTransfer.getInstance();
                if (ft.isSupportedType(e.currentDataType)) {
                    String[] fileList = (String[]) e.data;
                    episodeMap.addArrayOfStringsToQueue(fileList);
                }
            }
        });
    }

    private void handleSelectionEvent() {
        final Set<TableItem> newlySelected = new HashSet<>();
        final Set<TableItem> newlyDeselected = new HashSet<>(lastKnownSelection);
        lastKnownSelection.clear();
        for (final TableItem item : swtTable.getSelection()) {
            lastKnownSelection.add(item);
            if (newlyDeselected.contains(item)) {
                newlyDeselected.remove(item);
            } else {
                newlySelected.add(item);
            }
        }
        for (TableItem item : newlyDeselected) {
            logger.info("now deselected " + itemText(item));
            final EpisodeView epview = getEpisodeView(item);
            if (epview == null) {
                logger.info("selected row with no episode view!");
            } else {
                epview.rowDeselected();
            }
        }
        for (TableItem item : newlySelected) {
            logger.info("now selected " + itemText(item));
            final EpisodeView epview = getEpisodeView(item);
            if (epview == null) {
                logger.info("selected row with no episode view!");
            } else {
                epview.rowSelected();
            }
        }
    }

    private void handleCheckEvent(final TableItem eventItem) {
        // This assumes that the current status of the TableItem
        // already reflects its toggled state, which appears to
        // be the case.
        boolean checked = eventItem.getChecked();
        boolean isSelected = false;

        for (final TableItem item : swtTable.getSelection()) {
            if (item == eventItem) {
                isSelected = true;
                break;
            }
        }
        if (!isSelected) {
            checked = false;
        }
        for (final TableItem item : swtTable.getSelection()) {
            final EpisodeView epview = getEpisodeView(item);
            if (epview == null) {
                logger.severe("deleting table item with no EpisodeView: "
                              + itemText(item));
                deleteTableItem(item);
            } else {
                epview.setChecked(checked);
            }
        }
    }

    private void setupSelectionListener() {
        swtTable.addListener(SWT.Selection, event -> {
            if (event.detail == SWT.CHECK) {
                handleCheckEvent((TableItem) event.item);
            } else {
                // else, it's a generic SELECTED event
                handleSelectionEvent();
            }
        });
    }

    private synchronized void createColumns() {
        CHECKBOX_FIELD.createColumn(this, swtTable, appData.getWidthChecked());
        CURRENT_FILE_FIELD.createColumn(this, swtTable, appData.getWidthSource());
        NEW_FILENAME_FIELD.createColumn(this, swtTable, appData.getWidthDest());
        STATUS_FIELD.createColumn(this, swtTable, appData.getWidthStatus());
    }

    private void setSortColumn() {
        TableColumn sortColumn = CURRENT_FILE_FIELD.getTableColumn();
        if (sortColumn == null) {
            logger.warning("could not find preferred sort column");
        } else {
            swtTable.setSortColumn(sortColumn);
            swtTable.setSortDirection(SWT.UP);
        }
    }

    private void setupResultsTable() {
        swtTable.setHeaderVisible(true);
        swtTable.setLinesVisible(true);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        // gridData.widthHint = 780;
        gridData.heightHint = appData.getHeightHint();
        gridData.horizontalSpan = 3;
        swtTable.setLayoutData(gridData);

        createColumns();
        setColumnDestText();
        setSortColumn();

        // Allow deleting of elements
        swtTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if ((e.keyCode == '\u0008') // backspace
                    || (e.keyCode == '\u007F')) // delete
                {
                    deleteSelectedTableItems();
                }
            }
        });

        // editable table
        final TableEditor editor = new TableEditor(swtTable);
        editor.horizontalAlignment = SWT.CENTER;
        editor.grabHorizontal = true;

        setupSelectionListener();
    }

    private void setupMainWindow() {
        setupResultsTable();
        setupTableDragDrop();
        setupBottomComposite();

        TaskBar taskBar = display.getSystemTaskBar();
        if (taskBar != null) {
            taskItem = taskBar.getItem(shell);
            if (taskItem == null) {
                taskItem = taskBar.getItem(null);
            }
        }
    }

    ResultsTable(final UIStarter ui) {
        this.ui = ui;
        shell = ui.shell;
        display = ui.display;

        setupTopButtons();
        swtTable = new Table(shell, SWT.CHECK | SWT.FULL_SELECTION | SWT.MULTI);
        setupMainWindow();
    }
}
