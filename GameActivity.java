package com.example.miniangrybirds;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

// GameActivity EXTENDS Activity = inherits screen management for free
// IMPLEMENTS GameListener = signs the contract: must write onLevelComplete + onBackToMenu
public class GameActivity extends Activity implements GameView.GameListener {

    // CONSTANT = static final = never changes, shared, no object needed
    public static final String EXTRA_LEVEL = "start_level";

    @Override  // @Override means: "this method came from Activity, I'm replacing it"
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // call parent (Activity) version first

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // getIntent() = get the data sent when starting this Activity
        int startLevel = getIntent().getIntExtra(EXTRA_LEVEL, 1); // default 1

        // Create GameView OBJECT, pass: context + startLevel + listener (this class)
        // "this" = the current GameActivity object, which implements GameListener
        GameView gameView = new GameView(this, startLevel, this);
        setContentView(gameView);
    }

    // ── GameListener interface methods ──────────────────────────────────────
    // These are REQUIRED because we wrote "implements GameListener"

    @Override
    public void onLevelComplete(int level, int stars, int score) {
        // Called by GameView when a level is beaten
        PlayerData.updateAfterLevel(level, stars, score);
    }

    @Override
    public void onBackToMenu() {
        // finish() = close this Activity and go back to previous (MainActivity/Dashboard)
        finish();
    }
}