# shellcheck-maven-plugin
A maven plugin to execute shellcheck in a maven build

## How it works
The plugin searches for shell files in standard (and configurable) locations and invokes shellcheck on them.

Since shellcheck is a non-java application, and external requirements for a maven plugin are a complete
hassle, at plugin build time the shellcheck binaries for all architectures are downloaded and packed into the jar.
 
Then at plugin execution time, the correct binary is copied to `${project.buid.directory}/shellcheck/shellcheck`
and then invoked.

Optionally the plugin can be configured to fail the build if warnings are found (i.e. on non-zero 
shellcheck exit code).

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
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

## How to build
```
mvn clean install
```

## TODO
* Allow use of an existing shellcheck binary (given its path)
* Download shellcheck binaries at plugin-execution time instead of plugin-build time, and make shellcheck
version configurable
