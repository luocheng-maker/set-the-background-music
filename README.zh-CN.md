# Set The Background Music

[English](README.md) | **中文**

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-brightgreen)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.3-orange)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25-red)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

### 作者: luocheng-maker

在 Minecraft **主菜单**和**游戏中**播放自定义背景音乐，支持多种音频格式。

> 原版音乐只在特定场景出现？**现在主菜单就能播放你喜欢的音乐。**

---

## 特性

- 递归扫描 `config/setthebackgroundmusic/music/` 下的所有音频文件
- **支持格式**：`.ogg` `.wav` `.mp3` `.m4a` `.aac` `.aiff` `.aif` `.au` `.flac` `.opus` `.wma` `.ape` `.wv` `.mka`
- **双引擎播放**：Melody (OpenAL) + JavaFX MediaPlayer，自动回退
- **JAVE2 (FFmpeg)** 转码扩展格式，用户无需安装 FFmpeg
- 单曲循环 / 顺序循环 / 随机播放
- **三种随机模式**：真随机 / 不重复 / 加权（收藏歌曲权重 3 倍）
- **子文件夹分类**：按分类切换播放列表
- **收藏系统** + **播放历史**
- **HUD 显示**：切歌时显示当前曲目和音量
- **淡入淡出**：切歌平滑过渡
- **自动禁用原版背景音乐**
- 原版风格 Toast 弹窗
- 配置文件持久化（`config.json`）
- 完整的 `/music` 命令系统（支持 Tab 补全）

---

## 安装

1. 将模组 JAR 放入 `.minecraft/mods` 文件夹
2. 启动游戏（首次运行自动生成配置文件夹）
3. 将音乐文件放入 `config/setthebackgroundmusic/music/`
4. 使用 `/music reload` 扫描文件，或重启游戏

> **首次进入游戏**：JavaFX 引擎会先尝试播放，失败后自动回退到 Melody。日志里看到 `JavaFX playback failed, falling back to Melody` 是正常现象。

---

## 支持的格式

| 格式 | 引擎 | 说明 |
|:---|:---|:---|
| `.ogg` | Melody (原生) | 推荐格式，性能最佳 |
| `.wav` | Melody (原生) | 无压缩，文件大 |
| `.mp3` | JavaFX | 最常见格式 |
| `.m4a` | JAVE2 -> FFmpeg | 需打包 FFmpeg |
| `.aac` | JAVE2 -> FFmpeg | 需打包 FFmpeg |
| `.flac` | JAVE2 -> FFmpeg | 无损，需打包 FFmpeg |
| `.opus` | JAVE2 -> FFmpeg | 需打包 FFmpeg |
| `.wma` | JAVE2 -> FFmpeg | 需打包 FFmpeg |
| `.ape` | JAVE2 -> FFmpeg | 需打包 FFmpeg |
| `.wv` | JAVE2 -> FFmpeg | 需打包 FFmpeg |
| `.mka` | JAVE2 -> FFmpeg | 需打包 FFmpeg |
| `.aiff` / `.aif` / `.au` | Java Sound | 无损，需打包 FFmpeg |

> **无 FFmpeg 版**（`noffmpeg-universal`）只能播放 `.ogg` `.wav` `.mp3` `.aiff` `.aif` `.au`。

---

## JAR 变体

| JAR | 目标平台 | 体积 |
|:---|:---|:---|
| `set-the-background-music-windows.jar` | Windows x64 | ~47 MB |
| `set-the-background-music-macos.jar` | macOS Intel + Apple Silicon | ~69 MB |
| `set-the-background-music-linux.jar` | Linux x64 + ARM64 | ~69 MB |
| `set-the-background-music-universal.jar` | 全平台 | ~185 MB |
| `set-the-background-music-noffmpeg-universal.jar` | 全平台（无 FFmpeg） | ~47 MB |

> 每个 JAR 用的是同一个模组 ID，**只能装一个**，否则 Fabric 会报重复模组。

---

## 命令

