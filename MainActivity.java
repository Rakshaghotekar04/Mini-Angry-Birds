package com.example.miniangrybirds;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

public class MainActivity extends Activity {

    private DashboardView dashboardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // First time? Ask for name. Otherwise go straight to dashboard.
        if (PlayerData.playerName.equals("Hero")) {
            askForName();
        } else {
            showDashboard();

        }
    }

    // Called every time you come BACK to this screen (e.g. after a game)
    @Override
    protected void onResume() {
        super.onResume();
        if (dashboardView != null) {
            dashboardView.refresh(); // update score/stars on dashboard
        }
    }

    private void askForName() {
        EditText input = new EditText(this);
        input.setHint("Enter your name");

        new AlertDialog.Builder(this)
                .setTitle("Welcome to Mini Angry Birds! 🐦")
                .setMessage("What's your name, player?")
                .setView(input)
                .setPositiveButton("Let's Play!", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    PlayerData.playerName = name.isEmpty() ? "Hero" : name;
                    showDashboard();
                })
                .setCancelable(false)
                .show();
    }

    private void showDashboard() {
        dashboardView = new DashboardView(this, this);
        setContentView(dashboardView);
    }
}