package org.tvrenamer.controller;

import org.tvrenamer.model.Series;
import org.tvrenamer.model.UnresolvedShow;
import org.tvrenamer.model.except.TVRenamerIOException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

public class SeriesLookup {

    private static Logger logger = Logger.getLogger(SeriesLookup.class.getName());

    private static class ShowRegistrations {
        private final List<SeriesLookupListener> listeners;

        public ShowRegistrations() {
            listeners = new LinkedList<>();
        }

        public void addListener(SeriesLookupListener listener) {
            listeners.add(listener);
        }

        public List<SeriesLookupListener> getListeners() {
            return Collections.unmodifiableList(listeners);
        }
    }

    private static final Map<String, ShowRegistrations> SHOW_LISTENERS = new ConcurrentHashMap<>();

    private static final Map<String, Series> SERIES_MAP = new ConcurrentHashMap<>(100);

    private static final ExecutorService THREAD_POOL
        = Executors.newCachedThreadPool(new ThreadFactory() {
                // We want the lookup thread to run at the minimum priority, to try to
                // keep the UI as responsive as possible.
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.setDaemon(true);
                    return t;
                }
            });

    private static void notifyListeners(Series series) {
        ShowRegistrations registrations = SHOW_LISTENERS.get(series.getNameKey());

        if (registrations != null) {
            for (SeriesLookupListener informationListener : registrations.getListeners()) {
                if (series instanceof UnresolvedShow) {
                    informationListener.downloadFailed(series);
                } else {
                    informationListener.downloadComplete(series);
                }
            }
        }
    }

    public static void cleanUp() {
        THREAD_POOL.shutdownNow();
    }

    public static void clear() {
        SERIES_MAP.clear();
        SHOW_LISTENERS.clear();
    }

    /**
     * Add a series to the store, registered by the lower-cased series name.<br />
     * Added this distinct method to enable unit testing
     *
     * @param series
     *            the {@link Series}
     */
    public static void addSeriesToStore(Series series) {
        String key = series.getNameKey();
        if (series instanceof UnresolvedShow) {
            logger.info("Failed to get options or episodes for '" + key);
        } else {
            logger.fine("Options and episodes for '" + key + "' acquired");
        }
        SERIES_MAP.put(key, series);
        notifyListeners(series);
    }

    // Given a list of two or more options for which show we're dealing with,
    // choose the best one and return it.
    private static Series selectShowOption(String showName, List<Series> options) {
        for (Series s : options) {
            logger.info("option: " + s.getName() + " for " + showName);
        }
        // TODO: might not always be option zero...
        return options.get(0);
    }

    private static void downloadShow(final String showName) {
        Callable<Boolean> showFetcher = new Callable<Boolean>() {
            @Override
            public Boolean call() throws InterruptedException {
                List<Series> options;
                try {
                    options = TheTVDBProvider.querySeriesName(showName);
                } catch (TVRenamerIOException e) {
                    logger.info("exception getting options for " + showName);
                    addSeriesToStore(new UnresolvedShow(showName, e));
                    return true;
                }
                int nOptions = (options == null) ? 0 : options.size();
                if (nOptions == 0) {
                    logger.info("did not find any options for " + showName);
                    addSeriesToStore(new UnresolvedShow(showName));
                    return true;
                } else if (nOptions == 1) {
                    addSeriesToStore(options.get(0));
                } else {
                    logger.info("got " + nOptions + " options for " + showName);
                    addSeriesToStore(selectShowOption(showName, options));
                }

                return true;
            }
        };
        THREAD_POOL.submit(showFetcher);
    }


    /**
     * Does what downloadShow does, but in a single thread.
     *
     * Fetch the best option for a given series name, and return a Series object that
     * represents it.
     *
     * This method is intended only for debugging.  Aside from debugging, there's no reason
     * we'd want to limit downloading shows to a single thread; in fact, it would presumably
     * make the UI unresponsive if we did.  But sometimes, it can be easier to debug a problem
     * if it all goes on sequentially, in a single thread.
     *
     * It might be nice to have a way to structure downloadShow so that it could run in
     * a single thread or not, but it's just too intertwined.  The best way to have a method
     * to use a single thread is as a separate method.
     *
     * I'm marking the method deprecated.  That's not the exact right fit; it was never a
     * supported method.  But it shouldn't be used, and "deprecated" serves that purpose.
     *
     * @showName series the name of the TV series (presumably from a filename) that we are
     *           going to query for and try to figure out which show it refers to
     * @return a Series object: either one representing the show we found which we think
     *         is the correct option, or an UnresolvedShow instance if we didn't find anything.
     */
    @Deprecated
    public static Series getSeries(final String showName) {
        List<Series> options;
        try {
            options = TheTVDBProvider.querySeriesName(showName);
        } catch (TVRenamerIOException e) {
            logger.info("exception getting options for " + showName);
            return new UnresolvedShow(showName, e);
        }
        int nOptions = (options == null) ? 0 : options.size();
        if (nOptions == 0) {
            logger.info("did not find any options for " + showName);
            return new UnresolvedShow(showName);
        }
        if (nOptions == 1) {
            return options.get(0);
        }
        return selectShowOption(showName, options);
    }

    /**
     * <p>
     * Download the show details if required, otherwise notify listener.
     * </p>
     * <ul>
     * <li>if we have already downloaded the show (exists in SERIES_MAP) then just call the method on the listener</li>
     * <li>if we don't have the show, but are in the process of downloading the show (exists in SHOW_LISTENERS) then
     * add the listener to the registration</li>
     * <li>if we don't have the show and aren't downloading, then create the registration, add the listener and kick off
     * the download</li>
     * </ul>
     *
     * @param showName
     *            the name of the show
     * @param listener
     *            the listener to notify or register
     */
    public static void mapStringToShow(String showName, SeriesLookupListener listener) {
        String showKey = showName.toLowerCase();
        Series show = SERIES_MAP.get(showKey);
        if (show != null) {
            listener.downloadComplete(show);
        } else {
            boolean needToDownload = true;
            synchronized (SHOW_LISTENERS) {
                ShowRegistrations registrations = SHOW_LISTENERS.get(showKey);
                if (registrations == null) {
                    registrations = new ShowRegistrations();
                    SHOW_LISTENERS.put(showKey, registrations);
                } else {
                    // If we already have listeners for the show key, that means we're
                    // already looking up the show, so all we need to do is add another
                    // listener.
                    needToDownload = false;
                }
                registrations.addListener(listener);
            }
            if (needToDownload) {
                downloadShow(showName);
            }
        }
    }
}
