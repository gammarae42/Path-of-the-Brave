package com.ric.quizrpg;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MenuScreen implements Screen {

    final Main game;
    SpriteBatch batch;
    BitmapFont font;
    OrthographicCamera camera;
    Viewport viewport;

    // --- MAIN MENU ASSETS ---
    Texture titleTex, bgTexture;
    Texture startBtnUp, startBtnDown;
    Texture settingsBtnUp, settingsBtnDown;
    Texture creditsBtnUp, creditsBtnDown;
    Texture exitBtnUp, exitBtnDown;
    Rectangle startBtn, settingsBtn, creditsBtn, exitBtn;



    // --- SETTINGS OVERLAY ASSETS ---
    boolean isSettingsOpen = false;
    Texture dimTexture; // A semi-transparent black box to dim the background
    Rectangle windowRect;
    Rectangle closeBtn;

    Texture settingsWindowTex;
    Texture plusBtnTex, minusBtnTex, closeBtnTex;
    Texture plusBtnPressedTex, minusBtnPressedTex, closeBtnPressedTex;

    // Volume Controls
    Rectangle musicMinusBtn, musicPlusBtn;
    Rectangle sfxMinusBtn, sfxPlusBtn;

    // --- LOGIC DATA ---
    Preferences prefs;
    float musicVolume = 1.0f;
    float sfxVolume = 1.0f;

    static final float V_WIDTH = 480;
    static final float V_HEIGHT = 270;


    public MenuScreen(Main game) {
        this.game = game;

        batch = new SpriteBatch();
        font = new BitmapFont();

        // 1. SETUP PREFERENCES (Save/Load Data)
        prefs = Gdx.app.getPreferences("QuizRPG_Settings");
        musicVolume = prefs.getFloat("musicVol", 1.0f); // Default to 1.0 (100%)
        sfxVolume = prefs.getFloat("sfxVol", 1.0f);

        // --- ASSET LOADING (Placeholders for logic) ---
        titleTex = new Texture("menu_title2.png");
        bgTexture = new Texture("menu_bg.png");
        startBtnUp = new Texture("playbtn.png");
        startBtnDown = new Texture("playbtn_pressed.png");
        settingsBtnUp = new Texture("settingsbtn.png");
        settingsBtnDown = new Texture("settingsbtn_pressed.png");
        creditsBtnUp = new Texture("creditsbtn.png");
        creditsBtnDown = new Texture("creditsbtn_pressed.png");
        exitBtnUp = new Texture("exitbtn.png");
        exitBtnDown = new Texture("exitbtn_pressed.png");
        settingsWindowTex = new Texture("settings_window.png");
        plusBtnTex = new Texture("volUp.png");
        plusBtnPressedTex = new Texture("volup_pressed.png");
        minusBtnTex = new Texture("volDown.png");
        minusBtnPressedTex = new Texture("volDown_pressed.png");
        closeBtnTex = new Texture("settings_exit.png");
        closeBtnPressedTex = new Texture("settings_exit_pressed.png");

        // Create a 1x1 pixel texture for the "Dimmer" (semi-transparent black)
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(0, 0, 0, 0.7f); // Black with 70% opacity
        pix.fill();
        dimTexture = new Texture(pix);

        // Create a placeholder texture for the settings window background
        pix.setColor(0.2f, 0.2f, 0.2f, 1f); // Dark Gray
        pix.fill();
        pix.dispose();

        // --- MAIN MENU LAYOUT ---
        float btnWidth = 70;
        float btnHeight = 35;
        float spacing = 1.5f;
        float centerX = (V_WIDTH - btnWidth) / 2;
        float startY = V_HEIGHT * 0.50f;

        startBtn = new Rectangle(centerX, startY, btnWidth, btnHeight);
        settingsBtn = new Rectangle(centerX, startY - (btnHeight + spacing), btnWidth, btnHeight);
        creditsBtn = new Rectangle(centerX, startY - 2 * (btnHeight + spacing), btnWidth, btnHeight);
        exitBtn = new Rectangle(centerX, startY - 3 * (btnHeight + spacing), btnWidth, btnHeight);

        // --- SETTINGS OVERLAY LAYOUT ---
        float winW = 300;
        float winH = 250;
        windowRect = new Rectangle((V_WIDTH - winW)/2, (V_HEIGHT - winH)/2, winW, winH);

        // Close button (Top right of window)
        closeBtn = new Rectangle(windowRect.x + winW - 30, windowRect.y + winH - 30, 20, 20);

        // Volume Buttons (Simple boxes for logic)
        float controlX = windowRect.x + 150;
        musicMinusBtn = new Rectangle(controlX + 21, windowRect.y + 113, 20, 20);
        musicPlusBtn = new Rectangle(controlX + 71, windowRect.y + 113, 20, 20);

        sfxMinusBtn = new Rectangle(controlX + 21, windowRect.y + 80, 20, 20);
        sfxPlusBtn = new Rectangle(controlX + 71, windowRect.y + 80, 20, 20);

        camera = new OrthographicCamera();
        viewport = new FitViewport(V_WIDTH, V_HEIGHT, camera);
        viewport.apply();
        camera.position.set(V_WIDTH / 2f, V_HEIGHT / 2f, 0);
    }

    @Override
    public void show() {
        game.audioManager.playMusic("mainmenu_bgm.ogg", true, true);
    }

    // Enum for tracking clicks
    enum PressedButton { NONE, START, SETTINGS, CREDITS, EXIT, VOL_M_MINUS, VOL_M_PLUS, VOL_S_MINUS, VOL_S_PLUS, CLOSE }
    PressedButton pressedButton = PressedButton.NONE;

    private void drawUiButton(Rectangle rect, Texture up, Texture down, PressedButton type) {
        boolean pressed = pressedButton == type;
        float scale = pressed ? 0.95f : 1f;
        float w = rect.width * scale;
        float h = rect.height * scale;
        float x = rect.x + (rect.width - w) / 2;
        float y = rect.y + (rect.height - h) / 2;
        batch.draw(pressed ? down : up, x, y, w, h);
    }

    @Override
    public void render(float delta) {
        game.audioManager.update(delta);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        // 1. DRAW MAIN MENU (Always drawn)
        batch.draw(bgTexture, 0, 0, V_WIDTH, V_HEIGHT);

        float titleW = 200, titleH = 65;
        batch.draw(titleTex, (V_WIDTH - titleW) / 2, V_HEIGHT - titleH - 20, titleW, titleH);

        drawButton(startBtn, startBtnUp, startBtnDown, PressedButton.START);
        drawButton(settingsBtn, settingsBtnUp, settingsBtnDown, PressedButton.SETTINGS);
        drawButton(creditsBtn, creditsBtnUp, creditsBtnDown, PressedButton.CREDITS);
        drawButton(exitBtn, exitBtnUp, exitBtnDown, PressedButton.EXIT);

        // 2. DRAW SETTINGS OVERLAY (If Open)
        if (isSettingsOpen) {
            // A. Draw Dimmer (Full Screen)
            batch.draw(dimTexture, 0, 0, V_WIDTH, V_HEIGHT);

            // B. Draw Window Background
            batch.draw(settingsWindowTex,
                windowRect.x,
                windowRect.y,
                windowRect.width,
                windowRect.height);

            // C. Draw Text & Values
            font.setColor(Color.WHITE);

            // Music Row
            float musicVol = game.audioManager.getMusicVolume();
            font.draw(batch, (int)(musicVol * 100) + "%", musicMinusBtn.x + 25, musicMinusBtn.y + 14);
            drawUiButton(musicMinusBtn, minusBtnTex, minusBtnPressedTex, PressedButton.VOL_M_MINUS);
            drawUiButton(musicPlusBtn, plusBtnTex, plusBtnPressedTex, PressedButton.VOL_M_PLUS);

            // SFX Row
            font.draw(batch, (int)(sfxVolume * 100) + "%", sfxMinusBtn.x + 25, sfxMinusBtn.y + 14);
            drawUiButton(sfxMinusBtn, minusBtnTex, minusBtnPressedTex, PressedButton.VOL_S_MINUS);
            drawUiButton(sfxPlusBtn,  plusBtnTex, plusBtnPressedTex, PressedButton.VOL_S_PLUS);

            // Close Button
            drawUiButton(closeBtn, closeBtnTex, closeBtnPressedTex, PressedButton.CLOSE);
        }
        batch.end();
        // 3. HANDLE INPUT
        handleInput();
    }
    private void drawButton(Rectangle rect, Texture up, Texture down, PressedButton type) {
        boolean pressed = pressedButton == type;
        float scale = pressed ? 0.95f : 1f;
        float w = rect.width * scale;
        float h = rect.height * scale;
        float x = rect.x; //+ (rect.width - w);
        float y = rect.y; //+ (rect.height - h);
        batch.draw(pressed ? down : up, x, y, w, h);
    }
    private void handleInput() {
        if (Gdx.input.justTouched()) {
            Vector3 touch = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touch);
            float x = touch.x;
            float y = touch.y;

            if (isSettingsOpen) {
                // ONLY Check Settings Buttons
                if (closeBtn.contains(x, y)) pressedButton = PressedButton.CLOSE;
                else if (musicMinusBtn.contains(x, y)) updateVolume("music", -0.1f);
                else if (musicPlusBtn.contains(x, y)) updateVolume("music", 0.1f);
                else if (sfxMinusBtn.contains(x, y)) updateVolume("sfx", -0.1f);
                else if (sfxPlusBtn.contains(x, y)) updateVolume("sfx", 0.1f);
            } else {
                // Check Main Menu Buttons
                if (startBtn.contains(x, y)) pressedButton = PressedButton.START;
                else if (settingsBtn.contains(x, y)) pressedButton = PressedButton.SETTINGS;
                else if (creditsBtn.contains(x, y)) pressedButton = PressedButton.CREDITS;
                else if (exitBtn.contains(x, y)) pressedButton = PressedButton.EXIT;
            }
            if (closeBtn.contains(x, y)) pressedButton = PressedButton.CLOSE;
            else if (musicMinusBtn.contains(x, y)) pressedButton = PressedButton.VOL_M_MINUS;
            else if (musicPlusBtn.contains(x, y))  pressedButton = PressedButton.VOL_M_PLUS;
            else if (sfxMinusBtn.contains(x, y))   pressedButton = PressedButton.VOL_S_MINUS;
            else if (sfxPlusBtn.contains(x, y))    pressedButton = PressedButton.VOL_S_PLUS;
        }

        // Handle button release logic
        if (!Gdx.input.isTouched() && pressedButton != PressedButton.NONE) {
            switch (pressedButton) {
                case VOL_M_MINUS: updateVolume("music", -0.1f); break;
                case VOL_M_PLUS:  updateVolume("music",  0.1f); break;
                case VOL_S_MINUS: updateVolume("sfx",   -0.1f); break;
                case VOL_S_PLUS:  updateVolume("sfx",    0.1f); break;
                case CLOSE:       isSettingsOpen = false; break;
                case SETTINGS:    isSettingsOpen = true; break;
                case EXIT:        Gdx.app.exit(); break;
                case START:       game.setScreen(new TutorialScreen(game)); break;
                case CREDITS:     game.setScreen(new CreditsScreen(game)); break;
            }
            pressedButton = PressedButton.NONE;
        }
    }

    // --- HELPER TO SAVE/UPDATE VOLUME ---
    private void updateVolume(String type, float change) {
        if (type.equals("music")) {
            game.audioManager.setMusicVolume(
                game.audioManager.getMusicVolume() + change
            );
        } else {
            sfxVolume = MathUtils.clamp(sfxVolume + change, 0f, 1f);
            prefs.putFloat("sfxVol", sfxVolume);
            prefs.flush();
        }
    }

    @Override public void resize(int width, int height) { viewport.update(width, height); }
    @Override public void hide() {}
    @Override public void pause() {
        game.audioManager.pauseMusic();
    }
    @Override public void resume() {game.audioManager.resumeMusic();}
    @Override public void dispose() {
        batch.dispose();
        font.dispose();
        bgTexture.dispose();
        dimTexture.dispose();
        settingsWindowTex.dispose();
        plusBtnTex.dispose();
        minusBtnTex.dispose();
        closeBtnTex.dispose();
    }
}