| 命令 | 功能 |
|:---|:---|
| `/music help` | 显示帮助信息 |
| `/music reload` | 重新扫描音乐文件夹 |
| `/music shuffle` | 切换随机播放开关 |
| `/music shufflemode <mode>` | 设置随机模式（`TRUE_RANDOM` / `NO_REPEAT` / `WEIGHTED`） |
| `/music volume <0-100>` | 设置音量 |
| `/music next` | 播放下一首 |
| `/music prev` | 播放上一首 |
| `/music pause` | 暂停播放 |
| `/music continue` | 继续播放 |
| `/music category` | 列出所有分类 |
| `/music category <name\|all>` | 切换到指定分类 |
| `/music fav` | 收藏 / 取消收藏当前歌曲 |
| `/music fav list` | 查看收藏列表 |
| `/music history` | 查看最近播放（最多 10 首） |
| `/music hud` | 开关 HUD 显示 |
| `/music fade` | 开关淡入淡出 |
| `/music play` | 显示当前播放列表 |
| `/music play "<歌曲名>"` | 按名称播放（支持 Tab 补全） |
| `/music testfx` | 测试 JavaFX 窗口 |

### 使用示例

```
/music play "Aria Math"            -> 播放 Aria Math
/music volume 75                   -> 设置音量为 75%
/music shufflemode WEIGHTED        -> 收藏歌曲权重 3 倍
/music category BGM                -> 切换到 BGM 分类（music/BGM/ 子文件夹）
/music fav                         -> 收藏当前歌曲
```

---

## 音乐目录结构

```
config/setthebackgroundmusic/music/
├── song1.ogg
├── song2.mp3
├── BGM/                          <- 分类
│   ├── calm.ogg
│   └── battle.mp3
└── Vocal/                        <- 分类
    └── pop.flac
```

分类名就是子文件夹名。`/music category BGM` 只播放 `BGM/` 目录下的歌曲。

---

## 配置

首次运行自动生成 `config/setthebackgroundmusic/config.json`：

```json
{
  "modEnabled": true,
  "shuffle": false,
  "volume": 0.5,
  "fadeEnabled": true,
  "fadeDurationMs": 800,
  "hudEnabled": true,
  "hudDisplaySeconds": 5,
  "shuffleMode": "TRUE_RANDOM",
  "history": [],
  "favorites": [],
  "activeCategory": ""
}
```

| 字段 | 说明 | 范围 |
|:---|:---|:---|
| `modEnabled` | 是否启用模组 | `true` / `false` |
| `shuffle` | 是否随机播放 | `true` / `false` |
| `volume` | 音乐音量 | `0.0` - `1.0` |
| `fadeEnabled` | 是否启用淡入淡出 | `true` / `false` |
| `fadeDurationMs` | 淡入淡出时长（毫秒） | `0` - `5000` |
| `hudEnabled` | 是否显示 HUD | `true` / `false` |
| `hudDisplaySeconds` | HUD 显示时长（秒） | `1` - `60` |
| `shuffleMode` | 随机模式 | `TRUE_RANDOM` / `NO_REPEAT` / `WEIGHTED` |
| `history` | 播放历史（自动维护） | 最多 100 条 |
| `favorites` | 收藏列表（自动维护） | 无上限 |
| `activeCategory` | 当前分类（自动维护） | 子文件夹名或空 |

---

## 构建

### 单个平台

```cmd
gradlew clean build -Pffmpeg_platforms=win64
```

**平台标识**（大小写敏感，全小写）：

| 平台 | 标识 |
|:---|:---|
| Windows x64 | `win64` |
| macOS Intel | `osx64` |
| macOS Apple Silicon | `osxm1` |
| Linux x64 | `linux64` |
| Linux ARM64 | `linux-arm64` |
| Linux ARM32 | `linux-arm32` |

多个平台逗号分隔，无空格：

```cmd
gradlew clean build -Pffmpeg_platforms=win64,win-arm64
```

### 一次构建所有变体

双击 `build-all.bat`，产出到 `release/` 目录。

### 依赖要求

- JDK 25（必须）
- Gradle 9.7.1+
- Fabric Loom 1.17.17+

---

## 致谢

- [Melody](https://modrinth.com/mod/melody) - OpenAL 音频播放库
- [JavaFX](https://openjfx.io/) - MP3 / AAC 解码
- [JAVE2](https://github.com-a-schild/jave2) - FFmpeg Java 封装
- [Fabric](https://fabricmc.net/) - 模组加载器

---

## 反馈

遇到问题？请提交 [Issue](https://github.com/luocheng-maker/set-the-background-music/issues)。

---

**享受你的自定义背景音乐。**
