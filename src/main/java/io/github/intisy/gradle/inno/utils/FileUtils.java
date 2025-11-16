package io.github.intisy.gradle.inno.utils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * File utility helpers for copying, creating, and deleting files and directories.
 *
 * <p>Provides folder copy, recursive delete, and mkdirs helpers used by the
 * Inno Setup build workflow.</p>
 */
public class FileUtils {

    /**
     * Recursively copies a folder tree from a source path to a target path.
     *
     * @param source root folder to copy
     * @param target destination root
     * @throws IOException if walking or copying fails
     */
    public static void copyFolder(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            @NotNull
            public FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            @NotNull
            public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Deletes a file if it exists, throwing a runtime exception on failure.
     *
     * @param file file to delete
     */
    public static void delete(File file) {
        if (file.exists() && !file.delete()) {
            throw new RuntimeException("Failed to delete file: " + file.getAbsolutePath());
        }
    }

    /**
     * Ensures a directory exists for the provided path.
     *
     * @param path directory path to create
     */
    public static boolean mkdirs(Path path) {
        return mkdirs(path.toFile());
    }

    /**
     * Ensures a directory exists for the provided file (as directory).
     *
     * @param file directory to create
     */
    public static boolean mkdirs(File file) {
        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("Failed to create directories: " + file.getAbsolutePath());
        }
        return true;
    }

    /**
     * Recursively deletes a folder tree if it exists.
     *
     * @param path root directory to delete
     * @throws IOException if deletion fails
     */
    public static void deleteFolder(Path path) throws IOException {
        if (path.toFile().exists())
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                @NotNull
                public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                @NotNull
                public FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
    }
}
