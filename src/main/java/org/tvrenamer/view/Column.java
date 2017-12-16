package org.tvrenamer.view;

import static org.eclipse.swt.SWT.*;
import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import java.util.logging.Logger;

public final class Column {
    private static final Logger logger = Logger.getLogger(Column.class.getName());

    private static final int ITEM_NOT_IN_TABLE = -1;

    final Columns.AvailableColumn id;
    final String label;
    final int width;
    final int style;
    final ResultsTable resultsTable;
    final Table swtTable;
    final TableColumn swtColumn;

    int index;
    int sortDirection = UP;

    static TableColumn makeTableColumn(Table table, String label, int width, int style) {
        TableColumn col = new TableColumn(table, style);
        col.setText(label);
        col.setWidth(width);

        return col;
    }

    // Not safe to call arbitrarily!
    int updateSortDirection() {
        TableColumn previousSort = swtTable.getSortColumn();
        if (swtColumn.equals(previousSort)) {
            sortDirection = (sortDirection == DOWN) ? UP : DOWN;
        }
        return sortDirection;
    }

    Column(Columns.AvailableColumn id, String label, int width, int style, ResultsTable resultsTable) {
        this.id = id;
        this.label = label;
        this.width = width;
        this.style = style;
        this.resultsTable = resultsTable;
        this.swtTable = resultsTable.getTable();

        swtColumn = makeTableColumn(swtTable, label, width, style);
        index = swtTable.indexOf(swtColumn);
        if (ITEM_NOT_IN_TABLE == index) {
            logger.severe("unable to locate column in table: " + swtColumn);
            throw new IllegalStateException("could not locate column after adding it");
        }
        logger.fine("setting column " + id + " at index " + index);
        swtColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                logger.fine("selecting based on column " + index);
                resultsTable.sortTable(Column.this);
            }
        });
    }
}
