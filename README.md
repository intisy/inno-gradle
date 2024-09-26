# Inno Setup Gradle

Archives containing JAR files are available as [releases](https://github.com/intisy/intisy/innosetup-gradle/releases).

## What is Online Gradle?

inno-gradle lets you automatically use InnoSetup from gradle

## Usage

Using the plugins DSL:

```groovy
plugins {
    id "io.github.intisy.intisy/innosetup-gradle" version "1.0.2"
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
        classpath "io.github.intisy:intisy/innosetup-gradle:1.0.2"
    }
}

apply plugin: "io.github.intisy.intisy/innosetup-gradle"
```

Once you have the plugin installed you can use it like so:

```groovy
inno {
    filename = "your-exe.exe"
    name = "program-name"
}
```

## License

[![Apache License 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
