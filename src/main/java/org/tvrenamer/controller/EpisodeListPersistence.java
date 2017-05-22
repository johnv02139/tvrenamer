package org.tvrenamer.controller;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import org.tvrenamer.model.Episode;
import org.tvrenamer.model.Show;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EpisodeListPersistence {
    private static Logger logger = Logger.getLogger(EpisodeListPersistence.class.getName());

    // Use reflection provider so the default constructor is called, thus calling the superclass constructor
    private static final XStream xstream = new XStream(new PureJavaReflectionProvider());

    static {
        xstream.registerConverter(new AirDateConverter());
        xstream.alias("Data", Show.class);
        xstream.alias("episode", Episode.class);

        xstream.aliasField("Show", Show.class, "info");
        xstream.aliasField("ShowName", Show.class, "name");
        xstream.aliasField("id", Show.class, "idString");
        xstream.aliasField("Language", Show.class, "language");
        xstream.aliasField("IMDB_ID", Show.class, "imdb");

        xstream.omitField(Show.class, "seasons");
        xstream.omitField(Show.class, "idNum");
        // xstream.omitField(Show.class, "outer-class");

        // xstream.addImplicitCollection(Show.class, "episodes");

        xstream.alias("Episode", Episode.class);
        xstream.aliasField("SeasonNumber", Episode.class, "seasonNumber");
        xstream.aliasField("EpisodeNumber", Episode.class, "episodeNumber");
        xstream.aliasField("EpisodeName", Episode.class, "episodeName");
        xstream.aliasField("FirstAired", Episode.class, "firstAired");
    }

    /**
     * Save the episode list to the path.
     *
     * @param series
     *            the series whose episodes to save
     * @param path
     *            the path to save it to
     */
    public static void persist(Show series, Path path) {
        Path parent = path.getParent();
        try {
            Files.createDirectories(parent);
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Exception occured when creating cache directory", ioe);
        }
        if (!Files.exists(parent)) {
            logger.warning("could not create cache directory " + parent
                           + "; not attempting to write " + path);
            return;
        }

        String xml = xstream.toXML(series);

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(xml);
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Exception occured when writing list file", ioe);
        }
    }

    /**
     * Recreate a Show with a list of episodes from the cached XML file
     *
     * @param path
     *            path to read the file from
     */
    public static Show readShow(Path path) {
        if (Files.notExists(path)) {
            throw new IllegalStateException("cached show path doesn't exist");
        }

        try (InputStream in = Files.newInputStream(path)) {
            return (Show) xstream.fromXML(in);
        } catch (XStreamException xe) {
            logger.log(Level.WARNING, "exception trying to decache episodes from "
                       + path, xe);
            // If anything goes wrong, fail, and we'll re-fetch the episodes from
            // the internet provider
            return null;
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "I/O exception trying to decache episodes from "
                       + path, ioe);
            return null;
        }
    }
}
