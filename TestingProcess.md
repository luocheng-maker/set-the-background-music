# Set The Background Music — 标准测试流程

测试前准备：把 `build/libs/set-the-background-music.jar` 拷进 `.minecraft/mods`（或用 `gradlew runClient -Pffmpeg_platforms=win64`），确认 `config/setthebackgroundmusic/music/` 下有测试文件。

准备测试文件（至少各一个）：

| 格式 | 引擎路径 |
| :--- | :--- |
| `test.ogg` | Melody 原生 |
| `test.wav` | Melody 原生 |
| `test.mp3` | JavaFX |
| `test.aiff` | JavaFX |
| `test.m4a` | JAVE2 → Melody |
| `test.aac` | JAVE2 → Melody |
| `test.flac` | JAVE2 → Melody |
| `test.opus` | JAVE2 → Melody |
| `test.wma` | JAVE2 → Melody |

---

## 一、启动流程

| # | 操作 | 预期 |
| :--- | :--- | :--- |
| 1.1 | 首次启动，主菜单 | 日志出现 `Found N music files`、`Vanilla music disabled` |
| 1.2 | 观察日志 | OpenAL 未就绪时打 `OpenAL not ready yet, will retry on next tick.`，不切歌 |
| 1.3 | 听到音乐 | 主菜单开始播放（可能是 OGG 或按列表顺序） |
| 1.4 | 检查 HUD | 屏幕右上角短暂显示当前曲名 + 音量 |
| 1.5 | 检查原版音乐 | 主菜单不播 Minecraft 自带 BGM |

---

## 二、格式支持

逐首播放，每首都验证：

| # | 操作 | 预期 |
| :--- | :--- | :--- |
| 2.1 | `/music play "test"` | 播放成功，HUD 显示正确文件名 |
| 2.2 | OGG / WAV | 日志 `Playing OGG via Melody` |
| 2.3 | MP3 / AIFF | 日志 `Attempting playback via JavaFX`，无 fallback |
| 2.4 | M4A / AAC / FLAC / Opus / WMA | 日志 `JavaFX playback failed, falling back to Melody` → `JAVE2 transcoded xxx -> xxx.wav` → `Now playing` |
| 2.5 | 转码期间 | 游戏不卡顿，HUD 正常刷新 |
| 2.6 | 播放全部格式轮询 | 无缝切换，无崩溃 |

---

## 三、命令功能

在聊天栏逐条执行：

| # | 命令 | 预期 |
| :--- | :--- | :--- |
| 3.1 | `/music help` | 打出全部命令帮助 |
| 3.2 | `/music play` | 列出当前播放列表，当前曲目带 `▶` |
| 3.3 | `/music next` | 切下一首，HUD 更新 |
| 3.4 | `/music prev` | 切上一首 |
| 3.5 | `/music pause` | 音乐暂停 |
| 3.6 | `/music continue` | 从暂停位置继续 |
| 3.7 | `/music volume 50` | 音量降到 50%，日志 `Global volume set to: 50%` |
| 3.8 | `/music volume 100` | 音量回到 100% |
| 3.9 | `/music shuffle` | 随机播放开关切换 |
| 3.10 | `/music shuffle` 再按一次 | 回到顺序播放 |
| 3.11 | `/music shufflemode NO_REPEAT` | 切到不重复模式 |
| 3.12 | `/music shufflemode WEIGHTED` | 切到加权模式 |
| 3.13 | `/music hud` | HUD 开关切换，日志/聊天栏有反馈 |
| 3.14 | `/music fade` | 淡入淡出开关切换 |
| 3.15 | `/music reload` | 重新扫描音乐目录，列表刷新 |
| 3.16 | `/music testfx` | 弹出 JavaFX 测试窗口 |

---

## 四、收藏与历史

| # | 操作 | 预期 |
| :--- | :--- | :--- |
| 4.1 | `/music fav` | 收藏当前曲目，聊天栏提示 |
| 4.2 | `/music fav list` | 显示收藏列表，包含刚才的歌 |
| 4.3 | `/music fav` 再按一次 | 取消收藏 |
| 4.4 | `/music history` | 显示最近播放（最多 10 条） |
| 4.5 | 切换多首歌后 | history 顺序正确（最新的在前） |

---

## 五、分类（子文件夹）

准备结构：

```
config/setthebackgroundmusic/music/
├── song1.ogg
├── BGM/
│   ├── calm.ogg
│   └── battle.mp3
└── Vocal/
    └── pop.flac
```

| # | 操作 | 预期 |
| :--- | :--- | :--- |
| 5.1 | `/music category` | 列出 `BGM`、`Vocal` |
| 5.2 | `/music category BGM` | 播放列表只剩 BGM 里的歌 |
| 5.3 | `/music category all` | 恢复全部 |
| 5.4 | 重启游戏 | 分类被记住（`activeCategory` 持久化） |

---

## 六、边界情况

| # | 操作 | 预期 |
| :--- | :--- | :--- |
| 6.1 | **ESC → 设置 → 返回** | 音乐自动重播当前曲目，不切歌，不静音 |
| 6.2 | **F3+T 资源重载** | 同上 |
| 6.3 | **播放中切窗口 / 最小化** | 音乐继续 |
| 6.4 | **放一个 0 字节的坏文件** | 日志 `JAVE2 transcode failed`，自动跳下一首 |
| 6.5 | **放 10 个坏文件** | 连续失败 10 次后停自动跳，日志 `Too many consecutive failures` |
| 6.6 | **连续按 next 切 5 首** | 无崩溃，无重复播放 |
| 6.7 | **`/music play <不存在的名字>`** | 提示 `未找到歌曲` |
| 6.8 | **`/music category <不存在的分类>`** | 保持原列表不变 |

---

## 七、退出

| # | 操作 | 预期 |
| :--- | :--- | :--- |
| 7.1 | 退出游戏 | 日志 `Client stopping, releasing audio resources...` → `Shutting down JavaFX Toolkit...` |
| 7.2 | 进程 | **自动退出，回到命令行**，不用 Ctrl+C |
| 7.3 | 存档 | 已保存（`All chunks are saved`） |
| 7.4 | "无效会话"弹窗 | 忽略，点确定 |

---

## 八、性能与稳定性

| # | 操作 | 预期 |
| :--- | :--- | :--- |
| 8.1 | 连续播放 30 分钟 | 无内存泄漏（F3 看内存稳定） |
| 8.2 | 切换各种格式 20 次 | 无崩溃，无卡顿 |
| 8.3 | 后台放音乐 + 高强度游戏 | 帧率无明显下降 |
| 8.4 | 音量从 0 拉到 100 | 平滑过渡，无爆音 |

---

## 判定标准

全部 8 组测试通过 = 模组达到可发布状态。

任何一项失败 → 贴 `latest.log` 里相关日志段（含 `AudioPlayer` / `MelodyPlayer` / `JavaFXMediaPlayer` / `MusicTickHandler` 的输出），我定位。

---

**建议顺序**：先跑 1、2、7（核心链路），再跑 3、4、5（功能），最后跑 6、8（边界和稳定性）。