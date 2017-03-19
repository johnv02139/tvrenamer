package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;
import static org.tvrenamer.view.UIUtils.getDefaultSystemFont;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import org.tvrenamer.controller.UpdateChecker;

import java.io.InputStream;
import java.util.logging.Logger;

/**
 * The About Dialog box.
 */
public class AboutDialog extends Dialog {
    private static Logger logger = Logger.getLogger(AboutDialog.class.getName());

    private static Shell aboutShell;

    /**
     * AboutDialog constructor
     *
     * @param parent
     *            the parent {@link Shell}
     */
    public AboutDialog(Shell parent) {
        super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    private void createContents1() {
        GridLayout shellGridLayout = new GridLayout();
        shellGridLayout.numColumns = 2;
        shellGridLayout.marginRight = 15;
        shellGridLayout.marginBottom = 5;
        aboutShell.setLayout(shellGridLayout);

        Label iconLabel = new Label(aboutShell, SWT.NONE);
        GridData iconGridData = new GridData();
        iconGridData.verticalAlignment = GridData.FILL;
        iconGridData.horizontalAlignment = GridData.FILL;
        // Force the icon to take up the whole of the right column
        iconGridData.verticalSpan = 10;
        iconGridData.grabExcessVerticalSpace = false;
        iconGridData.grabExcessHorizontalSpace = false;
        iconLabel.setLayoutData(iconGridData);

        InputStream icon = getClass().getResourceAsStream(TVRENAMER_ICON_PATH);
        if (icon != null) {
            iconLabel.setImage(new Image(Display.getCurrent(), icon));
        } else {
            iconLabel.setImage(new Image(Display.getCurrent(),
                                         ICON_PARENT_DIRECTORY + TVRENAMER_ICON_PATH));
        }

        Label applicationLabel = new Label(aboutShell, SWT.NONE);
        applicationLabel.setFont(new Font(aboutShell.getDisplay(), getDefaultSystemFont().getName(),
            getDefaultSystemFont().getHeight() + 4, SWT.BOLD));
        applicationLabel.setText(APPLICATION_NAME);
        applicationLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        Label versionLabel = new Label(aboutShell, SWT.NONE);
        versionLabel.setFont(new Font(aboutShell.getDisplay(), getDefaultSystemFont().getName(), getDefaultSystemFont()
            .getHeight() + 2, SWT.BOLD));
        versionLabel.setText(VERSION_LABEL);
        versionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        Label descriptionLabel = new Label(aboutShell, SWT.NONE);
        descriptionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));
        descriptionLabel.setText(TVRENAMER_DESCRIPTION);
    }

    private void createContents2() {
        final Link licenseLink = new Link(aboutShell, SWT.NONE);
        licenseLink.setText(LICENSE_TEXT);
        licenseLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        licenseLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Program.launch(TVRENAMER_LICENSE_URL);
            }
        });

        final Link projectPageLink = new Link(aboutShell, SWT.NONE);
        projectPageLink.setText(PROJECT_TEXT);
        projectPageLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        projectPageLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Program.launch(TVRENAMER_PROJECT_URL);
            }
        });

        final Link issuesLink = new Link(aboutShell, SWT.NONE);
        issuesLink.setText(ISSUE_TRACKER);
        issuesLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        issuesLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Program.launch(TVRENAMER_ISSUES_URL);
            }
        });

        final Link supportEmailLink = new Link(aboutShell, SWT.NONE);
        supportEmailLink.setText(EMAIL_TEXT);
        supportEmailLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));
        supportEmailLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    Program.launch(EMAIL_LINK);
                }
            });
    }

    private void createContents3() {
        final Link sourceCodeLink = new Link(aboutShell, SWT.NONE);
        sourceCodeLink.setText(REPOSITORY_TEXT);
        sourceCodeLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        sourceCodeLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Program.launch(TVRENAMER_REPOSITORY_URL);
            }
        });

        Button updateCheckButton = new Button(aboutShell, SWT.PUSH);
        updateCheckButton.setText(UPDATE_TEXT);
        GridData gridDataUpdateCheck = new GridData();
        gridDataUpdateCheck.widthHint = 160;
        gridDataUpdateCheck.horizontalAlignment = GridData.END;
        updateCheckButton.setLayoutData(gridDataUpdateCheck);

        updateCheckButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                Boolean updateAvailable = UpdateChecker.isUpdateAvailable();

                if (updateAvailable == null) {
                    // Don't need to do anything here as the error message has been displayed already
                } else if (updateAvailable) {
                    logger.fine(NEW_VERSION_AVAILABLE);
                    UIUtils.showOkMessageBox(NEW_VERSION_TITLE, NEW_VERSION_AVAILABLE);
                } else {
                    UIUtils.showWarningMessageBox(NO_NEW_VERSION_TITLE, NO_NEW_VERSION_AVAILABLE);
                }
            }
        });
    }

    private void createContents4() {
        Button okButton = new Button(aboutShell, SWT.PUSH);
        okButton.setText(OK_LABEL);
        GridData gridDataOK = new GridData();
        gridDataOK.widthHint = 160;
        gridDataOK.horizontalAlignment = GridData.END;
        okButton.setLayoutData(gridDataOK);
        okButton.setFocus();

        okButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                aboutShell.close();
            }
        });

        // Set the OK button as the default, so
        // user can press Enter to dismiss
        aboutShell.setDefaultButton(okButton);
    }

    /**
     * Creates the dialog's contents.
     *
     */
    private void createContents() {
        createContents1();
        createContents2();
        createContents3();
        createContents4();
    }

    public void open() {
        // Create the dialog window
        aboutShell = new Shell(getParent(), getStyle());
        aboutShell.setText(ABOUT_TEXT);

        // Add the contents of the dialog window
        createContents();

        aboutShell.pack();
        aboutShell.open();
        Display display = getParent().getDisplay();
        while (!aboutShell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }
}
