package com.example.miniangrybirds;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.view.*;

// DashboardView EXTENDS View = gets all drawing + touch features from Android
public class DashboardView extends View {

    // PRIVATE fields — encapsulation, only this class touches these
    private final Activity activity; // need this to start GameActivity
    private final RectF[]  levelCards   = new RectF[5];   // clickable level button areas
    private final RectF    changeNameBtn = new RectF();

    // Paints (like brushes for Canvas drawing)
    private Paint bgPaint, titlePaint, namePaint, scorePaint;
    private Paint cardUnlocked, cardLocked, cardNumPaint, starPaint, lockPaint;
    private Paint sectionTitlePaint, footerPaint, nameBtnPaint, nameBtnTextPaint;

    // CONSTRUCTOR — runs when "new DashboardView(context, activity)" is called
    public DashboardView(Context context, Activity activity) {
        super(context); // call parent (View) constructor
        this.activity = activity;
        init(); // our own setup method
    }

    // Private method — only DashboardView can call this
    private void init() {
        // Initialize all 5 RectF OBJECTS (one per level card)
        for (int i = 0; i < 5; i++) levelCards[i] = new RectF();

        bgPaint = new Paint();

        titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#FDD835"));
        titlePaint.setTextSize(68f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);

        namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(Color.WHITE);
        namePaint.setTextSize(46f);
        namePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        namePaint.setTextAlign(Paint.Align.CENTER);

        scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setColor(Color.parseColor("#B2EBF2"));
        scorePaint.setTextSize(34f);
        scorePaint.setTextAlign(Paint.Align.CENTER);

        cardUnlocked = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardUnlocked.setColor(Color.parseColor("#2E7D32"));

        cardLocked = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardLocked.setColor(Color.parseColor("#37474F"));

        cardNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardNumPaint.setColor(Color.WHITE);
        cardNumPaint.setTextSize(54f);
        cardNumPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        cardNumPaint.setTextAlign(Paint.Align.CENTER);

        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setColor(Color.parseColor("#FDD835"));
        starPaint.setTextSize(26f);
        starPaint.setTextAlign(Paint.Align.CENTER);

        lockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lockPaint.setColor(Color.parseColor("#90A4AE"));
        lockPaint.setTextSize(42f);
        lockPaint.setTextAlign(Paint.Align.CENTER);

        sectionTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectionTitlePaint.setColor(Color.WHITE);
        sectionTitlePaint.setTextSize(36f);
        sectionTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        sectionTitlePaint.setTextAlign(Paint.Align.CENTER);

        footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        footerPaint.setColor(Color.argb(160, 255, 255, 255));
        footerPaint.setTextSize(28f);
        footerPaint.setTextAlign(Paint.Align.CENTER);

        nameBtnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nameBtnPaint.setColor(Color.parseColor("#1565C0"));

        nameBtnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nameBtnTextPaint.setColor(Color.WHITE);
        nameBtnTextPaint.setTextSize(28f);
        nameBtnTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override // Override = replace the parent View's onDraw with our own
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();

        // ── Background gradient ───────────────────────────────────────────
        LinearGradient bg = new LinearGradient(0,0,0,h,
                Color.parseColor("#0D1B2A"), Color.parseColor("#1B2A4A"),
                Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0,0,w,h, bgPaint);

        drawStars(canvas, w, h);           // decorative background stars
        drawTitle(canvas, w, h);
        drawPlayerCard(canvas, w, h);
        drawLevelSection(canvas, w, h);
        canvas.drawText("Tap a level to play!", w/2f, h*0.93f, footerPaint);
    }

    private void drawStars(Canvas canvas, int w, int h) {
        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setColor(Color.argb(100, 255, 255, 255));
        // Hard-coded star positions (decorative dots)
        int[][] pos = {{60,40},{200,90},{380,60},{580,30},{w-80,70},
                {120,180},{350,220},{w-150,200},{80,350},{w-50,300}};
        for (int[] p : pos) canvas.drawCircle(p[0], p[1], 3f, sp);
    }

    private void drawTitle(Canvas canvas, int w, int h) {
        canvas.drawText("MINI ANGRY BIRDS", w/2f, h*0.11f, titlePaint);

        // Underline decoration
        Paint line = new Paint(); line.setColor(Color.parseColor("#FDD835")); line.setStrokeWidth(3f);
        canvas.drawLine(w*0.25f, h*0.13f, w*0.75f, h*0.13f, line);
    }

    private void drawPlayerCard(Canvas canvas, int w, int h) {
        // Card background
        Paint card = new Paint(Paint.ANTI_ALIAS_FLAG);
        card.setColor(Color.argb(80, 255, 255, 255));
        RectF cardRect = new RectF(w*0.08f, h*0.16f, w*0.92f, h*0.36f);
        canvas.drawRoundRect(cardRect, 30, 30, card);

        // Bird avatar
        float ax = w*0.20f, ay = h*0.255f;
        drawMiniBird(canvas, ax, ay, 48f);

        // Name
        canvas.drawText(PlayerData.playerName, w*0.57f, h*0.225f, namePaint);

        // Score
        canvas.drawText("Score: " + PlayerData.totalScore, w*0.57f, h*0.268f, scorePaint);

        // Progress bar
        float barLeft = w*0.32f, barRight = w*0.88f;
        float barY = h*0.305f;
        Paint barBg = new Paint(Paint.ANTI_ALIAS_FLAG); barBg.setColor(Color.argb(80,0,0,0));
        canvas.drawRoundRect(barLeft, barY, barRight, barY+16, 8,8, barBg);
        float progress = (float) PlayerData.maxLevelUnlocked / 5f;
        Paint barFg = new Paint(Paint.ANTI_ALIAS_FLAG); barFg.setColor(Color.parseColor("#66BB6A"));
        canvas.drawRoundRect(barLeft, barY, barLeft+(barRight-barLeft)*progress, barY+16, 8,8, barFg);

        Paint prog = new Paint(Paint.ANTI_ALIAS_FLAG);
        prog.setColor(Color.argb(180,255,255,255)); prog.setTextSize(22f);
        canvas.drawText("Level " + PlayerData.maxLevelUnlocked + " / 5", w*0.57f, h*0.340f, scorePaint);

        // Change name button
        changeNameBtn.set(w*0.76f, h*0.175f, w*0.91f, h*0.225f);
        canvas.drawRoundRect(changeNameBtn, 12, 12, nameBtnPaint);
        canvas.drawText("✏ Name", w*0.835f, h*0.207f, nameBtnTextPaint);
    }

