package io.github.intisy.gradle.inno;

import io.github.intisy.gradle.inno.impl.InnoSetup;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Gradle task that builds a Windows installer using Inno Setup.
 *
 * <p>Required: {@code fileName}, {@code appName}, {@code jrePath}.
 * Optional: {@code icon}, {@code version}, {@code parameters}, {@code autoStartParameters}, {@code autoStart}, {@code debug}.
 * Produces an installer executable under the project's build directory.</p>
 */
@SuppressWarnings("unused")
public class InnoSetupTask extends DefaultTask {
    private final Logger logger = new Logger(this, getProject());

    String infile;
    String outfile;
    String appName;
    String appIcon;
    String appVersion;
    String jrePath;
    List<String> autoStartParameters;
    List<String> parameters;
    boolean autoStart;
    boolean debug;

    /**
     * Sets the file name of the application executable to package.
     *
     * @param infile executable file name under build/libs
     */
    public void setInfile(String infile) {
        this.infile = infile;
    }

    /**
     * Sets the file name of the application executable to package.
     *
     * @param outfile executable file name under build/libs
     */
    public void setOutfile(String outfile) {
        this.outfile = outfile;
    }

    /**
     * Sets the application display name.
     *
     * @param appName human-readable application name
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * Sets an optional icon file path (relative to project dir) for the installer and shortcuts.
     *
     * @param appIcon relative path to an .ico file
     */
    public void setAppIcon(String appIcon) {
        this.appIcon = appIcon;
    }

    /**
     * Sets the application version recorded in the installer metadata.
     *
     * @param appVersion version string (e.g., 1.2.3)
     */
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
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
    public void setJrePath(String jrePath) {
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
    @Optional
    @Input
    public String getOutfile() {
        return outfile;
    }

    /**
     * @return the executable file name to package (preferred over outfile if set)
     */
    @Optional
    @Input
    public String getInfile() {
        return infile;
    }

    /**
     * @return the application display name
     */
    @Optional
    @Input
    public String getAppName() {
        return appName;
    }

    /**
     * @return the optional icon path relative to the project, or null
     */
    @Optional
    @Input
    public String getAppIcon() {
        return appIcon;
    }

    /**
     * @return the application version string
     */
    @Optional
    @Input
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * @return parameters for Startup shortcut or null
     */
    @Optional
    @Input
    public List<String> getAutoStartParameters() {
        return autoStartParameters;
    }

    /**
     * @return parameters passed when launching after install or null
     */
    @Optional
    @Input
    public List<String> getParameters() {
        return parameters;
    }

    /**
     * @return the JRE folder path to bundle
     */
    @Optional
    @Input
    public String getJrePath() {
        return jrePath;
    }

    /**
     * @return true if a Startup shortcut should be created
     */
    @Input
    public boolean isAutoStart() {
        return autoStart;
    }

    /**
     * @return true if debug logging is enabled
     */
    @Input
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
        logger.debug("Initializing Inno Setup task...");
        if (appName == null) {
            logger.error("Please define 'appName'");
            return;
        }

        try {
            InnoSetup innoSetup = getInnoSetup();
            File buildDir = getProject().getLayout().getBuildDirectory().getAsFile().get();
            Path libDir = buildDir.toPath().resolve("libs");

            if (appIcon != null) {
                File iconFile = getProject().getProjectDir().toPath().resolve(appIcon).toFile();
                innoSetup.setIconFile(iconFile);
            }

            if (appVersion != null)
                innoSetup.setVersion(appVersion);

            if (infile == null) {
                infile = appName.toLowerCase().replace(" ", "-") + ".exe";
            }

            if (outfile == null) {
                outfile = appName.toLowerCase().replace(" ", "-") + "-installer.exe";
            }

            if (jrePath == null) {
                jrePath = buildDir.toPath().resolve("libs").resolve("jre").toString();
            }

            innoSetup.setName(appName);
            innoSetup.setInnoBuildPath(buildDir.toPath().resolve("inno"));
            innoSetup.setInputFile(libDir.resolve(infile).toFile());
            innoSetup.setOutputFile(libDir.resolve(outfile).toFile());
            innoSetup.setAutoStart(autoStart);
            innoSetup.setJrePath(new File(jrePath).toPath());
            innoSetup.setParameters(parameters);
            innoSetup.setAutoStartParameters(autoStartParameters);
            innoSetup.buildInstaller();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates and configures the {@code InnoSetup} helper for this project.
     *
     * @return configured {@code InnoSetup} instance
     * @throws IOException if build directories cannot be resolved
     */
    @NotNull
    private InnoSetup getInnoSetup() throws IOException {
        return new InnoSetup(logger);
    }
}