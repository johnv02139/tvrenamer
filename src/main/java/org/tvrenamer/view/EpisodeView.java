package org.tvrenamer.view;

import static org.tvrenamer.view.Fields.*;
import static org.tvrenamer.view.ItemState.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;

import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.ShowListingsListener;
import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.FailedShow;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.ProgressObserver;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowStore;
import org.tvrenamer.model.util.Constants;

import java.io.Serializable;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EpisodeView implements ShowListingsListener, ShowInformationListener, ProgressObserver {
    private static final Logger logger = Logger.getLogger(EpisodeView.class.getName());

    private final FileEpisode episode;
    private final ResultsTable rtable;
    private final Display display;

    private TableItem item = null;
    private Combo comboBox = null;
    private Label label = null;
    private NumberFormat format = null;
    private long maximum;
    private int loopCount = 0;

    // Initially we add items to the table unchecked.  When we successfully obtain enough
    // information about the episode to determine how to rename it, the check box will
    // automatically be activated.
    private boolean isSelected = false;
    private String currentFile = null;
    private String proposedDest = Constants.ADDED_PLACEHOLDER_FILENAME;
    private List<String> options = null;
    private int chosenEpisode = 0;
    private ItemState status = DOWNLOADING;

    /**
     * The purpose of this method is simply to get the TableItem associated with this
     * EpisodeView, which is very simple in that the item is stored as an instance
     * variable.  But while we're here, we do a bunch of validation.  There are a number
     * of inconsistent situations that could occur, and we're going to make sure none
     * of them are happening.
     *
     * @return the TableItem currently associated with this EpisodeView
     */
    public TableItem getItem() {
        if (item == null) {
            logger.severe("null table item in " + this);
            return null;
        }
        display.syncExec(() -> {
            final Object itemData = item.getData();
            if (itemData == null) {
                logger.severe("table item had no episode view; setting to " + this);
                item.setData(this);
            } else if (itemData instanceof EpisodeView) {
                if (itemData != this) {
                    logger.severe("inconsistent table item state: " + this);
                    logger.log(Level.SEVERE, "data is: " + itemData,
                               new IllegalStateException(this.toString()));
                    item.setData(this);
                }
            } else {
                logger.severe("serious internal error: table item data is of wrong type; "
                              + "setting to " + this);
                item.setData(this);
            }
        });
        return item;
    }

    private void deleteComboBox() {
        if (comboBox != null) {
            if (!comboBox.isDisposed()) {
                comboBox.dispose();
            }
            comboBox = null;
        }
    }

    public void delete() {
        deleteComboBox();
        item = null;
        isSelected = false;
    }

    private void setCheckbox() {
        display.syncExec(() -> CHECKBOX_FIELD.setCellChecked(item, isSelected));
    }

    private void setCurrentFile() {
        display.syncExec(() -> CURRENT_FILE_FIELD.setCellText(item, currentFile));
    }

    private void setStatus() {
        Image img = status.getIcon();
        if (img == null) {
            logger.warning("could not get image for " + status);
        } else {
            display.syncExec(() -> STATUS_FIELD.setCellImage(item, img));
        }
    }

    private void setProposedDestination() {
        display.syncExec(() -> NEW_FILENAME_FIELD.setCellText(item, proposedDest));
        int nOptions = episode.optionCount();
        if (nOptions > 1) {
            display.syncExec(() -> {
                final TableEditor editor = new TableEditor(item.getParent());
                editor.grabHorizontal = true;
                NEW_FILENAME_FIELD.setEditor(item, editor, comboBox);
            });
        }
    }

    /**
     * Fill in the value for the "Proposed File" column of the given row, with the text
     * we get from the given episode.  This is the only method that should ever set
     * this text, to ensure that the text of each row is ALWAYS the value returned by
     * getReplacementText() on the associated episode.
     *
     */
    private void updateProposedDestination() {
        int nOptions = episode.optionCount();
        if (nOptions <= 1) {
            deleteComboBox();
            String newText = episode.getReplacementText();
            if (newText == null) {
                logger.severe("ignoring null replacement text for " + this);
            } else if (!newText.equals(proposedDest)) {
                proposedDest = newText;
                setProposedDestination();
            }
        } else {
            List<String> newOptions = episode.getReplacementOptions();
            if (newOptions != null) {
                int nowChosen = episode.getChosenEpisode();

                if ((nowChosen != chosenEpisode) || !newOptions.equals(options)) {
                    chosenEpisode = nowChosen;
                    options = newOptions;
                    proposedDest = options.get(chosenEpisode);

                    display.syncExec(() -> {
                        deleteComboBox();
                        comboBox = rtable.newComboBox();
                        if (comboBox == null) {
                            // Only way this can happen is if the UI is shutting down.
                            return;
                        }
                        options.forEach(comboBox::add);
                        comboBox.setText(proposedDest);
                        comboBox.addModifyListener(e ->
                                                   episode.setChosenEpisode(comboBox.getSelectionIndex()));
                    });
                    setProposedDestination();
                }
            }
        }
    }

    public void onFileMoved(final String newFileLocation) {
        if (!newFileLocation.equals(currentFile)) {
            currentFile = newFileLocation;
            setCurrentFile();
        }
    }

    private void updateStatus(final ItemState newStatus) {
        if (status != newStatus) {
            status = newStatus;
            setStatus();
        }
    }

    public void setChecked(final boolean newStatus) {
        if (isSelected != newStatus) {
            isSelected = newStatus;
            setCheckbox();
        }
    }

    public void setFail() {
        setChecked(false);
        updateStatus(FAIL);
        updateProposedDestination();
    }

    private void updateOptions(final int epsFound) {
        if (epsFound > 1) {
            updateStatus(OPTIONS);
        } else if (epsFound == 1) {
            updateStatus(SUCCESS);
        } else {
            updateStatus(FAIL);
        }
        updateProposedDestination();
    }

    public void refreshProposedDest() {
        episode.refreshReplacement();
        updateOptions(episode.optionCount());
    }

    @Override
    public void listingsDownloadComplete() {
        int epsFound = episode.listingsComplete();
        setChecked(true);
        updateOptions(epsFound);
    }

    @Override
    public void listingsDownloadFailed(Exception err) {
        episode.listingsFailed(err);
        setFail();
    }

    private void setEpisodeShow(final Show show) {
        episode.setEpisodeShow(show);
        if (show == null) {
            setFail();
        } else {
            updateStatus(ADDED);
            updateProposedDestination();
            if (show.isValidSeries()) {
                show.asSeries().addListingsListener(this);
            }
        }
    }

    public FileEpisode getEpisode() {
        return episode;
    }

    public boolean isChecked() {
        return isSelected;
    }

    public String getCurrentFile() {
        return currentFile;
    }

    public String getProposedDest() {
        return proposedDest;
    }

    public Combo getCombo() {
        return comboBox;
    }

    public ItemState getStatus() {
        return status;
    }

    public static class Comparator implements java.util.Comparator<EpisodeView>, Serializable {
        @SuppressWarnings("unused")
        private static final long serialVersionUID = 1032L;
        private static final Collator COLLATOR = Collator.getInstance(Locale.getDefault());

        private final Field field;
        private final int sortDirection;

        public Comparator(final Field field, final int sortDirection) {
            this.field = field;
            this.sortDirection = sortDirection;
        }

        public int compare(final EpisodeView epview1, final EpisodeView epview2) {
            if (sortDirection == SWT.UP) {
                return COLLATOR.compare(field.getItemTextValue(epview1),
                                        field.getItemTextValue(epview2));
            } else {
                return COLLATOR.compare(field.getItemTextValue(epview2),
                                        field.getItemTextValue(epview1));
            }
        }
    }


    @Override
    public void downloadSucceeded(Show show) {
        setEpisodeShow(show);
    }

    @Override
    public void downloadFailed(FailedShow failedShow) {
        episode.setFailedShow(failedShow);
    }

    @Override
    public void apiHasBeenDeprecated() {
        rtable.noteApiFailure();
        setFail();
    }

    public void lookupShow() {
        final String showName = episode.getFilenameShow();
        if (StringUtils.isBlank(showName)) {
            logger.fine("no show name found for " + this);
        } else {
            new Thread(() -> {
                ShowStore.mapStringToShow(showName, EpisodeView.this);
            }).start();
        }
    }

    public void refreshTableItem() {
        setProposedDestination();
        setStatus();
        setCheckbox();
        setCurrentFile();
    }

    public void replaceTableItem(final TableItem newItem) {
        if (this.item == newItem) {
            return;
        }

        display.syncExec(() -> {
            this.item = newItem;
            newItem.setData(this);
        });

        refreshTableItem();
    }

    /**
     * Dispose of the label.  We need to do this whether the label was used or not.
     */
    @Override
    public void finishProgress(final boolean succeeded) {
        if (display.isDisposed()) {
            logger.info("display disposed while move of " + currentFile
                        + " was in progress...");
        } else {
            display.asyncExec(() -> {
                if ((label != null) && (!label.isDisposed())) {
                    label.dispose();
                }
                if (succeeded) {
                    rtable.successfulMove(item);
                } else {
                    // Should we do anything else, visible to the user?
                    // Uncheck the row?  We don't really have a good
                    // option, right now.  TODO.
                    logger.info("failed to move item: " + item);
                }
            });
        }
    }

    /**
     * Update the progress value.
     *
     * @param value the new value
     */
    @Override
    public void setProgressValue(final long value) {
        if (loopCount++ % 500 == 0) {
            display.asyncExec(() -> {
                if (label.isDisposed()) {
                    return;
                }
                label.setText(format.format((double) value / maximum));
            });
        }
    }

    /**
     * Update the status label.
     *
     * @param status the new status label
     */
    @Override
    public void setProgressStatus(final String status) {
        display.asyncExec(() -> {
            if (label.isDisposed()) {
                return;
            }
            label.setToolTipText(status);
        });
    }

    /**
     * Set the maximum value.
     *
     * @param max the new maximum value
     */
    @Override
    public void initializeProgress(final long max) {
        format = NumberFormat.getPercentInstance();
        format.setMaximumFractionDigits(1);
        loopCount = 0;
        display.syncExec(() -> label = rtable.getProgressLabel(item));
        maximum = max;
        setProgressValue(0);
    }

    /**
     * Creates an EpisodeView
     *
     * @param table
     *   the ResultsTable this is displayed in
     * @param episode
     *   the FileEpisode this refers to
     */
    public EpisodeView(final ResultsTable table, final FileEpisode episode) {
        if (table == null) {
            throw new IllegalStateException("EpisodeView cannot have a null table");
        }
        this.rtable = table;
        display = rtable.getDisplay();
        if (episode == null) {
            throw new IllegalStateException("EpisodeView cannot have a null episode");
        }
        this.episode = episode;
        currentFile = episode.getFilepath();
        item = rtable.newTableItem();
        item.setData(this);
        if (!episode.wasParsed()) {
            setFail();
        }
    }

    /**
     *
     */
    @Override
    public String toString() {
        return "EpisodeView {" + currentFile + "}";
    }
}
