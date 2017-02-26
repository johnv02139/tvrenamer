package org.tvrenamer.model;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public FileEpisode add(String filename) {
        FileEpisode ep = new FileEpisode(filename);
        put(filename, ep);
        return ep;
    }

    public void add(Path file) {
        // unused and untested, but compiles and looks right!
        boolean descend = prefs.isRecursivelyAddFolders();
        if (Files.exists(file)) {
            try {
                if (Files.isHidden(file)) {
                    logger.finer("ignoring hidden file " + file);
                } else if (Files.isDirectory(file)) {
                    if (descend) {
                        DirectoryStream<Path> contents = Files.newDirectoryStream(file);
                        if (contents != null) {
                            // recursive call
                            contents.forEach(pth -> add(pth));
                            contents.close();
                        }
                    }
                } else {
                    add(file.toAbsolutePath());
                }
            } catch (IOException e) {
                // TODO: catch other exceptions, and handle gracefully
                logger.warning("error trying to descend directories adding to EpisodeDb");
                return;
            }
        }
    }
}
