package com.fsaobdriiucmapi.setthebackgroundmusic;

import java.nio.file.Path;

public final class JavaFXBackend implements AudioBackend {

    @Override public String name() { return "JavaFX"; }

    @Override
    public boolean canPlay(String lowerFileName) {
        // JavaFX 兜底：不属于 Melody 原生格式的都走这里。
        return true;
    }

    @Override
    public void play(Path file, Runnable onFallback) {
        JavaFXMediaPlayer.play(file, onFallback);
    }

    @Override public void stop()                  { JavaFXMediaPlayer.stop(); }
    @Override public void pause()                 { JavaFXMediaPlayer.pause(); }
    @Override public void resume()                { JavaFXMediaPlayer.resume(); }
    @Override public void setVolume(float v)      { JavaFXMediaPlayer.setGlobalVolume(v); }
    @Override public void setLoopSingle(boolean l){ JavaFXMediaPlayer.setLoopSingle(l); }
    @Override public boolean isPlaying()          { return JavaFXMediaPlayer.isPlaying(); }
    @Override public boolean isLoading()          { return JavaFXMediaPlayer.isLoading(); }
    @Override public boolean consumeFailure()     { return false; }
    @Override public void invalidate()            { JavaFXMediaPlayer.invalidate(); }
}