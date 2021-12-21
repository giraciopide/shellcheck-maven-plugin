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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Light-hearted os/arch detection, just enough to pick up the shellcheck binary.
 * Some arch-dependent logic is also here.
 */
public enum Architecture {

    /**
     * amd64.
     */
    Linux_x86_64,

    /**
     * raspberry pi (arm 32).
     */
    Linux_armv6hf,

    /**
     * ARM64.
     */
    Linux_aarch64,

    /**
     * macosx on intel.
     */
    macOS_x86_64,

    /**
     * win32.
     */
    Windows_x86,

    /**
     * none of the above.
     */
    unsupported;

    /**
     * Returns the currently os/arch key identifier.
     * This identifier is printed as part of the plugin execution and can be used to provide different download urls
     * for different architectures, allowing multi-arch builds.
     *
     * @return the string identifying the architecture for download purposes.
     */
    public static String osArchKey() {
        return (System.getProperty("os.name") + "-" + System.getProperty("os.arch")).replace(" ", "_");
    }

    /**
     * Detects the current architecture on which the plugin is being run.
     *
     * @return the detected architecture.
     */
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
    public String embeddedBinPath() {
        if (this.equals(unsupported)) {
            throw new UnsupportedOperationException(notSupportedMessage("No embedded shellcheck binaries for this architecture."));
        }

        // Shellcheck release archives have a different structure depending on the architecture.
        // We don't mess with that, just reflect it in the hard-coded paths below.
        // Ofc this is fragile, but it's also simple and as long as updating to a new shellcheck version
        // is a manual process, it's fine: the correctness of these hard-coded paths is checked at build time in
        // ArchitectureTest.checkCorrectnessOfHardcodedEmbeddedBinaryPaths
        if (this.equals(Architecture.Windows_x86)) {
            return String.format("/shellcheck-bin/%s/shellcheck.exe", this.name());
        }

        return String.format("/shellcheck-bin/%s/shellcheck-v%s/shellcheck", this.name(), Shellcheck.VERSION);
    }

    /**
     * @return true if the system appears to be of the nix family (macosx included).
     */
    public boolean isUnixLike() {
        return !this.equals(Windows_x86);
    }

    /**
     * @return the default download url for the detected architecture.
     */
    public URL downloadUrl() {
        final String url;
        switch (this) {
            case Linux_x86_64:
                url = String.format("https://github.com/koalaman/shellcheck/releases/download/v%s/shellcheck-v%s.linux.x86_64.tar.xz", Shellcheck.VERSION, Shellcheck.VERSION);
                break;
            case Linux_armv6hf:
                url = String.format("https://github.com/koalaman/shellcheck/releases/download/v%s/shellcheck-v%s.linux.armv6hf.tar.xz", Shellcheck.VERSION, Shellcheck.VERSION);
                break;
            case Linux_aarch64:
                url = String.format("https://github.com/koalaman/shellcheck/releases/download/v%s/shellcheck-v%s.linux.aarch64.tar.xz", Shellcheck.VERSION, Shellcheck.VERSION);
                break;
            case macOS_x86_64:
                url = String.format("https://github.com/koalaman/shellcheck/releases/download/v%s/shellcheck-v%s.darwin.x86_64.tar.xz", Shellcheck.VERSION, Shellcheck.VERSION);
                break;
            case Windows_x86:
                url = String.format("https://github.com/koalaman/shellcheck/releases/download/v%s/shellcheck-v%s.zip", Shellcheck.VERSION, Shellcheck.VERSION);
                break;
            case unsupported:
            default:
                throw new UnsupportedOperationException(notSupportedMessage("No shellcheck binary for this architecture"));
        }
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Makes the given path executable (for unices, for windows does nothing).
     *
     * @param path the path to make executable.
     * @throws IOException if something goes wrong.
     */
    public void makeExecutable(Path path) throws IOException {
        switch (this) {
            case unsupported:
                throw new UnsupportedOperationException(notSupportedMessage("No support for this architecture."));

            case Linux_x86_64:
            case Linux_armv6hf:
            case Linux_aarch64:
            case macOS_x86_64:
                // make the extracted file executable
                final String perm755 = "rwxr-xr-x";
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(perm755));
                break;

            case Windows_x86:
                // windows doesn't support posix permissions
            default:
                break;
        }
    }

    /**
     * @return the idiomatic suffix for executables dependending on os/arch, i.e. "" for nixes and ".exe" for win.
     */
    public String idiomaticExecutableSuffix() {
        if (this.equals(Windows_x86)) {
            return ".exe";
        }
        return "";
    }

    private static String notSupportedMessage(String prefix) {
        return prefix + " os.name [" + System.getProperty("os.name") + "]" +
                " os.arch [" + System.getProperty("os.arch") + "]";
    }
}
