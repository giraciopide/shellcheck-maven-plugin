package dev.dimlight.maven.plugin.shellcheck;

/*-
 * #%L
 * dev.dimlight:shellcheck-maven-plugin
 * %%
 * Copyright (C) 2020 - 2023 Marco Nicolini
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A stateful decorator to divide a list in chunks of a (at most) given size.
 *
 * @param <T> the type of elements in the original list.
 * @author Marco Nicolini
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "We know what we're doing")
public class ChunkIterator<T> implements Iterator<List<T>> {

    private final Iterator<T> iterator;
    private final int chunkSize;

    private ChunkIterator(int chunkSize, Iterator<T> iter) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive: [" + chunkSize + "]");
        }
        this.chunkSize = chunkSize;
        this.iterator = iter;
    }

    /**
     * @param chunkSize the max chunk size.
     * @param iterable  the iterable we want to iterate over (in chunk)
     * @param <T>       the type of elements in the iterable
     * @return an iterator that will group items in chunks of (maximum) given chunk size.
     */
    public static <T> Iterator<List<T>> over(int chunkSize, Iterable<T> iterable) {
        return new ChunkIterator<>(chunkSize, iterable.iterator());
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public List<T> next() {
        int currentChunkSize = 0;
        final ArrayList<T> out = chunkSize < 2048 ? new ArrayList<>(chunkSize) : new ArrayList<>();
        while (iterator.hasNext() && currentChunkSize < chunkSize) {
            out.add(iterator.next());
            currentChunkSize++;
        }
        return out;
    }
}
