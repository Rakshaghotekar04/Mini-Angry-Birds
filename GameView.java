package com.example.miniangrybirds;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;
public class GameView extends View {

    // ── INTERFACE — a contract that GameActivity must follow ─────────────────
    // Any class that "implements GameListener" MUST write these two methods
    public interface GameListener {
        void onLevelComplete(int level, int stars, int score);
        void onBackToMenu();
    }

    // ── Constants (static final = never changes, one copy for whole class) ───
    private static final float SLING_X        = 220f;
    private static final float SLING_REST_X   = SLING_X;
    private static final float SLING_REST_Y   = 540f;
    private static final float MAX_PULL       = 220f;
    private static final float GRAVITY        = 0.6f;
    private static final int   EXPLODE_FRAMES = 20;
    private static final int   TOTAL_LEVELS   = 5;

    // ── Instance fields (each GameView object has its OWN copy of these) ─────
    private float   ballX, ballY, velX, velY, groundY;
    private boolean dragging = false, launched = false, ballActive = true;

    private int currentLevel;   // which level we're on
    private int waitingBirds;   // total shots this level
    private int birdsUsed  = 0; // shots taken
    private int score      = 0;

    // The listener (GameActivity object) — stored as the interface type
    private final GameListener listener;

    private enum GameState { PLAYING, LEVEL_WIN, GAME_WIN }
    private GameState gameState = GameState.PLAYING;

    // ── INNER CLASS — Bird (static = doesn't need outer class instance) ──────
    // This is a CLASS inside a CLASS
    private static class Bird {
        float x, y, radius;
        boolean alive = true, exploding = false;
        int explodeFrame = 0;

        // Constructor
        Bird(float x, float y, float r) { this.x=x; this.y=y; this.radius=r; }
    }

    // ArrayLists — resizable lists of objects
    private final List<Bird>    birds      = new ArrayList<>();
    private final List<float[]> trajectory = new ArrayList<>();
    private final List<float[]> trail      = new ArrayList<>();

    // ── Paints ────────────────────────────────────────────────────────────────
    private Paint skyPaint, groundPaint, slingStemPaint, slingBandPaint;
    private Paint ballPaint, ballHighlightPaint, trailPaint;
    private Paint birdBodyPaint, birdEyePaint, birdPupilPaint, birdBrowPaint, birdBeakPaint;
    private Paint explodePaint, dotPaint, platformPaint;
    private Paint winPaint, infoTextPaint, scorePaint, levelPaint, hudBgPaint;
    private Paint starPaint, starEmptyPaint, btnPaint, btnTextPaint, menuBtnPaint;

    private RectF nextBtnRect = new RectF();
    private RectF menuBtnRect = new RectF();

    // ── Game Loop ─────────────────────────────────────────────────────────────
    private Choreographer choreographer;
    private long lastFrameNanos = 0;

