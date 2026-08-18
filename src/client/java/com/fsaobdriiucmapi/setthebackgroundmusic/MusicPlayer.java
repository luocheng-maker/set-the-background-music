package com.fsaobdriiucmapi.setthebackgroundmusic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MusicPlayer");
    private static final Random RANDOM = new Random();

    private final List<Path> musicFiles;
    private int currentIndex = 0;
    private boolean singleSong;
    private boolean paused = false;
    private static boolean shuffle = false;

    public MusicPlayer(List<Path> musicFiles) {
        this.musicFiles = new ArrayList<>(musicFiles);
        shuffle = ConfigManager.get().shuffle;
        updateMode();
    }

    private void updateMode() {
        singleSong = musicFiles.size() == 1;
        MelodyPlayer.setLoopSingle(singleSong);
        MelodyPlayer.setGlobalVolume(ConfigManager.get().volume);
        if (singleSong) {
            LOGGER.info("Single song mode: loop forever.");
        } else {
            LOGGER.info("Playlist mode: {} songs, shuffle={}.", musicFiles.size(), shuffle);
        }
    }

    public static void setShuffle(boolean s) {
        shuffle = s;
        LOGGER.info("Shuffle mode: {}", s ? "ON" : "OFF");
    }

    public void playNext() {
        if (musicFiles.isEmpty()) return;
        selectNextIndex();
        playCurrent();
        paused = false;
    }

    private void selectNextIndex() {
        if (musicFiles.size() <= 1) {
            currentIndex = 0;
            return;
        }

        if (shuffle) {
            int newIndex;
            do {
                newIndex = RANDOM.nextInt(musicFiles.size());
            } while (newIndex == currentIndex && musicFiles.size() > 1);
            currentIndex = newIndex;
        } else {
            currentIndex = (currentIndex + 1) % musicFiles.size();
        }
    }

    public void next() {
        doNext(false);
    }

    public void nextForce() {
        doNext(true);
    }

    private void doNext(boolean force) {
        if (musicFiles.isEmpty()) return;
        if (MelodyPlayer.isPlaying() || paused) {
            MelodyPlayer.stop();
            paused = false;
        }
        selectNextIndex();
        playCurrent();
        LOGGER.info("Next song (force={})", force);
    }

    public void prev() {
        doPrev(false);
    }

    public void prevForce() {
        doPrev(true);
    }

    private void doPrev(boolean force) {
        if (musicFiles.isEmpty()) return;
        if (MelodyPlayer.isPlaying() || paused) {
            MelodyPlayer.stop();
            paused = false;
        }
        if (shuffle) {
            int newIndex;
            do {
                newIndex = RANDOM.nextInt(musicFiles.size());
            } while (newIndex == currentIndex && musicFiles.size() > 1);
            currentIndex = newIndex;
        } else {
            currentIndex = (currentIndex - 1 + musicFiles.size()) % musicFiles.size();
        }
        playCurrent();
        LOGGER.info("Previous song (force={})", force);
    }

    public void playIndex(int index) {
        if (index < 0 || index >= musicFiles.size()) {
            LOGGER.warn("Invalid index: {}", index);
            return;
        }
        if (MelodyPlayer.isPlaying() || paused) {
            MelodyPlayer.stop();
            paused = false;
        }
        currentIndex = index;
        playCurrent();
    }

    public void pause() {
        if (MelodyPlayer.isPlaying()) {
            MelodyPlayer.pause();
            paused = true;
            LOGGER.info("Music paused.");
        } else {
            LOGGER.warn("No music playing to pause.");
        }
    }

    public void resume() {
        if (paused) {
            MelodyPlayer.resume();
            paused = false;
            LOGGER.info("Music resumed.");
        } else if (!MelodyPlayer.isPlaying() && !musicFiles.isEmpty()) {
            playCurrent();
        } else {
            LOGGER.warn("Music is already playing or nothing to resume.");
        }
    }

    public List<String> getList() {
        List<String> names = new ArrayList<>();
        for (Path p : musicFiles) {
            names.add(p.getFileName().toString());
        }
        return names;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public boolean isSingleSong() {
        return singleSong;
    }

    public void reload() {
        List<Path> newFiles = MusicFileScanner.scan();
        if (newFiles.isEmpty()) {
            LOGGER.warn("No music files found after reload, keeping current list.");
            return;
        }

        Path currentFile = null;
        if (!musicFiles.isEmpty() && currentIndex < musicFiles.size()) {
            currentFile = musicFiles.get(currentIndex);
        }

        musicFiles.clear();
        musicFiles.addAll(newFiles);

        if (currentFile != null) {
            int newIndex = musicFiles.indexOf(currentFile);
            if (newIndex >= 0) {
                currentIndex = newIndex;
            } else {
                currentIndex = 0;
                LOGGER.info("Current song removed, switching to first in new list.");
            }
        } else {
            currentIndex = 0;
        }

        ConfigManager.load();
        shuffle = ConfigManager.get().shuffle;
        MelodyPlayer.setGlobalVolume(ConfigManager.get().volume);

        updateMode();
        LOGGER.info("Playlist reloaded: {} files.", musicFiles.size());
    }

    private void playCurrent() {
        if (musicFiles.isEmpty()) return;
        Path file = musicFiles.get(currentIndex);
        MelodyPlayer.play(file);
        paused = false;
        LOGGER.info("Now playing: {}", file.getFileName());
    }
}