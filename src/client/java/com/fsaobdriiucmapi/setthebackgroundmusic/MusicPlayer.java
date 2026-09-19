package com.fsaobdriiucmapi.setthebackgroundmusic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public class MusicPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MusicPlayer");
    private static final Random RANDOM = new Random();
    private static final int HISTORY_MAX = 100;

    private final List<Path> allFiles;
    private final List<Path> musicFiles;
    private int currentIndex = 0;
    private boolean singleSong;
    private boolean paused = false;
    private String activeCategory = "";
    private static boolean shuffle = false;
    private final Set<Integer> playedThisRound = new HashSet<>();

    public MusicPlayer(List<Path> musicFiles) {
        this.allFiles = new ArrayList<>(musicFiles);
        this.musicFiles = new ArrayList<>(musicFiles);
        shuffle = ConfigManager.get().shuffle;
        updateMode();

        JavaFXMediaPlayer.setOnEndOfMedia(() -> {
            if (!singleSong) playNext();
        });
    }

    public void setCategory(String category) {
        this.activeCategory = category == null ? "" : category;
        ConfigManager.get().activeCategory = this.activeCategory;
        ConfigManager.save();

        List<Path> filtered;
        if (activeCategory.isEmpty()) {
            filtered = new ArrayList<>(allFiles);
        } else {
            Path root = MusicFileScanner.getMusicDir().resolve(activeCategory);
            filtered = new ArrayList<>();
            for (Path p : allFiles) {
                if (p.startsWith(root)) filtered.add(p);
            }
        }

        if (filtered.isEmpty()) {
            LOGGER.warn("Category '{}' has no songs, keeping previous list", activeCategory);
            return;
        }

        Path currentFile = musicFiles.isEmpty() ? null : musicFiles.get(Math.min(currentIndex, musicFiles.size() - 1));
        musicFiles.clear();
        musicFiles.addAll(filtered);
        if (currentFile != null) {
            int newIdx = musicFiles.indexOf(currentFile);
            currentIndex = newIdx >= 0 ? newIdx : 0;
        } else {
            currentIndex = 0;
        }
        playedThisRound.clear();
        updateMode();
    }

    public String getActiveCategory() {
        return activeCategory;
    }

    private void updateMode() {
        singleSong = musicFiles.size() == 1;
        AudioPlayer.setLoopSingle(singleSong);
        AudioPlayer.setGlobalVolume(ConfigManager.get().volume);
        if (singleSong) LOGGER.info("Single song mode: loop forever.");
        else LOGGER.info("Playlist mode: {} songs, shuffle={}, mode={}",
                musicFiles.size(), shuffle, ConfigManager.get().shuffleMode);
    }

    public static void setShuffle(boolean s) { shuffle = s; }

    public void playNext() {
        if (musicFiles.isEmpty()) return;
        selectNextIndex();
        playCurrent();
        paused = false;
    }

    private void selectNextIndex() {
        if (musicFiles.size() <= 1) { currentIndex = 0; return; }
        if (!shuffle) {
            currentIndex = (currentIndex + 1) % musicFiles.size();
            return;
        }

        String mode = ConfigManager.get().shuffleMode;
        switch (mode) {
            case "NO_REPEAT": noRepeatNext(); break;
            case "WEIGHTED":  weightedNext(); break;
            case "TRUE_RANDOM":
            default:          trueRandomNext(); break;
        }
    }

    private void trueRandomNext() {
        int newIndex;
        do { newIndex = RANDOM.nextInt(musicFiles.size()); }
        while (newIndex == currentIndex && musicFiles.size() > 1);
        currentIndex = newIndex;
    }

    private void noRepeatNext() {
        if (playedThisRound.size() >= musicFiles.size()) {
            playedThisRound.clear();
            LOGGER.info("No-repeat round complete, reshuffling.");
        }
        int newIndex;
        do { newIndex = RANDOM.nextInt(musicFiles.size()); }
        while (playedThisRound.contains(newIndex) && playedThisRound.size() < musicFiles.size());
        playedThisRound.add(newIndex);
        currentIndex = newIndex;
    }

    private void weightedNext() {
        List<String> favorites = ConfigManager.get().favorites;
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < musicFiles.size(); i++) {
            String name = musicFiles.get(i).getFileName().toString();
            int weight = favorites.contains(name) ? 3 : 1;
            for (int w = 0; w < weight; w++) pool.add(i);
        }
        if (pool.isEmpty()) { trueRandomNext(); return; }
        int pick;
        do { pick = pool.get(RANDOM.nextInt(pool.size())); }
        while (pick == currentIndex && musicFiles.size() > 1);
        currentIndex = pick;
    }

    public void next() { doNext(false); }
    public void nextForce() { doNext(true); }

    private void doNext(boolean force) {
        if (musicFiles.isEmpty()) return;
        if (AudioPlayer.isPlaying() || paused) {
            AudioPlayer.stop();
            paused = false;
        }
        selectNextIndex();
        playCurrent();
    }

    public void prev() { doPrev(false); }
    public void prevForce() { doPrev(true); }

    private void doPrev(boolean force) {
        if (musicFiles.isEmpty()) return;
        if (AudioPlayer.isPlaying() || paused) {
            AudioPlayer.stop();
            paused = false;
        }
        currentIndex = (currentIndex - 1 + musicFiles.size()) % musicFiles.size();
        playCurrent();
    }

    public void playIndex(int index) {
        if (index < 0 || index >= musicFiles.size()) return;
        if (AudioPlayer.isPlaying() || paused) {
            AudioPlayer.stop();
            paused = false;
        }
        currentIndex = index;
        playCurrent();
    }

    public void replayPrevious() {
        if (musicFiles.isEmpty()) return;
        if (musicFiles.size() == 1) {
            LOGGER.info("Only one song, cannot replay previous.");
            return;
        }
        LOGGER.info("Playback failed, replaying previous track.");
        currentIndex = (currentIndex - 1 + musicFiles.size()) % musicFiles.size();
        playCurrent();
    }

    public void pause() {
        if (AudioPlayer.isPlaying()) {
            AudioPlayer.pause();
            paused = true;
        }
    }

    public void resume() {
        if (paused) {
            AudioPlayer.resume();
            paused = false;
        } else if (!AudioPlayer.isPlaying() && !musicFiles.isEmpty()) {
            playCurrent();
        }
    }

    public List<String> getList() {
        List<String> names = new ArrayList<>();
        for (Path p : musicFiles) names.add(p.getFileName().toString());
        return names;
    }

    public int getCurrentIndex() { return currentIndex; }
    public boolean isShuffle() { return shuffle; }
    public boolean isSingleSong() { return singleSong; }

    public String getCurrentTrackName() {
        if (musicFiles.isEmpty()) return "";
        Path p = musicFiles.get(Math.min(currentIndex, musicFiles.size() - 1));
        String name = p.getFileName().toString();
        return name.replaceFirst(MusicFileScanner.EXT_REGEX, "");
    }

    public void reload() {
        List<Path> newFiles = MusicFileScanner.scan();
        if (newFiles.isEmpty()) return;
        Path currentFile = null;
        if (!musicFiles.isEmpty() && currentIndex < musicFiles.size()) {
            currentFile = musicFiles.get(currentIndex);
        }
        allFiles.clear();
        allFiles.addAll(newFiles);
        musicFiles.clear();
        musicFiles.addAll(newFiles);
        if (currentFile != null) {
            int newIndex = musicFiles.indexOf(currentFile);
            currentIndex = (newIndex >= 0) ? newIndex : 0;
        } else {
            currentIndex = 0;
        }
        ConfigManager.load();
        shuffle = ConfigManager.get().shuffle;
        AudioPlayer.setGlobalVolume(ConfigManager.get().volume);
        playedThisRound.clear();
        updateMode();
        if (!activeCategory.isEmpty()) setCategory(activeCategory);
    }

    private void playCurrent() {
        if (musicFiles.isEmpty()) return;
        Path file = musicFiles.get(currentIndex);
        recordHistory(file);
        MusicHud.notifyTrackChanged(getCurrentTrackName());
        AudioPlayer.play(file);
        paused = false;
    }

    private void recordHistory(Path file) {
        ModConfig cfg = ConfigManager.get();
        String name = file.getFileName().toString();
        cfg.history.remove(name);
        cfg.history.add(0, name);
        while (cfg.history.size() > HISTORY_MAX) {
            cfg.history.remove(cfg.history.size() - 1);
        }
        ConfigManager.save();
    }

    public List<String> getHistory() {
        return new ArrayList<>(ConfigManager.get().history);
    }

    public boolean toggleFavoriteCurrent() {
        return toggleFavorite(getCurrentTrackName());
    }

    public boolean toggleFavorite(String trackName) {
        if (trackName == null || trackName.isEmpty()) return false;
        ModConfig cfg = ConfigManager.get();
        boolean added;
        if (cfg.favorites.contains(trackName)) {
            cfg.favorites.remove(trackName);
            added = false;
        } else {
            cfg.favorites.add(trackName);
            added = true;
        }
        ConfigManager.save();
        return added;
    }

    public boolean isCurrentFavorite() {
        return ConfigManager.get().favorites.contains(getCurrentTrackName());
    }

    public List<String> getFavorites() {
        return new ArrayList<>(ConfigManager.get().favorites);
    }

    public int findIndexByName(String query) {
        String q = query.toLowerCase();
        for (int i = 0; i < musicFiles.size(); i++) {
            String full = musicFiles.get(i).getFileName().toString().toLowerCase();
            String noExt = full.replaceFirst(MusicFileScanner.EXT_REGEX, "");
            if (full.equals(q) || noExt.equals(q) || noExt.contains(q)) return i;
        }
        return -1;
    }
}