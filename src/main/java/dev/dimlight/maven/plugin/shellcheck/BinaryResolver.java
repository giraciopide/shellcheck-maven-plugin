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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

/**
 * Groups differents ways of getting hold of the correct shellcheck binary.
 */
public class BinaryResolver {

    private final Path mavenTargetDirectory;
    private final Log log;
    private final MavenProject mavenProject;
    private final MavenSession mavenSession;
    private final BuildPluginManager pluginManager;
    private final Optional<Path> externalBinaryPath;
    private final Architecture arch;
    private final PluginPaths pluginPaths;
    private final Map<String, URL> releaseArchiveUrls;

    /**
     * @param mavenProject         maven component for the delegated plugin download
     * @param mavenSession         maven component for the delegated plugin download
     * @param pluginManager        maven component for the delegated plugin download
     * @param mavenTargetDirectory the path to the current project target directory
     * @param externalBinaryPath   the path to the external binary
     * @param releaseArchiveUrl    the url where to find the wanted release of shellcheck
     * @param log                  a maven logger
     */
    public BinaryResolver(MavenProject mavenProject, MavenSession mavenSession, BuildPluginManager pluginManager,
                          Path mavenTargetDirectory,
                          Optional<Path> externalBinaryPath,
                          Map<String, URL> releaseArchiveUrl,
                          Log log) {
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.pluginManager = pluginManager;
        this.mavenTargetDirectory = mavenTargetDirectory;
        this.releaseArchiveUrls = releaseArchiveUrl;
        this.externalBinaryPath = externalBinaryPath;
        this.log = log;
        this.arch = Architecture.detect();
        log.info("os arch: [" + Architecture.osArchKey() + "]");
        this.pluginPaths = new PluginPaths(mavenTargetDirectory);
    }

    /**
     * Performs binary resolution.
     *
     * @param resolutionMethod the desiderd resolution method.
     * @return a executable shellcheck binary path
     * @throws MojoExecutionException if there are problems while resolving
     * @throws IOException            in case some io operation fails (e.g download or permission change)
     */
    public Path resolve(BinaryResolutionMethod resolutionMethod) throws MojoExecutionException, IOException {
        switch (resolutionMethod) {
            case external:
                return validateBinaryPath(externalBinaryPath, BinaryResolutionMethod.external);
            case download:
                return downloadShellcheckBinary();
            case embedded:
                return extractEmbeddedShellcheckBinary();
            default:
                throw new IllegalStateException("Invalid resolution method: " + resolutionMethod);
        }
    }

    private Path validateBinaryPath(Optional<Path> binaryPath, BinaryResolutionMethod binaryOrigin) throws MojoExecutionException {
        return binaryPath
                .map(Path::toFile)
                .filter(File::exists)
                .filter(File::canRead)
                .filter(file -> !arch.isUnixLike() || file.canExecute())
                .map(File::toPath)
                .orElseThrow(() -> new MojoExecutionException("The " + binaryOrigin.name() + " shellcheck binary has not been provided or cannot be found or is not readable/ executable"));
    }

    private Path validateBinaryPath(Path binaryPath, BinaryResolutionMethod binaryOrigin) throws MojoExecutionException {
        return validateBinaryPath(Optional.ofNullable(binaryPath), binaryOrigin);
    }

    /**
     * Downloads shellcheck for the current architecture and returns the path of the downloaded binary.
     * <p>
     * The actual download is delegated to a maven plugin executed via mojo-executor.
     * This is less clean than a proper implementation but it's also orders of magnitude simpler as it automatically
     * deals with: caching, different compression formats and maven proxy settings.
     *
     * @return the path to the downloaded binary
     * @throws MojoExecutionException if the delegated execution to the download maven plugin fails or if we can't find
     *                                the file after the download
     * @throws IOException            in case we fail to make the downloaded binary executable (unix only)
     */
    private Path downloadShellcheckBinary() throws MojoExecutionException, IOException {
        URL u = releaseArchiveUrls.get(Architecture.osArchKey());
        if (u == null) {
            log.warn("No shellcheck download url provided for current os.name-os.arch [" + Architecture.osArchKey() + "]");
            u = arch.downloadUrl();
        }
        final String url = u.toExternalForm();

        log.info("shellcheck release will be fetched at [" + url + "]");

        final Path downloadAndUnpackPath = pluginPaths.getPluginOutputDirectory();
        executeMojo(
                plugin(
                        groupId("com.googlecode.maven-download-plugin"),
                        artifactId("download-maven-plugin"),
                        version("1.6.0")
                ),
                goal("wget"),
                configuration(
                        element(name("uri"), url), // url is an alias!
                        element(name("unpack"), "true"),
                        element(name("outputDirectory"), downloadAndUnpackPath.toFile().getAbsolutePath())
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );

        final Path expectedDownloadedBinary = pluginPaths.guessUnpackedBinary(downloadAndUnpackPath, arch);
        arch.makeExecutable(expectedDownloadedBinary);

        return validateBinaryPath(expectedDownloadedBinary, BinaryResolutionMethod.download);
    }

    /**
     * Extracts the shellcheck binary choosing from the binaries embedded in the jar according to the detected arch.
     *
     * @return the path to the usable, architecture-dependent, shellcheck binary.
     * @throws IOException            if something goes bad while extracting and copying to the project build directory.
     * @throws MojoExecutionException if the extracted file cannot be read or executed.
     */
    // a false positive, javac in java 11+ due to redundant null checks in try-with-resources synthetized finally
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    private Path extractEmbeddedShellcheckBinary() throws IOException, MojoExecutionException {
        log.debug("Detected arch is [" + arch + "]");

        final String binaryTargetName = "shellcheck" + arch.idiomaticExecutableSuffix();
        final Path binaryPath = pluginPaths.getPathInPluginOutputDirectory(binaryTargetName);

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

        return validateBinaryPath(binaryPath, BinaryResolutionMethod.embedded);
    }
}
