package com.vinz.mclauncher;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private ProgressBar progressBar;
    private Button btnPlay;

    private Downloader downloader;
    private GameRunner gameRunner;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Ganti sesuai lokasi file kamu sendiri (JRE, assets, libraries, dsb).
    private static final String JRE_URL = "https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch/releases/download/jre17-ec28559/jre17-arm64-20210825-release.tar.xz";
    private static final String ASSETS_URL = "https://example.com/path/to/assets.tar.gz";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);
        btnPlay = findViewById(R.id.btnPlay);

        File baseDir = getFilesDir();
        downloader = new Downloader(baseDir);
        gameRunner = new GameRunner(this, baseDir);

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGameFlow();
            }
        });
    }

    private void startGameFlow() {
        btnPlay.setEnabled(false);
        tvStatus.setText(R.string.status_downloading);

        // Semua kerja berat (download, ekstrak, launch) dilakukan di background thread
        new Thread(() -> {
            try {
                boolean needsJre = !gameRunner.isJreReady();
                boolean needsAssets = !gameRunner.areAssetsReady();

                if (needsJre) {
                    downloader.downloadAndExtract(JRE_URL, "jre", this::onProgress);
                }
                if (needsAssets) {
                    downloader.downloadAndExtract(ASSETS_URL, "assets", this::onProgress);
                }

                mainHandler.post(() -> tvStatus.setText(R.string.status_launching));

                gameRunner.launchMinecraft();

                mainHandler.post(() -> {
                    tvStatus.setText(R.string.status_idle);
                    btnPlay.setEnabled(true);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this,
                            "Gagal menjalankan: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    tvStatus.setText(R.string.status_idle);
                    btnPlay.setEnabled(true);
                });
            }
        }).start();
    }

    private void onProgress(int percent) {
        mainHandler.post(() -> progressBar.setProgress(percent));
    }
}
