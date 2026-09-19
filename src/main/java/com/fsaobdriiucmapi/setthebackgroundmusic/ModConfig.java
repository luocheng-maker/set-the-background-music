package com.fsaobdriiucmapi.setthebackgroundmusic;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    public boolean modEnabled = true;
    public boolean shuffle = false;
    public float volume = 0.5f;

    // 淡入淡出
    public boolean fadeEnabled = true;
    public int fadeDurationMs = 800;

    // HUD
    public boolean hudEnabled = true;
    public int hudDisplaySeconds = 5;

    // 随机模式: "TRUE_RANDOM" / "NO_REPEAT" / "WEIGHTED"
    public String shuffleMode = "TRUE_RANDOM";

    // 历史与收藏
    public List<String> history = new ArrayList<>();
    public List<String> favorites = new ArrayList<>();

    // 当前分类（空 = 全部）
    public String activeCategory = "";

    public ModConfig() {
    }
}