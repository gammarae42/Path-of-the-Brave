package com.ric.quizrpg;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class GameScreen2 extends GameScreen {

    public GameScreen2(Main game) {
        super(game);

        // 1. Dispose of the Level 1 background to free memory
        if (backgroundTex != null) backgroundTex.dispose();

        // 2. Load the specific Level 2 background
        backgroundTex = new Texture("map1_bg.png");
        this.bgZoomScale = 1.5f;        // Zoom in less (or more) for this map
        this.bgVerticalShift = -10f;
        levelLabelTex = new Texture("level1.png");
    }

    @Override
    public void show() {
        super.show();
        // Custom setup for Level 2
        setupLevel2Layout();
    }

    private void setupLevel2Layout() {
        // 1. Clear Level 1 obstacles
        spikes.clear();
        platforms.clear();
        platformOrigins.clear();
        staticPlatforms.clear();
        // Inside setupLevel2Layout()
        float gHeight = groundRegion.getRegionHeight(); // Use the actual texture height

        // Use groundOffsetY directly (matching Level 1)
        staticPlatforms.add(new Rectangle(0, groundOffsetY, 150, gHeight));
        staticPlatforms.add(new Rectangle(350, groundOffsetY + 80, 45, 16));
        staticPlatforms.add(new Rectangle(1000, groundOffsetY, 450, gHeight));
        staticPlatforms.add(new Rectangle(250, groundOffsetY + 100, 45, 16));


        // 3. ADD LEVEL 2 CHALLENGES
        // Add floating platforms over the pit so the player can cross
        createPlatform(520, groundOffsetY + 90);

        staticPlatforms.add(new Rectangle(500, ground.y + ground.height + 50, 45, 16));
        createPlatform(320, ground.y + ground.height + 120);
        createPlatform(550, ground.y + ground.height + 170);
        staticPlatforms.add(new Rectangle(750, ground.y + ground.height + 120, 45, 16));
        createPlatform(950, ground.y + ground.height + 95);

        // Add spikes on the second ground segment for difficulty
        // spikes.add(new Rectangle(850, ground.y + ground.height, 55, 32));

        // 4. POSITION THE GOAL
        // Flag is placed at the very end of the second ground segment
        goalFlag.setPosition(1120, ground.getHeight() - 41);

        resetPlayer();
    }

    public void resetPlayer() {
        // Start player on the first platform
        player.setPosition(20, V_HEIGHT / 2f);
        velocityY = 0;
    }


}
