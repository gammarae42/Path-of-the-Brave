package com.ric.quizrpg;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.MathUtils;

public class AudioManager {
    private Music music;

    private float userMusicVolume;
    private float fadeMultiplier = 1f;

    private static final float FADE_SPEED = 0.8f;

    private final Preferences prefs;
    private String currentTrack;
    private float userSfxVolume;


    public AudioManager() {
        prefs = Gdx.app.getPreferences("settings");
        userMusicVolume = prefs.getFloat("musicVol", 0.6f);
        userSfxVolume = prefs.getFloat("sfxVol", 0.8f); // Load SFX preference
    }

    public void playSound(com.badlogic.gdx.audio.Sound sound) {
        if (sound != null) {
            sound.play(userSfxVolume);
        }
    }

    public void setSfxVolume(float value) {
        userSfxVolume = MathUtils.clamp(value, 0f, 1f);
        prefs.putFloat("sfxVol", userSfxVolume);
        prefs.flush();
    }
    public void playMusic(String file, boolean loop, boolean fadeIn) {

        //If same track is already playing, do nothing
        if (music != null && file.equals(currentTrack)) {
            return;
        }

        stopMusic();

        currentTrack = file;
        music = Gdx.audio.newMusic(Gdx.files.internal(file));
        music.setLooping(loop);

        fadeMultiplier = fadeIn ? 0f : 1f;
        applyVolume();

        music.play();
    }

    public void update(float delta) {
        if (fadeMultiplier < 1f) {
            fadeMultiplier += FADE_SPEED * delta;
            fadeMultiplier = Math.min(fadeMultiplier, 1f);
            applyVolume();
        }
    }

    public void setMusicVolume(float value) {
        userMusicVolume = MathUtils.clamp(value, 0f, 1f);
        prefs.putFloat("musicVol", userMusicVolume);
        prefs.flush();
        applyVolume();
    }

    public float getMusicVolume() {
        return userMusicVolume;
    }

    private void applyVolume() {
        if (music != null) {
            music.setVolume(userMusicVolume * fadeMultiplier);
        }
    }

    public void stopMusic() {
        if (music != null) {
            music.stop();
            music.dispose();
            music = null;
        }
    }
    public void pauseMusic() {
        if (music != null && music.isPlaying()) {
            music.pause();
        }
    }

    public void resumeMusic() {
        if (music != null && !music.isPlaying()) {
            music.play();
            applyVolume();
        }
    }
}
