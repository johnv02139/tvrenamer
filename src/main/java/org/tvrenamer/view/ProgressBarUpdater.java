package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TaskBar;
import org.eclipse.swt.widgets.TaskItem;

import org.tvrenamer.controller.FileMover;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class ProgressBarUpdater implements Runnable {
    private static Logger logger = Logger.getLogger(ProgressBarUpdater.class.getName());

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Queue<Future<Boolean>> futures = new LinkedList<>();
    private final int totalNumFiles;

    private final UIStarter ui;
    private final Display display;
    private final Shell shell;
    private final TaskItem taskItem;
    private final ProgressBar progressBar;
    private final int barSize;

    private static TaskItem getTaskItem(Display display, Shell shell) {
        TaskItem taskItem = null;
        TaskBar taskBar = display.getSystemTaskBar();
        if (taskBar != null) {
            taskItem = taskBar.getItem(shell);
            if (taskItem == null) {
                taskItem = taskBar.getItem(null);
            }
        }
        return taskItem;
    }

    private static int createFutures(Map<Path, List<FileMover>> episodes,
                                     Queue<Future<Boolean>> futures)
    {
        int count = 0;

        for (List<FileMover> moves : episodes.values()) {
            for (FileMover move : moves) {
                futures.add(executor.submit(new Callable<Boolean>() {
                        @Override
                        public Boolean call() {
                            return move.moveFile();
                        }
                    }));
                count++;
            }
        }
        logger.fine("have " + count + " files to move");

        return count;
    }

    public ProgressBarUpdater(Map<Path, List<FileMover>> episodes, UIStarter ui) {
        this.ui = ui;
        this.display = ui.getDisplay();
        this.shell = ui.getShell();
        this.progressBar = ui.getProgressBar();
        this.barSize = progressBar.getMaximum();
        this.taskItem = getTaskItem(display, shell);
        this.totalNumFiles = createFutures(episodes, futures);
    }

    private void setProgress(int nRemaining) {
        if (display.isDisposed()) {
            return;
        }

        final float progress = (float) (totalNumFiles - nRemaining) / totalNumFiles;
        display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (progressBar.isDisposed()) {
                        return;
                    }
                    progressBar.setSelection(Math.round(progress * barSize));
                    if (taskItem.isDisposed()) {
                        return;
                    }
                    taskItem.setProgress(Math.round(progress * 100));
                }
            });
    }

    @Override
    public void run() {
        while (true) {
            int remaining = futures.size();
            setProgress(remaining);

            if (remaining > 0) {
                try {
                    Future<Boolean> future = futures.remove();
                    Boolean success = future.get();
                    logger.finer("future returned: " + success);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            } else {
                display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            taskItem.setOverlayImage(null);
                            taskItem.setProgressState(SWT.DEFAULT);
                            ui.refreshTable();
                        }
                    });

                return;
            }
        }
    }

    public void runThread() {
        Thread progressThread = new Thread(this);
        progressThread.setName(PROGRESS_THREAD_LABEL);
        progressThread.setDaemon(true);
        progressThread.start();
    }

    public static void shutDown() {
        executor.shutdownNow();
    }
}
