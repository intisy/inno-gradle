package io.github.intisy.gradle.inno.impl;

import io.github.intisy.gradle.inno.Logger;
import io.github.intisy.gradle.inno.utils.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates preparing sources and invoking Inno Setup to build a Windows installer.
 *
 * <p>Handles copying inputs (executable, JRE, optional icon), generating the
 * Inno Setup script, downloading the tool (cached under Gradle home), and
 * executing the compiler to produce the final installer.</p>
 */
public class InnoSetup {
    private String name;
    private String safeName;
    private Path innoBuildPath;
    private Path innoBuildSourcePath;
    private File inputFile;
    private File outputFile;
    private File iconFile;
    private String version = "1.0";
    private Path jrePath;
    private List<String> autoStartParameters;
    private List<String> parameters;
    private boolean autoStart;
    private boolean debug;
    private final Logger logger;

    /**
     * Creates a new InnoSetup helper bound to the given project build path and inputs.
     *
     * @param logger logger to use for logging messages
     */
    public InnoSetup(Logger logger) {
        this.logger = logger;
    }

    /**
     * Sets the application display name.
     *
     * @param name application display name
     */
    public void setName(String name) {
        this.name = name;
        this.safeName = name.replace(" ", "-");
    }

    /**
     * Sets the Inno Setup build directory.
     *
     * @param innoBuildPath Inno Setup build directory
     */
    public void setInnoBuildPath(Path innoBuildPath) {
        this.innoBuildPath = innoBuildPath;
        this.innoBuildSourcePath = innoBuildPath.resolve("source");
    }

    /**
     * Sets the application executable to package.
     *
     * @param inputFile executable file to package
     */
    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    /**
     * Sets the resulting installer file destination.
     *
     * @param outputFile installer file destination
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * Enables or disables creating a Startup entry to auto-launch the app after login.
     *
     * @param autoStart true to add a Startup shortcut, false otherwise
     */
    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    /**
     * Sets an optional icon file for the installer and shortcuts.
     *
     * @param iconFile ICO file to use as setup icon
     */
    public void setIconFile(File iconFile) {
        this.iconFile = iconFile;
    }

    /**
     * Sets the application version written to the installer metadata.
     *
     * @param version version string (e.g., 1.2.3)
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Sets the path to a bundled JRE to include in the installer.
     *
     * @param jrePath path to the JRE directory
     */
    public void setJrePath(Path jrePath) {
        this.jrePath = jrePath;
    }

    /**
     * Enables debug logging of the Inno Setup process output.
     *
     * @param debug true to print logs, false to silence
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Sets optional parameters passed to the app when launched from Startup.
     *
     * @param autoStartParameters list of parameters, or null for none
     */
    public void setAutoStartParameters(List<String> autoStartParameters) {
        this.autoStartParameters = autoStartParameters;
    }

    /**
     * Sets optional parameters passed to the app when launched post-install.
     *
     * @param parameters list of parameters, or null for none
     */
    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    /**
     * Logs a message if debug mode is enabled.
     *
     * @param log message to print
     */
    public void log(String log) {
        if (debug)
            System.out.println(log);
    }

    /**
     * Builds the installer by preparing sources, generating the script, and invoking Inno Setup.
     *
     * @throws IOException if file operations fail
     * @throws InterruptedException if the external process is interrupted
     */
    public void buildInstaller() throws IOException, InterruptedException {
        GitHub gitHub = new GitHub("https://api.github.com/repos/intisy/InnoSetup/releases/latest", logger);
        FileUtils.deleteFolder(innoBuildPath);
        FileUtils.copyFolder(Objects.requireNonNull(gitHub.download()), innoBuildPath);
        File innoSetupCompiler = innoBuildPath.resolve("ISCC.exe").toFile();
        File scriptPath = innoBuildPath.resolve("build.iss").toFile();
        copySourceFiles();
        createInnoSetupScript(scriptPath);
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", innoSetupCompiler.getAbsolutePath() + " " + scriptPath.getAbsolutePath());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log(line);
            }
        }
        process.waitFor();
        Path outputPath = innoBuildPath.resolve("output");
        FileUtils.delete(outputFile);
        Files.copy(outputPath.resolve(outputFile.getName()), outputFile.toPath());
        log("Process finished with exit code: " + process.exitValue());
    }

    /**
     * Copies input executable, JRE, and optional icon into the build source directory.
     *
     * @throws IOException if copy operations fail
     */
    public void copySourceFiles() throws IOException {
        FileUtils.mkdirs(innoBuildSourcePath.toFile());
        Files.copy(inputFile.toPath(), innoBuildSourcePath.resolve(inputFile.getName()));
        FileUtils.copyFolder(jrePath, innoBuildSourcePath.resolve("jre"));
        if (iconFile != null)
            Files.copy(iconFile.toPath(), innoBuildSourcePath.resolve(iconFile.getName()));
    }

    /**
     * Creates the Inno Setup script file used by the compiler.
     *
     * @param scriptPath output path for the generated .iss script
     * @throws IOException if writing the file fails
     */
    public void createInnoSetupScript(File scriptPath) throws IOException {
        String scriptContent = "[Setup]\n" +
                "AppName=" + name + "\n" +
                "AppVersion=" + version + "\n" +
                "DefaultDirName={pf}\\" + safeName + "\n" +
                "DefaultGroupName=" + safeName + "\n" +
                "OutputDir=output\n" +
                "OutputBaseFilename=" + outputFile.getName().split("\\.")[0] + "\n" +
                (iconFile != null ? "SetupIconFile=source\\" + iconFile.getName() + "\n" : "") +
                "Compression=lzma\n" +
                "SolidCompression=yes\n" +
                "\n" +
                "[Files]\n" +
                "; Add executable and JRE files\n" +
                "Source: \"source\\" + inputFile.getName() + "\"; DestDir: \"{app}\"; Flags: ignoreversion\n" +
                "Source: \"source\\jre\\*\"; DestDir: \"{app}\\jre\"; Flags: recursesubdirs\n" +
                "\n" +
                "[Icons]\n" +
                "; Create desktop shortcut\n" +
                "Name: \"{commondesktop}\\" + name + "\"; Filename: \"{app}\\" + safeName + ".exe\"\n" +
                (autoStart ? "Name: \"{userstartup}\\" + name + "\"; Filename: \"{app}\\" + safeName + ".exe\"; Parameters: \"/auto" + (autoStartParameters != null ? " " + String.join(" ", autoStartParameters) : "") +  "\"\n" : "") +
                "\n" +
                "[Run]\n" +
                "; Run the application after installation\n" +
                "Filename: \"{app}\\" + safeName + ".exe\"; " + (parameters != null ? "Parameters: \"" + String.join(" ", parameters) + "\"; " : "") +  "Description: \"Launch " + name + "\"; Flags: nowait postinstall skipifsilent\n";
        FileUtils.delete(scriptPath);
        try (FileWriter writer = new FileWriter(scriptPath)) {
            writer.write(scriptContent);
        }
        log("Inno Setup script created at: " + scriptPath);
    }
}
