package dev.dimlight.maven.plugin.shellcheck;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    
    @Override
    public void execute() throws MojoExecutionException {

        final Log log = getLog();

        try {
            final List<Path> scriptsToCheck = searchFilesToBeChecked();

            final BinaryResolver binaryResolver = new BinaryResolver(outputDirectory.toPath(), log);
            final Path binary = binaryResolver.extractEmbeddedShellcheckBinary();

            final Shellcheck.Result result = Shellcheck.run(binary, binary.getParent(), scriptsToCheck);

            // print stdout and stderr to maven log.
            Files.readAllLines(result.stdout).forEach(log::warn);
            Files.readAllLines(result.stderr).forEach(log::error);

            if (result.isNotOk() && failBuildIfWarnings) {
                throw new MojoExecutionException("There are shellcheck problems: shellcheck exit code [" + result.exitCode + "]");
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
    private List<Path> searchFilesToBeChecked() throws IOException {
        final List<Path> foundFiles = new ArrayList<>();
        for (File sourceLocation : sourceLocations()) {
            if (sourceLocation.isFile()) {
                foundFiles.add(sourceLocation.toPath());
            } else if (sourceLocation.isDirectory()) {
                try (final Stream<Path> paths = Files.walk(Paths.get(sourceLocation.getAbsolutePath()))) {
                    foundFiles.addAll(paths
                            .filter(path -> path.toFile().isFile())
                            .filter(path -> path.toFile().getName().endsWith(shellFileExtension))
                            .collect(Collectors.toList()));
                }
            } else {
                getLog().warn("Skipped shellcheck source location [" + sourceLocation + "]");
            }
        }
        return foundFiles;
    }
}
