package com.BloodSouls.Souls.android;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;

import com.BloodSouls.Souls.BloodSoulsGame;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {

    private static final String TAG = "AndroidLauncher";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception in thread " + thread.getName() + ": " + throwable.getMessage(), throwable);
            runOnUiThread(() -> {
                try {
                    new AlertDialog.Builder(AndroidLauncher.this)
                        .setTitle("Game Notice")
                        .setMessage("An unexpected error occurred:\n" + throwable.getMessage() + "\n\nStack:\n" + Log.getStackTraceString(throwable))
                        .setPositiveButton("OK", null)
                        .show();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to show error dialog: " + e.getMessage());
                }
            });
        });

        try {
            AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
            configuration.useImmersiveMode = true;
            configuration.useGL30 = false;
            configuration.useCompass = false;
            configuration.useAccelerometer = false;

            initialize(new BloodSoulsGame(), configuration);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize BloodSoulsGame: " + t.getMessage(), t);
            new AlertDialog.Builder(this)
                .setTitle("Initialization Error")
                .setMessage(t.getMessage())
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
        }
    }
}
