package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import static org.tvrenamer.view.UIStarter.showMessageBox;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import org.tvrenamer.controller.EpisodeInformationListener;
import org.tvrenamer.model.EpisodeDb;
import org.tvrenamer.model.EpisodeInfo;
import org.tvrenamer.model.FailedShow;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.NotFoundException;
import org.tvrenamer.model.SWTMessageBoxType;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowStore;
import org.tvrenamer.model.UserPreference;
import org.tvrenamer.model.UserPreferences;
import org.tvrenamer.model.util.Environment;

import java.io.File;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

public class ResultsTable implements Observer, EpisodeInformationListener, Listener {
    private static Logger logger = Logger.getLogger(ResultsTable.class.getName());
    private final UserPreferences prefs;

    private static final int SELECTED_COLUMN = 0;
    private static final int FILENAME_COLUMN = 1;
    private static final int FOUND_SHOW_COLUMN = 2;
    private static final int SHOW_OPTIONS_COLUMN = 3;
    private static final int SHOW_ID_COLUMN = 4;

    private static final int ITEM_NOT_IN_TABLE = -1;

    private boolean isUIRunning = false;

    private Shell shell;
    private Display display;
    private Table resultsTable;
    private final EpisodeDb episodeMap;
    private ProgressBar totalProgressBar;

    private Button addFilesButton;
    private Button addFolderButton;
    private Button clearFilesButton;
    private Button selectAllButton;
    private Button deselectAllButton;
    private TableColumn destinationColumn;
    private Font italicFont;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final File preloadFolder;

    private enum FileMoveIcon {
        ADDED("/icons/SweetieLegacy/16-circle-blue.png"),
        DOWNLOADING("/icons/SweetieLegacy/16-clock.png"),
        SUCCESS("/icons/SweetieLegacy/16-em-check.png"),
        FAIL("/icons/SweetieLegacy/16-em-cross.png"),
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

    private TableItem[] getTableItems() {
        return resultsTable.getItems();
    }

    private void setShowOptionsColumn(final TableItem item, final FileEpisode ep) {
        Deque<Show> options = ep.getShows();
        if (options == null) {
            options = new ArrayDeque<Show>();
            options.push(new FailedShow("no options yet"));
            setShowOptionsColumn(item, "no options yet");
            return;
        }
        int count = options.size();
        if (count == 0) {
            setShowOptionsColumn(item, "no options");
            return;
        }

        if (count == 1) {
            Show only = options.getFirst();
            String id = only.getId();
            if ((id != null) && (id.length() > 1)) {
                setShowOptionsColumn(item, only.getName());
                item.setText(SHOW_ID_COLUMN, id);
                return;
            }
        }

        final TableEditor editor = new TableEditor(resultsTable);
        final CCombo combo = new CCombo(resultsTable, SWT.BORDER);
        if (count > 1) {
            combo.setBackground(display.getSystemColor(SWT.COLOR_YELLOW));
        }
        combo.setText(options.getFirst().getName());
        item.setText(SHOW_ID_COLUMN, options.getFirst().getId());
        for (Show s : options) {
            combo.add(s.getName());
        }
        editor.grabHorizontal = true;
        editor.setEditor(combo, item, SHOW_OPTIONS_COLUMN);

        combo.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent e) {
                    logger.info("widget selected: " + e.item);
                    String newId = ep.getShowId(combo.getText());
                    logger.info("got new id " + newId);
                    item.setText(SHOW_ID_COLUMN, newId);
                }