    private void drawMiniBird(Canvas canvas, float cx, float cy, float r) {
        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG); body.setColor(Color.parseColor("#EF5350"));
        canvas.drawCircle(cx, cy, r, body);
        Paint eye = new Paint(Paint.ANTI_ALIAS_FLAG); eye.setColor(Color.WHITE);
        canvas.drawCircle(cx-r*.28f, cy-r*.1f, r*.22f, eye);
        canvas.drawCircle(cx+r*.28f, cy-r*.1f, r*.22f, eye);
        Paint pupil = new Paint(Paint.ANTI_ALIAS_FLAG); pupil.setColor(Color.BLACK);
        canvas.drawCircle(cx-r*.25f, cy-r*.08f, r*.1f, pupil);
        canvas.drawCircle(cx+r*.31f, cy-r*.08f, r*.1f, pupil);
        Paint brow = new Paint(Paint.ANTI_ALIAS_FLAG); brow.setColor(Color.parseColor("#212121"));
        brow.setStrokeWidth(4f); brow.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(cx-r*.55f, cy-r*.4f, cx-r*.1f, cy-r*.15f, brow);
        canvas.drawLine(cx+r*.55f, cy-r*.4f, cx+r*.1f, cy-r*.15f, brow);
    }

    private void drawLevelSection(Canvas canvas, int w, int h) {
        canvas.drawText("— SELECT LEVEL —", w/2f, h*0.43f, sectionTitlePaint);

        float cardW = w * 0.155f;
        float cardH = h * 0.22f;
        float gap   = w * 0.035f;
        float totalW = 5*cardW + 4*gap;
        float startX = (w - totalW) / 2f;
        float cardY  = h * 0.47f;

        for (int i = 0; i < 5; i++) {
            float left = startX + i*(cardW+gap);
            levelCards[i].set(left, cardY, left+cardW, cardY+cardH);
            drawLevelCard(canvas, i+1, levelCards[i]);
        }
    }

    private void drawLevelCard(Canvas canvas, int level, RectF card) {
        boolean unlocked = (level <= PlayerData.maxLevelUnlocked);
        int stars = PlayerData.levelStars[level]; // 0-3

        // Shadow
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setColor(Color.argb(70,0,0,0));
        canvas.drawRoundRect(card.left+5, card.top+5, card.right+5, card.bottom+5, 18,18, shadow);

        // Card body
        if (unlocked) {
            // Change color based on completion
            if (stars == 3)      cardUnlocked.setColor(Color.parseColor("#F57F17")); // gold
            else if (stars > 0)  cardUnlocked.setColor(Color.parseColor("#2E7D32")); // green
            else                 cardUnlocked.setColor(Color.parseColor("#1565C0")); // blue (not tried)
            canvas.drawRoundRect(card, 18, 18, cardUnlocked);
        } else {
            canvas.drawRoundRect(card, 18, 18, cardLocked);
        }

        float cx = card.centerX(), cy = card.centerY();

        if (unlocked) {
            // "LEVEL" small label
            Paint lbl = new Paint(Paint.ANTI_ALIAS_FLAG);
            lbl.setColor(Color.argb(200,255,255,255)); lbl.setTextSize(20f);
            lbl.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("LEVEL", cx, card.top+26, lbl);

            // Level number (big)
            canvas.drawText("" + level, cx, cy+16, cardNumPaint);

            // Stars row at bottom
            StringBuilder starStr = new StringBuilder();
            for (int s=0; s<3; s++) starStr.append(s < stars ? "★" : "☆");
            canvas.drawText(starStr.toString(), cx, card.bottom-14, starPaint);
        } else {
            // Locked — show padlock emoji
            canvas.drawText("🔒", cx, cy+16, lockPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            float tx = ev.getX(), ty = ev.getY();

            // Check change name button
            if (changeNameBtn.contains(tx, ty)) {
                showChangeNameDialog();
                return true;
            }

            // Check level cards
            for (int i = 0; i < 5; i++) {
                if (levelCards[i].contains(tx, ty)) {
                    int level = i + 1;
                    if (level <= PlayerData.maxLevelUnlocked) {
                        // Create an Intent = a "message" to start another Activity
                        Intent intent = new Intent(activity, GameActivity.class);
                        // putExtra = attach data to the message
                        intent.putExtra(GameActivity.EXTRA_LEVEL, level);
                        activity.startActivity(intent);
                    }
                    return true;
                }
            }
        }
        return true;
    }

    private void showChangeNameDialog() {
        android.widget.EditText input = new android.widget.EditText(activity);
        input.setHint("New name");
        input.setText(PlayerData.playerName);
        new android.app.AlertDialog.Builder(activity)
                .setTitle("Change Name")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String n = input.getText().toString().trim();
                    if (!n.isEmpty()) PlayerData.playerName = n;
                    invalidate(); // redraw the view
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Called when Activity resumes (back from game) — refresh the display
    public void refresh() { invalidate(); }
}