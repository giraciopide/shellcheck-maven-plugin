package dev.dimlight.maven.plugin.shellcheck;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Things that prepare the shellcheck binary to be invoked, by downloading it or extracting it from the jar,
 * when embedded.
 */
public class BinaryResolver {

    private final Path mavenTargetDirectory;
    private final Log log;

    public BinaryResolver(Path mavenTargetDirectory, Log log) {
        this.mavenTargetDirectory = mavenTargetDirectory;
        this.log = log;
    }

    /**
     * Extracts the shellcheck binary choosing from the binaries embedded in the jar according to the detected arch.
     *
     * @return the path to the usable, architecture-dependent, shellcheck binary.
     * @throws IOException            if something goes bad while extracting and copying to the project build directory.
     * @throws MojoExecutionException if the extracted file cannot be read or executed.
     */
    public Path extractEmbeddedShellcheckBinary() throws IOException, MojoExecutionException {
        final Architecture arch = Architecture.detect();
        log.debug("Detected arch is [" + arch + "]");

        final String binaryTargetName = "shellcheck" + arch.executableSuffix();
        final Path binaryPath = Paths.get(mavenTargetDirectory.toFile().getAbsolutePath(), "shellcheck", binaryTargetName);

        final boolean created = binaryPath.toFile().mkdirs();
        log.debug("Path [" + binaryPath + "] was created? [" + created + "]");

        // copy from inside the jar to /target/shellcheck
        final String binResourcePath = arch.embeddedBinPath();
        log.debug("Will try to use binary [" + binResourcePath + "]");
        try (final InputStream resourceAsStream = getClass().getResourceAsStream(binResourcePath)) {
            if (resourceAsStream == null) {
                throw new MojoExecutionException("No embedded binary found for shellcheck");
            }
            Files.copy(resourceAsStream, binaryPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // make the extracted file executable
        arch.makeExecutable(binaryPath);

        final File binaryFile = binaryPath.toFile();
        // check that we copied it and we can execute it.
        if (!binaryFile.exists()) {
            throw new MojoExecutionException("Could not find extracted file [" + binaryFile + "]");
        }

        if (!binaryFile.canExecute()) {
            throw new MojoExecutionException("Extracted file [" + binaryFile + "] is not executable");
        }

        return binaryPath;
    }
}
