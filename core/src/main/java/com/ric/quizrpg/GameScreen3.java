package com.ric.quizrpg;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class GameScreen3 extends GameScreen {

    private Rectangle keyRect;
    private Texture keySheet;
    private Animation<TextureRegion> keyAnimation;
    private float keyStateTimer = 0;
    private float keyFloatingTimer = 0;
    boolean hasPlayedKeySound = false;
    com.badlogic.gdx.audio.Sound keySound;

    // Region for the small floating platforms
    private TextureRegion level3PlatformRegion;
    // In the class variables
    private TextureRegion slimPlatformRegion;

    private Texture doorSheet;
    private TextureRegion doorLocked;
    private TextureRegion doorOpen;

    public GameScreen3(Main game) {
        super(game);
        super.hasKey = false;

        // 1. Dispose of old Level 1 assets
        if (backgroundTex != null) backgroundTex.dispose();
        if (groundTexture != null) groundTexture.dispose();
        if (platformTex != null) platformTex.dispose();

        // 2. Load Level 3 Assets
        backgroundTex = new Texture("map3_bg.png");

        // Create a TextureRegion for small platforms (slicing the first 100x40 of the ground texture)
        level3PlatformRegion = new TextureRegion(groundTexture, 0, 0, 100, 40);

        groundTexture = new Texture("map3_ground1.png");
        // Slice only the top row of bricks (assuming 16px height for one row)
        slimPlatformRegion = new TextureRegion(groundTexture, 0, 0, groundTexture.getWidth(), 16);

        // Load Key Animation
        keySheet = new Texture("keySprite.png");
        keySound = Gdx.audio.newSound(Gdx.files.internal("keySound_placeholder.ogg"));
        TextureRegion[][] tmpKey = TextureRegion.split(keySheet, keySheet.getWidth() / 7, keySheet.getHeight());
        keyAnimation = new Animation<>(0.12f, tmpKey[0]);
        keyAnimation.setPlayMode(Animation.PlayMode.LOOP);

        doorSheet = new Texture("goalDoorSprite.png");
        TextureRegion[][] tmpDoor = TextureRegion.split(doorSheet, doorSheet.getWidth() / 2, doorSheet.getHeight());
        doorLocked = tmpDoor[0][0]; // First frame: Closed door
        doorOpen = tmpDoor[0][1];   // Second frame: Open door
        levelLabelTex = new Texture("level3.png");
    }

    @Override
    public void show() {
        super.show();
        setupLevel3Layout();
    }

    private void setupLevel3Layout() {
        spikes.clear();
        platforms.clear();
        platformOrigins.clear();
        staticPlatforms.clear();
        LEVEL_WIDTH = 1500f;

        // We use a visual height for the floor segments
        float gHeight = 80;

        // --- 1. FLOOR SEGMENTS (PHYSICS) ---
        // These are the areas the player can walk on. Gaps between them will now render as pits.
        staticPlatforms.add(new Rectangle(0, groundOffsetY, 400, gHeight));
        // Gap from 400 to 500
        staticPlatforms.add(new Rectangle(500, groundOffsetY, 300, gHeight));
        // Gap from 800 to 1312
        staticPlatforms.add(new Rectangle(1312, groundOffsetY, 568, gHeight));

        // --- 2. OBSTACLES & FLOATING PLATFORMS ---
        staticPlatforms.add(new Rectangle(80, groundOffsetY + 430, 64, 16));
        keyRect = new Rectangle(100, groundOffsetY + 450, 24, 24);

        staticPlatforms.add(new Rectangle(650, groundOffsetY + 130, 64, 16));
        createPlatform(880, groundOffsetY + 190);
        createPlatform(600, groundOffsetY + 230);
        staticPlatforms.add(new Rectangle(750, groundOffsetY + 300, 64, 16));
        staticPlatforms.add(new Rectangle(650, groundOffsetY + 360, 64, 16));
        createPlatform(900, groundOffsetY + 400);

        staticPlatforms.add(new Rectangle(470, groundOffsetY + 360, 64, 16));
        createPlatform(250, groundOffsetY + 410);
        spikes.add(new Rectangle(50, groundOffsetY + 320, 600, 32));
        staticPlatforms.add(new Rectangle(50, groundOffsetY + 310, 600, 16));

        staticPlatforms.add(new Rectangle(1100, groundOffsetY + 320, 64, 16));
        staticPlatforms.add(new Rectangle(1220, groundOffsetY + 330, 64, 16));
        staticPlatforms.add(new Rectangle(1300, groundOffsetY + 400, 128, 32));
        goalFlag.setSize(64, 64); // Adjust size to match door proportions
        // Position on the floor (groundOffsetY + gHeight)
        goalFlag.setPosition(1350, groundOffsetY + 427);

        resetPlayer();
    }

    private void renderLevel3Ground(Rectangle plat) {
        final int TILE_WIDTH = groundTexture.getWidth();
        final int TILE_HEIGHT = groundTexture.getHeight();

        boolean isFloor = plat.y == groundOffsetY;

        if (isFloor) {
            // Floor Logic: Use full 32px height bricks
            float drawY = plat.y + 40f; // Meet the player's feet
            for (float x = plat.x; x < plat.x + plat.width; x += TILE_WIDTH) {
                float remaining = (plat.x + plat.width) - x;
                float drawWidth = Math.min(TILE_WIDTH, remaining);
                batch.draw(groundTexture, x, drawY, drawWidth, TILE_HEIGHT,
                    0, 0, (int)drawWidth, TILE_HEIGHT, false, false);
            }
        } else {
            // Platform Logic: Use the slim 16px region
            // This ensures the visuals match the 16px physics box height
            batch.draw(slimPlatformRegion, plat.x, plat.y, plat.width, plat.height);
        }
    }

    @Override
    public void render(float delta) {
        game.audioManager.update(delta);
        if (currentGameState == GameState.PLAYING) {
            update(delta);
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 1. Draw Background
        float bgX = camera.position.x - camera.viewportWidth / 2;
        float bgY = camera.position.y - camera.viewportHeight / 2;

// --- MANUAL PARALLAX LOGIC (Fixes Streaking & Squishing) ---

// 1. ZOOM: Make the image 20% bigger than its original size
// This brings the castle closer without squishing it.
        float zoomScale = 2f;
        float drawWidth = backgroundTex.getWidth() * zoomScale;
        float drawHeight = backgroundTex.getHeight() * zoomScale;

// 2. SCROLL: Calculate offset using Modulo (%)
// This creates the infinite loop without relying on texture settings
        float parallaxSpeed = 0.2f;
        float scrollOffset = (camera.position.x * parallaxSpeed) % drawWidth;

// 3. VERTICAL ADJUST: Move image up/down to center the castle
// Change this value if the castle is too high or too low
        float verticalShift = -50f;

// 4. DOUBLE DRAW: Draw the image twice to cover the seam
// Draw Left Instance
        batch.draw(backgroundTex,
            bgX - scrollOffset, bgY + verticalShift,
            drawWidth, drawHeight);

// Draw Right Instance (immediately after the first)
        batch.draw(backgroundTex,
            bgX - scrollOffset + drawWidth, bgY + verticalShift,
            drawWidth, drawHeight);

        for (Rectangle plat : staticPlatforms) {
            renderLevel3Ground(plat);
        }

        for (Rectangle plat : platforms) {
            renderLevel3Ground(plat);
        }

        // 4. Draw Spikes
        for (Rectangle spike : spikes) {
            batch.draw(spikeTex, spike.x, spike.y, spike.width, spike.height,
                0, 0, (int)spike.width, (int)spike.height, false, false);
        }

        // 5. Draw Player
        stateTimer += delta;
        TextureRegion currentFrame = getFrame(delta);
        if (!runningRight && !currentFrame.isFlipX()) currentFrame.flip(true, false);
        else if (runningRight && currentFrame.isFlipX()) currentFrame.flip(true, false);
        batch.draw(currentFrame, player.x - ((48 - player.width) / 2f), player.y - 1f, 48, 48);

        // 6. Draw Key & Flag
        boolean isMenuOpen = (currentGameState == GameState.PAUSED || currentGameState == GameState.SETTINGS);
        if (isMenuOpen) batch.setColor(0.5f, 0.5f, 0.5f, 1f);

        // Draw Key
        if (!hasKey && keyRect != null) {
            keyStateTimer += delta;
            keyFloatingTimer += delta;
            float floatOffset = (float) Math.sin(keyFloatingTimer * 3.0f) * 5.0f;
            batch.draw(keyAnimation.getKeyFrame(keyStateTimer, true), keyRect.x, keyRect.y + floatOffset, keyRect.width, keyRect.height);
        }

        // Draw Door
        if (doorLocked != null && doorOpen != null) {
            if (!hasKey) {
                // Locked state: Use the closed door frame
                float lockDim = isMenuOpen ? 0.25f : 0.6f;
                batch.setColor(lockDim, lockDim, lockDim, 1f);
                batch.draw(doorLocked, goalFlag.x, goalFlag.y, goalFlag.width, goalFlag.height);
            } else {
                // Unlocked state: Use the open door frame
                batch.setColor(Color.WHITE);
                batch.draw(doorOpen, goalFlag.x, goalFlag.y, goalFlag.width, goalFlag.height);
            }
        }
        batch.setColor(Color.WHITE);
        batch.end();

        renderHUD();
        handleInput(delta);
    }

    @Override
    protected void handleInput(float delta) {
        super.handleInput(delta); // Keep the parent movement logic

        if (moveLeft) {
            runningRight = false;
        } else if (moveRight) {
            runningRight = true;
        }
    }
    private void renderHUD() {
        hudCamera.update();
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();

        if (currentGameState == GameState.PLAYING) {
            batch.draw(moveLeft ? leftBtnPressedTex : leftBtnTex, leftBtn.x, leftBtn.y, leftBtn.width, leftBtn.height);
            batch.draw(moveRight ? rightBtnPressedTex : rightBtnTex, rightBtn.x, rightBtn.y, rightBtn.width, rightBtn.height);
            batch.draw(jumpPressed ? jumpBtnPressedTex : jumpBtnTex, jumpBtn.x, jumpBtn.y, jumpBtn.width, jumpBtn.height);
            batch.draw(pauseBtnNormal, pauseBtnRect.x, pauseBtnRect.y, pauseBtnRect.width, pauseBtnRect.height);
            font.setColor(Color.WHITE);
            font.draw(batch, "TIME: " + timeString, V_WIDTH - 100, V_HEIGHT - 10);

            //Draw level label
            if (levelLabelTex != null) {
                batch.draw(levelLabelTex,
                    pauseBtnRect.x + pauseBtnRect.width + 5,
                    pauseBtnRect.y + (pauseBtnRect.height / 4),
                    80, 20); // Adjust size as needed based on image scale
            }
            // --- DRAW BEST TIME ---
            // Fetch best time from preferences using a unique key for the level
            float bestTime = prefs.getFloat(this.getClass().getSimpleName() + "_bestTime", 0);
            int bMin = (int) bestTime / 60;
            int bSec = (int) bestTime % 60;
            String bestText = (bestTime == 0) ? "BEST: --:--" : String.format("BEST: %02d:%02d", bMin, bSec);

            // Draw 20 pixels below the current time
            font.draw(batch, bestText, V_WIDTH - 100, V_HEIGHT - 30);
            if (!hasKey) {
                font.setColor(Color.RED);
                font.draw(batch, "GOAL LOCKED: FIND KEY!", V_WIDTH / 2 - 80, V_HEIGHT - 40);
            } else {
                font.setColor(Color.GREEN);
                font.draw(batch, "GOAL UNLOCKED!", V_WIDTH / 2 - 60, V_HEIGHT - 40);
            }
        } else if (currentGameState == GameState.PAUSED) {
            drawPauseMenu();
        } else if (currentGameState == GameState.SETTINGS) {
            drawSettingsMenu();
        } else if (currentGameState == GameState.LEVEL_COMPLETE) {
            drawLevelCompleteMenu();
        }
        batch.end();
    }

    @Override
    protected void update(float delta) {
        // Key Logic
        if (!hasKey && keyRect != null && player.overlaps(keyRect)) {
            hasKey = true;
            float sfxVol = prefs.getFloat("sfxVol", 1.0f);
            keySound.play(sfxVol);
        }
        // Goal Logic
        if (player.overlaps(goalFlag)) {
            if (hasKey) {
                if (!hasPlayedWinSound) {
                    float sfxVol = prefs.getFloat("sfxVol", 1.0f);
                    winSound.play(sfxVol);
                    hasPlayedWinSound = true;
                    currentGameState = GameState.LEVEL_COMPLETE;

                    // --- ADD THIS SAVE LOGIC HERE ---
                    String levelKey = this.getClass().getSimpleName() + "_bestTime"; // "GameScreen3_bestTime"
                    float currentBest = prefs.getFloat(levelKey, 9999f);

                    if (levelTimer < currentBest) {
                        prefs.putFloat(levelKey, levelTimer);
                        prefs.flush(); // Saves to disk immediately
                    }
                }
            } else {
                player.x -= 5;
            }
        }
        super.update(delta);
    }

    protected void resetPlayer() {
        player.setPosition(60, V_HEIGHT / 2f);
        hasKey = false;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (keySheet != null) keySheet.dispose();
        if (keySound != null) keySound.dispose();
    }
}
