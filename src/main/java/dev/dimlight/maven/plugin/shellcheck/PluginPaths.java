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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utilities for paths related to the plugin.
 */
public class PluginPaths {

    private final Path mavenTargetDirectory;
    private final Path pluginOutputDirectory;

    public PluginPaths(Path mavenTargetDirectory) {
        this.mavenTargetDirectory = mavenTargetDirectory;
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

    public Path downloadedAndUnpackedBinPath(Architecture arch) {
        if (arch.equals(Architecture.unsupported)) {
            Architecture.throwArchNotSupported("No embedded shellcheck binaries for this architecture.");
        }

        // Release archives have a different structure, don't mess with that, just reflect it.
        // Ofc this is fragile, but it's also simple and as long as updating to a new shellcheck version
        // is a manual process, it's fine.
        if (arch.equals(Architecture.Windows_x86)) {
            getPathInPluginOutputDirectory(String.format("shellcheck-v%s.exe", Shellcheck.VERSION));
        }

        return getPathInPluginOutputDirectory(String.format("shellcheck-v%s/shellcheck", Shellcheck.VERSION));
    }
}
