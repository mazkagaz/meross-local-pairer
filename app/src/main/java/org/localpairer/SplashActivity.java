package org.localpairer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.localpairer.model.HttpClientManager;
import org.localpairer.merosslib.MerossHttpClient;
import org.localpairer.merosslib.model.http.ApiCredentials;
import com.google.android.material.button.MaterialButton;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (AndroidPreferencesManager.isDisclaimerAccepted(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        final CheckBox neverAskAgain = findViewById(R.id.neverAskAgainCheckBox);
        MaterialButton agreeButton = findViewById(R.id.acceptButton);
        agreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (neverAskAgain.isChecked()) {
                    AndroidPreferencesManager.setDisclaimerAccepted(SplashActivity.this, true);
                }
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        });
        MaterialButton quitButton = findViewById(R.id.quitButton);
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        hideSystemUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        init();
    }

    private void init() {
        // Load credentials
        HttpClientManager instance = HttpClientManager.getInstance();
        ApiCredentials creds = AndroidPreferencesManager.loadHttpCredentials(this);
        if (creds != null)
            instance.loadFromCredentials(creds);
    }

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
