package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.IMDB_BASE_URL;

import org.tvrenamer.controller.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a TV Show, with a name, url and list of seasons.
 */
public class Show implements Comparable<Show> {
    private final String id;
    private final String name;
    private final String dirName;
    private final String imdb;

    private final Map<Integer, Season> seasons;

    public Show(String id, String name, String imdb) {
        this.id = id;
        this.name = name;
        this.imdb = imdb;
        dirName = StringUtils.sanitiseTitle(name);

        seasons = new ConcurrentHashMap<>();
    }

    public Show(String id, String name) {
        this(id, name, null);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNameKey() {
        return name.toLowerCase();
    }

    public String getDirName() {
        return dirName;
    }

    public String getUrl() {
        return (imdb == null) ? "" : IMDB_BASE_URL + imdb;
    }

    public void setSeason(int sNum, Season season) {
        seasons.put(sNum, season);
    }

    public Season getSeason(int sNum) {
        if (seasons.containsKey(sNum)) {
            return seasons.get(sNum);
        } else {
            return null;
        }
    }

    public boolean hasSeasons() {
        return (seasons.size() > 0);
    }

    @Override
    public String toString() {
        return "Show [" + name + ", id=" + id + ", imdb=" + imdb + ", " + seasons.size() + " seasons]";
    }

    public String toLongString() {
        return "Show [id=" + id + ", name=" + name + ", imdb=" + imdb + ", seasons=" + seasons + "]";
    }

    @Override
    public int compareTo(Show other) {
        return Integer.parseInt(other.id) - Integer.parseInt(this.id);
    }
}
