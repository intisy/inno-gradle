package io.github.intisy.gradle.inno;

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

public class GitHub {
    private final String releaseUrl;
    private final boolean debug;

    public GitHub(String releaseUrl, boolean debug) {
        this.releaseUrl = releaseUrl;
        this.debug = debug;
    }

    public void log(String log) {
        if (debug)
            System.out.println(log);
    }

    public Path download() {
        try {
            JsonObject latestReleaseZip = getLatestReleaseZip(releaseUrl);
            if (latestReleaseZip != null) {
                Path path = GradleUtils.getGradleHome().resolve("inno").resolve(latestReleaseZip.get("tag_name").getAsString());
                File output = path.resolve("inno.zip").toFile();
                if (!path.toFile().exists()) {
                    log("Downloading Inno Setup from: " + latestReleaseZip.get("zipball_url"));
                    path.toFile().mkdirs();
                    downloadFile(latestReleaseZip.get("zipball_url").getAsString(), output);
                    log("Download completed.");
                    unzipAndFlatten(output, path.toFile());
                    log("Unzip completed to " + path);
                }
                return path;
            } else {
                log("Failed to get the latest release ZIP URL.");
            }
        } catch (Exception e) {
            log("Error: " + e.getMessage());
        }
        return null;
    }

    public static void unzipAndFlatten(File zipFilePath, File outputDirectory) throws IOException {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
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
        zipFilePath.delete();
    }

    private static void createDirectoriesForFile(File file) {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
    }

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
            log("Failed to get latest release info. HTTP Response Code: " + responseCode);
        }
        httpConnection.disconnect();
        return null;
    }

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
            log("Failed to download file. HTTP Response Code: " + responseCode);
        }
        httpConnection.disconnect();
    }
}