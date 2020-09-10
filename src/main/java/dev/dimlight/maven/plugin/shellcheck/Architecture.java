package dev.dimlight.maven.plugin.shellcheck;

/**
 * Light-hearted os/arch detection, just enough to pick the shellcheck binary.
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
            throw new UnsupportedOperationException("No embedded shellcheck binaries for os.name ["
                    + System.getProperty("os.name") + "] os.arch [" + System.getProperty("os.arch") + "]");
        }

        // Release archives have a different structure, don't mess with that, just reflect it.
        // Ofc this is fragile, but it's also simple and as long as updating to a new shellcheck version
        // is a manual process, it's fine.
        if (this.equals(Architecture.Windows_x86)) {
            return String.format("/shellcheck-bin/%s/shellcheck-v%s.exe", this.name(), SHELLCHECK_VERSION);
        }

        return String.format("/shellcheck-bin/%s/shellcheck-v%s/shellcheck", this.name(), SHELLCHECK_VERSION);
    }

    public String executableSuffix() {
        if (this.equals(Windows_x86)) {
            return ".exe";
        }
        return "";
    }
}
