package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import java.awt.HeadlessException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

public class UIUtils {

    private static Logger logger = Logger.getLogger(UIUtils.class.getName());
    private static Shell shell = null;
    private static boolean disableMessageBoxes = true;

    /**
     * Setter for shell
     *
     * @param shell
     *            the shell to use.
     */
    public static void setShell(Shell shell) {
        UIUtils.shell = shell;
        disableMessageBoxes = false;
    }

    /**
     * Setter for disableMessageBoxes
     *
     * @param disableMessageBoxes
     *            whether or not to disable dialog boxes
     */
    public static void setDisableMessageBoxes(boolean disableMessageBoxes) {
        UIUtils.disableMessageBoxes = disableMessageBoxes;
    }

    /**
     * Show a message box of the given type with the given message content and window title.
     *
     * @param type the type of {@link SWT} icon to display in this dialog box
     * @param title the window title
     * @param message the message content
     * @param exception the exception to display
     */
    private static void showMessageBox(final int type, final String title,
                                       final String message, final Exception exception)
    {
        if (disableMessageBoxes) {
            logger.warning("not displaying message boxes");
            logger.warning("*** " + title);
            if (exception == null) {
                logger.warning(message);
            } else {
                logger.log(Level.WARNING, message, exception);
            }
        } else if (shell == null) {
            // Shell not established yet, try using JOPtionPane instead
            try {
                JOptionPane.showMessageDialog(null, message);
                return;
            } catch (HeadlessException he) {
                logger.warning("Could not show message graphically: " + message);
                return;
            }
        } else {
            Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        MessageBox msgBox = new MessageBox(shell, type);
                        msgBox.setText(title);

                        if (exception == null) {
                            msgBox.setMessage(message);
                        } else {
                            msgBox.setMessage(message + "/n" + exception.getLocalizedMessage());
                        }

                        msgBox.open();
                    }
                });
        }
    }

    /**
     * Show an "error" message box displaying an exception along with the given
     *  message content and window title.
     *
     * @param title the window title
     * @param message the message content
     * @param exception the exception to display
     */
    public static void showErrorMessageBox(final String title, final String message,
                                           final Exception exception)
    {
        showMessageBox(SWT.ICON_ERROR, title, message, exception);
    }

    /**
     * Show a "warning" message box with the given message content and window title.
     *
     * @param title the window title
     * @param message the message content
     */
    public static void showWarningMessageBox(final String title, final String message) {
        showMessageBox(SWT.ICON_WARNING, title, message, null);
    }

    /**
     * Show an "ok" message box with the given message content and window title.
     *
     * @param title the window title
     * @param message the message content
     */
    public static void showOkMessageBox(final String title, final String message) {
        showMessageBox(SWT.OK, title, message, null);
    }

    /**
     * Determine the system default font
     *
     * @return the system default font
     */
    public static FontData getDefaultSystemFont() {
        FontData defaultFont = null;
        try {
            defaultFont = shell.getDisplay().getSystemFont().getFontData()[0];
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error attempting to determine system default font", e);
        }

        return defaultFont;
    }

    // utility class; prevent instantiation
    private UIUtils() {
    }
}
