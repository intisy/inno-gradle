# Inno Setup Gradle

Archives containing JAR files are available as [releases](https://github.com/intisy/inno-gradle/releases).

## What is inno-gradle?

inno-gradle lets you automatically use InnoSetup from gradle

## Usage

Using the plugins DSL:

```groovy
plugins {
    id "io.github.intisy.inno-gradle" version "1.6.4.1"
}
```

Using legacy plugin application:

```groovy
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        classpath "io.github.intisy.inno-gradle:1.6.4.1"
    }
}

apply plugin: "io.github.intisy.inno-gradle"
```

Once you have the plugin installed you can use it like so:

```groovy
import io.github.intisy.gradle.inno.InnoSetupTask

tasks.register("createInstaller", InnoSetupTask) {
   infile = artifact_name + "-" + platformArch + "-obfuscated.exe"
   outfile = artifact_name + "-" + platformArch + "-installer.exe"
   name = artifact_name
   version = project.version
   icon = "${projectDir}/icon.ico"
   jrePath = "${buildDir}/libs/jre"
   autoStart = true
   debug = true
}
```

## License

[![Apache License 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
