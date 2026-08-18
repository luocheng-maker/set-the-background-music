package com.fsaobdriiucmapi.setthebackgroundmusic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigManager");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", MyMusicMod.MOD_ID, "config.json");

    private static ModConfig config = new ModConfig();

    public static void load() {
        if (!CONFIG_PATH.toFile().exists()) {
            LOGGER.info("Config file not found, creating default.");
            save();
            return;
        }

        try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
            config = GSON.fromJson(reader, ModConfig.class);
            LOGGER.info("Config loaded: shuffle={}, volume={}", config.shuffle, config.volume);
        } catch (IOException e) {
            LOGGER.error("Failed to load config, using defaults.", e);
            config = new ModConfig();
        }
    }

    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(config, writer);
            LOGGER.info("Config saved.");
        } catch (IOException e) {
            LOGGER.error("Failed to save config.", e);
        }
    }

    public static ModConfig get() {
        return config;
    }

    public static void setShuffle(boolean shuffle) {
        config.shuffle = shuffle;
        save();
        MusicPlayer.setShuffle(shuffle);
        LOGGER.info("Shuffle mode: {}", shuffle ? "ON" : "OFF");
    }

    public static void setVolume(float volume) {
        if (volume < 0.0f) volume = 0.0f;
        if (volume > 1.0f) volume = 1.0f;
        config.volume = volume;
        save();
        MelodyPlayer.setGlobalVolume(volume);
        LOGGER.info("Global volume set to: {}%", Math.round(volume * 100));
    }

    public static void toggleShuffle() {
        setShuffle(!config.shuffle);
    }
}