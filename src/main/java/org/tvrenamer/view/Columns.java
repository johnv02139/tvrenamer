package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableColumn;

import org.tvrenamer.model.AppData;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class Columns {
    private static final Logger logger = Logger.getLogger(Columns.class.getName());

    private static final AppData APP_DATA = AppData.getInstance();

    public enum AvailableColumn {
        CHECKBOX_COLUMN,
        CURRENT_FILE_COLUMN,
        NEW_FILENAME_COLUMN,
        STATUS_COLUMN
    }

    private static final Map<AvailableColumn, Column> COLUMNS = new ConcurrentHashMap<>();

    private static ResultsTable resultsTable = null;

    public static synchronized Collection<Column> getActiveColumns() {
        return COLUMNS.values();
    }

    public static synchronized Column getColumnWrapper(AvailableColumn id) {
        return COLUMNS.get(id);
    }

    private static synchronized boolean setColumn(AvailableColumn id, Column col) {
        Column oldCol = COLUMNS.get(id);
        if (oldCol == null) {
            COLUMNS.put(id, col);
            logger.fine("put column " + col + " at id " + id);
            return true;
        }
        logger.warning("cannot re-set column " + id);
        return false;
    }

    private static boolean createColumn(AvailableColumn id, String label, int width, int style) {
        final Column col = new Column(id, label, width, style, resultsTable);
        return setColumn(id, col);
    }

    public static synchronized void createColumns(ResultsTable resultsTable) {
        Columns.resultsTable = resultsTable;
        createColumn(AvailableColumn.CHECKBOX_COLUMN, CHECKBOX_HEADER,
                     APP_DATA.getWidthSelected(), SWT.LEFT);
        createColumn(AvailableColumn.CURRENT_FILE_COLUMN, SOURCE_HEADER,
                     APP_DATA.getWidthSource(), SWT.LEFT);
        // (prefs.isMoveEnabled() ? MOVE_HEADER : RENAME_HEADER),
        createColumn(AvailableColumn.NEW_FILENAME_COLUMN, MOVE_HEADER,
                     APP_DATA.getWidthDest(), SWT.LEFT);
        createColumn(AvailableColumn.STATUS_COLUMN, STATUS_HEADER,
                     APP_DATA.getWidthStatus(), SWT.LEFT);
    }

    public static TableColumn getTableColumn(AvailableColumn id) {
        Column col = COLUMNS.get(id);
        if (col == null) {
            return null;
        }
        return col.swtColumn;
    }

    public static int getColumnIndex(AvailableColumn id) {
        Column col = COLUMNS.get(id);
        if (col == null) {
            return -1;
        }
        return col.index;
    }

    public static int getColumnWidth(AvailableColumn id) {
        switch (id) {
            case CHECKBOX_COLUMN:
                return APP_DATA.getWidthSelected();
            case CURRENT_FILE_COLUMN:
                return APP_DATA.getWidthSource();
            case NEW_FILENAME_COLUMN:
                return APP_DATA.getWidthDest();
            case STATUS_COLUMN:
                return APP_DATA.getWidthStatus();
        }
        return 0;
    }

    public static String getColumnLabel(AvailableColumn id) {
        Column col = COLUMNS.get(id);
        if (col == null) {
            return EMPTY_STRING;
        }
        return col.label;
    }
}
