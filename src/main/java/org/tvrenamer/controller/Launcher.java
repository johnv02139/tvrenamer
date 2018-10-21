package org.tvrenamer.controller;

import org.tvrenamer.view.ResultsTable;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Launcher {
    private static Logger logger = Logger.getLogger(Launcher.class.getName());

    // Static initalisation block
    static {
        // Find logging.properties file inside jar
        InputStream loggingConfigStream = Launcher.class.getResourceAsStream("/etc/logging.properties");

        if (loggingConfigStream != null) {
            try {
                LogManager.getLogManager().readConfiguration(loggingConfigStream);
            } catch (IOException e) {
                System.err.println("Exception thrown while loading logging config");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ResultsTable ui = new ResultsTable();
        ui.runUi();
    }
}
