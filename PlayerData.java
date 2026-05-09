package com.example.miniangrybirds;

public class PlayerData {

    public static String playerName        = "Hero";
    public static int    maxLevelUnlocked  = 1;   // highest level player can access
    public static int    totalScore        = 0;
    public static int[]  levelStars        = new int[6]; // index 1-5, stars per level (0,1,2,3)

    // Static METHOD — call like PlayerData.updateAfterLevel(...)
    // No object needed because everything is static
    public static void updateAfterLevel(int level, int stars, int score) {

        // Add score
        totalScore += score;

        // Update stars only if better than previous attempt
        if (stars > levelStars[level]) {
            levelStars[level] = stars;
        }

        // Unlock next level
        if (level + 1 > maxLevelUnlocked && level < 5) {
            maxLevelUnlocked = level + 1;
        }
    }

    // Reset everything (used by "Replay All" button)
    public static void reset() {
        playerName       = "Hero";
        maxLevelUnlocked = 1;
        totalScore       = 0;
        levelStars       = new int[6];
    }
}