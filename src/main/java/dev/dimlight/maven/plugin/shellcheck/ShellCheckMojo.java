package dev.dimlight.maven.plugin.shellcheck;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Runs the shellcheck binary on the all shell files found in the sourceLocations.
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY)
public class ShellCheckMojo extends AbstractMojo {

    /**
     * A list of directory or files where to look for shell files to be checked.
     */
    @Parameter(required = false, readonly = true)
    private List<File> sourceLocations;

    /**
     * The expected extension to filter shell files (e.g. ".sh").
     */
    @Parameter(required = true, defaultValue = ".sh", readonly = true)
    private String shellFileExtension;

    @Parameter(required = true, defaultValue = "false")
    private boolean failBuildIfWarnings;

    @Parameter(required = true, defaultValue = "${project.build.directory}")
    private File outputDirectory;

    @Parameter(required = true, defaultValue = "${project.basedir}")
    private File baseDir;

    /**
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {

        final Log log = getLog();

        try {
            final List<File> scriptsToCheck = searchFilesToBeChecked();
            final Path binary = extractShellcheckBinary();
            final Path pluginOutDir = binary.getParent();
            final String pluginOutDirAbsPath = pluginOutDir.toFile().getAbsolutePath();

            // build the cmd line args "shellcheck file1.sh file2.sh ..."
            final List<String> commandAndArgs = new ArrayList<>(scriptsToCheck.size() + 1);
            commandAndArgs.add(binary.toFile().getAbsolutePath()); // the shellcheck binary
            commandAndArgs.addAll(scriptsToCheck.stream() // all the files to be checked
                    .map(File::getAbsolutePath)
                    .peek(scriptPath -> log.debug("will check [" + scriptPath + "]"))
                    .collect(Collectors.toList()));

            final Path stdout = Paths.get(pluginOutDirAbsPath, "shellcheck.stdout");
            final Path stderr = Paths.get(pluginOutDirAbsPath, "shellcheck.stderr");

            // finally launch shellcheck
            final Process process = new ProcessBuilder()
                    .redirectOutput(stdout.toFile())
                    .redirectError(stderr.toFile())
                    .command(commandAndArgs)
                    .start();

            final int exitCode = process.waitFor();

            // print stdout and stderr to maven log.
            Files.readAllLines(stdout).forEach(log::info);
            Files.readAllLines(stderr).forEach(log::error);

            if (exitCode != 0 && failBuildIfWarnings) {
                throw new MojoExecutionException("There are shellcheck problems: shellcheck exit code [" + exitCode + "]");
            }

        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * @return the source locations to be searched for shell files.
     */
    private List<File> sourceLocations() {
        final File srcMainSh = Paths.get(baseDir.getAbsolutePath(), "src", "main", "sh").toFile();
        return sourceLocations == null ? Collections.singletonList(srcMainSh) : sourceLocations;
    }

    /**
     * Walks the source locations searching for shell files.
     *
     * @return the list of files to be checked by shellcheck.
     * @throws IOException if something goes bad while walking the filesystem.
     */
    private List<File> searchFilesToBeChecked() throws IOException {
        final List<File> foundFiles = new ArrayList<>();
        for (File sourceLocation : sourceLocations()) {
            if (sourceLocation.isFile()) {
                foundFiles.add(sourceLocation);
            } else if (sourceLocation.isDirectory()) {
                try (final Stream<Path> paths = Files.walk(Paths.get(sourceLocation.getAbsolutePath()))) {
                    foundFiles.addAll(paths
                            .map(Path::toFile)
                            .filter(f -> f.getName().endsWith(shellFileExtension))
                            .collect(Collectors.toList()));
                }
            } else {
                getLog().warn("Skipped shellcheck source location [" + sourceLocation + "]");
            }
        }
        return foundFiles;
    }

    /**
     * Extracts the shellcheck binary choosing from the binaries embedded in the jar according to the detected arch.
     *
     * @return the path to the usable, architecture-dependent, shellcheck binary.
     * @throws IOException            if something goes bad while extracting and copying to the project build directory.
     * @throws MojoExecutionException if the extracted file cannot be read or executed.
     */
    private Path extractShellcheckBinary() throws IOException, MojoExecutionException {
        final Architecture arch = Architecture.detect();
        final Log log = getLog();
        log.debug("Detected arch is [" + arch + "]");

        final String binaryTargetName = "shellcheck" + arch.executableSuffix();
        final Path binaryPath = Paths.get(outputDirectory.getAbsolutePath(), "shellcheck", binaryTargetName);

        final boolean created = binaryPath.toFile().mkdirs();
        log.debug("Path [" + binaryPath + "] was created? [" + created + "]");

        // copy from inside the jar to /target/shellcheck
        final String binResourcePath = arch.binPath();
        log.debug("Will try to use binary [" + binResourcePath + "]");
        try (final InputStream resourceAsStream = getClass().getResourceAsStream(binResourcePath)) {
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
