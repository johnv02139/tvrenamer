package org.tvrenamer.model;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import java.io.InputStream;

public enum ItemState {
    ADDED("/icons/SweetieLegacy/16-circle-blue.png"), DOWNLOADING("/icons/SweetieLegacy/16-clock.png"), RENAMING(
        "/icons/SweetieLegacy/16-em-pencil.png"), SUCCESS("/icons/SweetieLegacy/16-em-check.png"), FAIL(
        "/icons/SweetieLegacy/16-em-cross.png");

    public final Image icon;

    private String fsPrefix = "src/main/resources/";

    private ItemState(String path) {
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) {
            icon = new Image(Display.getCurrent(), stream);
        } else {
            icon = new Image(Display.getCurrent(), fsPrefix + path);
        }
    }
}
