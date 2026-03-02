    package com.ric.quizrpg;

    import com.badlogic.gdx.Gdx;
    import com.badlogic.gdx.Preferences;
    import com.badlogic.gdx.Screen;
    import com.badlogic.gdx.graphics.Color;
    import com.badlogic.gdx.graphics.GL20;
    import com.badlogic.gdx.graphics.OrthographicCamera;
    import com.badlogic.gdx.graphics.Pixmap;
    import com.badlogic.gdx.graphics.Texture;
    import com.badlogic.gdx.graphics.g2d.Animation;
    import com.badlogic.gdx.graphics.g2d.BitmapFont;
    import com.badlogic.gdx.graphics.g2d.GlyphLayout;
    import com.badlogic.gdx.graphics.g2d.SpriteBatch;
    import com.badlogic.gdx.graphics.g2d.TextureRegion;
    import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
    import com.badlogic.gdx.math.MathUtils;
    import com.badlogic.gdx.math.Rectangle;
    import com.badlogic.gdx.math.Vector3;
    import com.badlogic.gdx.utils.Array;
    import com.badlogic.gdx.utils.viewport.FitViewport;
    import com.badlogic.gdx.utils.viewport.Viewport;

    public class GameScreen implements Screen {

        final Main game;
        SpriteBatch batch;
        BitmapFont font;

        // --- GAME STATES ---
        public enum GameState { PLAYING, PAUSED, SETTINGS, LEVEL_COMPLETE }
        GameState currentGameState = GameState.PLAYING;

        // --- TIMER SYSTEM ---
        float levelTimer = 0;
        String timeString = "00:00";
        GlyphLayout glyphLayout = new GlyphLayout(); // Helps center text

        // --- LEVEL CLEAR GUI ---
        Texture levelClearTex;
        Rectangle levelClearRect;
        // Touch zones for the buttons on the image
        Rectangle btnMenuRect, btnRetryRect, btnNextRect;

        // --- ANIMATION VARIABLES ---
        public enum State { IDLE, WALKING, JUMPING }
        State currentState;
        State previousState;

        Texture idleSheet, walkSheet, jumpSheet;
        Animation<TextureRegion> idleAnimation;
        Animation<TextureRegion> jumpAnimation;
        TextureRegion walkStartFrame;
        Animation<TextureRegion> walkLoopAnimation;

        float stateTimer;
        boolean runningRight;

        // --- VISUALS ---
        Texture backgroundTex;
        Texture groundTexture;
        TextureRegion groundRegion;
        final float groundOffsetY = -35f;

        // --- OBSTACLES ---
        Texture spikeTex, platformTex;
        Array<Rectangle> spikes;
        Array<Rectangle> platforms;
        Array<Float> platformOrigins;

        float platformTimer = 0;
        final float PLATFORM_RANGE = 100f;
        final float PLATFORM_SPEED = 2f;

        // --- CONTROLS ---
        Texture leftBtnTex, leftBtnPressedTex;
        Texture rightBtnTex, rightBtnPressedTex;
        Texture jumpBtnTex, jumpBtnPressedTex;

        // --- PAUSE HUD ---
        Texture pauseSheet;
        TextureRegion pauseBtnNormal, pauseBtnPressed;
        Rectangle pauseBtnRect;

        // --- PAUSE MENU OVERLAY ASSETS ---
        Texture dimTexture;
        Texture resumeSheet;
        TextureRegion resumeNormal, resumePressed;
        Texture settingsBtnTex, settingsBtnPressedTex;
        Texture exitBtnTex, exitBtnPressedTex;

        Rectangle pmResumeBtn, pmSettingsBtn, pmExitBtn;

        // --- SETTINGS OVERLAY ASSETS ---
        Texture settingsWindowTex;
        Texture plusBtnTex, plusBtnPressedTex;
        Texture minusBtnTex, minusBtnPressedTex;
        Texture closeBtnTex, closeBtnPressedTex;

        Rectangle settingsWindowRect;
        Rectangle settingsCloseBtn;
        Rectangle musicMinusBtn, musicPlusBtn;
        Rectangle sfxMinusBtn, sfxPlusBtn;

        Preferences prefs;

        // --- PHYSICS ---
        Rectangle player;
        Rectangle ground;
        float velocityY = 0;
        final float GRAVITY = -1150f;
        final float MOVE_SPEED = 200f;
        final float JUMP_FORCE = 450f;
        boolean onGround = false;

        Rectangle leftBtn, rightBtn, jumpBtn;
        boolean moveLeft, moveRight, jumpPressed;

        static final float V_WIDTH = 480;
        static final float V_HEIGHT = 270;
        static float LEVEL_WIDTH = 1200f;
        static final float LEVEL_HEIGHT = 600f;

        OrthographicCamera camera;
        Viewport viewport;
        OrthographicCamera hudCamera;
        Viewport hudViewport;

        Texture flagTex;
        Rectangle goalFlag;
        float landingTimer = 0;
        Array<Rectangle> staticPlatforms;

        private float accumulator = 0;
        private final float TIME_STEP = 1/60f; // 60 updates per second
        // Input tracking
        enum PressedUi { NONE, RESUME, PM_SETTINGS, PM_EXIT, VOL_M_MINUS, VOL_M_PLUS, VOL_S_MINUS, VOL_S_PLUS, CLOSE_SETTINGS, BTN_MENU, BTN_RETRY, BTN_NEXT }
        PressedUi pressedUi = PressedUi.NONE;
        boolean hasPlayedWinSound = false;
        com.badlogic.gdx.audio.Sound winSound;
        private com.badlogic.gdx.audio.Sound jumpSound;
        private Screen nextScreen = null;
        protected boolean hasKey = false;
        private com.badlogic.gdx.audio.Sound deathSound;

        Texture goalFlagSheet;
        Animation<TextureRegion> goalFlagAnimation;
        float flagStateTimer = 0;
        private TextureRegion bgRegion;

        protected float bgZoomScale = 2.45f;    // Default zoom
        protected float bgVerticalShift = 1f;

        Rectangle leftHitbox, rightHitbox, jumpHitbox;
        Texture levelLabelTex;


        public GameScreen(Main game) {
            this.game = game;
            batch = new SpriteBatch();
            font = new BitmapFont();
            prefs = Gdx.app.getPreferences("QuizRPG_Settings");
            winSound = Gdx.audio.newSound(Gdx.files.internal("level_complete.ogg"));
            jumpSound = Gdx.audio.newSound(Gdx.files.internal("jump.ogg"));
            deathSound = Gdx.audio.newSound(Gdx.files.internal("respawn .ogg"));

            spikes = new Array<>();
            platforms = new Array<>();
            platformOrigins = new Array<>();
            staticPlatforms = new Array<>();

            // Level Setup
            groundTexture = new Texture("ground1.png");
            groundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            groundRegion = new TextureRegion(groundTexture);

            ground = new Rectangle(0, groundOffsetY, LEVEL_WIDTH, groundRegion.getRegionHeight());

            // 1. SETUP ASSETS
            backgroundTex = new Texture("map2_bg.png");
            backgroundTex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
            bgRegion = new TextureRegion(backgroundTex);
            bgRegion.flip(false, true);
            idleSheet = new Texture("char_idle_sprite.png");
            walkSheet = new Texture("char_walk_sprite.png");
            jumpSheet = new Texture("char_jump_sprite2.png");
            levelLabelTex = new Texture("level2.png");

            // Obstacles
            spikeTex = new Texture("spike1.png");
            spikeTex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
            platformTex = new Texture("ground1.png");
            goalFlagSheet = new Texture("goalFlagSprite.png");
            TextureRegion[][] tmpFlag = TextureRegion.split(goalFlagSheet, goalFlagSheet.getWidth() / 5, goalFlagSheet.getHeight());
            goalFlagAnimation = new Animation<>(0.15f, tmpFlag[0]);
            goalFlagAnimation.setPlayMode(Animation.PlayMode.LOOP);


            // Center the GUI window
            float guiW = 200;
            float guiH = 240; // Approx based on your image ratio
            float guiX = (V_WIDTH - guiW) / 2;
            float guiY = (V_HEIGHT - guiH) / 2;
            levelClearRect = new Rectangle(guiX, guiY, guiW, guiH);

            // Define Invisible Touch Zones relative to the GUI bottom
            // Adjust these numbers to match the buttons on your specific image
            float btnY = guiY + 25; // Height from bottom of GUI
            float btnSize = 40;

            btnMenuRect  = new Rectangle(guiX + 20, btnY, btnSize, btnSize);  // Left
            btnRetryRect = new Rectangle(guiX + 80, btnY, btnSize, btnSize);  // Center
            btnNextRect  = new Rectangle(guiX + 140, btnY, btnSize, btnSize); // Right


            float btnSizeC = 40;
            leftBtn  = new Rectangle(30, 20, btnSizeC, btnSizeC);
            // Hitbox (Physics/Touch) - Expanded by 20px on all sides
            float padding = 20;
            leftHitbox = new Rectangle(
                leftBtn.x - padding,
                leftBtn.y - padding,
                leftBtn.width + (padding * 2),
                leftBtn.height + (padding * 2)
            );
            rightBtn = new Rectangle(90, 20, btnSizeC, btnSizeC);
            rightHitbox = new Rectangle(
                rightBtn.x - padding,
                rightBtn.y - padding,
                leftBtn.width + (padding * 2),
                leftBtn.height + (padding * 2)
            );
            jumpBtn  = new Rectangle(V_WIDTH - 84, 20, btnSizeC, btnSizeC);
            jumpHitbox = new Rectangle(
                jumpBtn.x - padding,
                jumpBtn.y - padding,
                leftBtn.width + (padding * 2),
                leftBtn.height + (padding * 2)
            );

            // Pause Assets
            pauseSheet = new Texture("pause_btn_sprite.png");
            TextureRegion[][] tmpPause = TextureRegion.split(pauseSheet, pauseSheet.getWidth() / 2, pauseSheet.getHeight());
            pauseBtnNormal = tmpPause[0][0];
            pauseBtnPressed = tmpPause[0][1];
            pauseBtnRect = new Rectangle(-7, V_HEIGHT - 40, 50, 50);

            Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pix.setColor(0, 0, 0, 0.7f);
            pix.fill();
            dimTexture = new Texture(pix);
            pix.dispose();

            // Inside public GameScreen(Main game)
            resumeSheet = new Texture("resumebtnSprite.png");
            TextureRegion[][] tmpResume = TextureRegion.split(resumeSheet, resumeSheet.getWidth() / 2, resumeSheet.getHeight());
            resumeNormal = tmpResume[0][0]; // The standard button
            resumePressed = tmpResume[0][1]; // The version with blue sparkles

            settingsBtnTex = new Texture("settingsbtn.png");
            settingsBtnPressedTex = new Texture("settingsbtn_pressed.png");
            exitBtnTex = new Texture("exitbtn.png");
            exitBtnPressedTex = new Texture("exitbtn_pressed.png");

            settingsWindowTex = new Texture("settings_window.png");
            plusBtnTex = new Texture("volUp.png");
            plusBtnPressedTex = new Texture("volup_pressed.png");
            minusBtnTex = new Texture("volDown.png");
            minusBtnPressedTex = new Texture("volDown_pressed.png");
            closeBtnTex = new Texture("settings_exit.png");
            closeBtnPressedTex = new Texture("settings_exit_pressed.png");

            // Controls
            leftBtnTex = new Texture("left_btn.png");
            leftBtnPressedTex = new Texture("leftbtn_pressed.png");
            rightBtnTex = new Texture("right_btn.png");
            rightBtnPressedTex = new Texture("rightbtn_pressed.png");
            jumpBtnTex = new Texture("jump_btn.png");
            jumpBtnPressedTex = new Texture("jumpbtn_pressed.png");

            // --- LEVEL CLEAR SETUP ---
            levelClearTex = new Texture("levelcleargui1.png"); // Your new image

            player = new Rectangle(60, V_HEIGHT / 2f, 32, 48);
            goalFlag = new Rectangle(0, 0, 48, 64);

            // Set filters
            idleSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            walkSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            jumpSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

            // Animation Setup...
            TextureRegion[][] tmpIdle = TextureRegion.split(idleSheet, idleSheet.getWidth() / 3, idleSheet.getHeight());
            idleAnimation = new Animation<>(0.15f, tmpIdle[0]);
            idleAnimation.setPlayMode(Animation.PlayMode.LOOP);

            TextureRegion[][] tmpJump = TextureRegion.split(jumpSheet, jumpSheet.getWidth() / 4, jumpSheet.getHeight());
            jumpAnimation = new Animation<>(0.1f, tmpJump[0]);
            jumpAnimation.setPlayMode(Animation.PlayMode.LOOP);

            TextureRegion[][] tmpWalk = TextureRegion.split(walkSheet, walkSheet.getWidth() / 7, walkSheet.getHeight());
            TextureRegion[] allWalkFrames = tmpWalk[0];
            walkStartFrame = allWalkFrames[0];
            TextureRegion[] loopFrames = new TextureRegion[allWalkFrames.length - 1];
            System.arraycopy(allWalkFrames, 1, loopFrames, 0, loopFrames.length);
            walkLoopAnimation = new Animation<>(0.1f, loopFrames);
            walkLoopAnimation.setPlayMode(Animation.PlayMode.LOOP);

            stateTimer = 0;
            runningRight = true;
            currentState = State.IDLE;
            previousState = State.IDLE;


            // Layout Pause Menu
            float pmBtnW = 70; float pmBtnH = 35; float pmSpacing = 10;
            float pmX = (V_WIDTH - pmBtnW) / 2;
            float pmStartY = V_HEIGHT / 2 - 40; // Lowered buttons

            // Update the rectangle to match the wider aspect ratio of the new button if needed
            pmResumeBtn = new Rectangle(pmX - 5, pmStartY + (pmBtnH + pmSpacing) * 2, pmBtnW + 10, pmBtnH);
            pmSettingsBtn = new Rectangle(pmX, pmStartY + (pmBtnH + pmSpacing), pmBtnW, pmBtnH);
            pmExitBtn = new Rectangle(pmX, pmStartY, pmBtnW, pmBtnH);

            float winW = 300; float winH = 250;
            settingsWindowRect = new Rectangle((V_WIDTH - winW)/2, (V_HEIGHT - winH)/2, winW, winH);
            settingsCloseBtn = new Rectangle(settingsWindowRect.x + winW - 30, settingsWindowRect.y + winH - 30, 20, 20);

            float controlX = settingsWindowRect.x + 150;
            musicMinusBtn = new Rectangle(controlX + 21, settingsWindowRect.y + 113, 20, 20);
            musicPlusBtn = new Rectangle(controlX + 71, settingsWindowRect.y + 113, 20, 20);
            sfxMinusBtn = new Rectangle(controlX + 21, settingsWindowRect.y + 80, 20, 20);
            sfxPlusBtn = new Rectangle(controlX + 71, settingsWindowRect.y + 80, 20, 20);

            camera = new OrthographicCamera();
            viewport = new FitViewport(V_WIDTH, V_HEIGHT, camera);
            hudCamera = new OrthographicCamera();
            hudViewport = new FitViewport(V_WIDTH, V_HEIGHT, hudCamera);
            setupLevelLayout();
        }

        protected void setupLevelLayout() {
            float gHeight = groundRegion.getRegionHeight();
            float collisionOffset = 4f;
            staticPlatforms.add(new Rectangle(0, groundOffsetY + collisionOffset, 350, gHeight - (collisionOffset * 2)));

            // Level Geometry
            //spikes.add(new Rectangle(350, ground.y + ground.height - 5, 500, 32));
            createPlatform(700, 350);
            staticPlatforms.add(new Rectangle(200, ground.y + ground.height + 50, 64, 16));
            staticPlatforms.add(new Rectangle(350, ground.y + ground.height + 105, 64, 16));
            staticPlatforms.add(new Rectangle(600, ground.y + ground.height + 130, 64, 16));
            staticPlatforms.add(new Rectangle(845, ground.y + ground.height + 160, 64, 16));
            staticPlatforms.add(new Rectangle(800, ground.y + ground.height + 225, 64, 16));
            createPlatform(600, ground.y + ground.height + 60);
            createPlatform(750, ground.y + ground.height + 90);
            createPlatform(1000, ground.y + ground.height + 320);
            staticPlatforms.add(new Rectangle(1150, ground.y + ground.height + 340, 64, 16));

            goalFlag = new Rectangle(1150, ground.y + ground.height + 354, 48, 64);
        }

        void createPlatform(float x, float y) {
            float colWidth = 60;  // Slightly thinner to prevent edge-floating
            float colHeight = 10; // Shorter to match the thick grass line

            // Offset the collision box so it sits inside the image
            // x + 2 centers the 60px box inside the 64px image
            // y + 2 moves the 'floor' down so it's not at the very top of the pixels
            Rectangle plat = new Rectangle(x + 2, y + 2, colWidth, colHeight);

            platforms.add(plat);
            platformOrigins.add(x);
        }

        @Override
        public void render(float delta) {
            game.audioManager.update(delta);

            // --- UPDATE LOGIC ---
            if (currentGameState == GameState.PLAYING) {
                // Clamp the delta to avoid "Spiral of Death" if a device lags heavily
                float frameTime = Math.min(delta, 0.25f);
                accumulator += frameTime;

                // While we have enough time saved up, run a physics step
                while (accumulator >= TIME_STEP) {
                    update(TIME_STEP); // Physics now always uses a consistent 0.0166s
                    accumulator -= TIME_STEP;
                }
            }

            Gdx.gl.glClearColor(0f, 0f, 0f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            // 1. DRAW WORLD
            camera.update();
            batch.setProjectionMatrix(camera.combined);
            batch.begin();

            // Calculate background position to follow the camera
            float bgX = camera.position.x - camera.viewportWidth / 2;
            float bgY = camera.position.y - camera.viewportHeight / 2;

            // --- MANUAL PARALLAX LOGIC (Fixes Streaking & Squishing) ---

            // 1. ZOOM: Make the image 20% bigger than its original size
            // This brings the castle closer without squishing it.
            float drawWidth = backgroundTex.getWidth() * bgZoomScale;
            float drawHeight = backgroundTex.getHeight() * bgZoomScale;

            // 2. SCROLL: Calculate offset using Modulo (%)
            // This creates the infinite loop without relying on texture settings
            float parallaxSpeed = 0.2f;
            float scrollOffset = (camera.position.x * parallaxSpeed) % drawWidth;

            // 3. VERTICAL ADJUST: Move image up/down to center the castle
            // Change this value if the castle is too high or too low

            // 4. DOUBLE DRAW: Draw the image twice to cover the seam
            // Draw Left Instance
            batch.draw(backgroundTex, bgX - scrollOffset, bgY + bgVerticalShift, drawWidth, drawHeight);
            batch.draw(backgroundTex, bgX - scrollOffset + drawWidth, bgY + bgVerticalShift, drawWidth, drawHeight);

            // --- UPDATED GROUND DRAWING ---
            // Instead of drawing across the whole screen, we draw per platform segment
            // Inside render(float delta) - Updated Ground Loop
            for (Rectangle plat : staticPlatforms) {
                // We check if it's "low enough" to be considered ground
                if (plat.y <= groundOffsetY + 40) {
                    int tileWidth = groundRegion.getRegionWidth();
                    for (float x = plat.x; x < plat.x + plat.width; x += tileWidth) {
                        float drawW = Math.min(tileWidth, (plat.x + plat.width) - x);

                        // Fix: Use the exact plat.y and plat.height to match the physics rectangle
                        batch.draw(groundRegion, x, plat.y, drawW, plat.height);
                    }
                } else {
                    batch.draw(platformTex, plat.x - 4, plat.y, plat.width + 8, plat.height);
                }
            }

            // Draw other obstacles
            for (Rectangle spike : spikes) {
                // We draw the spike image tiled to match the rectangle's width
                batch.draw(spikeTex,
                    spike.x, spike.y,           // Position
                    spike.width, spike.height,  // Draw size (600x32)
                    0, 0,                       // Start at 0,0 in the texture
                    (int)spike.width,           // Use the rect width as the source width to trigger tiling
                    (int)spike.height,          // Use the rect height as the source height
                    false, false);              // Flip X/Y
            }
            for (Rectangle plat : platforms) {
                // We draw the texture 4 pixels LOWER than the platform's Y
                // This makes the 'top' of the grass line up with plat.y + plat.height
                batch.draw(groundRegion, plat.x - 2, plat.y - 4, 64, 16);
            }

            // Draw Player
            stateTimer += delta;
            TextureRegion currentFrame = getFrame(delta);
            if (moveLeft) runningRight = false;
            if (moveRight) runningRight = true;
            if (!runningRight && !currentFrame.isFlipX()) currentFrame.flip(true, false);
            else if (runningRight && currentFrame.isFlipX()) currentFrame.flip(true, false);
            // These should match the size of a SINGLE frame from your sprite sheet
            // These are the dimensions of one frame in your sprite sheets
            float spriteWidth = 48;
            float spriteHeight = 48;

            // offsetX: Centers the sprite horizontally over the thin blue hitbox
            float offsetX = (spriteWidth - player.width) / 2f;

            // offsetY: Adjust this until the boots sit ON the red line.
            // Based on your image, try 14 or 15 pixels.
            float offsetY = 1f;

            batch.draw(
                currentFrame,
                player.x - offsetX,
                player.y - offsetY,
                spriteWidth,
                spriteHeight
            );

            // Inside render(float delta)
            flagStateTimer += delta; // Update the timer
            TextureRegion currentFlagFrame = goalFlagAnimation.getKeyFrame(flagStateTimer, true);

            // Update the flag draw call
            batch.draw(currentFlagFrame, goalFlag.x, goalFlag.y, goalFlag.width, goalFlag.height);
            batch.end();

            // 2. DRAW HUD & OVERLAYS
            hudCamera.update();
            batch.setProjectionMatrix(hudCamera.combined);
            batch.begin();

            if (currentGameState == GameState.PLAYING) {
                batch.draw(moveLeft ? leftBtnPressedTex : leftBtnTex, leftBtn.x, leftBtn.y, leftBtn.width, leftBtn.height);
                batch.draw(moveRight ? rightBtnPressedTex : rightBtnTex, rightBtn.x, rightBtn.y, rightBtn.width, rightBtn.height);
                batch.draw(jumpPressed ? jumpBtnPressedTex : jumpBtnTex, jumpBtn.x, jumpBtn.y, jumpBtn.width, jumpBtn.height);
                batch.draw(pauseBtnNormal, pauseBtnRect.x, pauseBtnRect.y, pauseBtnRect.width, pauseBtnRect.height);

                if (levelLabelTex != null) {
                    batch.draw(levelLabelTex,
                        pauseBtnRect.x + pauseBtnRect.width + 5,
                        pauseBtnRect.y + (pauseBtnRect.height / 4),
                        80, 20); // Adjust size as needed based on image scale
                }

                font.setColor(Color.WHITE);
                font.draw(batch, "TIME: " + timeString, V_WIDTH - 100, V_HEIGHT - 10);

                // --- DRAW BEST TIME ---
                // Fetch best time from preferences using a unique key for the level
                float bestTime = prefs.getFloat(this.getClass().getSimpleName() + "_bestTime", 0);
                int bMin = (int) bestTime / 60;
                int bSec = (int) bestTime % 60;
                String bestText = (bestTime == 0) ? "BEST: --:--" : String.format("BEST: %02d:%02d", bMin, bSec);

                // Draw 20 pixels below the current time
                font.draw(batch, bestText, V_WIDTH - 100, V_HEIGHT - 30);
            }

            if (currentGameState == GameState.PAUSED) drawPauseMenu();
            else if (currentGameState == GameState.SETTINGS) drawSettingsMenu();
            else if (currentGameState == GameState.LEVEL_COMPLETE) drawLevelCompleteMenu();

            batch.end();

            // 3. HANDLE INPUT
            handleInput(delta);
            if (nextScreen != null) {
                // We don't call dispose() here manually; let the game lifecycle do it.
                game.setScreen(nextScreen);
            }
        }

        protected void drawLevelCompleteMenu() {
            batch.draw(dimTexture, 0, 0, V_WIDTH, V_HEIGHT);
            batch.draw(levelClearTex, levelClearRect.x, levelClearRect.y, levelClearRect.width, levelClearRect.height);

            // Format current time
            font.setColor(Color.valueOf("4a4eb2"));
            font.draw(batch, timeString, levelClearRect.x + 105, levelClearRect.y + 165);

            // --- DRAW BEST TIME ---
            float bestTime = prefs.getFloat(this.getClass().getSimpleName() + "_bestTime", 0);
            int bMin = (int)bestTime / 60;
            int bSec = (int)bestTime % 60;
            String bestText = (bestTime == 0) ? "--:--" : String.format("%02d:%02d", bMin, bSec);

            // Draw slightly below the current time
            font.draw(batch, bestText, levelClearRect.x + 105, levelClearRect.y + 131);
        }

        void update(float delta) {
            // 1. Update Timer
            levelTimer += delta;
            int minutes = (int)levelTimer / 60;
            int seconds = (int)levelTimer % 60;
            timeString = String.format("%02d:%02d", minutes, seconds);

            // 4. Handle Jumping
            // (This works now because onGround was 'true' at the end of the LAST frame)
            if (jumpPressed && onGround) {
                velocityY = JUMP_FORCE;
                onGround = false;

                float sfxVol = prefs.getFloat("sfxVol", 1.0f);

                jumpSound.play(sfxVol);
            }

            if (!(this instanceof GameScreen3)) {
                if (player.overlaps(goalFlag) && !hasPlayedWinSound) {
                    float sfxVol = prefs.getFloat("sfxVol", 1.0f);
                    winSound.play(sfxVol); // Use the preferences volume here

                    hasPlayedWinSound = true;
                    currentGameState = GameState.LEVEL_COMPLETE;
                    // --- SAVE BEST TIME ---
                    String levelKey = this.getClass().getSimpleName() + "_bestTime";
                    float currentBest = prefs.getFloat(levelKey, 9999f); // Default to a high number

                    if (levelTimer < currentBest) {
                        prefs.putFloat(levelKey, levelTimer);
                        prefs.flush(); // Commit to storage
                    }
                }
            }

            // 2. Reset onGround at the start of the frame
            // This allows jumping to change the state correctly
            onGround = false;

            // 3. Apply Horizontal Movement
            if (moveLeft) player.x -= MOVE_SPEED * delta;
            if (moveRight) player.x += MOVE_SPEED * delta;


            // 5. Apply Gravity & Vertical Movement
            velocityY += GRAVITY * delta;
            player.y += velocityY * delta;

            // Screen Bounds
            if (player.x < 0) player.x = 0;
            if (player.x > LEVEL_WIDTH - player.width) player.x = LEVEL_WIDTH - player.width;

            // 6. Moving Platform Logic (ONLY ONE LOOP)
            platformTimer += delta;
            for(int i = 0; i < platforms.size; i++) {
                Rectangle plat = platforms.get(i);
                float startX = platformOrigins.get(i);
                float oldPlatX = plat.x;

                // Update position
                plat.x = startX + (float)Math.sin(platformTimer * PLATFORM_SPEED + i) * PLATFORM_RANGE;
                float platMovedX = plat.x - oldPlatX;

                // Collision: Only if falling or standing
                if (velocityY <= 0 && player.overlaps(plat)) {
                    float platTop = plat.y + plat.height;

                    // If the player is falling and hits the top area of our collision box
                    if (player.y < platTop && player.y > platTop - 8) {
                        player.y = platTop; // Force feet to the exact top of the solid box
                        velocityY = 0;
                        onGround = true;
                        player.x += platMovedX;
                    }
                }
            }

            // 7. Static Platform Logic
            for (Rectangle plat : staticPlatforms) {
                if (velocityY <= 0 && player.overlaps(plat)) {
                    if (player.y + (Math.abs(velocityY) * delta) >= (plat.y + plat.height) - 5) {
                        player.y = plat.y + plat.height;
                        velocityY = 0;
                        onGround = true;
                    }
                }
            }

            // 8. Hazards & Death
            for (Rectangle spikeRect : spikes) {
                if (player.overlaps(spikeRect)) {
                    resetPlayer();
                    return; // Exit method immediately on death
                }
            }
            if (player.y < -50) {
                resetPlayer();
                return;
            }

            // 9. Camera & Goal
            camera.position.x = MathUtils.clamp(player.x + player.width / 2, V_WIDTH / 2f, LEVEL_WIDTH - (V_WIDTH / 2f));
            camera.position.y = MathUtils.clamp(player.y + player.height / 2, V_HEIGHT / 2f, LEVEL_HEIGHT - (V_HEIGHT / 2f));

        }

        protected void handleInput(float delta) {
            if (Gdx.input.justTouched()) {
                Vector3 touch = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                hudViewport.unproject(touch);

                if (currentGameState == GameState.PLAYING) {
                    if (pauseBtnRect.contains(touch.x, touch.y)) currentGameState = GameState.PAUSED;
                }
                else if (currentGameState == GameState.LEVEL_COMPLETE) {
                    // Check Level Clear Buttons
                    if (btnMenuRect.contains(touch.x, touch.y)) pressedUi = PressedUi.BTN_MENU;
                    else if (btnRetryRect.contains(touch.x, touch.y)) pressedUi = PressedUi.BTN_RETRY;
                    else if (btnNextRect.contains(touch.x, touch.y)) pressedUi = PressedUi.BTN_NEXT;
                }
                else if (currentGameState == GameState.PAUSED) {
                    if (pmResumeBtn.contains(touch.x, touch.y)) pressedUi = PressedUi.RESUME;
                    else if (pmSettingsBtn.contains(touch.x, touch.y)) pressedUi = PressedUi.PM_SETTINGS;
                    else if (pmExitBtn.contains(touch.x, touch.y)) pressedUi = PressedUi.PM_EXIT;
                }
                else if (currentGameState == GameState.SETTINGS) {
                    if (settingsCloseBtn.contains(touch.x, touch.y)) pressedUi = PressedUi.CLOSE_SETTINGS;
                    else if (musicMinusBtn.contains(touch.x, touch.y)) updateVolume("music", -0.1f);
                    else if (musicPlusBtn.contains(touch.x, touch.y)) updateVolume("music", 0.1f);
                    else if (sfxMinusBtn.contains(touch.x, touch.y)) updateVolume("sfx", -0.1f);
                    else if (sfxPlusBtn.contains(touch.x, touch.y)) updateVolume("sfx", 0.1f);
                }
            }

            // Release Logic
            if (!Gdx.input.isTouched() && pressedUi != PressedUi.NONE) {
                switch (pressedUi) {
                    case RESUME: currentGameState = GameState.PLAYING; break;
                    case PM_SETTINGS: currentGameState = GameState.SETTINGS; break;
                    case PM_EXIT: game.setScreen(new MenuScreen(game)); break;
                    case CLOSE_SETTINGS: currentGameState = GameState.PAUSED; break;
                    // Level Clear Logic
                    case BTN_MENU:
                        game.setScreen(new MenuScreen(game));
                        break;
                    case BTN_RETRY:
                        if (this instanceof GameScreen2) {
                            game.setScreen(new GameScreen2(game));
                        } else if (this instanceof GameScreen3) {
                            game.setScreen(new GameScreen3(game));
                        } else if (this instanceof TutorialScreen) {
                            game.setScreen(new TutorialScreen(game));
                        } else {
                            game.setScreen(new GameScreen(game));
                        }// Reload screen
                        break;
                    case BTN_NEXT:
                        // Logic to decide next screen
                        if (this instanceof GameScreen2) {
                            nextScreen = new GameScreen(game);
                        } else if (this instanceof GameScreen3) {
                            game.setScreen(new MenuScreen(game));
                            return;//Resets to Main Menu
                        }else if (this instanceof TutorialScreen) {
                            nextScreen = new GameScreen2(game);
                        } else {
                            nextScreen = new GameScreen3(game);
                        }
                        break;
                    default: break;
                }
                pressedUi = PressedUi.NONE;
            }

            // Gameplay inputs (Movement)
            if (currentGameState == GameState.PLAYING) {
                moveLeft = moveRight = jumpPressed = false;
                for (int i = 0; i < 5; i++) {
                    if (Gdx.input.isTouched(i)) {
                        Vector3 touch = new Vector3(Gdx.input.getX(i), Gdx.input.getY(i), 0);
                        hudViewport.unproject(touch);
                        if (!pauseBtnRect.contains(touch.x, touch.y)) {
                            if (leftHitbox.contains(touch.x, touch.y)) moveLeft = true;
                            if (rightHitbox.contains(touch.x, touch.y)) moveRight = true;
                            if (jumpHitbox.contains(touch.x, touch.y)) jumpPressed = true;
                        }
                    }
                }
            }
        }

        // ... (rest of standard methods: drawPauseMenu, drawSettingsMenu, updateVolume, getFrame, resize, dispose) ...
        // Note: Ensure all previous draw/dispose methods are kept.
        // Specifically add levelClearTex.dispose() to your dispose method.

        protected void drawPauseMenu() {
            batch.draw(dimTexture, 0, 0, V_WIDTH, V_HEIGHT);

            batch.setColor(Color.WHITE);

            // Draw Resume Button using the new Regions
            boolean isResumePressed = (pressedUi == PressedUi.RESUME);
            batch.draw(isResumePressed ? resumePressed : resumeNormal,
                pmResumeBtn.x, pmResumeBtn.y, pmResumeBtn.width, pmResumeBtn.height);

            // Keep other buttons as they were
            drawUiButton(pmSettingsBtn, settingsBtnTex, settingsBtnPressedTex, PressedUi.PM_SETTINGS);
            drawUiButton(pmExitBtn, exitBtnTex, exitBtnPressedTex, PressedUi.PM_EXIT);
        }

        protected void drawSettingsMenu() {
            batch.draw(dimTexture, 0, 0, V_WIDTH, V_HEIGHT);
            batch.draw(settingsWindowTex, settingsWindowRect.x, settingsWindowRect.y, settingsWindowRect.width, settingsWindowRect.height);
            font.setColor(Color.WHITE);

            float musicVol = game.audioManager.getMusicVolume();
            font.draw(batch, (int)(musicVol * 100) + "%", musicMinusBtn.x + 25, musicMinusBtn.y + 14);
            drawUiButton(musicMinusBtn, minusBtnTex, minusBtnPressedTex, PressedUi.VOL_M_MINUS);
            drawUiButton(musicPlusBtn, plusBtnTex, plusBtnPressedTex, PressedUi.VOL_M_PLUS);

            float sfxVol = prefs.getFloat("sfxVol", 1.0f);
            font.draw(batch, (int)(sfxVol * 100) + "%", sfxMinusBtn.x + 25, sfxMinusBtn.y + 14);
            drawUiButton(sfxMinusBtn, minusBtnTex, minusBtnPressedTex, PressedUi.VOL_S_MINUS);
            drawUiButton(sfxPlusBtn, plusBtnTex, plusBtnPressedTex, PressedUi.VOL_S_PLUS);
            drawUiButton(settingsCloseBtn, closeBtnTex, closeBtnPressedTex, PressedUi.CLOSE_SETTINGS);
        }

        private void drawUiButton(Rectangle rect, Texture up, Texture down, PressedUi type) {
            boolean pressed = pressedUi == type;
            float scale = pressed ? 0.95f : 1f;
            float w = rect.width * scale;
            float h = rect.height * scale;
            float x = rect.x + (rect.width - w) / 2;
            float y = rect.y + (rect.height - h) / 2;
            batch.draw(pressed ? down : up, x, y, w, h);
        }

        private void updateVolume(String type, float change) {
            if (type.equals("music")) game.audioManager.setMusicVolume(game.audioManager.getMusicVolume() + change);
            else {
                float sfxVol = prefs.getFloat("sfxVol", 1.0f);
                sfxVol = MathUtils.clamp(sfxVol + change, 0f, 1f);
                prefs.putFloat("sfxVol", sfxVol);
                prefs.flush();
            }
        }

        protected TextureRegion getFrame(float delta) {
            if (onGround) {
                if (moveLeft || moveRight) currentState = State.WALKING;
                else currentState = State.IDLE;
            } else {
                currentState = State.JUMPING;
            }
            if (previousState != currentState) {
                stateTimer = 0;
                if (previousState == State.JUMPING && currentState != State.JUMPING) landingTimer = 0.1f;
                previousState = currentState;
            }
            TextureRegion region;
            switch (currentState) {
                case JUMPING:
                    TextureRegion[] jumpFrames = jumpAnimation.getKeyFrames();
                    if (stateTimer < 0.1f) region = jumpFrames[0];
                    else if (stateTimer < 0.2f) region = jumpFrames[1];
                    else region = jumpFrames[2];
                    break;
                case WALKING:
                case IDLE:
                    if (landingTimer > 0) {
                        landingTimer -= delta;
                        region = jumpAnimation.getKeyFrames()[3];
                    } else if (currentState == State.WALKING) {
                        if (stateTimer < 0.1f) region = walkStartFrame;
                        else region = walkLoopAnimation.getKeyFrame(stateTimer - 0.1f, true);
                    } else {
                        region = idleAnimation.getKeyFrame(stateTimer, true);
                    }
                    break;
                default: region = idleAnimation.getKeyFrame(stateTimer, true); break;
            }
            return region;
        }

        private void resetPlayer() {
            player.setPosition(60, V_HEIGHT / 2f);
            velocityY = 0;

            float sfxVol = prefs.getFloat("sfxVol", 1.0f);

            deathSound.play(sfxVol);

            hasKey = false;
        }

        @Override public void resize(int width, int height) { viewport.update(width, height, true); hudViewport.update(width, height, true); }
        @Override public void show() { game.audioManager.playMusic("game_bgm.ogg", true, true); }
        @Override public void hide() {}
        @Override public void pause() {}
        @Override public void resume() {}
        @Override public void dispose() {
            batch.dispose(); font.dispose(); levelClearTex.dispose();
            idleSheet.dispose(); walkSheet.dispose(); jumpSheet.dispose(); pauseSheet.dispose();
            backgroundTex.dispose(); groundTexture.dispose();
            leftBtnTex.dispose(); leftBtnPressedTex.dispose(); rightBtnTex.dispose(); rightBtnPressedTex.dispose(); jumpBtnTex.dispose(); jumpBtnPressedTex.dispose();
            resumeSheet.dispose(); settingsBtnTex.dispose(); settingsBtnPressedTex.dispose(); exitBtnTex.dispose(); exitBtnPressedTex.dispose();
            settingsWindowTex.dispose(); plusBtnTex.dispose(); plusBtnPressedTex.dispose(); minusBtnTex.dispose(); minusBtnPressedTex.dispose(); closeBtnTex.dispose(); closeBtnPressedTex.dispose();
            spikeTex.dispose(); platformTex.dispose(); goalFlagSheet.dispose(); dimTexture.dispose(); winSound.dispose(); jumpSound.dispose(); deathSound.dispose(); levelLabelTex.dispose();
        }
    }
