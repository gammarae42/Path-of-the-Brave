package com.ric.quizrpg;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class SplashScreen  implements Screen {
    private final Game game;
    private SpriteBatch batch;
    private Texture splash;
    private float timer;

    private static final float SHOW_TIME = 2.0f;

    public SplashScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        splash = new Texture("menu_title2.png");
        timer = 0f;
    }

    @Override
    public void render(float delta) {
        timer += delta;

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(
            splash,
            (Gdx.graphics.getWidth() - splash.getWidth()) / 2f,
            (Gdx.graphics.getHeight() - splash.getHeight()) / 2f
        );
        batch.end();

        if (timer >= SHOW_TIME) {
            game.setScreen(new MenuScreen((Main) game));
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        splash.dispose();
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
