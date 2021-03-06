// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.launcher.process;

import fi.jumi.core.util.Immutables;

import javax.annotation.concurrent.Immutable;
import java.nio.file.Path;
import java.util.*;

@Immutable
public class JvmArgs {

    // TODO: make private, or keep public to ease testing?

    public final Path workingDir;
    public final Path javaHome;
    public final List<String> jvmOptions;
    public final Map<String, String> systemProperties;
    public final Path executableJar;
    public final List<String> programArgs;

    public JvmArgs(JvmArgsBuilder src) {
        this.workingDir = src.getWorkingDir();
        this.javaHome = src.getJavaHome();
        this.jvmOptions = Immutables.list(src.getJvmOptions());
        this.systemProperties = Immutables.map(src.getSystemProperties());
        this.executableJar = src.getExecutableJar();
        this.programArgs = Immutables.list(src.getProgramArgs());
    }

    public Path getWorkingDir() {
        return workingDir;
    }

    public List<String> toCommand() {
        List<String> command = new ArrayList<>();
        command.add(javaHome.resolve("bin/java").toAbsolutePath().toString());
        command.addAll(jvmOptions);
        command.addAll(asJvmOptions(systemProperties));
        command.add("-jar");
        command.add(executableJar.toAbsolutePath().toString());
        command.addAll(programArgs);
        return command;
    }

    private static List<String> asJvmOptions(Map<String, String> systemProperties) {
        List<String> jvmOptions = new ArrayList<>();
        for (Map.Entry<String, String> p : systemProperties.entrySet()) {
            jvmOptions.add("-D" + p.getKey() + "=" + p.getValue());
        }
        return jvmOptions;
    }
}
