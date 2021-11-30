import java.nio.file.Files
import java.nio.file.Paths

assert Files.readAllLines(Paths.get(basedir.getAbsolutePath(), "target/shellcheck-plugin/shellcheck.0.stdout"))
        .stream()
        .filter({ line -> line.contains("^ SC") })
        .count() == 2

