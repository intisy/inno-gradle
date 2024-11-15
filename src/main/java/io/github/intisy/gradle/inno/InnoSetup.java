package io.github.intisy.gradle.inno;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class InnoSetup {
    private final File path;
    private final String fileName;
    private final String iconPath;
    private final String name;
    private final String jreName;
    private final boolean debug;

    public InnoSetup(File path, String fileName, String name, File icon, boolean copyIcon, boolean debug) throws IOException {
        this.path = path;
        this.fileName = "libs\\" + fileName;
        this.name = name;
        this.iconPath = name.toLowerCase().replace(" ", "-") + ".ico";
        if (copyIcon) {
            path.toPath().resolve(iconPath).toFile().delete();
            Files.copy(icon == null ? path.getParentFile().toPath().resolve(iconPath) : icon.toPath(), path.toPath().resolve(iconPath));
        }
        this.jreName = "libs\\jre-windows";
        this.debug = debug;
    }

    public void log(String log) {
        if (debug)
            System.out.println(log);
    }

    public void buildInstaller() throws IOException, InterruptedException {
        Path innoFolder = path.toPath().resolve("inno");
        innoFolder.toFile().delete();
        GitHub gitHub = new GitHub("https://api.github.com/repos/intisy/InnoSetup/releases/latest", debug);
        FileUtils.copyFolder(Objects.requireNonNull(gitHub.download()), innoFolder);
        File innoSetupCompiler = innoFolder.resolve("ISCC.exe").toFile();
        File scriptPath = path.toPath().resolve("build.iss").toFile();
        createInnoSetupScript(scriptPath);
        log("Starting Inno Setup script " + scriptPath.getAbsolutePath() + " using " + innoSetupCompiler.getAbsolutePath());
        File output = path.toPath().resolve("libs").resolve(name.toLowerCase().replace(" ", "-") + "-installer.exe").toFile();
        output.delete();
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", innoSetupCompiler.getAbsolutePath() + " " + scriptPath.getAbsolutePath());
        processBuilder.redirectErrorStream(true); // Combine standard and error output
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log(line);
            }
        }
        process.waitFor();
        log("Process finished with exit code: " + process.exitValue());
    }

    public void createInnoSetupScript(File scriptPath) throws IOException {
        String scriptContent = "[Setup]\n" +
                "AppName=" + name + "\n" +
                "AppVersion=1.0\n" +
                "DefaultDirName={pf}\\" + name.replace(" ", "") + "\n" +
                "DefaultGroupName=" + name.replace(" ", "") + "\n" +
                "OutputDir=libs\n" +
                "OutputBaseFilename=" + name.toLowerCase().replace(" ", "-") + "-installer\n" +
                "SetupIconFile=" + iconPath + "\n" +
                "Compression=lzma\n" +
                "SolidCompression=yes\n" +
                "\n" +
                "[Files]\n" +
                "; Add executable and JRE files\n" +
                "Source: \"" + fileName + "\"; DestDir: \"{app}\"; Flags: ignoreversion\n" +
                "Source: \"" + jreName + "\\*\"; DestDir: \"{app}\\jre\"; Flags: recursesubdirs\n" +
                "\n" +
                "[Icons]\n" +
                "; Create desktop shortcut\n" +
                "Name: \"{commondesktop}\\" + name + "\"; Filename: \"{app}\\" + name.replace(" ", "") + ".exe\"\n" +
                "\n" +
                "[Run]\n" +
                "; Run the application after installation\n" +
                "Filename: \"{app}\\" + name.replace(" ", "") + ".exe\"; Description: \"Launch " + name + "\"; Flags: nowait postinstall skipifsilent\n";
        scriptPath.delete();
        try (FileWriter writer = new FileWriter(scriptPath)) {
            writer.write(scriptContent);
        }
        log("Inno Setup script created at: " + scriptPath);
    }
}
