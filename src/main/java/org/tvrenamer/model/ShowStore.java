package org.tvrenamer.model;

import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.TheTVDBProvider;
import org.tvrenamer.controller.util.StringUtils;
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

public class ShowStore {

    private static Logger logger = Logger.getLogger(ShowStore.class.getName());

    private static class ShowRegistrations {
        private final List<ShowInformationListener> mListeners;

        public ShowRegistrations() {
            this.mListeners = new LinkedList<>();
        }

        public void addListener(ShowInformationListener listener) {
            this.mListeners.add(listener);
        }

        public List<ShowInformationListener> getListeners() {
            return Collections.unmodifiableList(mListeners);
        }
    }

    private static final Map<String, Series> _shows = new ConcurrentHashMap<>(100);
    private static final Map<String, ShowRegistrations> _showRegistrations = new ConcurrentHashMap<>();

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


    /**
     * Transform a string which we believe represents a show name, to the string we will
     * use for the query.
     *
     * For the internal data structures used by this class, the keys should always be
     * the result of this method.  It's not up to callers to worry about; they can pass
     * in show names however they have them, and the methods here will be sure to
     * normalize them in preparation for querying.
     *
     * @param showName
     *            the substring of the file path that we think represents the show name
     * @return a version of the show name that is more suitable for a query; this may
     *         include case normalization, removal of superfluous whitepsace and
     *         punctuation, etc.
     */
    public static String makeQueryString(String showName) {
        return StringUtils.replacePunctuation(showName).toLowerCase();
    }

    private static void notifyListeners(String showName, Series show) {
        ShowRegistrations registrations = _showRegistrations.get(showName);

        if (registrations != null) {
            for (ShowInformationListener informationListener : registrations.getListeners()) {
                if (show instanceof UnresolvedShow) {
                    informationListener.downloadFailed(show);
                } else {
                    informationListener.downloadComplete(show);
                }
            }
        }
    }

    public static void cleanUp() {
        THREAD_POOL.shutdownNow();
    }

    public static void clear() {
        _shows.clear();
        _showRegistrations.clear();
    }

    /**
     * Add a show to the store, registered by the show name.<br />
     * Added this distinct method to enable unit testing
     *
     * @param showName
     *            the show name
     * @param show
     *            the {@link Series}
     */
    static void addShow(String showName, Series show) {
        if (show instanceof UnresolvedShow) {
            logger.info("Failed to get options or episodes for '" + show.getName());
        } else {
            logger.fine("Options and episodes for '" + show.getName() + "' acquired");
        }
        _shows.put(makeQueryString(showName), show);
        notifyListeners(showName, show);
    }

    // Given a list of one or more options for which show we're dealing with,
    // choose the best one and return it.
    private static Series selectShowOption(String showName, List<Series> options) {
        for (Series s : options) {
            logger.fine("option: " + s.getName() + " for " + showName);
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
                    addShow(showName, new UnresolvedShow(showName, e));
                    return true;
                }
                int nOptions = (options == null) ? 0 : options.size();
                if (nOptions == 0) {
                    logger.info("did not find any options for " + showName);
                    addShow(showName, new UnresolvedShow(showName));
                    return true;
                }
                addShow(showName, selectShowOption(showName, options));

                return true;
            }
        };
        THREAD_POOL.submit(showFetcher);
    }

    /**
     * <p>
     * Download the show details if required, otherwise notify listener.
     * </p>
     * <ul>
     * <li>if we have already downloaded the show (exists in _shows) then just call the method on the listener</li>
     * <li>if we don't have the show, but are in the process of downloading the show (exists in _showRegistrations) then
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
    public static void mapStringToShow(String showName, ShowInformationListener listener) {
        String showKey = makeQueryString(showName);
        Series show = _shows.get(showKey);
        if (show != null) {
            listener.downloadComplete(show);
        } else {
            ShowRegistrations registrations = _showRegistrations.get(showKey);
            if (registrations != null) {
                registrations.addListener(listener);
            } else {
                registrations = new ShowRegistrations();
                registrations.addListener(listener);
                _showRegistrations.put(showKey, registrations);
                downloadShow(showKey);
            }
        }
    }
}
