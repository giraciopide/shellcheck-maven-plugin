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
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Runs the shellcheck binary on the files specified with sourceDirs.
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY)
public class ShellCheckMojo extends AbstractMojo {

    /**
     * A list of directory or FileSets where to look for sh files to check.
     */
    @Parameter(required = false, readonly = true)
    private List<SourceDir> sourceDirs;

    /**
     * The expected extension to filter shell files (e.g. ".sh").
     */
    @Parameter(required = true, defaultValue = ".sh", readonly = true)
    private String shellFileExtension;

    /**
     * The way the plugin should attempt binary resolution
     */
    @Parameter(required = true, readonly = true, defaultValue = "download")
    private BinaryResolutionMethod binaryResolutionMethod;

    /**
     * The path of the external binary, used only if binaryResolutionMethod is set to "external"
     *
     * @see BinaryResolutionMethod
     */
    @Parameter(required = false, readonly = true)
    private File externalBinaryPath;

    /**
     * The URL at which the release archive containing shellcheck will be downloaded,
     * used only if binaryResolutionMethod is set to "download"
     *
     * @see BinaryResolutionMethod
     */
    @Parameter(required = false, readonly = true)
    private Map<String, URL> releaseArchiveUrls;

    /**
     * The command line options to use when invoking the shellcheck binary.
     * A map is used to avoid having to parse a command line from scratch (which is not as easy as splitting on
     * whitespace since whitespace might be quoted).
     * The inconvenience is rather small, since configuration is written and rarely changed.
     */
    @Parameter(required = false, readonly = true, defaultValue = "")
    private List<String> args;

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
                    Optional.ofNullable(releaseArchiveUrls).orElse(Collections.emptyMap()),
                    log);

            final Path binary = binaryResolver.resolve(binaryResolutionMethod);

            final Shellcheck.Result result = Shellcheck.run(binary,
                    args == null ? Collections.emptyList() : args,
                    pluginPaths.getPluginOutputDirectory(), scriptsToCheck);

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
     * By default we search in src/main/sh for all *.sh files.
     */
    private SourceDir defaultSourceDir() {
        final File srcMainSh = Paths.get(baseDir.getAbsolutePath(), "src", "main", "sh").toFile();
        final SourceDir sourceDir = new SourceDir();
        sourceDir.setDirectory(srcMainSh.getAbsolutePath());
        sourceDir.addInclude("**/*.sh");
        return sourceDir;
    }

    /**
     * Walks the source locations searching for shell files.
     *
     * @return the list of files to be checked by shellcheck.
     */
    // a false positive due to due to redundant null checks in try-with-resources synthetized finally
    private List<Path> searchFilesToBeChecked() throws MojoExecutionException {

        final Log log = getLog();
        final FileSetManager fileSetManager = new FileSetManager(log, true);

        final List<Path> filesToCheck = new ArrayList<>();

        final List<SourceDir> sourceDirs = Optional.ofNullable(this.sourceDirs)
                .orElse(Collections.singletonList(defaultSourceDir()));

        for (SourceDir sourceDir : sourceDirs) {
            final List<Path> includedFiles = Arrays.stream(fileSetManager.getIncludedFiles(sourceDir))
                    .map(includedFile -> Paths.get(sourceDir.getDirectory(), includedFile))
                    .collect(Collectors.toList());
            includedFiles.forEach(f -> log.debug("found included file: [" + f + "]"));
            filesToCheck.addAll(includedFiles);
        }

        return filesToCheck;
    }
}
