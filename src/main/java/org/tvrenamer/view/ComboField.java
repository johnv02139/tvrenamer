package org.tvrenamer.view;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.TableItem;

public class ComboField extends TextField {

    @SuppressWarnings("SameParameterValue")
    ComboField(final String name, final String label) {
        super(Field.Type.COMBO, name, label);
    }

    String comboDisplayedText(final Combo combo, final String text) {
        if (combo == null) {
            return text;
        }
        final int selected = combo.getSelectionIndex();
        final String[] options = combo.getItems();
        return options[selected];
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private String itemDestDisplayedText(final TableItem item) {
        synchronized (item) {
            final Object data = item.getData();
            final Combo combo = (data instanceof Combo)
                ? (Combo) data : null;
            return comboDisplayedText(combo, getCellText(item));
        }
    }

    @Override
    public String getItemTextValue(final TableItem item) {
        return itemDestDisplayedText(item);
    }
}
