package org.tvrenamer.model;

import org.tvrenamer.controller.FileMover;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public class Moves implements Iterable<FileMover> {
    private final List<FileMover> list;

    public Moves(List<FileMover> list) {
        this.list = list;
    }

    public Moves() {
        list = new LinkedList<>();
    }

    public void add(FileMover move) {
        list.add(move);
    }

    public int size() {
        return list.size();
    }

    class MoveIterator implements Iterator<FileMover> {
        private int current = 0;

        public boolean hasNext() {
            if (current < Moves.this.list.size()) {
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

    public Stream<FileMover> stream() {
        return list.stream();
    }

    public static interface ValueProvider {
        public long getValue(FileMover mv);
    }

    public void sortBy(ValueProvider fn) {
        list.sort((m1, m2) -> (int) (fn.getValue(m2) - fn.getValue(m1)));
    }
}
