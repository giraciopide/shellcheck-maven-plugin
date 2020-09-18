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
    * lets you target a specific shellcheck version different from the embedded one
* `external` the path to a shellcheck binary should be provided.
    * you have all control
    * requiring external tools makes the build less self-contained

For embedded and download resolution at plugin execution time, the "resolved" binary
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

                            <!-- if you have "download" as resolution method, you may also provide the url of the shellcheck
                                  release archive (zip or tar.xz) for your (os/arch) to be used at plugin execution time.
                                  If you don't provide it, the same url (for your detected os/arch) that was used to 
                                  fetch the embedded binaries will be used instead, but at that point you may as well
                                  use the embedded binaries -->
                            <releaseArchiveUrl>https://github.com/koalaman/shellcheck/releases/download/v0.7.1/shellcheck-v0.7.1.linux.x86_64.tar.xz</releaseArchiveUrl>
                            
                            <!-- If you have "external" as resolution method you need also to provide "externalBinaryPath" -->
                            <!-- externalBinaryPath>/path/to/shellcheck</externalBinaryPath -->
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
- make the download urls be a map with an entry for each arch
- release on maven central

## Copyright notice

shellcheck-maven-plugin is licensed under the GNU General Public License, v3. A copy of this license 
is included in the file LICENSE.txt.

copyright 2020, Marco Nicolini.

##  Shellcheck copyright notice

ShellCheck is licensed under the GNU General Public License.

Copyright 2012-2019, Vidar 'koala_man' Holen and contributors.