package dev.dimlight.maven.plugin.shellcheck;

/*-
 * #%L
 * shellcheck-maven-plugin
 * %%
 * Copyright (C) 2020 Marco Nicolini
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for paths related to the plugin.
 */
public class PluginPaths {

    private final Path pluginOutputDirectory;

    /**
     * @param mavenTargetDirectory the target directory for the current maven project (project.buildDirectory in maven
     *                             parlance).
     */
    public PluginPaths(Path mavenTargetDirectory) {
        this.pluginOutputDirectory = Paths.get(mavenTargetDirectory.toFile().getAbsolutePath(), "shellcheck-plugin");
    }

    /**
     * @return a subdir of the current project target that is dedicated to output of this process.
     */
    public Path getPluginOutputDirectory() {
        return pluginOutputDirectory;
    }

    /**
     * @param pathFragments the path fragments within the plugin output directory.
     * @return a path in the plugin output directory.
     */
    public Path getPathInPluginOutputDirectory(String... pathFragments) {
        return Paths.get(pluginOutputDirectory.toFile().getAbsolutePath(), pathFragments);
    }

    /**
     * Walks the files in fromPath to find what is likely the shellcheck binary.
     * This is done cause the windows released archive has a different structure (directory and binary-name wise).
     * <p>
     * No actual check inspecting the binary is done, the likely binary is "found" only by name.
     *
     * @param fromPath the root path from which to start the search.
     * @param arch     the current detected architecture
     * @return the path to the binary, if found
     * @throws FileNotFoundException if the binary is not found
     * @throws IOException           if the there is an IO problem while walking the filesystem
     */
    // a false positive due to due to redundant null checks in try-with-resources synthetized finally
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public Path guessUnpackedBinary(Path fromPath, Architecture arch) throws IOException {

        try (final Stream<Path> paths = Files.walk(fromPath)) {
            final List<File> canditates = paths
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .filter(file -> file.getName().equals("shellcheck" + arch.idiomaticExecutableSuffix()))
                    .collect(Collectors.toList());

            if (canditates.size() > 1) {
                throw new FileNotFoundException("There are multiple binaries candidate in the unpacked shellcheck release: [" +
                        canditates + "] at [" + fromPath + "]");
            }

            if (canditates.isEmpty()) {
                throw new FileNotFoundException("No binary candidates found in the unpacked shellcheck release at [" +
                        fromPath + "]");
            }

            return canditates.iterator().next().toPath();
        }
    }
}
