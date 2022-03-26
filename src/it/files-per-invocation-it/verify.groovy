import java.nio.file.Files
import java.nio.file.Paths

// Every goofy file will triggers 2 shellcheck warnings.
def countWarnings(String pathFromPrjRoot) {
    return Files.readAllLines(Paths.get(basedir.getAbsolutePath(), pathFromPrjRoot))
            .stream()
            .filter({ line -> line.contains("^ SC") })
            .count()
}

assert countWarnings("target/shellcheck-plugin/shellcheck.default.0.stdout") == 4
assert countWarnings("target/shellcheck-plugin/shellcheck.default.1.stdout") == 4
assert countWarnings("target/shellcheck-plugin/shellcheck.default.2.stdout") == 4
assert countWarnings("target/shellcheck-plugin/shellcheck.default.3.stdout") == 2
