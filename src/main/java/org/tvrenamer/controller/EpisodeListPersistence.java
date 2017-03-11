package org.tvrenamer.controller;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import org.tvrenamer.model.Episode;
import org.tvrenamer.model.EpisodeList;

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
        xstream.alias("episodelist", EpisodeList.class);
        xstream.alias("episode", Episode.class);
    }

    /**
     * Save the episode list to the path.
     *
     * @param prefs
     *            the episode list to save
     * @param path
     *            the path to save it to
     */
    public static void persist(EpisodeList list, Path path) {
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

        String xml = xstream.toXML(list);

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(xml);
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Exception occured when writing list file", ioe);
        }
    }

    /**
     * Load the list from path.
     *
     * @param path
     *            the file to read
     * @return the populated episode list
     */
    public static EpisodeList retrieve(Path path) {
        // Instantiate the object so the Observable superclass is called corrected
        EpisodeList list = null;

        try (InputStream in = Files.newInputStream(path)) {
            list = (EpisodeList) xstream.fromXML(in);
        } catch (IllegalArgumentException | UnsupportedOperationException
                 | IOException  | SecurityException e)
        {
            // If anything goes wrong, assume defaults
            return null;
        }

        return list;
    }
}
