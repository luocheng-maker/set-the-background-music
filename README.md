# Set The Background Music

**English** | [中文](README.zh-CN.md)

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-brightgreen)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.3-orange)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25-red)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

### Author: luocheng-maker

Play your own background music in the Minecraft **main menu** and **in-game**, with support for multiple audio formats.

> Vanilla music only plays in specific situations? **Now you can play your favorite music right from the main menu.**

---

## Features

- Recursively scans all audio files under `config/setthebackgroundmusic/music/`
- **34+ supported formats** (see the format table below)
- **Dual-engine playback**: Melody (OpenAL) + JavaFX MediaPlayer, with automatic fallback
- **JAVE2 (FFmpeg)** transcodes extended formats; users do not need to install FFmpeg
- **Bilingual UI**: English and Simplified Chinese
- **Auto-recovery**: resumes playback after resource reload (F3+T) or opening the settings menu
- Single track loop / sequential loop / shuffle
- **Three shuffle modes**: true random / no-repeat / weighted (favorites get 3x weight)
- **Subfolder categories**: switch playlists by category
- **Favorites** and **play history**
- **HUD**: shows current track and volume when switching songs
- **Fade in / fade out**: smooth transitions between tracks
- **Automatically disables vanilla background music**
- Vanilla-style toast notifications
- Persistent config (`config.json`)
- Full `/music` command system with tab completion

---

## Installation

1. Put the mod JAR into your `.minecraft/mods` folder
2. Launch the game (the config folder is created on first run)
3. Put your music files into `config/setthebackgroundmusic/music/`
4. Run `/music reload` in-game, or restart the game

> **First launch**: the JavaFX engine may fail first and then fall back to Melody. Seeing `JavaFX playback failed, falling back to Melody` in the log is expected.

---

## Supported Formats

| Format | Engine | Notes |
|:---|:---|:---|
| `.ogg` | Melody (native) | Recommended; best performance |
| `.wav` | Melody (native) | Uncompressed; large files |
| `.mp3` | JavaFX | Most common format |
| `.aiff` `.aif` `.aifc` `.au` | Java Sound / JavaFX | Lossless |
| `.m4a` `.m4b` `.m4p` `.caf` `.aac` | JAVE2 -> FFmpeg | Apple / AAC family |
| `.flac` `.opus` `.wma` `.ape` `.wv` `.tta` | JAVE2 -> FFmpeg | Lossless / lossy |
| `.mp2` `.ac3` `.eac3` `.dts` `.amr` | JAVE2 -> FFmpeg | Legacy / surround |
| `.rm` `.ra` `.voc` | JAVE2 -> FFmpeg | Legacy |
| `.webm` `.weba` `.mkv` `.mka` `.mp4` | JAVE2 -> FFmpeg | Container formats |
| `.3gp` `.3g2` | JAVE2 -> FFmpeg | Mobile |

> The **no-FFmpeg build** (`noffmpeg-universal`) only supports `.ogg` `.wav` `.mp3` `.aiff` `.aif` `.aifc` `.au`.

---

## JAR Variants

| JAR | Target platform | Size |
|:---|:---|:---|
| `set-the-background-music-windows.jar` | Windows x64 | ~47 MB |
| `set-the-background-music-macos.jar` | macOS Intel + Apple Silicon | ~69 MB |
| `set-the-background-music-linux.jar` | Linux x64 + ARM64 | ~69 MB |
| `set-the-background-music-universal.jar` | All platforms | ~185 MB |
| `set-the-background-music-noffmpeg-universal.jar` | All platforms (no FFmpeg) | ~47 MB |

> All variants share the same mod ID. **Install only one**, or Fabric will report a duplicate mod.

---

## Commands

| Command | Description |
|:---|:---|
| `/music help` | Show help |
| `/music reload` | Rescan the music folder |
| `/music shuffle` | Toggle shuffle |
| `/music shufflemode <mode>` | Set shuffle mode (`TRUE_RANDOM` / `NO_REPEAT` / `WEIGHTED`) |
| `/music volume <0-100>` | Set volume |
| `/music next` | Play next track |
| `/music prev` | Play previous track |
| `/music pause` | Pause |
| `/music continue` | Resume |
| `/music category` | List all categories |
| `/music category <name\|all>` | Switch to a category |
| `/music fav` | Toggle favorite for the current track |
| `/music fav list` | Show favorites |
| `/music history` | Show recent plays (up to 10) |
| `/music hud` | Toggle HUD |
| `/music fade` | Toggle fade in/out |
| `/music play` | Show current playlist |
| `/music play "<song name>"` | Play by name (tab completion supported) |
| `/music testfx` | Test the JavaFX window |

### Examples

```
/music play "Aria Math"            -> Play Aria Math
/music volume 75                   -> Set volume to 75%
/music shufflemode WEIGHTED        -> Favorites get 3x weight
/music category BGM                -> Switch to the BGM category (music/BGM/)
/music fav                         -> Favorite the current track
```

---

## Music Directory Layout

```
config/setthebackgroundmusic/music/
├── song1.ogg
├── song2.mp3
├── BGM/                          <- category
│   ├── calm.ogg
│   └── battle.mp3
└── Vocal/                        <- category
    └── pop.flac
```

A category is simply a subfolder name. `/music category BGM` only plays tracks inside `BGM/`.

---

## Configuration

The file `config/setthebackgroundmusic/config.json` is created on first run:

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

| Field | Description | Range |
|:---|:---|:---|
| `modEnabled` | Enable the mod | `true` / `false` |
| `shuffle` | Enable shuffle | `true` / `false` |
| `volume` | Music volume | `0.0` - `1.0` |
| `fadeEnabled` | Enable fade in/out | `true` / `false` |
| `fadeDurationMs` | Fade duration in ms | `0` - `5000` |
| `hudEnabled` | Show HUD | `true` / `false` |
| `hudDisplaySeconds` | HUD display time in seconds | `1` - `60` |
| `shuffleMode` | Shuffle mode | `TRUE_RANDOM` / `NO_REPEAT` / `WEIGHTED` |
| `history` | Play history (auto) | up to 100 entries |
| `favorites` | Favorite list (auto) | unlimited |
| `activeCategory` | Active category (auto) | subfolder name or empty |

---

## Building

### Single platform

```cmd
gradlew clean build -Pffmpeg_platforms=win64
```

**Platform identifiers** (case-sensitive, lowercase):

| Platform | Identifier |
|:---|:---|
| Windows x64 | `win64` |
| macOS Intel | `osx64` |
| macOS Apple Silicon | `osxm1` |
| Linux x64 | `linux64` |
| Linux ARM64 | `linux-arm64` |
| Linux ARM32 | `linux-arm32` |

Multiple platforms, comma-separated, no spaces:

```cmd
gradlew clean build -Pffmpeg_platforms=win64,osx64,osxm1
```

### Build all variants at once

Double-click `build-all.bat`. Output goes to `release/`.

### Requirements

- JDK 25 (required)
- Gradle 9.7.1+
- Fabric Loom 1.17.17+

---

## Credits

- [Melody](https://modrinth.com/mod/melody) - OpenAL audio playback library
- [JavaFX](https://openjfx.io/) - MP3 / AAC decoding
- [JAVE2](https://github.com/a-schild/jave2) - FFmpeg Java wrapper
- [Fabric](https://fabricmc.net/) - Mod loader

---

## Feedback

Found a problem? Please open an [Issue](https://github.com/luocheng-maker/set-the-background-music/issues).

---

**Enjoy your custom background music.**