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
            throwArchNotSupported("No embedded shellcheck binaries for this architecture.");
        }

        // Release archives have a different structure, don't mess with that, just reflect it.
        // Ofc this is fragile, but it's also simple and as long as updating to a new shellcheck version
        // is a manual process, it's fine.
        if (this.equals(Architecture.Windows_x86)) {
            return String.format("/shellcheck-bin/%s/shellcheck-v%s.exe", this.name(), Shellcheck.VERSION);
        }

        return String.format("/shellcheck-bin/%s/shellcheck-v%s/shellcheck", this.name(), Shellcheck.VERSION);
    }

    public boolean isUnixLike() {
        return !this.equals(Windows_x86);
    }

    public String downloadUrl() {
        switch (this) {
            case Linux_x86_64:
                return String.format("https://github.com/koalaman/shellcheck/releases/download/v%s/shellcheck-v%s.linux.x86_64.tar.xz", Shellcheck.VERSION, Shellcheck.VERSION);
            case Linux_armv6hf:
                return String.format("https://github.com/koalaman/shellcheck/releases/download/v%s/shellcheck-v%s.linux.aarch64.tar.xz", Shellcheck.VERSION, Shellcheck.VERSION);
            case Linux_aarch64:
                return String.format("https://github.com/koalaman/shellcheck/releases/download/v$%s/shellcheck-v%s.linux.armv6hf.tar.xz", Shellcheck.VERSION, Shellcheck.VERSION);
            case macOS_x86_64:
                return String.format("https://github.com/koalaman/shellcheck/releases/download/v%s/shellcheck-v%s.darwin.x86_64.tar.xz", Shellcheck.VERSION, Shellcheck.VERSION);
            case Windows_x86:
                return String.format("https://github.com/koalaman/shellcheck/releases/download/v%s/shellcheck-v%s.zip", Shellcheck.VERSION, Shellcheck.VERSION);
            case unsupported:
            default:
                throw new UnsupportedOperationException(notSupportedMessage("No shellcheck binary for this architecture"));
        }
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

    public static String notSupportedMessage(String prefix) {
        return prefix + " os.name [" + System.getProperty("os.name") + "]" +
                " os.arch [" + System.getProperty("os.arch") + "]";
    }

    public static void throwArchNotSupported(String msg) {
        throw new UnsupportedOperationException(notSupportedMessage(msg));
    }
}
