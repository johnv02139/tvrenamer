package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import static org.tvrenamer.view.UIStarter.getDefaultSystemFont;

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

import java.io.InputStream;
import java.util.logging.Logger;

/**
 * The About Dialog box.
 */
public class AboutDialog extends Dialog {
    private static Logger logger = Logger.getLogger(AboutDialog.class.getName());

    private static final String SHOWFINDER_LICENSE_URL = "http://www.gnu.org/licenses/gpl-2.0.html";
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

    public void open() {
        // Create the dialog window
        aboutShell = new Shell(getParent(), getStyle());
        aboutShell.setText("About " + APPLICATION_NAME);

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

    /**
     * Creates the dialog's contents.
     *
     */
    private void createContents() {
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

        InputStream icon = getClass().getResourceAsStream("/icons/tvrenamer.png");
        if (icon != null) {
            iconLabel.setImage(new Image(Display.getCurrent(), icon));
        } else {
            iconLabel.setImage(new Image(Display.getCurrent(), "src/main/resources/icons/tvrenamer.png"));
        }

        Label applicationLabel = new Label(aboutShell, SWT.NONE);
        applicationLabel.setFont(
                new Font(
                        aboutShell.getDisplay(),
                        getDefaultSystemFont().getName(),
                        getDefaultSystemFont().getHeight() + 4,
                        SWT.BOLD));
        applicationLabel.setText(APPLICATION_NAME);
        applicationLabel.setLayoutData(
                new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        Label versionLabel = new Label(aboutShell, SWT.NONE);
        versionLabel.setFont(
                new Font(
                        aboutShell.getDisplay(),
                        getDefaultSystemFont().getName(),
                        getDefaultSystemFont().getHeight() + 2,
                        SWT.BOLD));
        versionLabel.setText("Version: " + VERSION_NUMBER);
        versionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        Label descriptionLabel = new Label(aboutShell, SWT.NONE);
        descriptionLabel.setLayoutData(
                new GridData(GridData.BEGINNING, GridData.CENTER, true, true));
        descriptionLabel.setText(
                "TV Show Finder is a Java GUI utility to identify TV series from TV listings");

        final Link licenseLink = new Link(aboutShell, SWT.NONE);
        licenseLink.setText(
                "Licensed under the <a href=\""
                        + SHOWFINDER_LICENSE_URL
                        + "\">GNU General Public License v2</a>");
        licenseLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        licenseLink.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent arg0) {
                        Program.launch(SHOWFINDER_LICENSE_URL);
                    }
                });

        final Link projectPageLink = new Link(aboutShell, SWT.NONE);
        projectPageLink.setText("<a href=\"" + SHOWFINDER_PROJECT_URL + "\">Project Page</a>");
        projectPageLink.setLayoutData(
                new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        projectPageLink.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent arg0) {
                        Program.launch(SHOWFINDER_PROJECT_URL);
                    }
                });

        final Link sourceCodeLink = new Link(aboutShell, SWT.NONE);
        sourceCodeLink.setText("<a href=\"" + SHOWFINDER_PROJECT_URL + "\">Source Code</a>");
        sourceCodeLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        sourceCodeLink.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent arg0) {
                        Program.launch(SHOWFINDER_PROJECT_URL);
                    }
                });


        Button okButton = new Button(aboutShell, SWT.PUSH);
        okButton.setText("OK");
        GridData gridDataOK = new GridData();
        gridDataOK.widthHint = 160;
        gridDataOK.horizontalAlignment = GridData.END;
        okButton.setLayoutData(gridDataOK);
        okButton.setFocus();

        okButton.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        aboutShell.close();
                    }
                });

        // Set the OK button as the default, so
        // user can press Enter to dismiss
        aboutShell.setDefaultButton(okButton);
    }
}
