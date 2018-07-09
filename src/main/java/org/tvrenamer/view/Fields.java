package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.model.UserPreferences;

class Fields {
    private static final UserPreferences prefs = UserPreferences.getInstance();

    public static final CheckboxField CHECKBOX_FIELD
        = new CheckboxField("CHECKBOX_FIELD", CHECKBOX_HEADER) {
                public String getItemTextValue(final EpisodeView epview) {
                    return getCheckTextValue(epview.isChecked());
                }
            };

    public static final TextField CURRENT_FILE_FIELD
        = new TextField("CURRENT_FILE_FIELD", SOURCE_HEADER) {
                public String getItemTextValue(final EpisodeView epview) {
                    return epview.getCurrentFile();
                }
            };

    public static final ComboField NEW_FILENAME_FIELD
        = new ComboField("NEW_FILENAME_FIELD",
                         (prefs.isMoveEnabled() ? MOVE_HEADER : RENAME_HEADER)) {
                public String getItemTextValue(final EpisodeView epview) {
                    return comboDisplayedText(epview.getCombo(), epview.getProposedDest());
                }
            };

    public static final ImageField STATUS_FIELD
        = new ImageField("STATUS_FIELD", STATUS_HEADER) {
                public String getItemTextValue(final EpisodeView epview) {
                    return getStatusString(epview.getStatus());
                }
            };
}
