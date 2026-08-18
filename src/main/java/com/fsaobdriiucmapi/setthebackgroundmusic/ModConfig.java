package com.fsaobdriiucmapi.setthebackgroundmusic;

public class ModConfig {
    public boolean modEnabled = true;
    public boolean shuffle = false;
    public float volume = 0.5f;  // 0.0 ~ 1.0

    public ModConfig() {
    }

    public ModConfig(boolean modEnabled, boolean shuffle, float volume) {
        this.modEnabled = modEnabled;
        this.shuffle = shuffle;
        this.volume = volume;
    }
}