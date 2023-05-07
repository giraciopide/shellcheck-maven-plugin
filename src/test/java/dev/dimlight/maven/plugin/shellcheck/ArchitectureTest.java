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

import java.io.File;

public class ArchitectureTest {

    @Test
    public void detect() {
        // unlikely that the plugin will be built on a unsupported shellcheck arch
        Assert.assertNotEquals(Architecture.unsupported, Architecture.detect());
    }

    @Test
    public void checkCorrectnessOfHardcodedEmbeddedBinaryPaths() {
        final String outputDir = "target/classes";
        for (Architecture arch : Architecture.values()) {
            if (arch.equals(Architecture.unsupported)) {
                continue;
            }

            final File binary = new File(outputDir + arch.embeddedBinPath());
            Assert.assertTrue("File [" + binary + "] does not exists", binary.exists());
            Assert.assertTrue("File [" + binary + "] is not a file", binary.isFile());
        }
    }
}