    // Anonymous class implementing Choreographer.FrameCallback interface
    private final Choreographer.FrameCallback frameCallback =
            new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    float dt = lastFrameNanos == 0 ? 1f
                            : (frameTimeNanos - lastFrameNanos) / 16_666_666f;
                    dt = Math.min(dt, 2f);
                    lastFrameNanos = frameTimeNanos;
                    update(dt);
                    invalidate();
                    choreographer.postFrameCallback(this);
                }
            };

    // ── CONSTRUCTORS ─────────────────────────────────────────────────────────
    // Constructor 1: used when creating from code with level + listener
    public GameView(Context context, int startLevel, GameListener listener) {
        super(context);
        this.currentLevel = startLevel;
        this.listener     = listener;
        init();
    }

    // Constructor 2: fallback (XML inflation etc.)
    public GameView(Context context, android.util.AttributeSet a) {
        super(context, a);
        this.currentLevel = 1;
        this.listener     = null;
        init();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void init() {
        skyPaint = new Paint();

        groundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        groundPaint.setColor(Color.parseColor("#4CAF50"));

        slingStemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        slingStemPaint.setColor(Color.parseColor("#5D4037"));
        slingStemPaint.setStrokeWidth(14f);
        slingStemPaint.setStyle(Paint.Style.STROKE);
        slingStemPaint.setStrokeCap(Paint.Cap.ROUND);

        slingBandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        slingBandPaint.setColor(Color.parseColor("#795548"));
        slingBandPaint.setStrokeWidth(7f);
        slingBandPaint.setStyle(Paint.Style.STROKE);

        ballPaint          = new Paint(Paint.ANTI_ALIAS_FLAG);
        ballHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ballHighlightPaint.setColor(Color.argb(120,255,255,255));

        trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        birdBodyPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        birdEyePaint   = new Paint(Paint.ANTI_ALIAS_FLAG); birdEyePaint.setColor(Color.WHITE);
        birdPupilPaint = new Paint(Paint.ANTI_ALIAS_FLAG); birdPupilPaint.setColor(Color.BLACK);
        birdBrowPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        birdBrowPaint.setColor(Color.parseColor("#212121"));
        birdBrowPaint.setStrokeWidth(5f); birdBrowPaint.setStrokeCap(Paint.Cap.ROUND);
        birdBeakPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        birdBeakPaint.setColor(Color.parseColor("#FFB300"));

        explodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.argb(180,255,255,100));
        platformPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        platformPaint.setColor(Color.parseColor("#8D6E63"));

        winPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        winPaint.setColor(Color.parseColor("#FDD835"));
        winPaint.setTextSize(80f);
        winPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        winPaint.setTextAlign(Paint.Align.CENTER);

        infoTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        infoTextPaint.setColor(Color.WHITE);
        infoTextPaint.setTextSize(36f);
        infoTextPaint.setTextAlign(Paint.Align.CENTER);
        infoTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));

        scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(42f);
        scorePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));

        levelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        levelPaint.setColor(Color.WHITE);
        levelPaint.setTextSize(36f);
        levelPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        levelPaint.setTextAlign(Paint.Align.CENTER);

        hudBgPaint = new Paint();
        hudBgPaint.setColor(Color.argb(130,0,0,0));

        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setColor(Color.parseColor("#FDD835"));
        starPaint.setTextSize(64f); starPaint.setTextAlign(Paint.Align.CENTER);

        starEmptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starEmptyPaint.setColor(Color.parseColor("#777777"));
        starEmptyPaint.setTextSize(64f); starEmptyPaint.setTextAlign(Paint.Align.CENTER);

        btnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnPaint.setColor(Color.parseColor("#2E7D32"));

        btnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnTextPaint.setColor(Color.WHITE);
        btnTextPaint.setTextSize(46f);
        btnTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        btnTextPaint.setTextAlign(Paint.Align.CENTER);

        menuBtnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        menuBtnPaint.setColor(Color.parseColor("#B71C1C"));

        choreographer = Choreographer.getInstance();
        choreographer.postFrameCallback(frameCallback);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        groundY = h * 0.78f;
        loadLevel(currentLevel);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LEVELS — 5 levels with increasing difficulty
    // ─────────────────────────────────────────────────────────────────────────
    private void loadLevel(int level) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        birds.clear(); trail.clear();
        gameState  = GameState.PLAYING;
        birdsUsed  = 0;
        float g = groundY, r = 52f;

        switch (level) {
            case 1: // 3 birds, 3 shots — wide spread, easy
                waitingBirds = 3;
                birds.add(new Bird(w*.50f, g-r,        r));
                birds.add(new Bird(w*.64f, g-r,        r));
                birds.add(new Bird(w*.78f, g-r*1.8f,  r*.85f));
                break;

            case 2: // 4 birds, 4 shots — mixed heights
                waitingBirds = 4;
                birds.add(new Bird(w*.46f, g-r,        r));
                birds.add(new Bird(w*.58f, g-r*1.9f,  r));
                birds.add(new Bird(w*.70f, g-r,        r));
                birds.add(new Bird(w*.82f, g-r*1.9f,  r*.8f));
                break;

            case 3: // 5 birds, 4 shots — tighter cluster, harder
                waitingBirds = 4;
                birds.add(new Bird(w*.45f, g-r,        r));
                birds.add(new Bird(w*.54f, g-r*2.0f,  r));
                birds.add(new Bird(w*.63f, g-r,        r));
                birds.add(new Bird(w*.72f, g-r*2.0f,  r*.85f));
                birds.add(new Bird(w*.85f, g-r,        r*.8f));
                break;

            case 4: // 6 birds, 5 shots — two towers
                waitingBirds = 5;
                birds.add(new Bird(w*.44f, g-r,        r));
                birds.add(new Bird(w*.44f, g-r*2.1f,  r));  // stacked!
                birds.add(new Bird(w*.57f, g-r,        r*.9f));
                birds.add(new Bird(w*.70f, g-r,        r));
                birds.add(new Bird(w*.70f, g-r*2.1f,  r));  // stacked!
                birds.add(new Bird(w*.84f, g-r*1.5f,  r*.8f));
                break;

            case 5: // 7 birds, 5 shots — boss level, spread all over
                waitingBirds = 5;
                birds.add(new Bird(w*.42f, g-r,        r*.9f));
                birds.add(new Bird(w*.50f, g-r*2.2f,  r));
                birds.add(new Bird(w*.58f, g-r,        r));
                birds.add(new Bird(w*.66f, g-r*2.2f,  r*.9f));
                birds.add(new Bird(w*.74f, g-r,        r));
                birds.add(new Bird(w*.74f, g-r*2.2f,  r));  // stacked on 74%
                birds.add(new Bird(w*.86f, g-r*1.6f,  r*.75f));
                break;
        }
        resetBall();
    }

    private void resetBall() {
        ballX=SLING_REST_X; ballY=SLING_REST_Y;
        velX=0; velY=0;
        launched=false; dragging=false; ballActive=true;
        trajectory.clear(); trail.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UPDATE
    // ─────────────────────────────────────────────────────────────────────────
    private void update(float dt) {
        if (gameState != GameState.PLAYING) return;

        for (Bird b : birds) {
            if (b.exploding) {
                b.explodeFrame++;
                if (b.explodeFrame > EXPLODE_FRAMES) { b.exploding=false; b.alive=false; }
            }
        }

        if (!launched || !ballActive) return;

        velY  += GRAVITY*dt;
        ballX += velX*dt;
        ballY += velY*dt;

        trail.add(new float[]{ballX, ballY});
        if (trail.size() > 28) trail.remove(0);

        for (Bird b : birds) {
            if (!b.alive || b.exploding) continue;
            float dx=ballX-b.x, dy=ballY-b.y;
            if (Math.sqrt(dx*dx+dy*dy) < b.radius+28f) {
                b.exploding=true; b.explodeFrame=0;
                score += 500;
                velX*=0.5f; velY*=-0.25f;
            }
        }

        if (ballY>groundY || ballX>getWidth()+100 || ballX<-100) {
            ballActive=false;
            birdsUsed++;
            checkLevelEnd();
        }
    }

    private void checkLevelEnd() {
        boolean allDead = true;
        for (Bird b : birds) if (b.alive || b.exploding) { allDead=false; break; }

        if (allDead) {
            int bonus = (waitingBirds - birdsUsed) * 1000;
            score += bonus;
            int stars = getStars();

            // Tell the listener (GameActivity) level is done
            // Check for null in case listener wasn't provided
            if (listener != null) listener.onLevelComplete(currentLevel, stars, score);

            gameState = (currentLevel >= TOTAL_LEVELS) ? GameState.GAME_WIN : GameState.LEVEL_WIN;

        } else if (birdsUsed >= waitingBirds) {
            // Out of shots — retry
            loadLevel(currentLevel);
        }
    }

    private int getStars() {
        if (birdsUsed <= 1) return 3;
        if (birdsUsed <= 3) return 2;
        return 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DRAW
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        int w=getWidth(), h=getHeight();
        LinearGradient sky = new LinearGradient(0,0,0,groundY,
                Color.parseColor("#1565C0"), Color.parseColor("#42A5F5"),
                Shader.TileMode.CLAMP);
        skyPaint.setShader(sky); canvas.drawRect(0,0,w,groundY,skyPaint);
        drawClouds(canvas,w);
        canvas.drawRect(0,groundY,w,h,groundPaint);
        Paint g2=new Paint(Paint.ANTI_ALIAS_FLAG); g2.setColor(Color.parseColor("#388E3C"));
        canvas.drawRect(0,groundY,w,groundY+18,g2);
        drawPlatforms(canvas);
        drawSlingshot(canvas);
        if (dragging) for (float[] p:trajectory) canvas.drawCircle(p[0],p[1],7f,dotPaint);
        drawTrail(canvas);
        for (Bird b:birds) drawBird(canvas,b);
        if (ballActive) drawBall(canvas,ballX,ballY,28f);
        drawBirdQueue(canvas);
        drawHUD(canvas,w,h);
        if (gameState==GameState.LEVEL_WIN) drawLevelWinScreen(canvas,w,h);
        if (gameState==GameState.GAME_WIN)  drawGameWinScreen(canvas,w,h);
    }

    private void drawClouds(Canvas canvas, int w) {
        Paint cp=new Paint(Paint.ANTI_ALIAS_FLAG); cp.setColor(Color.argb(200,255,255,255));
        float[][] cl={{w*.2f,80,60},{w*.5f,50,45},{w*.78f,100,55}};
        for (float[] c:cl) {
            canvas.drawCircle(c[0],c[1],c[2],cp);
            canvas.drawCircle(c[0]+c[2]*.7f,c[1]+8,c[2]*.7f,cp);
            canvas.drawCircle(c[0]-c[2]*.6f,c[1]+12,c[2]*.6f,cp);
        }
    }

    private void drawPlatforms(Canvas canvas) {
        for (Bird b:birds) {
            float boxTop=b.y+b.radius;
            if (boxTop < groundY-10) {
                float bw=b.radius*1.6f;
                canvas.drawRect(b.x-bw/2,boxTop,b.x+bw/2,groundY,platformPaint);
                Paint gn=new Paint(Paint.ANTI_ALIAS_FLAG);
                gn.setColor(Color.parseColor("#6D4C41")); gn.setStrokeWidth(2f);
                for (float y=boxTop+18; y<groundY; y+=18)
                    canvas.drawLine(b.x-bw/2,y,b.x+bw/2,y,gn);
                canvas.drawLine(b.x,boxTop,b.x,groundY,gn);
            }
        }
    }

    private void drawSlingshot(Canvas canvas) {
        float stemTop=groundY-180f;
        float lx=SLING_X-40f, ly=stemTop-100f;
        float rx=SLING_X+40f, ry=stemTop-100f;
        canvas.drawLine(SLING_X,groundY,SLING_X,stemTop,slingStemPaint);
        canvas.drawLine(SLING_X,stemTop,lx,ly,slingStemPaint);
        canvas.drawLine(SLING_X,stemTop,rx,ry,slingStemPaint);
        // FIXED: bands only connect to ball BEFORE launch, snap back after
        float bx = !launched ? ballX : SLING_REST_X;
        float by = !launched ? ballY : SLING_REST_Y;
        canvas.drawLine(lx,ly,bx,by,slingBandPaint);
        canvas.drawLine(rx,ry,bx,by,slingBandPaint);
    }

    private void drawTrail(Canvas canvas) {
        for (int i=0; i<trail.size(); i++) {
            float[] t=trail.get(i);
            float alpha=(float)i/trail.size();
            trailPaint.setColor(Color.argb((int)(170*alpha),255,100,30));
            canvas.drawCircle(t[0],t[1],7f*alpha,trailPaint);
        }
    }

    private void drawBall(Canvas canvas, float cx, float cy, float r) {
        RadialGradient rg=new RadialGradient(cx-r*.3f,cy-r*.3f,r*1.2f,
                Color.parseColor("#EF9A9A"),Color.parseColor("#B71C1C"),Shader.TileMode.CLAMP);
        ballPaint.setShader(rg);
        canvas.drawCircle(cx,cy,r,ballPaint);
        canvas.drawCircle(cx-r*.28f,cy-r*.28f,r*.35f,ballHighlightPaint);
    }

    private void drawBird(Canvas canvas, Bird b) {
        if (!b.alive&&!b.exploding) return;
        if (b.exploding) { drawExplosion(canvas,b); return; }
        float r=b.radius;
        RadialGradient bg=new RadialGradient(b.x-r*.3f,b.y-r*.3f,r*1.3f,
                Color.parseColor("#EF5350"),Color.parseColor("#B71C1C"),Shader.TileMode.CLAMP);
        birdBodyPaint.setShader(bg);
        canvas.drawCircle(b.x,b.y,r,birdBodyPaint);
        canvas.drawLine(b.x-r*.55f,b.y-r*.4f,b.x-r*.1f,b.y-r*.15f,birdBrowPaint);
        canvas.drawLine(b.x+r*.55f,b.y-r*.4f,b.x+r*.1f,b.y-r*.15f,birdBrowPaint);
        canvas.drawCircle(b.x-r*.28f,b.y-r*.1f,r*.22f,birdEyePaint);
        canvas.drawCircle(b.x+r*.28f,b.y-r*.1f,r*.22f,birdEyePaint);
        canvas.drawCircle(b.x-r*.25f,b.y-r*.08f,r*.11f,birdPupilPaint);
        canvas.drawCircle(b.x+r*.31f,b.y-r*.08f,r*.11f,birdPupilPaint);
        Path beak=new Path();
        beak.moveTo(b.x-r*.15f,b.y+r*.18f);
        beak.lineTo(b.x+r*.15f,b.y+r*.18f);
        beak.lineTo(b.x,b.y+r*.42f);
        beak.close();
        canvas.drawPath(beak,birdBeakPaint);
        Paint tp=new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.parseColor("#C62828")); tp.setStrokeWidth(5f); tp.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(b.x,b.y-r,b.x-8,b.y-r-22,tp);
        canvas.drawLine(b.x,b.y-r,b.x+5,b.y-r-26,tp);
    }

    private void drawExplosion(Canvas canvas, Bird b) {
        float p=(float)b.explodeFrame/EXPLODE_FRAMES, cr=b.radius*3.5f*p;
        int al=(int)(255*(1f-p));
        explodePaint.setStyle(Paint.Style.FILL);
        explodePaint.setColor(Color.argb(al,255,200,0));
        canvas.drawCircle(b.x,b.y,cr,explodePaint);
        explodePaint.setColor(Color.argb(Math.min(255,al+80),255,255,100));
        canvas.drawCircle(b.x,b.y,cr*.5f,explodePaint);
        Paint sp=new Paint(Paint.ANTI_ALIAS_FLAG); sp.setStrokeWidth(5f); sp.setStrokeCap(Paint.Cap.ROUND);
        for (int i=0;i<10;i++) {
            double angle=2*Math.PI*i/10+b.explodeFrame*.15;
            float sx=(float)Math.cos(angle), sy=(float)Math.sin(angle);
            sp.setColor(Color.argb((int)(200*(1f-p)),255,140,0));
            canvas.drawLine(b.x+sx*cr*.5f,b.y+sy*cr*.5f,b.x+sx*cr*1.1f,b.y+sy*cr*1.1f,sp);
        }
    }

    private void drawBirdQueue(Canvas canvas) {
        int remaining = waitingBirds - birdsUsed - (ballActive && !launched ? 1 : 0);
        if (remaining < 0) remaining = 0;
        for (int i=0; i<remaining; i++) {
            float qx=SLING_X-80f-i*52f, qy=groundY-28f;
            Paint qp=new Paint(Paint.ANTI_ALIAS_FLAG); qp.setColor(Color.parseColor("#EF5350"));
            canvas.drawCircle(qx,qy,22f,qp);
            Paint qe=new Paint(Paint.ANTI_ALIAS_FLAG); qe.setColor(Color.WHITE);
            canvas.drawCircle(qx-6,qy-4,7f,qe); canvas.drawCircle(qx+6,qy-4,7f,qe);
            Paint qd=new Paint(Paint.ANTI_ALIAS_FLAG); qd.setColor(Color.BLACK);
            canvas.drawCircle(qx-5,qy-4,3f,qd); canvas.drawCircle(qx+7,qy-4,3f,qd);
        }
    }

    private void drawHUD(Canvas canvas, int w, int h) {
        canvas.drawRect(0,0,w,78,hudBgPaint);
        canvas.drawText("SCORE: "+score, 28, 56, scorePaint);
        canvas.drawText("LEVEL "+currentLevel+" / "+TOTAL_LEVELS, w/2f, 56, levelPaint);
        Paint rp=new Paint(Paint.ANTI_ALIAS_FLAG); rp.setColor(Color.WHITE); rp.setTextSize(34f); rp.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Shots: "+(waitingBirds-birdsUsed), w-16, 52, rp);
        // Menu button top-right
        menuBtnRect.set(w-150f, 6f, w-6f, 72f);
        canvas.drawRoundRect(menuBtnRect, 12,12, menuBtnPaint);
        Paint mp=new Paint(Paint.ANTI_ALIAS_FLAG); mp.setColor(Color.WHITE); mp.setTextSize(28f); mp.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("☰ MENU", w-78f, 48f, mp);
        if (gameState==GameState.PLAYING) {
            if (!launched&&!dragging) canvas.drawText("Drag the ball to shoot!", w/2f, groundY+60, infoTextPaint);
            else if (!ballActive&&launched) canvas.drawText("Tap to shoot next bird", w/2f, groundY+60, infoTextPaint);
        }
    }

    private void drawLevelWinScreen(Canvas canvas, int w, int h) {
        Paint ov=new Paint(); ov.setColor(Color.argb(170,0,0,0));
        canvas.drawRect(0,0,w,h,ov);
        Paint panel=new Paint(Paint.ANTI_ALIAS_FLAG); panel.setColor(Color.parseColor("#1B5E20"));
        canvas.drawRoundRect(w*.2f,h*.12f,w*.8f,h*.88f,40,40,panel);
        canvas.drawText("LEVEL CLEAR!", w/2f, h*.28f, winPaint);
        int stars=getStars();
        float[] sx={w*.32f,w*.50f,w*.68f};
        for (int i=0;i<3;i++) canvas.drawText("★", sx[i], h*.46f, i<stars?starPaint:starEmptyPaint);
        canvas.drawText("Score: "+score, w/2f, h*.58f, infoTextPaint);
        canvas.drawText("Bonus: +"+(waitingBirds-birdsUsed)*1000, w/2f, h*.65f, infoTextPaint);
        nextBtnRect.set(w*.28f,h*.70f,w*.72f,h*.82f);
        canvas.drawRoundRect(nextBtnRect,20,20,btnPaint);
        canvas.drawText(currentLevel<TOTAL_LEVELS?"NEXT LEVEL ▶":"FINISH", w/2f, h*.766f, btnTextPaint);
        menuBtnRect.set(w*.28f,h*.84f,w*.72f,h*.92f);
        canvas.drawRoundRect(menuBtnRect,20,20,menuBtnPaint);
        canvas.drawText("◀ MENU", w/2f, h*.892f, btnTextPaint);
    }

    private void drawGameWinScreen(Canvas canvas, int w, int h) {
        Paint ov=new Paint(); ov.setColor(Color.argb(180,0,0,0));
        canvas.drawRect(0,0,w,h,ov);
        Paint panel=new Paint(Paint.ANTI_ALIAS_FLAG); panel.setColor(Color.parseColor("#4A148C"));
        canvas.drawRoundRect(w*.15f,h*.08f,w*.85f,h*.92f,40,40,panel);
        winPaint.setColor(Color.parseColor("#FFD600"));
        canvas.drawText("YOU WIN! 🎉", w/2f, h*.24f, winPaint);
        canvas.drawText("All 5 Levels Done!", w/2f, h*.36f, infoTextPaint);
        canvas.drawText("Final Score: "+score, w/2f, h*.47f, infoTextPaint);
        float[] sx={w*.32f,w*.50f,w*.68f};
        for (float x:sx) canvas.drawText("★",x,h*.60f,starPaint);
        nextBtnRect.set(w*.28f,h*.67f,w*.72f,h*.79f);
        canvas.drawRoundRect(nextBtnRect,20,20,btnPaint);
        canvas.drawText("REPLAY ALL", w/2f, h*.748f, btnTextPaint);
        menuBtnRect.set(w*.28f,h*.81f,w*.72f,h*.90f);
        canvas.drawRoundRect(menuBtnRect,20,20,menuBtnPaint);
        canvas.drawText("◀ MENU", w/2f, h*.870f, btnTextPaint);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TOUCH
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float tx=ev.getX(), ty=ev.getY();

        if (ev.getAction()==MotionEvent.ACTION_UP) {

            // Menu button (always active)
            if (menuBtnRect.contains(tx,ty)) {
                if (listener!=null) listener.onBackToMenu();
                return true;
            }

            // Win screen buttons
            if (gameState==GameState.LEVEL_WIN || gameState==GameState.GAME_WIN) {
                if (nextBtnRect.contains(tx,ty)) {
                    if (gameState==GameState.GAME_WIN) {
                        currentLevel=1; score=0;
                        loadLevel(1);
                    } else {
                        currentLevel++;
                        score=0;
                        loadLevel(currentLevel);
                    }
                    return true;
                }
            }
        }

        if (gameState!=GameState.PLAYING) return true;

        // Tap to load next ball
        if (!ballActive&&launched) { if (ev.getAction()==MotionEvent.ACTION_UP) resetBall(); return true; }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!launched&&dist(tx,ty,ballX,ballY)<80f) dragging=true; break;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    float dx=tx-SLING_REST_X, dy=ty-SLING_REST_Y;
                    float d=(float)Math.sqrt(dx*dx+dy*dy);
                    if (d>MAX_PULL){dx=dx/d*MAX_PULL;dy=dy/d*MAX_PULL;}
                    ballX=SLING_REST_X+dx; ballY=SLING_REST_Y+dy;
                    computeTrajectory();
                } break;
            case MotionEvent.ACTION_UP:
                if (dragging){dragging=false;launch();} break;
        }
        return true;
    }

    private void launch() { velX=(SLING_REST_X-ballX)*.38f; velY=(SLING_REST_Y-ballY)*.38f; launched=true; trajectory.clear(); }

    private void computeTrajectory() {
        trajectory.clear();
        float vx=(SLING_REST_X-ballX)*.38f, vy=(SLING_REST_Y-ballY)*.38f, px=ballX, py=ballY;
        for (int i=0;i<35;i++) { vy+=GRAVITY; px+=vx; py+=vy; if(py>groundY)break; if(i%2==0)trajectory.add(new float[]{px,py}); }
    }

    private float dist(float x1,float y1,float x2,float y2) { float dx=x1-x2,dy=y1-y2; return (float)Math.sqrt(dx*dx+dy*dy); }
}