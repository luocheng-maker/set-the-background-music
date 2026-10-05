package com.fsaobdriiucmapi.setthebackgroundmusic;

import java.nio.file.Path;

public final class MelodyBackend implements AudioBackend {

    @Override public String name() { return "Melody"; }

    @Override
    public boolean canPlay(String lowerFileName) {
        return lowerFileName.endsWith(".ogg") || lowerFileName.endsWith(".wav");
    }

    @Override
    public void play(Path file, Runnable onFallback) {
        // Melody 无自身回退路径；onFallback 被忽略。
        MelodyPlayer.play(file);
    }

    @Override public void stop()                  { MelodyPlayer.stop(); }
    @Override public void pause()                 { MelodyPlayer.pause(); }
    @Override public void resume()                { MelodyPlayer.resume(); }
    @Override public void setVolume(float v)      { MelodyPlayer.setGlobalVolume(v); }
    @Override public void setLoopSingle(boolean l){ MelodyPlayer.setLoopSingle(l); }
    @Override public boolean isPlaying()          { return MelodyPlayer.isPlaying(); }
    @Override public boolean isLoading()          { return MelodyPlayer.isLoading(); }
    @Override public boolean consumeFailure()     { return MelodyPlayer.consumeClipFailure(); }
    @Override public void invalidate()            { MelodyPlayer.invalidateClip(); }
}