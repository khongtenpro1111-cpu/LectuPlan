package com.tkyl.lectuplan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // System Bars inset handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splashMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set status bar icons light (white) for splash background
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
            insetsController.setAppearanceLightNavigationBars(false);
        }

        // Ánh xạ các View để chạy Animation
        View cardLogo = findViewById(R.id.cardLogo);
        View layoutText = findViewById(R.id.layoutText);
        View layoutFooter = findViewById(R.id.layoutFooter);

        // 1. Animation cho Logo (Phóng to nhẹ + Hiện dần)
        if (cardLogo != null) {
            cardLogo.setAlpha(0f);
            cardLogo.setScaleX(0.5f);
            cardLogo.setScaleY(0.5f);
            cardLogo.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(1000)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // 2. Animation cho Tên ứng dụng & Chú thích (Trượt từ dưới lên + Hiện dần)
        if (layoutText != null) {
            layoutText.setAlpha(0f);
            layoutText.setTranslationY(50f);
            layoutText.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(350)
                    .setDuration(900)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // 3. Animation cho Chân trang (Hiện dần)
        if (layoutFooter != null) {
            layoutFooter.setAlpha(0f);
            layoutFooter.animate()
                    .alpha(1f)
                    .setStartDelay(700)
                    .setDuration(800)
                    .start();
        }

        // Chuyển sang MainActivity sau 2.5 giây
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2500);
    }
}
