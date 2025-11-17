package io.github.intisy.gradle.inno.impl;

import io.github.intisy.gradle.inno.Logger;
import io.github.intisy.gradle.inno.utils.FileUtils;
import io.github.intisy.gradle.inno.utils.GradleUtils;
import org.gradle.internal.impldep.com.google.gson.JsonObject;
import org.gradle.internal.impldep.com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Helper for retrieving and caching the latest Inno Setup release from GitHub.
 *
 * <p>Fetches release metadata via the GitHub API, downloads the ZIP artifact to
 * the Gradle cache, and unpacks it into a flattened directory usable by the
 * InnoSetup build process.</p>
 */
public class GitHub {
    private final String releaseUrl;
    private final Logger logger;

    /**
     * Constructs a new helper for a specific GitHub releases API URL.
     *
     * @param releaseUrl GitHub API URL to query for the latest release
     * @param logger logger to use for logging messages
     */
    public GitHub(String releaseUrl, Logger logger) {
        this.releaseUrl = releaseUrl;
        this.logger = logger;
    }

    /**
     * Downloads (and caches) the latest release ZIP and unpacks it.
     *
     * @return path where the ZIP content was unpacked, or null on failure
     */
    public Path download() {
        try {
            JsonObject latestReleaseZip = getLatestReleaseZip(releaseUrl);
            if (latestReleaseZip != null) {
                Path path = GradleUtils.getGradleHome().resolve("inno").resolve(latestReleaseZip.get("tag_name").getAsString());
                File output = path.resolve("inno.zip").toFile();
                if (!path.toFile().exists()) {
                    logger.debug("Downloading Inno Setup from: " + latestReleaseZip.get("zipball_url"));
                    if (FileUtils.mkdirs(path)) {
                        downloadFile(latestReleaseZip.get("zipball_url").getAsString(), output);
                        logger.debug("Download completed.");
                        unzipAndFlatten(output, path.toFile());
                        logger.debug("Unzip completed to " + path);
                    } else {
                        logger.warn("Failed to create directory: " + path);
                    }
                }
                return path;
            } else {
                logger.warn("Failed to get the latest release ZIP URL.");
            }
        } catch (Exception e) {
            logger.error("Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Unzips the given ZIP file into the output directory while flattening the top-level folder.
     *
     * @param zipFilePath path to the downloaded ZIP file
     * @param outputDirectory directory to extract into
     * @throws IOException if extraction fails
     */
    public static void unzipAndFlatten(File zipFilePath, File outputDirectory) throws IOException {
        if (!outputDirectory.exists()) {
            FileUtils.mkdirs(outputDirectory);
        }
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                String prefix = entryName.split("/")[0];
                File file = new File(outputDirectory, entryName.replace(prefix, ""));
                if (entryName.split("/").length > 2) {
                    createDirectoriesForFile(file);
                    extractFile(zipFile, entry, file);
                } else {
                    extractFile(zipFile, entry, file);
                }
            }
        }

        if (!zipFilePath.delete())
            throw new IOException("Failed to delete file: " + zipFilePath.getAbsolutePath());
    }

    /**
     * Ensures parent directories exist for the given file.
     *
     * @param file target file whose parent directories should exist
     */
    private static void createDirectoriesForFile(File file) {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            FileUtils.mkdirs(parentFile);
        }
    }

    /**
     * Extracts a single entry from a ZIP file into a destination file.
     *
     * @param zipFile open ZIP file
     * @param entry entry to extract
     * @param file destination file
     * @throws IOException if extraction fails
     */
    private static void extractFile(ZipFile zipFile, ZipEntry entry, File file) throws IOException {
        try (InputStream inputStream = zipFile.getInputStream(entry);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    /**
     * Retrieves JSON metadata for the latest release from a GitHub API endpoint.
     *
     * @param apiUrl GitHub API URL
     * @return parsed JSON object or null on failure
     * @throws Exception if the request or parse fails
     */
    public JsonObject getLatestReleaseZip(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        httpConnection.setRequestMethod("GET");

        int responseCode = httpConnection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                return JsonParser.parseString(response.toString()).getAsJsonObject();
            }
        } else {
            logger.warn("Failed to get latest release info. HTTP Response Code: " + responseCode);
        }
        httpConnection.disconnect();
        return null;
    }

    /**
     * Downloads a remote file to a local path.
     *
     * @param fileUrl URL to download
     * @param outputFile destination file
     * @throws Exception if the download fails
     */
    public void downloadFile(String fileUrl, File outputFile) throws Exception {
        URL url = new URL(fileUrl);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        httpConnection.setRequestMethod("GET");

        int responseCode = httpConnection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream inputStream = new BufferedInputStream(httpConnection.getInputStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer, 0, 1024)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            }
        } else {
            logger.warn("Failed to download file. HTTP Response Code: " + responseCode);
        }
        httpConnection.disconnect();
    }
}