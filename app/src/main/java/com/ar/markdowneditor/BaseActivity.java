package com.ar.markdowneditor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

import com.ar.markdowneditor.util.PrefsHelper;
import com.ar.markdowneditor.util.BiometricHelper;
import com.ar.markdowneditor.util.BiometricPromptHelper;
public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navView;
    private static final long BACKGROUND_TIMEOUT_MS = 2 * 60 * 1000; // 2 minutes
    private long lastBackgroundTime = 0;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyScreenshotFlag();
    }

    private void applyScreenshotFlag() {
        if (!PrefsHelper.isScreenshotAllowed(this)) {
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    protected void setupDrawer(@IdRes int currentNavItemId) {
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        if (navView != null) {
            navView.setCheckedItem(currentNavItemId);

            navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == currentNavItemId) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }

                drawerLayout.closeDrawer(GravityCompat.START);
                drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                        @Override
                        public void onDrawerClosed(View drawerView) {
                            drawerLayout.removeDrawerListener(this);
                            boolean handled = navigate(id);
                            if (!handled) {
                                navView.setCheckedItem(currentNavItemId);
                            }
                        }
                    });

                return true;
            });
        }
    }

    private boolean navigate(int itemId) {
        Intent intent = null;

        if (itemId == R.id.nav_all_notes) {
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else if (itemId == R.id.nav_settings) {
            intent = new Intent(this, SettingsActivity.class);
        } else if (itemId == R.id.nav_archive) {
			intent = new Intent(this, ArchiveActivity.class);
		} else if (itemId == R.id.nav_trash) {
			intent = new Intent(this, TrashActivity.class);
		} else if (itemId == R.id.nav_help) {
            intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("open_feedback", true);
        }
        
        if (intent != null) {
            startActivity(intent);
            overridePendingTransition(0, 0);
            return true;
        }

        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        lastBackgroundTime = System.currentTimeMillis();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (PrefsHelper.isBiometricEnabled(this)
                && BiometricHelper.isAvailable(this)
                && lastBackgroundTime > 0
                && System.currentTimeMillis() - lastBackgroundTime > BACKGROUND_TIMEOUT_MS) {
            BiometricPromptHelper.show(this, "Unlock", "Authenticate to continue",
                new BiometricPromptHelper.Callback() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(String message) { finishAffinity(); }
                });
        }
    }

}