                public void widgetDefaultSelected(SelectionEvent e) {
                    logger.info("widget default selected: " + e.item);
                    ep.setFilenameShow(combo.getText());
                    Control oldEditor = editor.getEditor();
                    if (oldEditor != null) oldEditor.dispose();
                    ep.lookupShow();
                }
            });

        combo.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    logger.info("text modified: " + combo.getText());
                }
            });
    }

    private void setShowOptionsColumn(TableItem item, String s) {
        item.setText(SHOW_OPTIONS_COLUMN, s);
    }

    private TableItem failToFindOptions(TableItem item, FileEpisode episode, String fileName) {
        logger.severe("Couldn't parse file: " + fileName);
        item.setGrayed(true); // makes checkbox use a dot; very weird
        item.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
        item.setFont(italicFont);
        setShowOptionsColumn(item, "filename not parsed");
        return item;
    }

    private void refreshTable() {
        logger.finer("Refreshing table");
        for (TableItem item : getTableItems()) {
            String fileName = item.getText(FILENAME_COLUMN);
            FileEpisode episode = episodeMap.remove(fileName);
            if (episode == null) {
                failToFindOptions(item, episode, fileName);
            } else {
                String newFileName = episode.getFile().getAbsolutePath();
                episodeMap.put(newFileName, episode);
                item.setText(FILENAME_COLUMN, newFileName);
                setShowOptionsColumn(item, episode);
            }
        }
    }

    private static String destColumnText(boolean isMoveEnabled) {
        if (isMoveEnabled) {
            return "Proposed File Path";
        } else {
            return "Proposed File Name";
        }
    }

    private void updateUserPreferences(UserPreferences observed, UserPreference upref) {
        logger.info("Preference change event: " + upref);

        if (upref == UserPreference.MOVE_ENABLED) {
            boolean isMoveEnabled = observed.isMoveEnabled();
            destinationColumn.setText(destColumnText(isMoveEnabled));
            shell.layout(false, true);
        }
        if ((upref == UserPreference.REPLACEMENT_MASK)
            || (upref == UserPreference.MOVE_ENABLED)
            || (upref == UserPreference.DEST_DIR)
            || (upref == UserPreference.SEASON_PREFIX)
            || (upref == UserPreference.LEADING_ZERO))
        {
            refreshTable();
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

    private TableItem createTableItem(FileEpisode episode) {
        TableItem item = new TableItem(resultsTable, SWT.NONE);
        item.setData(episode);
        episode.setTableItem(item);

        File actualFile = episode.getFile();
        String fileName = actualFile.getName();
        item.setText(FILENAME_COLUMN, fileName);

        item.setText(FOUND_SHOW_COLUMN, episode.getFilenameShow());

        if (episode.wasNotParsed()) {
            return failToFindOptions(item, episode, fileName);
        }
        try {
            setShowOptionsColumn(item, episode);
        } catch (NotFoundException e) {
            item.setChecked(false);
            setShowOptionsColumn(item, e.getMessage());
            item.setForeground(display.getSystemColor(SWT.COLOR_RED));
        }
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

    private TableItem getTableItem(int index) {
        return resultsTable.getItem(index);
    }

    private String getShowOptionsColumn(TableItem item) {
        return item.getText(SHOW_OPTIONS_COLUMN);
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
        item.setText(FILENAME_COLUMN, oldItem.getText(FILENAME_COLUMN));
        setShowOptionsColumn(item, getShowOptionsColumn(oldItem));
        item.setChecked(wasChecked);
        item.setData(oldEpisode);

        oldEpisode.setTableItem(item);

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
                        // the snippet replaces the items with the new items,
                        // we do the same
                        items = getTableItems();
                        break;
                    }
                } else if (collator.compare(value1, value2) > 0) {
                    setSortedItem(i, j);
                    // the snippet replaces the items with the new items,
                    // we do the same
                    items = getTableItems();
                    break;
                }
            }
        }
        resultsTable.setSortColumn(col);
        resultsTable.setSortDirection(newSortDirection);
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

    private void lookupTableItem(final TableItem item) {
        getTableItemEpisode(item).lookupShow();
    }

    private void addListOfFiles(final List<String> fileNames) {
        for (final String fileName : fileNames) {
            final FileEpisode episode = episodeMap.add(fileName);
            episode.listen(this);
            // We add the file to the table even if we couldn't parse the filename
            final TableItem item = createTableItem(episode);
        }
        for (TableItem item : getTableItems()) {
            lookupTableItem(item);
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

    private void deleteSelectedTableItems() {
        int index = ITEM_NOT_IN_TABLE;
        for (final TableItem item : resultsTable.getSelection()) {
            index = getTableItemIndex(item);
            if (ITEM_NOT_IN_TABLE == index) {
                logger.info("error: somehow selected item not found in table");
                continue;
            }

            FileEpisode ep = getTableItemEpisode(item);
            ep.setTableItem(null);
            item.setData(null);

            String filename = item.getText(FILENAME_COLUMN);
            episodeMap.remove(filename);

            resultsTable.remove(index);
            item.dispose();
        }
        resultsTable.deselectAll();
    }

    private void setupSelectionListener() {
        resultsTable.addListener(SWT.Selection, this);
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

        setupTableColumn(SELECTED_COLUMN, 60, "Selected");
        setupTableColumn(FILENAME_COLUMN, 350, "Filename");
        setupTableColumn(FOUND_SHOW_COLUMN, 200, "Search String");
        destinationColumn = setupTableColumn(SHOW_OPTIONS_COLUMN, 500, "Best Option");
        setupTableColumn(SHOW_ID_COLUMN, 150, "Show ID");

        // Allow deleting of elements
        resultsTable.addKeyListener(new KeyAdapter() {
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

        setupSelectionListener();
    }

    private void setupTableDragDrop() {
        DropTarget dt = new DropTarget(resultsTable, DND.DROP_DEFAULT | DND.DROP_MOVE);
        dt.setTransfer(new Transfer[] {FileTransfer.getInstance()});
        dt.addDropListener(new DropTargetAdapter() {
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

    private void doCleanup() {
        executor.shutdownNow();
        ShowStore.cleanUp();
        shell.dispose();
        display.dispose();
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

        quitButton.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        doCleanup();
                    }
                });

        final Button showShowStoreStoreButton = new Button(bottomButtonsComposite, SWT.PUSH);
        GridData showShowStoreStoreButtonGridData =
            new GridData(GridData.END, GridData.CENTER, false, false);
        showShowStoreStoreButton.setLayoutData(showShowStoreStoreButtonGridData);
        showShowStoreStoreButton.setText("Show Store");
        showShowStoreStoreButton.setToolTipText("TODO...");
        showShowStoreStoreButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ShowStore.showStore();
                }
            });
    }

    private Menu setupHelpMenuBar(Menu menuBar) {
        MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        helpMenuHeader.setText("Help");

        Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
        helpMenuHeader.setMenu(helpMenu);

        MenuItem helpVisitWebpageItem = new MenuItem(helpMenu, SWT.PUSH);
        helpVisitWebpageItem.setText("Visit Webpage");
        helpVisitWebpageItem.addSelectionListener(
                new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Program.launch(SHOWFINDER_PROJECT_URL);
                    }
                });

        return helpMenu;
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

    public void handleCheckboxEvent(Event event) {
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

    public void handleEvent(Event event) {
        if (event.detail == SWT.CHECK) {
            handleCheckboxEvent(event);
        }
        // else, it's a SELECTED event, which we just don't care about
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

    private void setupIcons() {
        try {
            InputStream icon = getClass().getResourceAsStream("/icons/tvrenamer.png");
            if (icon != null) {
                shell.setImage(new Image(display, icon));
            } else {
                shell.setImage(new Image(display, "src/main/resources/icons/tvrenamer.png"));
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
        new UIStarter(shell);

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
            showMessageBox(SWTMessageBoxType.ERROR, "Error", UNKNOWN_EXCEPTION, exception);
            logger.log(Level.SEVERE, UNKNOWN_EXCEPTION, exception);
            System.exit(2);
        }
    }

    public void run() {
        init();
        launch();
    }

    private static FileMoveIcon getFileMoveIcon(final EpisodeInfo status) {
        switch (status) {
        case ADDED:
            return FileMoveIcon.ADDED;
        case NOPARSE:
            return FileMoveIcon.NOPARSE;
        case MOVING:
            return FileMoveIcon.RENAMING;
        case DOWNLOADED:
        case RENAMED:
            return FileMoveIcon.SUCCESS;
        case FAIL_TO_MOVE:
        case BROKEN:
        default:
            return FileMoveIcon.FAIL;
        }
    }

    public void onEpisodeUpdate(final FileEpisode ep) {
        if (!isUIRunning) {
            logger.severe("got update, but UI not running: " + ep);
            return;
        }
        if (display.isDisposed()) {
            logger.severe("got update, but display is disposed: " + ep);
            return;
        }
        if (ep == null) {
            logger.severe("got update, but episode is null.");
            return;
        }
        final TableItem item = ep.getTableItem();
        if (item == null) {
            logger.severe("episode not associated with table item: " + ep);
            return;
        }
        final EpisodeInfo status = ep.getStatus();
        final FileMoveIcon fmi = getFileMoveIcon(status);
        display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (item.isDisposed()) {
                        return;
                    }
                    logger.info("got some options for " + ep.getFilenameShow());
                    setShowOptionsColumn(item, ep);
                    item.setChecked(status == EpisodeInfo.DOWNLOADED);
                }
            });
    }

    public ResultsTable(File preloadFolder) {
        prefs = UserPreferences.getInstance();
        this.preloadFolder = preloadFolder;
        episodeMap = new EpisodeDb();
    }
}
