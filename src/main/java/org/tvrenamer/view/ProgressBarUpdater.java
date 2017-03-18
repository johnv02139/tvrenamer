package org.tvrenamer.view;

import org.tvrenamer.controller.FileMover;
import org.tvrenamer.controller.UpdateCompleteHandler;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class ProgressBarUpdater implements Runnable {
    private static Logger logger = Logger.getLogger(ProgressBarUpdater.class.getName());

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final int totalNumFiles;
    private final Queue<Future<Boolean>> futures = new LinkedList<>();

    private final UpdateCompleteHandler updateCompleteHandler;

    private final ProgressProxy proxy;

    public ProgressBarUpdater(ProgressProxy proxy, Queue<FileMover> moves,
                              UpdateCompleteHandler updateComplete)
    {
        this.proxy = proxy;
        for (FileMover move : moves) {
            futures.add(executor.submit(move));
        }
        totalNumFiles = futures.size();
        updateCompleteHandler = updateComplete;
    }

    @Override
    public void run() {
        while (true) {
            final int size = futures.size();
            proxy.setProgress((float) (totalNumFiles - size) / totalNumFiles);

            if (size == 0) {
                this.updateCompleteHandler.onUpdateComplete();
                return;
            }

            try {
                Future<Boolean> future = futures.remove();
                Boolean success = future.get();
                logger.finer("future returned: " + success);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public static void shutDown() {
        executor.shutdownNow();
    }
}
