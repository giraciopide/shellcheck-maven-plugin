import java.nio.file.Files
import java.nio.file.Paths

// Every goofy file will triggers 2 shellcheck warnings.
def countWarnings(String pathFromPrjRoot) {
    return Files.readAllLines(Paths.get(basedir.getAbsolutePath(), pathFromPrjRoot))
            .stream()
            .filter({ line -> line.contains("^ SC") })
            .count()
}

// Parent module should check only 2 files hence get 4 warnings.
assert countWarnings("target/shellcheck-plugin/shellcheck.stdout") == 4

// child-module2 should check only 1 file  hence get 2 warnings.
assert countWarnings("child-module-two/target/shellcheck-plugin/shellcheck.stdout") == 2
assert countWarnings("child-module-three/target/shellcheck-plugin/shellcheck.stdout") == 2
