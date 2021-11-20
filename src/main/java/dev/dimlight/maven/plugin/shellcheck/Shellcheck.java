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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A thin java wrapper over the shellcheck binary for execution.
 */
public class Shellcheck {

    /**
     * The version of shellcheck that is embedded in the plugin jar.
     */
    public static final String VERSION = "0.8.0";

    private Shellcheck() {
    }

    /**
     * The results of a shellcheck run.
     */
    public static class Result {

        /**
         * The os exit code for the shellcheck invocation.
         */
        public final int exitCode;

        /**
         * Path where the captured stdout has been redirected.
         */
        public final Path stdout;

        /**
         * Path where the captured stderr has been redirected.
         */
        public final Path stderr;

        /**
         * @param exitCode the exit code of the shellcheck invocation.
         * @param stdout   the path where stdout has been redirected.
         * @param stderr   the path where stderr has been redirected.
         */
        public Result(int exitCode, Path stdout, Path stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        /**
         * @return true if the exit code is non-zero.
         */
        public boolean isNotOk() {
            return exitCode != 0;
        }
    }

    /**
     * Runs the provided shellcheck binary capturing its output and return code.
     *
     * @param shellcheckBinary the binary for shellcheck
     * @param outdir           where the output files will be stored
     * @param args             the command line args to be passed to the shellcheck binary
     * @param scriptsToCheck   the list of arguments to shellcheck
     * @return a result object containing exit code and captured outputs (on file)
     * @throws IOException          if something goes bad doing io things (writing files etc...)
     * @throws InterruptedException if the thread gets interrupted while waiting for shellcheck to finish
     */
    public static Result run(Path shellcheckBinary,
                             List<String> args,
                             Path outdir,
                             List<Path> scriptsToCheck) throws IOException, InterruptedException {

        final String pluginOutDirAbsPath = outdir.toFile().getAbsolutePath();

        // build the cmd line args "shellcheck file1.sh file2.sh ..."
        final List<String> commandAndArgs = new ArrayList<>();
        commandAndArgs.add(shellcheckBinary.toFile().getAbsolutePath()); // the shellcheck binary
        commandAndArgs.addAll(args); // the args
        commandAndArgs.addAll(scriptsToCheck.stream()
                .map(path -> path.toFile().getAbsolutePath())
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
        return new Result(exitCode, stdout, stderr);
    }
}
