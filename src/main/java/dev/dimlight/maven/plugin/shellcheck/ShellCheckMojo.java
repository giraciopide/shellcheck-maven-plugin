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
import org.apache.maven.plugin.MojoExecution;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Runs the shellcheck binary on the files specified with sourceDirs.
 */
@Mojo(name = "check", threadSafe = false, defaultPhase = LifecyclePhase.VERIFY)
public class ShellCheckMojo extends AbstractMojo {

    /**
     * Skips the plugin execution if set to true.
     */
    @Parameter(property = "skip.shellcheck", required = true, defaultValue = "false", readonly = true)
    private boolean skip;

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
     * The command line options to use when invoking the shellcheck binary (this should not include the actual
     * files to check).
     * <p>
     * A map is used to avoid having to parse a command line from scratch (which is not as easy as splitting on
     * whitespace since whitespace might be quoted).
     * The inconvenience is rather small, since configuration is written and rarely changed.
     */
    @Parameter(required = false, readonly = true, defaultValue = "")
    private List<String> args;

    /**
     * Name of the file (that will be placed in the plugin output directory) where the shellcheck stdout will be
     * captured.
     * It can be a simple filename or, if multiple execution/invocations of the plugin are being configured,
     * the placeholders "@executionId@" and "@runNumber@" can be used.
     * Defaults to "shellcheck.@executionId@.@runNumber@.stdout".
     */
    @Parameter(required = true, readonly = true, defaultValue = "shellcheck.@executionId@.@runNumber@.stdout")
    private String capturedStdoutFileName;

    /**
     * Name of the file (that will be placed in the plugin output directory) where the shellcheck stderr will be
     * captured.
     * It can be a simple filename or, if multiple execution/invocations of the plugin are being configured,
     * the placeholders "@executionId@" and "@runNumber@" can be used.
     * Defaults to "shellcheck.@executionId@.@runNumber@.stderr".
     */
    @Parameter(required = true, readonly = true, defaultValue = "shellcheck.@executionId@.@runNumber@.stderr")
    private String capturedStderrFileName;

    /**
     * Perform multiple invocations of shellcheck, each invocation checking filesPerInvocation file at the time.
     * Defaults to false, which means that we will perform a single shellcheck invocation passing all files
     * to be checked. Enable this if you are encountering limits on max args reached for your underlying OS.
     */
    @Parameter(required = false, readonly = true, defaultValue = "false")
    private boolean splitInvocations = false;

    /**
     * The max number of files to pass to a single shellcheck invocation when splitInvocations is set to true.
     * Defaults to Short.MAX_VALUE (32767).
     */
    @Parameter(required = false, readonly = true)
    private int filesPerInvocation = Short.MAX_VALUE;

    /**
     * If true, the build will fail if a shellcheck invocation has a non-zero return value (meaning that it
     * reported some errors)
     */
    @Parameter(required = true, defaultValue = "false")
    private boolean failBuildIfWarnings;

    //
    // non externally configurable stuff
    //

    @Parameter(required = true, defaultValue = "${project.build.directory}", readonly = true)
    private File outputDirectory;

