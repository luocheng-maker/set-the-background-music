package com.fsaobdriiucmapi.setthebackgroundmusic;

import java.util.Set;

/**
 * 唯一扩展名数据源（P1-7）。
 * 之前 MusicFileScanner.SUPPORTED_EXT 与 MelodyPlayer.EXTENDED_FORMATS
 * 是两份独立清单，容易漂移，统一到这里。
 */
public final class AudioFormats {
    private AudioFormats() {}

    /** 全部支持的音频扩展名（含点，小写）。 */
    public static final Set<String> SUPPORTED_EXT = Set.of(
            ".ogg", ".wav",
            ".mp3", ".m4a", ".aiff", ".aif", ".aifc", ".au",
            ".flac", ".opus", ".wma", ".aac", ".ape", ".wv", ".mka",
            ".m4b", ".m4p", ".caf", ".amr", ".mp2",
            ".ac3", ".eac3", ".dts", ".tta",
            ".rm", ".ra", ".voc",
            ".webm", ".weba", ".mkv",
            ".3gp", ".3g2"
    );

    /** 与 SUPPORTED_EXT 保持一致的正则（不带点），大小写不敏感。 */
    public static final String EXT_REGEX =
            "(?i)\\.(ogg|wav|mp3|m4a|aiff|aif|aifc|au|flac|opus|wma|aac|ape|wv|mka"
            + "|m4b|m4p|caf|amr|mp2|ac3|eac3|dts|tta|rm|ra|voc|webm|weba|mkv|3gp|3g2)$";

    /**
     * Melody 无法直接加载、需要 JAVE2 / Java Sound 转码的扩展名（含点）。
     * 与 SUPPORTED_EXT 相比，去掉了 .ogg / .wav / .mp3 / .aiff / .aif / .au
     * （前者由 Melody 或 JavaFX 原生处理）。
     */
    public static final Set<String> EXTENDED_FORMATS = Set.of(
            ".flac", ".opus", ".wma", ".m4a", ".m4b", ".m4p", ".mp4",
            ".aac", ".ape", ".wv", ".mka",
            ".mp2", ".ac3", ".eac3", ".dts", ".tta",
            ".caf", ".aifc", ".amr",
            ".rm", ".ra", ".voc",
            ".webm", ".weba", ".mkv",
            ".3gp", ".3g2"
    );
}