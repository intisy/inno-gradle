package io.github.intisy.gradle.inno;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("unused")
public class InnoSetupTask extends DefaultTask {
    String fileName;
    String jrePath = "libs\\jre-windows";
    String name;
    String icon;
    boolean debug;

    @TaskAction
    public void createExe() {
        if (fileName != null && name != null) {
            try {
                File iconFile = icon != null ? getProject().getProjectDir().toPath().resolve(icon).toFile() : null;
                InnoSetup innoSetup = new InnoSetup(getProject().getBuildDir(), fileName, name, jrePath, iconFile, debug);
                innoSetup.buildInstaller();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Please define 'fileName' and 'name'");
        }
    }
}