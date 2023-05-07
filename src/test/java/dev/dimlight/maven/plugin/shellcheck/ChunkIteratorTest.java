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

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Marco Nicolini
 */
public class ChunkIteratorTest {

    @Test
    public void testChunking() {
        for (int chunkSize = 1; chunkSize < 110; ++chunkSize) {
            for (int listSize = 0; listSize < 100; ++listSize) {
                assertCorrectChunking(listSize, chunkSize);
            }
        }
    }

    private void assertCorrectChunking(int listSize, int chunkSize) {
        final List<Integer> items = IntStream.range(0, listSize).boxed().collect(Collectors.toList());
        final Iterator<List<Integer>> iter = ChunkIterator.over(chunkSize, items);
        int sumOfChunkSizes = 0;
        while (iter.hasNext()) {
            final List<Integer> chunk = iter.next();
            Assert.assertTrue(chunk.size() <= chunkSize);
            sumOfChunkSizes += chunk.size();
        }
        Assert.assertEquals(items.size(), sumOfChunkSizes);
    }
}
