package org.tvrenamer.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.tvrenamer.controller.ShowInformationListener;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FileEpisodeTest {
    private static Logger logger = Logger.getLogger(FileEpisodeTest.class.getName());

    private List<File> testFiles;

    private UserPreferences prefs;
    private ShowInformationListener mockListener;

    @Before
    public void setUp() throws Exception {
        testFiles = new ArrayList<>();
        prefs = UserPreferences.getInstance();
        prefs.setMoveEnabled(false);
        mockListener = mock(ShowInformationListener.class);
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
        String showName = "The Simpsons";
        String title = "$pringfield";
        int seasonNum = 5;
        int episodeNum = 10;
        String resolution = "720p";
        File file = new File(new File(System.getProperty("java.io.tmpdir")), filename);
        createFile(file);

        Series series = new Series(showName, "1");
        Season season5 = new Season(series, seasonNum);
        season5.addEpisode(episodeNum, title, LocalDate.now());
        series.setSeason(seasonNum, season5);
        ShowStore.addShow(showName, series);

        FileEpisode episode = new FileEpisode(filename);
        episode.setFilenameSeries(showName);
        episode.setFilenameSeason(seasonNum);
        episode.setFilenameEpisode(episodeNum);
        episode.setFilenameResolution(resolution);
        episode.setDownloaded();

        String newFilename = episode.getNewFilename();

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
        String showName = "Steven Seagal: Lawman";
        String title = "The Way of the Gun";
        int seasonNum = 1;
        int episodeNum = 1;
        String resolution = "";
        File file = new File(new File(System.getProperty("java.io.tmpdir")), filename);
        createFile(file);

        Series series = new Series(showName, "1");
        Season season1 = new Season(series, seasonNum);
        season1.addEpisode(episodeNum, title, LocalDate.now());
        series.setSeason(seasonNum, season1);
        ShowStore.addShow(showName, series);

        FileEpisode fileEpisode = new FileEpisode(filename);
        fileEpisode.setFilenameSeries(showName);
        fileEpisode.setFilenameSeason(seasonNum);
        fileEpisode.setFilenameEpisode(episodeNum);
        fileEpisode.setFilenameResolution(resolution);
        fileEpisode.setRenamed();

        String newFilename = fileEpisode.getNewFilename();

        assertFalse("Resulting filename must not contain a ':' as it breaks Windows", newFilename.contains(":"));
    }

    /**
     * Helper method to physically create the file and add to file list for later deletion.
     */
    private void createFile(File file) throws IOException {
        file.createNewFile();
        testFiles.add(file);
    }

    @After
    public void teardown() throws Exception {
        for (File file : testFiles) {
            logger.info("Deleting " + file);
            file.delete();
        }
    }
}
