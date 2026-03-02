package com.ric.quizrpg;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

public class TutorialScreen extends GameScreen {

    private Texture arrowTex;
    private Texture skipSheet;
    private TextureRegion skipNormal, skipPressed;
    private Rectangle skipBtnRect;
    private Animation<TextureRegion> arrowAnimation;
    private float arrowTimer = 0;

    public TutorialScreen(Main game) {
        super(game);
        // 1. Dispose of the Level 1 background to free memory
        if (backgroundTex != null) backgroundTex.dispose();

        // 2. Load the specific Level 2 background
        backgroundTex = new Texture("map1_bg.png");
        Texture arrowSheet = new Texture("downArrowSprite.png");
        TextureRegion[][] tmp = TextureRegion.split(arrowSheet, arrowSheet.getWidth() / 3, arrowSheet.getHeight());

        // Create an animation using the 3 frames
        arrowAnimation = new Animation<>(0.15f, tmp[0]);
        arrowAnimation.setPlayMode(Animation.PlayMode.LOOP);
        skipSheet = new Texture("skipbtnSprite.png");
        TextureRegion[][] tmpSkip = TextureRegion.split(skipSheet, skipSheet.getWidth() / 2, skipSheet.getHeight());
        skipNormal = tmpSkip[0][0];  // Normal state
        skipPressed = tmpSkip[0][1]; // Sparkle state

        // 2. Position skip button (Top right, below pause)
        skipBtnRect = new Rectangle(V_WIDTH - 280, V_HEIGHT - 36, 80, 30);
        this.bgZoomScale = 1.5f;        // Zoom in less (or more) for this map
        this.bgVerticalShift = -10f;
        levelLabelTex = new Texture("tutorial.png");
    }

    @Override
    protected void setupLevelLayout() {
        // 1. Clear whatever GameScreen put in the arrays
        staticPlatforms.clear();
        platforms.clear();
        spikes.clear();
        platformOrigins.clear();

        // 2. Setup the Tutorial Ground (Long enough to test movement)
        // Using your offset logic: y + 4, height - 8
        staticPlatforms.add(new Rectangle(0, groundOffsetY + 4, 800, groundRegion.getRegionHeight() - 8));

        // 3. Setup ONE Floating Platform (Forces a Jump)
        // Placed at x=400, slightly high so they must jump
        createPlatform(250, groundOffsetY + 150);

        spikes.add(new Rectangle(550, groundOffsetY + 90, 32, 32));

        // 4. Place the Goal Flag ON the platform
        goalFlag.set(750, ground.getHeight() - 41, 48, 64);

        // 5. Reset Player Position
        player.setPosition(50, groundOffsetY + 100);
    }

    @Override
    public void render(float delta) {
        // 1. Reset color to white BEFORE parent render to ensure UI starts clean
        batch.setColor(Color.WHITE);
        super.render(delta); // This draws the world and the Pause Menu from GameScreen

        // 2. Tutorial Overlays (Only if playing)
        if (currentGameState == GameState.PLAYING) {
            batch.setProjectionMatrix(hudCamera.combined);
            batch.begin();
            // Draw Skip Button
            boolean isSkipPressed = Gdx.input.isTouched() &&
                skipBtnRect.contains(hudViewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)).x,
                    hudViewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)).y);

            batch.draw(isSkipPressed ? skipPressed : skipNormal, skipBtnRect.x, skipBtnRect.y, skipBtnRect.width, skipBtnRect.height);
            drawTutorialHints();
            batch.end();

            if (Gdx.input.justTouched()) {
                Vector3 touch = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                hudViewport.unproject(touch);
                if (skipBtnRect.contains(touch.x, touch.y)) {
                    game.setScreen(new GameScreen2(game));
                }
            }
        }

        // 3. Transition Logic
        if (currentGameState == GameState.LEVEL_COMPLETE && Gdx.input.justTouched()) {
            Vector3 touch = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            hudViewport.unproject(touch);

            if (btnNextRect.contains(touch.x, touch.y)) {
                game.setScreen(new GameScreen2(game));
            }
        }
    }

    private void drawTutorialHints() {
        arrowTimer += Gdx.graphics.getDeltaTime();
        TextureRegion currentArrowFrame = arrowAnimation.getKeyFrame(arrowTimer, true);
        font.setColor(Color.GREEN);

        // --- PHASE 1: MOVEMENT ---
        // If player is on the left side, show arrows pointing to Left/Right buttons
        if (player.x < 200) {
            font.draw(batch, "USE ARROWS TO MOVE", 50, 200);
            // Pointing to Left Button
            batch.draw(currentArrowFrame, leftBtn.x + 5, leftBtn.y + 50, 32, 32);
            // Pointing to Right Button
            batch.draw(currentArrowFrame, rightBtn.x + 5, rightBtn.y + 50, 32, 32);
        } else if (player.x >= 200 && player.x < 450) {
            font.draw(batch, "TAP UP TO JUMP!", 250, 200);
            // Pointing to Jump Button
            batch.draw(currentArrowFrame, jumpBtn.x + 5, jumpBtn.y + 50, 32, 32);
        } else if (player.x >= 450 && player.x < 600) {
            font.draw(batch, "AVOID THE SPIKE!", 250, 200);
        } else if (player.x >= 600 && player.x < 800) {
            font.draw(batch, "TOUCH FLAG TO WIN!", 300, 200);
        }
    }



    @Override
    public void dispose() {
        super.dispose(); // Disposes winSound and other GameScreen assets
        if (arrowTex != null) arrowTex.dispose();
        if (skipSheet != null) skipSheet.dispose();
    }
}