    @Parameter(required = true, defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;

    @Parameter(required = true, defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    @Parameter(required = true, defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    @Component
    private MojoExecution execution; // used to get the execution id

    @Component
    private BuildPluginManager pluginManager;

    @Override
    public void execute() throws MojoExecutionException {
        final Log log = getLog();
        if (skip) {
            log.info("Skipping plugin execution");
            return;
        }

        log.debug("Execution id is [" + execution.getExecutionId() + "]");
        final PluginPaths pluginPaths = new PluginPaths(outputDirectory.toPath());

        try {

            final BinaryResolver binaryResolver = new BinaryResolver(mavenProject, mavenSession, pluginManager,
                outputDirectory.toPath(),
                Optional.ofNullable(externalBinaryPath).map(File::toPath),
                Optional.ofNullable(releaseArchiveUrls).orElseGet(Collections::emptyMap),
                log);

            final Path binary = binaryResolver.resolve(binaryResolutionMethod);

            // perform the runs in chunks or a single chunk with everything in it, depending on the splitInvocationConfiguration
            final List<Shellcheck.Result> runs = new ArrayList<>();
            final List<Path> allFilesCheck = searchFilesToBeChecked();
            final Iterator<List<Path>> runIter = splitInvocations ? ChunkIterator.over(filesPerInvocation(), allFilesCheck) : Collections.singletonList(allFilesCheck).iterator();
            int runNum = 0;
            while (runIter.hasNext()) {
                final List<Path> scriptsToCheck = runIter.next();
                final String runId = execution.getExecutionId() + "." + runNum;
                final long startTime = System.currentTimeMillis();

                log.debug("Running shellcheck [" + runId + "] on [" + scriptsToCheck.size() + "] files");
                final Shellcheck.Result result = Shellcheck.run(
                    runId,
                    binary,
                    Optional.ofNullable(args).orElseGet(Collections::emptyList),
                    scriptsToCheck,
                    pluginPaths.getPathInPluginOutputDirectory(renderTemplatedFilename(capturedStdoutFileName, execution, runNum)),
                    pluginPaths.getPathInPluginOutputDirectory(renderTemplatedFilename(capturedStderrFileName, execution, runNum))
                );
                final long elapsed = System.currentTimeMillis() - startTime;
                log.debug("Shellcheck run [" + result.runId + "] on [" + scriptsToCheck.size() + "] files took [" + elapsed + "] millis");

                runs.add(result);
                ++runNum;
            }

            // inspect the failures and fail the build if configured to do so.,
            final List<Shellcheck.Result> failures = runs.stream().filter(Shellcheck.Result::isNotOk).collect(Collectors.toList());
            if (!failures.isEmpty()) {
                for (Shellcheck.Result failedRun : failures) {
                    log.warn("------ Shellcheck run [" + failedRun.runId + "] returned [" + failedRun.exitCode + "] stdout will follow -----------------------------------------");
                    Files.readAllLines(failedRun.stdout).forEach(log::warn);
                    log.warn("------ Shellcheck run [" + failedRun.runId + "] returned [" + failedRun.exitCode + "] stderr will follow -----------------------------------------");
                    Files.readAllLines(failedRun.stderr).forEach(log::error);
                }

                if (failBuildIfWarnings) {
                    throw new MojoExecutionException("There are shellcheck problems: [" + failures.size() + "]/[" + runs.size() + "] shellcheck runs had non-zero exit codes");
                }
            }

        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private int filesPerInvocation() {
        return filesPerInvocation <= 0 ? Integer.MAX_VALUE : filesPerInvocation;
    }

    /**
     * By default, we search in src/main/sh for all *.sh files.
     *
     * @return the default source dir configuration.
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
    private List<Path> searchFilesToBeChecked() throws MojoExecutionException {

        final Log log = getLog();
        final FileSetManager fileSetManager = new FileSetManager(log, true);

        final List<Path> filesToCheck = new ArrayList<>();

        final List<SourceDir> sourceDirs = Optional.ofNullable(this.sourceDirs)
            .orElse(Collections.singletonList(defaultSourceDir()));

        for (SourceDir sourceDir : sourceDirs) {
            final List<Path> includedFiles = Arrays.stream(fileSetManager.getIncludedFiles(sourceDir))
                .map(includedFile -> Paths.get(sourceDir.getDirectory(), includedFile))
                .peek(includedPath -> log.debug("Shellcheck will check file: [" + includedPath.toFile().getAbsolutePath() + "]"))
                .collect(Collectors.toList());
            filesToCheck.addAll(includedFiles);
        }

        return filesToCheck;
    }

    private String renderTemplatedFilename(String fileName, MojoExecution execution, int runNumber) {
        // "shellcheck.@executionId@.@runNumber@.stdout")
        return fileName
            .replace("@executionId@", execution.getExecutionId())
            .replace("@runNumber@", Integer.toString(runNumber));
    }
}
