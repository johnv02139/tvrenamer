package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.graphics.Image;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemState {
    public enum Status {
        SUCCESS,
        OPTIONS,
        ADDED,
        DOWNLOADING,
        RENAMING,
        FAIL;
    }

    private static final String ICON_PATH = "/icons/SweetieLegacy/";

    private final Status status;
    private final Image image;
    private final String ordering;

    ItemState(String ordering, Status status, String imageFilename) {
        this.ordering = ordering;
        this.status = status;
        this.image = UIUtils.readImageFromPath(ICON_PATH + imageFilename);
    }

    private static final ItemState[] STANDARD_STATUSES = {
        new ItemState("a", Status.SUCCESS, "16-em-check.png"),
        new ItemState("b", Status.OPTIONS, "16-circle-green-add.png"),
        new ItemState("c", Status.ADDED, "16-circle-blue.png"),
        new ItemState("d", Status.DOWNLOADING, "16-clock.png"),
        new ItemState("e", Status.RENAMING, "16-em-pencil.png"),
        new ItemState("f", Status.FAIL, "16-em-cross.png")
    };

    private static final Map<Status, ItemState> MAPPING = new ConcurrentHashMap<>();
    private static final Map<Image, ItemState> IMAGES = new ConcurrentHashMap<>();

    static {
        for (ItemState state : STANDARD_STATUSES) {
            MAPPING.put(state.status, state);
            IMAGES.put(state.image, state);
        }
    }

    public static Image getIcon(final Status status) {
        ItemState state = MAPPING.get(status);
        if (state == null) {
            return null;
        }
        return state.image;
    }

    public static String getImagePriority(final Image img) {
        ItemState state = IMAGES.get(img);
        if (state == null) {
            return null;
        }
        return state.ordering;
    }

    public static Status getImageStatus(final Image img) {
        ItemState state = IMAGES.get(img);
        if (state == null) {
            return Status.FAIL;
        }
        return state.status;
    }
}
