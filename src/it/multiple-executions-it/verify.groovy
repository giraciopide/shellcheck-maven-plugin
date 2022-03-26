import java.nio.file.Files
import java.nio.file.Paths

// Every goofy file will triggers 2 shellcheck warnings.
def countWarnings(String pathFromPrjRoot) {
    return Files.readAllLines(Paths.get(basedir.getAbsolutePath(), pathFromPrjRoot))
            .stream()
            .filter({ line -> line.contains("^ SC") })
            .count()
}

// for the default execution (if we are not splitting invocations) we retain the simple shellcheck.stdout
// naming.
assert countWarnings("target/shellcheck-plugin/shellcheck.default.0.stdout") == 14
assert countWarnings("target/shellcheck-plugin/shellcheck.check1.0.stdout") == 14
assert countWarnings("target/shellcheck-plugin/shellcheck.check2.0.stdout") == 14

