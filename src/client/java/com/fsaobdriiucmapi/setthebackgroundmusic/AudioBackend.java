package com.fsaobdriiucmapi.setthebackgroundmusic;

import java.nio.file.Path;

/**
 * 音频后端接口（P2-1）。
 * 把「选路 + 通用控制」从 AudioPlayer 的 if-else 里抽出来。
 * 实现类内部仍调用既有的 MelodyPlayer / JavaFXMediaPlayer static 方法，
 * 不改变现有行为。
 */
public interface AudioBackend {

    /** 后端名，仅用于日志。 */
    String name();

    /**
     * 是否能处理该文件。
     * @param lowerFileName 小写文件名（含扩展名）
     */
    boolean canPlay(String lowerFileName);

    /**
     * 开始播放。失败时（若 onFallback != null）回调。
     */
    void play(Path file, Runnable onFallback);

    void stop();
    void pause();
    void resume();
    void setVolume(float v);
    void setLoopSingle(boolean loop);

    boolean isPlaying();
    boolean isLoading();

    /** 上次 isPlaying() 查询是否因后端异常被强制失效（Melody 特有）。 */
    boolean consumeFailure();

    /** SoundEngine 重启后清引用，不 dispose。 */
    void invalidate();
}