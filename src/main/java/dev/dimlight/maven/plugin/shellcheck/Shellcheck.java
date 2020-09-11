package dev.dimlight.maven.plugin.shellcheck;

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

    private Shellcheck() {
    }

    /**
     * The results of a shellcheck run.
     */
    public static class Result {
        public final int exitCode;
        public final Path stdout;
        public final Path stderr;

        public Result(int exitCode, Path stdout, Path stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public boolean isNotOk() {
            return exitCode != 0;
        }
    }

    /**
     * Runs the provided shellcheck binary capturing its output and return code.
     *
     * @param shellcheckBinary the binary for shellcheck
     * @param outdir           where the output files will be stored
     * @param scriptsToCheck   the list of arguments to shellcheck
     * @return a result object containing exit code and captured outputs (on file)
     * @throws IOException          if something goes bad doing io things (writing files etc...)
     * @throws InterruptedException if the thread gets interrupted while waiting for shellcheck to finish
     */
    public static Result run(Path shellcheckBinary, Path outdir, List<Path> scriptsToCheck) throws IOException, InterruptedException {
        final String pluginOutDirAbsPath = outdir.toFile().getAbsolutePath();

        // build the cmd line args "shellcheck file1.sh file2.sh ..."
        final List<String> commandAndArgs = new ArrayList<>(scriptsToCheck.size() + 1);
        commandAndArgs.add(shellcheckBinary.toFile().getAbsolutePath()); // the shellcheck binary
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
