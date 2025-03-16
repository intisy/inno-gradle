package io.github.intisy.gradle.inno;

import io.github.intisy.gradle.inno.utils.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class InnoSetup {
    private final File inputFile;
    private final File outputFile;
    private final String name;
    private final String safeName;
    private final Path innoBuildPath;
    private final Path innoBuildSourcePath;
    private File iconFile;
    private String version = "1.0";
    private Path jrePath;
    private List<String> autoStartParameters;
    private List<String> parameters;
    private boolean autoStart;
    private boolean debug;

    public InnoSetup(File path, File inputFile, File outputFile, String name) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.name = name;
        this.innoBuildPath = path.toPath().resolve("inno");
        this.innoBuildSourcePath = innoBuildPath.resolve("source");
        this.safeName = name.replace(" ", "-");
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public void setIconFile(File iconFile) {
        this.iconFile = iconFile;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setJrePath(Path jrePath) {
        this.jrePath = jrePath;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setAutoStartParameters(List<String> autoStartParameters) {
        this.autoStartParameters = autoStartParameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public void log(String log) {
        if (debug)
            System.out.println(log);
    }

    public void buildInstaller() throws IOException, InterruptedException {
        GitHub gitHub = new GitHub("https://api.github.com/repos/intisy/InnoSetup/releases/latest", debug);
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

    public void copySourceFiles() throws IOException {
        FileUtils.mkdirs(innoBuildSourcePath.toFile());
        Files.copy(inputFile.toPath(), innoBuildSourcePath.resolve(inputFile.getName()));
        FileUtils.copyFolder(jrePath, innoBuildSourcePath.resolve("jre"));
        if (iconFile != null)
            Files.copy(iconFile.toPath(), innoBuildSourcePath.resolve(iconFile.getName()));
    }

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
                (autoStart ? "Name: \"{userstartup}\\" + name + "\"; Filename: \"{app}\\" + safeName + ".exe\"; " + (parameters != null ? "Parameters: \"" + String.join(" ", parameters) + "\"; " : "") +  "\n" : "") +
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
