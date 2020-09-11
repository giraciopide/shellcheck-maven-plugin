package dev.dimlight.maven.plugin.shellcheck;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Light-hearted os/arch detection, just enough to pick the shellcheck binary.
 * Some arch-dependent paths and logic is also here.
 */
public enum Architecture {

    Linux_x86_64,       // amd64
    Linux_armv6hf,      // raspberry pi
    Linux_aarch64,      // ARM64
    macOS_x86_64,       // macos
    Windows_x86,        // windows 32 bit
    unsupported;

    private static final String SHELLCHECK_VERSION = "0.7.1";

    public static Architecture detect() {
        final String osName = System.getProperty("os.name").toLowerCase();
        final String osArch = System.getProperty("os.arch");

        // windows
        if (osName.contains("win")) {
            if (osArch.contains("amd64") || osArch.contains("x86")) {
                return Windows_x86;
            }

            // mac
        } else if (osName.contains("mac")) {
            return macOS_x86_64;

            // linux
        } else if (osName.contains("nux")) {
            switch (osArch) {
                case "amd64":
                    return Linux_x86_64;
                case "aarch64":
                    return Linux_aarch64;
                case "arm":
                    return Linux_armv6hf;
                default:
                    // fall through
            }
        }

        return unsupported;
    }

    /**
     * @return the path (within our jar) of the shellcheck binary, according to the architecture.
     */
    public String binPath() {
        if (this.equals(unsupported)) {
            throwArchNotSupported("No embedded shellcheck binaries for this architecture.");
        }

        // Release archives have a different structure, don't mess with that, just reflect it.
        // Ofc this is fragile, but it's also simple and as long as updating to a new shellcheck version
        // is a manual process, it's fine.
        if (this.equals(Architecture.Windows_x86)) {
            return String.format("/shellcheck-bin/%s/shellcheck-v%s.exe", this.name(), SHELLCHECK_VERSION);
        }

        return String.format("/shellcheck-bin/%s/shellcheck-v%s/shellcheck", this.name(), SHELLCHECK_VERSION);
    }

    public void makeExecutable(Path path) throws IOException {
        switch (this) {
            case unsupported:
                throwArchNotSupported("No support for this architecture.");
            case Linux_x86_64:
            case Linux_armv6hf:
            case Linux_aarch64:
            case macOS_x86_64:
                // make the extracted file executable
                final String perm755 = "rwxr-xr-x";
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(perm755));
            case Windows_x86:
                // windows doesn't support posix permissions
            default:
                break;
        }
    }

    public String executableSuffix() {
        if (this.equals(Windows_x86)) {
            return ".exe";
        }
        return "";
    }

    private static void throwArchNotSupported(String msg) {
        throw new UnsupportedOperationException(msg +
                " os.name [" + System.getProperty("os.name") + "]" +
                " os.arch [" + System.getProperty("os.arch") + "]");
    }
}
