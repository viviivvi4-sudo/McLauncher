package com.vinz.mclauncher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * Membantu mendownload file (JRE .tar.gz, assets, dsb) dari sebuah URL
 * dan mengekstraknya secara lokal ke dalam folder aplikasi.
 *
 * Catatan: parser TAR di sini adalah implementasi minimal (tanpa dependensi
 * eksternal) yang menangani entry file & folder pada arsip tar biasa (format
 * ustar). Untuk kebutuhan produksi yang lebih kompleks (symlink, sparse file,
 * dll), pertimbangkan menambahkan library seperti Apache Commons Compress
 * ke app/libs/.
 */
public class Downloader {

    public interface ProgressCallback {
        void onProgress(int percent);
    }

    private final File baseDir;

    public Downloader(File baseDir) {
        this.baseDir = baseDir;
    }

    public void downloadAndExtract(String urlString, String targetFolderName, ProgressCallback callback) throws IOException {
        File tempFile = new File(baseDir, targetFolderName + ".tar.xz");
        download(urlString, tempFile, callback);

        File targetDir = new File(baseDir, targetFolderName);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        extractTarGz(tempFile, targetDir);
        tempFile.delete();
    }

    private void download(String urlString, File outputFile, ProgressCallback callback) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        int fileLength = connection.getContentLength();

        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             FileOutputStream output = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[8192];
            long total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                output.write(buffer, 0, count);
                if (fileLength > 0 && callback != null) {
                    callback.onProgress((int) (total * 100 / fileLength));
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    private void extractTarGz(File tarGzFile, File targetDir) throws IOException {
        try (InputStream fis = new java.io.FileInputStream(tarGzFile);
             GZIPInputStream gzis = new GZIPInputStream(fis)) {
            extractTar(gzis, targetDir);
        }
    }

    private void extractTar(InputStream in, File targetDir) throws IOException {
        byte[] header = new byte[512];

        while (true) {
            int read = readFully(in, header);
            if (read < 512) {
                break;
            }

            // Deteksi blok kosong (penanda akhir arsip tar)
            boolean isEmpty = true;
            for (byte b : header) {
                if (b != 0) {
                    isEmpty = false;
                    break;
                }
            }
            if (isEmpty) {
                break;
            }

            String name = new String(header, 0, 100).trim();
            // Buang karakter null di akhir nama file
            int nullIndex = name.indexOf('\0');
            if (nullIndex >= 0) {
                name = name.substring(0, nullIndex);
            }

            String sizeOctal = new String(header, 124, 12).trim();
            long size = sizeOctal.isEmpty() ? 0 : Long.parseLong(sizeOctal.trim(), 8);

            char typeFlag = (char) header[156];

            File outputFile = new File(targetDir, name);

            if (typeFlag == '5' || name.endsWith("/")) {
                // Direktori
                outputFile.mkdirs();
            } else {
                File parent = outputFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    long remaining = size;
                    while (remaining > 0) {
                        int toRead = (int) Math.min(buffer.length, remaining);
                        int r = in.read(buffer, 0, toRead);
                        if (r < 0) break;
                        fos.write(buffer, 0, r);
                        remaining -= r;
                    }
                }
            }

            // Setiap entry di-pad hingga kelipatan 512 byte
            long padding = (512 - (size % 512)) % 512;
            skipFully(in, padding);
        }
    }

    private int readFully(InputStream in, byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int r = in.read(buffer, total, buffer.length - total);
            if (r < 0) break;
            total += r;
        }
        return total;
    }

    private void skipFully(InputStream in, long count) throws IOException {
        long remaining = count;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped <= 0) break;
            remaining -= skipped;
        }
    }
}
