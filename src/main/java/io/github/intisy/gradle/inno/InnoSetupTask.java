package io.github.intisy.gradle.inno;

import org.gradle.api.DefaultTask;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("unused")
public class InnoSetupTask extends DefaultTask {
    String fileName;
    String name;
    String icon;
    String version;
    List<String> autoStartParameters;
    List<String> parameters;
    Path jrePath;
    boolean autoStart;
    boolean debug;

    @TaskAction
    public void createExe() {
        if (fileName != null && name != null && jrePath != null) {
            try {
                InnoSetup innoSetup = getInnoSetup();
                LogLevel logLevel = getProject().getGradle().getStartParameter().getLogLevel();
                innoSetup.setDebug(debug || logLevel.equals(LogLevel.INFO) || logLevel.equals(LogLevel.DEBUG));
                if (icon != null) {
                    File iconFile = getProject().getProjectDir().toPath().resolve(icon).toFile();
                    innoSetup.setIconFile(iconFile);
                }
                if (version != null)
                    innoSetup.setVersion(version);
                innoSetup.setAutoStart(autoStart);
                innoSetup.setJrePath(jrePath);
                innoSetup.setParameters(parameters);
                innoSetup.setAutoStartParameters(autoStartParameters);
                innoSetup.buildInstaller();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Please define 'fileName' and 'name' and 'jrePath'");
        }
    }

    private @NotNull InnoSetup getInnoSetup() throws IOException {
        File buildDir = getProject().getBuildDir();
        Path libDir = buildDir.toPath().resolve("libs");
        return new InnoSetup(buildDir, libDir.resolve(fileName).toFile(), libDir.resolve(name.toLowerCase().replace(" ", "-") + "-installer.exe").toFile(), name);
    }
}