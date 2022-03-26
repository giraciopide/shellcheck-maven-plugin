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
import java.util.ArrayList;
import java.util.Collections;
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
         * Run id.
         * A label to discriminate different shellcheck runs.
         */
        public final String runId;

        /**
         * The actual command line that was ran to execute shellcheck, as a list.
         * The content of the list is made up of:
         * <ol>
         *     <li>/the/path/to/shellcheck</li>
         *     <li>the options</li>
         *     <li>the actual file arguments</li>
         * </ol>
         */
        public final List<String> cmdLine;

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
         * @param runId    the id of the run.
         * @param cmdLine  the cmd line of the run.
         * @param exitCode the exit code of the shellcheck invocation.
         * @param stdout   the path where stdout has been redirected.
         * @param stderr   the path where stderr has been redirected.
         */
        public Result(String runId, List<String> cmdLine, int exitCode, Path stdout, Path stderr) {
            this.runId = runId;
            this.cmdLine = cmdLine;
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
     * @param runId            a string label used to mark the output stdout/stderr files for the run.
     * @param shellcheckBinary the binary for shellcheck
     * @param args             the command line args to be passed to the shellcheck binary
     * @param scriptsToCheck   the list of arguments to shellcheck
     * @param capturedStdout   the path where the captured stdout should be redirected
     * @param capturedStderr   the path where the captured stderr should be redirected
     * @return a result object containing exit code and captured outputs (on file)
     * @throws IOException          if something goes bad doing io things (writing files etc...)
     * @throws InterruptedException if the thread gets interrupted while waiting for shellcheck to finish
     */
    public static Result run(String runId,
                             Path shellcheckBinary,
                             List<String> args,
                             List<Path> scriptsToCheck,
                             Path capturedStdout,
                             Path capturedStderr) throws IOException, InterruptedException {

        // build the cmd line args "shellcheck file1.sh file2.sh ..."
        final List<String> commandAndArgs = new ArrayList<>();
        commandAndArgs.add(shellcheckBinary.toFile().getAbsolutePath()); // the shellcheck binary
        commandAndArgs.addAll(args); // the args
        commandAndArgs.addAll(scriptsToCheck.stream()
            .map(path -> path.toFile().getAbsolutePath())
            .collect(Collectors.toList()));

        // finally launch shellcheck
        final Process process = new ProcessBuilder()
            .redirectOutput(capturedStdout.toFile())
            .redirectError(capturedStderr.toFile())
            .command(commandAndArgs)
            .start();

        final int exitCode = process.waitFor();
        return new Result(runId, Collections.unmodifiableList(commandAndArgs), exitCode, capturedStdout, capturedStderr);
    }
}
