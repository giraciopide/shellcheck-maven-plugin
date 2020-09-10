package dev.dimlight.maven.plugin.shellcheck;

import org.junit.Assert;
import org.junit.Test;

public class ArchitectureTest {

    @Test
    public void detect() {
        // unlikely that the plugin will be built on a unsupported shellcheck arch
        Assert.assertNotEquals(Architecture.unsupported, Architecture.detect());
    }
}