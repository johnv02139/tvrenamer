package org.tvrenamer.controller;

import org.tvrenamer.model.Series;
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

public class ListingsLookup {

    private static Logger logger = Logger.getLogger(ListingsLookup.class.getName());

    private static class ListingsRegistrations {
        private final List<ShowListingsListener> listeners;

        public ListingsRegistrations() {
            this.listeners = new LinkedList<>();
        }

        public void addListener(ShowListingsListener listener) {
            this.listeners.add(listener);
        }

        public List<ShowListingsListener> getListeners() {
            return Collections.unmodifiableList(listeners);
        }
    }

    private static final Map<String, ListingsRegistrations> listenersMap = new ConcurrentHashMap<>(100);

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
        ListingsRegistrations registrations = listenersMap.get(series.getNameKey());

        if (registrations != null) {
            for (ShowListingsListener listener : registrations.getListeners()) {
                if (series.hasSeasons()) {
                    listener.downloadListingsComplete(series);
                } else {
                    listener.downloadListingsFailed(series);
                }
            }
        }
    }

    private static void downloadListings(final Series series) {
        Callable<Boolean> showFetcher = new Callable<Boolean>() {
                @Override
                public Boolean call() throws InterruptedException {
                    try {
                        series.addEpisodes(TheTVDBProvider.getListings(series.getIdString(),
                                                                       series.getName()));
                        notifyListeners(series);
                        return true;
                    } catch (TVRenamerIOException e) {
                        notifyListeners(series);
                        return false;
                    } catch (Exception e) {
                        // Because this is running in a separate thread, an uncaught
                        // exception does not get caught by the main thread, and
                        // prevents this thread from dying.  Try to make sure that the
                        // thread dies, one way or another.
                        logger.info("generic exception doing getListings for " + series);
                        logger.info(e.toString());
                        return false;
                    }
                }
            };
        THREAD_POOL.submit(showFetcher);
    }

    /**
     * <p>
     * Download the series details if required, otherwise notify listener.
     * </p>
     * <ul>
     * <li>if we already have the series listings (the Series has season info) then just  call the method on the listener</li>
     * <li>if we don't have the listings, but are in the process of processing them (exists in listenersMap) then
     * add the listener to the registration</li>
     * <li>if we don't have the listings and aren't processing, then create the
     registration, add the listener and kick off
     * the download</li>
     * </ul>
     *
     * @param series
     *            the Series object representing the series
     * @param listener
     *            the listener to notify or register
     */
    public static void getListings(final Series series, ShowListingsListener listener) {
        if (series.hasSeasons()) {
            listener.downloadListingsComplete(series);
        } else {
            String key = series.getNameKey();
            ListingsRegistrations registrations = listenersMap.get(key);
            if (registrations == null) {
                registrations = new ListingsRegistrations();
                registrations.addListener(listener);
                listenersMap.put(key, registrations);
                downloadListings(series);
            } else {
                registrations.addListener(listener);
            }
        }
    }

    public static void cleanUp() {
        THREAD_POOL.shutdownNow();
    }

    public static void clear() {
        listenersMap.clear();
    }
}
