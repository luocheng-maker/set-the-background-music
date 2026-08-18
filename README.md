# Set The Background Music      

[![Minecraft    ](https://img.shields.io/badge/Minecraft-26.1.2-brightgreen)](https://minecraft.net/)  
[![Fabric      ](https://img.shields.io/badge/Fabric-0.19.3-orange)](https://fabricmc.net/)  
[![License       ](https://img.shields.io/badge/License-MIT-blue)](LICENSE              )   

在 Minecraft **主菜单**和**游戏中**播放自定义背景音乐，支持 `.ogg              ` 格式。

> 原版音乐仅在特定场景出现？**主菜单就能播放你喜欢的音乐！**

---

## 特性

- 自动扫描 `config/setthebackgroundmusic/music/               ` 下的 `.ogg               ` 文件
- 单曲循环 / 顺序循环 / **随机播放**（一键切换）
- **自动禁用原版背景音乐**（互不干扰）
- 原版风格 Toast 弹窗（显示当前播放歌曲）
- **独立音量控制**（不影响游戏音效）
- 配置文件持久化（`config.json`）
- 完整的 `/music               ` 命令系统（支持 Tab 补全）

---

## 安装

1. 将模组 JAR 放入 `.minecraft/mods               ` 文件夹
2. 启动游戏（自动生成配置文件夹）
3. 将 `.ogg               ` 音乐文件放入 `config/setthebackgroundmusic/music/               `
4. 进入游戏，音乐自动播放！

---

## 命令

| 命令 | 功能 |
|:---|:---|
| `/music help                ` | 显示帮助信息 |
| `/music reload                ` | 重新扫描音乐文件夹 |
| `/music shuffle                ` | 切换随机播放模式 |
| `/music volume                ` | 显示当前音量 |
| `/music volume <0-100>                ` | 设置音量 |
| `/music next                ` | 播放下一首 |
| `/music next force                ` | 强制播放下一首 |
| `/music prev                ` | 播放上一首 |
| `/music prev force                ` | 强制播放上一首 |
| `/music pause                ` | 暂停播放 |
| `/music continue                ` | 继续播放 |
| `/music play                ` | 显示播放列表 |
| `/music play "<歌曲名>"` | 按名称播放（支持 Tab 补全） |
| `/music play <编号>` | 按编号播放 |

### 使用示例

```
/music play "Aria Math"     → 播放 Aria Math
/music play 3               → 播放列表中的第 3 首歌
/music volume 75            → 设置音量为 75%
/music shuffle              → 开启/关闭随机播放
```

---

## 配置

首次运行后自动生成 `config/setthebackgroundmusic/config.json                 `：

```json 
{
  "modEnabled": true,        
  "shuffle": false,        
  "volume": 0.5         
}
```

| 字段 | 说明 | 范围 |
|:---|:---|:---|
| `modEnabled          ` | 是否启用模组 | `true           ` / `false            ` |
| `shuffle             ` | 是否随机播放 | `true              ` / `false               ` |
| `volume                ` | 音乐音量 | `0.0` ~ `1.0` |

---

## 构建

```bash                 
gradlew build                 
```

生成的 JAR 位于 `build/libs/setthebackgroundmusic-1.0.0.jar                  `

---

## 许可证

MIT License      

---

## 致谢

- [Melody       ](https://modrinth.com/mod/melody) - 音频播放库
- [Fabric        ](https://fabricmc.net/) - 模组加载器

---

## 注意

- 仅支持 `.ogg         ` 和 `.wav          ` 格式（建议使用 `.ogg           `）
- 音乐文件路径：`config/setthebackgroundmusic/music/            `
- 在 **主菜单** 和 **游戏中** 都会播放
- 原版音乐音量会自动静音，避免冲突

---

## 反馈

遇到问题？请提交 [Issue             ](https://github.com/luocheng-maker/set-the-background-music/issues)

---

**享受你的自定义背景音乐!**

---

现在你的 GitHub 仓库有一个完整的、专业的自述文件了。🎵🚀
