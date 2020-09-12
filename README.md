# shellcheck-maven-plugin
A maven plugin to execute shellcheck in a maven build

## How it works
The plugin has a single `check` goal that searches for shell files in standard (and configurable) locations and invokes
shellcheck on them.

Since shellcheck is a non-java application the plugin provides automatic ways to get hold of the shellcheck binary.
This is controlled by the `binaryResolutionMethod` plugin configuration property:
* `embedded` the plugin will use a shellcheck binary embedded in the plugin jar.
    * useful if you're behind proxy and you want zero-hassles in configuring things
    * you're bound to the embedded shellcheck version (currently 0.7.1)
* `download` the binary will be downloaded at plugin execution time.
    * lets you target a specific shellcheck version (not yet implemented)
* `external` the path to a shellcheck binary should be provided.
    * you have all control
    * requiring external tools makes the build less self-contained

For embedded and download resolution at plugin execution time, the binary for the current architecture
is copied to `${project.buid.directory}/shellcheck-plugin/shellcheck` and the invoked.

Optionally the plugin can be configured to fail the build if warnings are found (i.e. on non-zero 
shellcheck exit code) with the `failBuildIfWarnings` property.

## Usage
```
    <build>
        <plugins>
            <plugin>
                <groupId>dev.dimlight</groupId>
                <artifactId>shellcheck-maven-plugin</artifactId>
                <version>${shellcheck-maven-plugin.version}</version>
                <executions>
                    <execution>
                        <id>simple-check</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                        <configuration>
                            <failBuildIfWarnings>false</failBuildIfWarnings>
                            <sourceLocations>
                                <sourceLocation>${project.basedir}/src/main/sh</sourceLocation>
                            </sourceLocations>

                            <!-- embedded, download or external --> 
                            <binaryResolutionMethod>download</binaryResolutionMethod>
                            
                            <!-- If you have external as resolution method you need also to provide "externalBinaryPath") -->
                            <!-- externalBinaryPath>/path/to/shellcheck/</externalBinaryPath -->
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

## How to build

Requirements
* jdk 8
* maven 3.5.4 or later

```
mvn clean install
```

## TODO
- make the download url for the shellcheck binary configurable
- release on maven central
