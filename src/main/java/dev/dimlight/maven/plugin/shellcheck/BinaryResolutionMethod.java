package dev.dimlight.maven.plugin.shellcheck;

/**
 * All the ways the plugin can resolve the shellcheck binary.
 */
public enum BinaryResolutionMethod {

    /**
     * The path to an externally provided shellcheck binary must be provided.
     */
    external,

    /**
     * The binary will be downloaded at plugin execution time.
     */
    download,

    /**
     * Use the embedded binary.
     */
    embedded
}
