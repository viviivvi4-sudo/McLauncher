package com.vinz.mclauncher;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Bertanggung jawab menyiapkan argumen JVM Minecraft, mengatur
 * LD_LIBRARY_PATH ke folder native library internal aplikasi,
 * dan mengeksekusi proses Minecraft.
 */
public class GameRunner {

    private final Context context;
    private final File baseDir;

    // Path native library internal aplikasi (hasil ekstrak jniLibs oleh sistem Android
    // ada di nativeLibraryDir, tapi untuk lib tambahan yang didownload runtime,
    // kita simpan di folder files/jniLibs sesuai spesifikasi project).
    private static final String INTERNAL_LIB_PATH = "/data/data/com.vinz.mclauncher/files/jniLibs/";

    public GameRunner(Context context, File baseDir) {
        this.context = context;
        this.baseDir = baseDir;
    }

    public boolean isJreReady() {
        return new File(baseDir, "jre/bin/java").exists();
    }

    public boolean areAssetsReady() {
        return new File(baseDir, "assets").exists();
    }

    /**
     * Menyusun argumen JVM + argumen Minecraft, lalu menjalankan proses.
     * Argumen game (username, versi, dsb) sebaiknya diisi sesuai kebutuhan
     * profil akun/versi yang dipilih user.
     */
    public void launchMinecraft() throws IOException, InterruptedException {
        File javaBin = new File(baseDir, "jre/bin/java");
        File gameDir = new File(baseDir, "game");
        File assetsDir = new File(baseDir, "assets");
        File librariesDir = new File(baseDir, "libraries");
        File clientJar = new File(baseDir, "game/client.jar");

        if (!gameDir.exists()) {
            gameDir.mkdirs();
        }

        List<String> command = new ArrayList<>();
        command.add(javaBin.getAbsolutePath());

        // JVM args
        command.add("-Djava.library.path=" + INTERNAL_LIB_PATH);
        command.add("-Xms512M");
        command.add("-Xmx1024M");
        command.add("-cp");
        command.add(clientJar.getAbsolutePath() + ":" + librariesDir.getAbsolutePath() + "/*");

        // Main class Minecraft
        command.add("net.minecraft.client.main.Main");

        // Argumen Minecraft (sesuaikan dengan versi & akun yang dipakai)
        command.add("--gameDir");
        command.add(gameDir.getAbsolutePath());
        command.add("--assetsDir");
        command.add(assetsDir.getAbsolutePath());
        command.add("--version");
        command.add("1.20.1");

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        // Set LD_LIBRARY_PATH agar linker native (LWJGL, GL4ES, dll) ditemukan
        builder.environment().put("LD_LIBRARY_PATH", INTERNAL_LIB_PATH);
        builder.directory(gameDir);

        Process process = builder.start();

        // Baca output log proses (opsional, berguna untuk debugging)
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder log = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            log.append(line).append('\n');
        }

        process.waitFor();
    }
}
