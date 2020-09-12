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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
     * The way the plugin should attempt binary resolution
     */
    @Parameter(required = true, readonly = true, defaultValue = "download")
    private BinaryResolutionMethod binaryResolutionMethod;

    /**
     * The path
     */
    @Parameter(required = false, readonly = true)
    private File externalBinaryPath;

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

    //
    // non externally configurable stuff
    //

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Override
    public void execute() throws MojoExecutionException {

        final Log log = getLog();
        final PluginPaths pluginPaths = new PluginPaths(outputDirectory.toPath());

        try {

            final List<Path> scriptsToCheck = searchFilesToBeChecked();

            final BinaryResolver binaryResolver = new BinaryResolver(mavenProject, mavenSession, pluginManager,
                    outputDirectory.toPath(),
                    Optional.ofNullable(externalBinaryPath).map(File::toPath),
                    log);

            final Path binary = binaryResolver.resolve(binaryResolutionMethod);

            final Shellcheck.Result result = Shellcheck.run(binary, pluginPaths.getPluginOutputDirectory(), scriptsToCheck);

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
