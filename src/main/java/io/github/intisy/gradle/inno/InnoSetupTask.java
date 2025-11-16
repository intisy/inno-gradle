package io.github.intisy.gradle.inno;

import io.github.intisy.gradle.inno.impl.InnoSetup;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Gradle task that builds a Windows installer using Inno Setup.
 *
 * <p>Required: {@code fileName}, {@code name}, {@code jrePath}.
 * Optional: {@code icon}, {@code version}, {@code parameters}, {@code autoStartParameters}, {@code autoStart}, {@code debug}.
 * Produces an installer executable under the project's build directory.</p>
 */
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

    /**
     * Sets the file name of the application executable to package.
     *
     * @param fileName executable file name under build/libs
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Sets the application display name.
     *
     * @param name human-readable application name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets an optional icon file path (relative to project dir) for the installer and shortcuts.
     *
     * @param icon relative path to an .ico file
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * Sets the application version recorded in the installer metadata.
     *
     * @param version version string (e.g., 1.2.3)
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Sets parameters passed when launching from the Startup shortcut.
     *
     * @param autoStartParameters list of parameters or null
     */
    public void setAutoStartParameters(List<String> autoStartParameters) {
        this.autoStartParameters = autoStartParameters;
    }

    /**
     * Sets parameters passed when launching the app after installation.
     *
     * @param parameters list of parameters or null
     */
    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    /**
     * Sets the path to the JRE directory to bundle in the installer.
     *
     * @param jrePath path to a JRE folder
     */
    public void setJrePath(Path jrePath) {
        this.jrePath = jrePath;
    }

    /**
     * Enables or disables creation of a Startup shortcut.
     *
     * @param autoStart true to add Startup shortcut
     */
    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    /**
     * Enables debug logging for the build process.
     *
     * @param debug true to enable verbose logging
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * @return the executable file name to package
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the application display name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the optional icon path relative to the project, or null
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @return the application version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return parameters for Startup shortcut or null
     */
    public List<String> getAutoStartParameters() {
        return autoStartParameters;
    }

    /**
     * @return parameters passed when launching after install or null
     */
    public List<String> getParameters() {
        return parameters;
    }

    /**
     * @return the JRE folder path to bundle
     */
    public Path getJrePath() {
        return jrePath;
    }

    /**
     * @return true if a Startup shortcut should be created
     */
    public boolean isAutoStart() {
        return autoStart;
    }

    /**
     * @return true if debug logging is enabled
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Executes the task to build the Windows installer via Inno Setup.
     *
     * <p>Validates required properties, configures the {@code InnoSetup}
     * helper, and triggers the build process.</p>
     */
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

    /**
     * Creates and configures the {@code InnoSetup} helper for this project.
     *
     * @return configured {@code InnoSetup} instance
     * @throws IOException if build directories cannot be resolved
     */
    private @NotNull InnoSetup getInnoSetup() throws IOException {
        File buildDir = getProject().getBuildDir();
        Path libDir = buildDir.toPath().resolve("libs");
        return new InnoSetup(buildDir, libDir.resolve(fileName).toFile(), libDir.resolve(name.toLowerCase().replace(" ", "-") + "-installer.exe").toFile(), name);
    }
}