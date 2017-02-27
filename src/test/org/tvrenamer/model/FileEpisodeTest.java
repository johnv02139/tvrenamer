package org.tvrenamer.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.tvrenamer.controller.SeriesLookupListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FileEpisodeTest {
    private static Logger logger = Logger.getLogger(FileEpisodeTest.class.getName());

    private static final String TEMP_DIR_NAME = System.getProperty("java.io.tmpdir");
    private static final Path TEMP_DIR = Paths.get(TEMP_DIR_NAME);

    private List<Path> testFiles;

    private UserPreferences prefs;
    private SeriesLookupListener mockListener;

    @Before
    public void setUp() throws Exception {
        testFiles = new ArrayList<>();
        prefs = UserPreferences.getInstance();
        prefs.setMoveEnabled(false);
        mockListener = mock(SeriesLookupListener.class);
    }

    // Helper method to generate a Path for use in testing FileEpisode.getExtension.
    private void testGetExtensionString(String filepath, String extension) {
        Path testPath = Paths.get(filepath);
        String suffix = FileEpisode.getExtension(testPath);
        assertEquals(extension, suffix);
    }

    @Test
    public void testGetExtension() {
        testGetExtensionString("dexter.407.720p.hdtv.x264-sys.mkv", ".mkv");
        testGetExtensionString("Marvels.Agents.of.S.H.I.E.L.D.S04E03.1080p.HDTV.x264-KILLERS[ettv].avi",
                               ".avi");
        testGetExtensionString("/TV/Dexter/S05E05 First Blood.mp4", ".mp4");
        testGetExtensionString("Supernatural", "");
        testGetExtensionString(".gitignore", "");
        testGetExtensionString("foo/bar/.gitignore", "");
    }

    /**
     * Test case for <a href="https://github.com/tvrenamer/tvrenamer/issues/36">Issue 36</a> where the title
     * "$pringfield" breaks the regex used for String.replaceAll()
     */
    @Test
    public void testGetNewFilenameSpecialRegexChars() throws Exception {
        final String testReplacementPattern = "%S [%sx%e] %t %r";
        prefs.setRenameReplacementString(testReplacementPattern);

        String filename = "the.simpsons.5.10.720p.avi";
        Path path = TEMP_DIR.resolve(filename);
        Files.createFile(path);
        testFiles.add(path);

        FileEpisode episode = new FileEpisode(filename);
        String showName = "The Simpsons";
        String seasonNum = "5";
        String episodeNum = "10";
        String resolution = "720p";
        episode.setRawSeries(showName);
        episode.setFilenameSeason(seasonNum);
        episode.setFilenameEpisode(episodeNum);
        episode.setFilenameResolution(resolution);

        Series series = new Series(showName, "1");
        ShowStore.addSeriesToStore(showName, series);
        episode.setSeries(series);

        Season season5 = new Season(series, seasonNum);
        series.setSeason(seasonNum, season5);
        episode.setSeason();

        String title = "$pringfield";
        Episode ep = new Episode.Builder()
            .seasonNum(seasonNum)
            .episodeNum(episodeNum)
            .title(title)
            .airDate("")
            .build();

        season5.addEpisode(ep);

        String newFilename = episode.getProposedFilename();
        assertEquals("The Simpsons [5x10] $pringfield 720p.avi", newFilename);
    }

    /**
     * Ensure that colons (:) don't make it into the renamed filename <br />
     * Fixes <a href="https://github.com/tvrenamer/tvrenamer/issues/46">Issue 46</a>
     */
    @Test
    public void testColon() throws Exception {
        final String testReplacementPattern = "%S [%sx%e] %t";
        prefs.setRenameReplacementString(testReplacementPattern);

        String filename = "steven.segal.lawman.1.01.avi";
        Path path = TEMP_DIR.resolve(filename);
        Files.createFile(path);
        testFiles.add(path);

        FileEpisode fileEpisode = new FileEpisode(filename);

        String showName = "Steven Seagal: Lawman";
        String seasonNum = "1";
        String episodeNum = "1";
        fileEpisode.setRawSeries(showName);
        fileEpisode.setFilenameSeason(seasonNum);
        fileEpisode.setFilenameEpisode(episodeNum);

        Series series = new Series(showName, "1");
        ShowStore.addSeriesToStore(showName, series);
        fileEpisode.setSeries(series);

        Season season1 = new Season(series, seasonNum);
        series.setSeason(seasonNum, season1);
        fileEpisode.setSeason();

        String title = "The Way of the Gun";
        Episode ep = new Episode.Builder()
            .seasonNum(seasonNum)
            .episodeNum(episodeNum)
            .title(title)
            .airDate("")
            .build();
        season1.addEpisode(ep);

        String newFilename = fileEpisode.getProposedFilename();
        assertFalse("Resulting filename must not contain a ':' as it breaks Windows", newFilename.contains(":"));
    }

    @After
    public void teardown() throws Exception {
        for (Path path : testFiles) {
            logger.info("Deleting " + path);
            Files.delete(path);
        }
    }
}
