package dev.dimlight.maven.plugin.shellcheck;

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
