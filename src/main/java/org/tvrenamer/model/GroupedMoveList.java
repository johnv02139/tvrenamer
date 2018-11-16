package org.tvrenamer.model;

import org.tvrenamer.controller.FileMover;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a mapping from Strings to <code>Moves</code> objects.
 * In fact, it is primarily a wrapper around a HashMap.
 */
public class GroupedMoveList {
    private final Map<String, Moves> table;
    private final String userData;

    public Set<String> keys() {
        return table.keySet();
    }

    public Moves getMoves(String key) {
        return table.get(key);
    }

    public Collection<Moves> moveLists() {
        return table.values();
    }

    public String getUserData() {
        return userData;
    }

    public static interface KeyProvider {
        public String getKey(FileMover mv);
    }

    /**
     * Turns a flat list of file moves into a hash map, keyed by the given
     * KeyProvider.
     *
     * @param fn
     *   a KeyProvider that gets the key from a FileMover
     * @param moves
     *   a group of FileMovers
     * @param userData
     *   intended to let the user document what all the values have in common;
     *   can be null, or ultimately used for any purpose
     */
    private GroupedMoveList(final KeyProvider fn,
                            final Moves moves,
                            final String userData)
    {
        this.userData = userData;
        table = new HashMap<>();

        for (FileMover move : moves) {
            String key = fn.getKey(move);

            // Associate the given move with the given key.  If the
            // key is already mapped to a list, adds the move to the
            // list.  If the key had not yet been mapped, creates a
            // new list, maps it to the key, and adds the move.
            Moves mappedList = table.get(key);
            if (mappedList == null) {
                mappedList = new Moves();
                table.put(key, mappedList);
            }

            mappedList.add(move);
        }
    }

    /**
     * Turns a flat list of file moves into a hash map, keyed by the given
     * KeyProvider.
     *
     * @param fn
     *   a KeyProvider that gets the key from a FileMover
     * @param moves
     *   a group of FileMovers
     */
    public GroupedMoveList(final KeyProvider fn,
                           final Moves moves)
    {
        this(fn, moves, null);
    }

    /**
     * Re-groups those moves previously mapped to the given key.
     *
     * @param fn
     *   a KeyProvider that gets the key from a FileMover
     * @param key
     *   the key that indicates what subgroup to look at
     * @return
     *   a Map from basename to list of moves that resolve to that basename
     */
    public GroupedMoveList subGroup(final KeyProvider fn,
                                    final String key)
    {
        return new GroupedMoveList(fn, table.get(key), key);
    }
}
