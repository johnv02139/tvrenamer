package org.tvrenamer.model;

import org.tvrenamer.controller.FileMover;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class simply represents a collection of FileMover objects.  In fact,
 * it's a very thin wrapper around (and a plug-in replacement for) a LinkedList
 * of FileMovers, and doesn't add a lot; but, I think it's cleaner.  It does
 * add one thing, the sortBy method.
 */
public class Moves implements Iterable<FileMover> {
    private final List<FileMover> list = new LinkedList<>();

    /**
     * Adds a FileMover to this object.
     *
     * @param move
     *    the FileMover to add to this object
     *
     */
    public void add(FileMover move) {
        list.add(move);
    }

    /**
     * Gets the number of FileMovers in this object.
     *
     * @return
     *    the number of FileMovers in this object
     *
     */
    public int size() {
        return list.size();
    }

    private class MoveIterator implements Iterator<FileMover> {
        private int current = 0;
        private int sizeAtCreation = Moves.this.list.size();

        public boolean hasNext() {
            if (Moves.this.list.size() != sizeAtCreation) {
                throw new ConcurrentModificationException();
            }
            if (current < sizeAtCreation) {
                return true;
            } else {
                return false;
            }
        }

        public FileMover next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return Moves.this.list.get(current++);
        }
    }

    public Iterator<FileMover> iterator() {
        return new MoveIterator();
    }

    /**
     * Wrapper around a method that takes a FileMover and returns a long.
     *
     * <p>I'm not sure if this interface is necessary.  Really, I want
     * {@link #sortBy} to just take a method as an argument.  But,
     * I don't think that exists.  Prior to Java 8, we'd need to do
     * something like this, and then the caller would need to create
     * a new ValueProvider, and explicitly define getValue.  With Java 8,
     * a much nicer syntax can be used, because when an interface (or
     * class) exposes only one method, Java 8 allows you to just provide
     * the implementation of that method, without the declarations.
     *
     * <p>Again, there may be an even simpler way to do this, either now or
     * perhaps with some later enhancement to the language.  For now, this
     * is simple enough.
     */
    public static interface ValueProvider {
        public long getValue(FileMover mv);
    }

    /**
     * Sorts the list of FileMovers by the given function.
     *
     * <p>Officially, takes an instance of "ValueProvider".  But in
     * practice, code that takes a FileMover and returns a long will
     * do just fine.
     *
     * @param fn
     *    code which takes a FileMover and returns a long, and which
     *    should be used as the "key" for sorting the list
     */
    public void sortBy(ValueProvider fn) {
        list.sort((m1, m2) -> (int) (fn.getValue(m2) - fn.getValue(m1)));
    }
}
