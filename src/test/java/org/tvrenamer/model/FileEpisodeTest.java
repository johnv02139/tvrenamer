package org.tvrenamer.model;

import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.tvrenamer.controller.ShowInformationListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
        prefs.setRenameReplacementString("%S [%sx%e] %t");
        prefs.setMoveEnabled(false);
        mockListener = mock(ShowInformationListener.class);
    }

    /**
     * Test case for <a href="https://github.com/tvrenamer/tvrenamer/issues/36">Issue 36</a> where the title
     * "$pringfield" breaks the regex used for String.replaceAll()
     */
    @Test
    public void testGetNewFilenameSpecialRegexChars() throws Exception {
        String filename = "the.simpsons.5.10.avi";
        String showName = "The Simpsons";
        String title = "$pringfield";
        int seasonNum = 5;
        int episodeNum = 10;
        String resolution = "";
        File file = new File(new File(System.getProperty("java.io.tmpdir")), filename);
        createFile(file);

        Show show = new Show("1", showName, "");
        Season season5 = new Season(seasonNum);
        season5.addEpisode(episodeNum, title, new Date());
        show.setSeason(seasonNum, season5);
        ShowStore.addShow(showName, show);

        FileEpisode episode = new FileEpisode(showName,
                                              seasonNum,
                                              episodeNum,
                                              "avi",
                                              file);
    }

    /**
     * Ensure that colons (:) don't make it into the renamed filename <br />
     * Fixes <a href="https://github.com/tvrenamer/tvrenamer/issues/46">Issue 46</a>
     */
    @Test
    public void testColon() throws Exception {
        String filename = "steven.segal.lawman.1.01.avi";
        String showName = "Steven Seagal: Lawman";
        String title = "The Way of the Gun";
        int seasonNum = 1;
        int episodeNum = 1;
        String resolution = "";
        File file = new File(new File(System.getProperty("java.io.tmpdir")), filename);
        createFile(file);

        Show show = new Show("1", showName, "");
        Season season1 = new Season(seasonNum);
        season1.addEpisode(episodeNum, title, new Date());
        show.setSeason(seasonNum, season1);
        ShowStore.addShow(showName, show);

        FileEpisode episode = new FileEpisode(showName,
                                              seasonNum,
                                              episodeNum,
                                              "avi",
                                              file);
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
