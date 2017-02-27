package org.tvrenamer.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class EpisodeDb {

    private static Logger logger = Logger.getLogger(EpisodeDb.class.getName());

    private final UserPreferences prefs = UserPreferences.getInstance();

    // This class is currently just a very thin wrapper around a ConcurrentHashMap,
    // but I intend to extend it later.
    private final Map<String, FileEpisode> episodes = new ConcurrentHashMap<>(1000);

    public void clear() {
        episodes.clear();
    }

    public boolean containsKey(Object key) {
        return episodes.containsValue(key);
    }

    public FileEpisode get(String key) {
        return episodes.get(key);
    }

    public Set<String> keySet() {
        return episodes.keySet();
    }

    public boolean put(String key, FileEpisode value) {
        if (value == null) {
            logger.info("cannot put null value into EpisodeDb!!!");
            return false;
        }
        FileEpisode val = episodes.put(key, value);
        return (value.equals(val));
    }

    public FileEpisode remove(String key) {
        return episodes.remove(key);
    }

    public boolean remove(String key, FileEpisode value) {
        return episodes.remove(key, value);
    }

    public Collection<FileEpisode> values() {
        return episodes.values();
    }

    public FileEpisode add(FileEpisode ep) {
        String filename = ep.getPathString();
        put(filename, ep);
        return ep;
    }
}
