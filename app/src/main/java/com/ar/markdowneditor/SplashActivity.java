package com.ar.markdowneditor;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.ar.markdowneditor.util.BiometricHelper;
import com.ar.markdowneditor.util.BiometricPromptHelper;
import com.ar.markdowneditor.util.PrefsHelper;
import com.ar.markdowneditor.util.ThemeUtils;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1100;
    private static final long ANIM_DURATION_MS = 600;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private boolean navigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        hideSystemBars();
        runEntranceAnimation();
		cleanupOldTrash();
		setupNotificationPermission();
        setupStoragePermission();
		
        new Handler(Looper.getMainLooper()).postDelayed(this::decideNext, SPLASH_DELAY_MS);
    }

    private void hideSystemBars() {
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        WindowInsetsControllerCompat controller =
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    private void runEntranceAnimation() {
        View content = findViewById(R.id.splash_content);

        if (content == null) return;

        ObjectAnimator alpha = ObjectAnimator.ofFloat(content, View.ALPHA, 0f, 1f);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(content, View.TRANSLATION_Y, 30f, 0f);
        alpha.setDuration(ANIM_DURATION_MS);
        translationY.setDuration(ANIM_DURATION_MS);

        AnimatorSet contentSet = new AnimatorSet();
        contentSet.playTogether(alpha, translationY);
        contentSet.setInterpolator(new DecelerateInterpolator());
        contentSet.start();

    }
	
	private void cleanupOldTrash() {
		int days = PrefsHelper.getAutoremoveDays(this);
		if (days > 0) {
			new com.ar.markdowneditor.data.ReadmeRepository(this).purgeOldTrash(days);
		}
	}
	
	private void setupNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher = registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(), granted -> {});
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (notificationPermissionLauncher != null) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                    }
                }, SPLASH_DELAY_MS + 500);
            }
        }
    }
    
    private void setupStoragePermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            
                ActivityResultLauncher<String> storagePermissionLauncher = registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(), granted -> {});

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }, SPLASH_DELAY_MS + 500);
            }
        }
    }

    // Biometric decision
    private void decideNext() {
        boolean lockEnabled = PrefsHelper.isBiometricEnabled(this);
        boolean available = BiometricHelper.isAvailable(this);

        if (lockEnabled && available) {
            BiometricPromptHelper.show(this,
				"Unlock Markdown Editor",
				"Authenticate to continue",
				new BiometricPromptHelper.Callback() {
					@Override
					public void onSuccess() {
						goToMain();
					}

					@Override
					public void onFailure(String message) {
						finishAffinity();
					}
				});
        } else {
            goToMain();
        }
    }

    private void goToMain() {
        if (navigating) return;
        navigating = true;

        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {}
}