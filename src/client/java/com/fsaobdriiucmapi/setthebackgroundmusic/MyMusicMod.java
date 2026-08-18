package com.fsaobdriiucmapi.setthebackgroundmusic;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MyMusicMod implements ClientModInitializer {
    public static final String MOD_ID = "setthebackgroundmusic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static MusicPlayer player;

    @Override
    public void onInitializeClient() {
        // 先加载配置
        ConfigManager.load();

        List<Path> musicFiles = MusicFileScanner.scan();
        if (musicFiles.isEmpty()) {
            LOGGER.warn("No music files found, mod disabled.");
            return;
        }

        // 检查是否启用
        if (!ConfigManager.get().modEnabled) {
            LOGGER.info("Mod disabled in config.");
            return;
        }

        player = new MusicPlayer(musicFiles);
        MusicTickHandler tickHandler = new MusicTickHandler(player);
        tickHandler.register();

        registerCommands();
        LOGGER.info("Registered /music commands.");
    }

    @SuppressWarnings("unchecked")
    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(
                ClientCommands.literal("music")

                    // reload
                    .then(ClientCommands.literal("reload")
                        .executes(context -> {
                            if (player == null) {
                                context.getSource().sendError(Component.literal("§c播放器未初始化"));
                                return 1;
                            }
                            player.reload();
                            context.getSource().sendFeedback(Component.literal("§a音乐列表已刷新！"));
                            return 1;
                        })
                    )

                    // shuffle (切换随机播放)
                    .then(ClientCommands.literal("shuffle")
                        .executes(context -> {
                            if (player == null) {
                                context.getSource().sendError(Component.literal("§c播放器未初始化"));
                                return 1;
                            }
                            ConfigManager.toggleShuffle();
                            context.getSource().sendFeedback(Component.literal(
                                "§a随机播放: " + (ConfigManager.get().shuffle ? "§a开启" : "§c关闭")
                            ));
                            return 1;
                        })
                    )

                    // volume <0-100>
                    .then(ClientCommands.literal("volume")
                        .then(ClientCommands.argument("value", FloatArgumentType.floatArg(0.0f, 100.0f))
                            .executes(context -> {
                                if (player == null) {
                                    context.getSource().sendError(Component.literal("§c播放器未初始化"));
                                    return 1;
                                }
                                float percent = FloatArgumentType.getFloat(context, "value");
                                float volume = percent / 100.0f;
                                ConfigManager.setVolume(volume);
                                context.getSource().sendFeedback(Component.literal(
                                    "§a音量已设置为: §e" + Math.round(percent) + "%"
                                ));
                                return 1;
                            })
                        )
                        .executes(context -> {
                            // 显示当前音量
                            int currentPercent = Math.round(ConfigManager.get().volume * 100);
                            context.getSource().sendFeedback(Component.literal(
                                "§7当前音量: §e" + currentPercent + "%"
                            ));
                            context.getSource().sendFeedback(Component.literal(
                                "§e/music volume <0-100> §7- 设置音量"
                            ));
                            return 1;
                        })
                    )

                    // next
                    .then(ClientCommands.literal("next")
                        .executes(context -> {
                            if (player == null) {
                                context.getSource().sendError(Component.literal("§c播放器未初始化"));
                                return 1;
                            }
                            player.next();
                            context.getSource().sendFeedback(Component.literal("§a切换到下一首"));
                            return 1;
                        })
                        .then(ClientCommands.literal("force")
                            .executes(context -> {
                                if (player == null) {
                                    context.getSource().sendError(Component.literal("§c播放器未初始化"));
                                    return 1;
                                }
                                player.nextForce();
                                context.getSource().sendFeedback(Component.literal("§a强制切换到下一首"));
                                return 1;
                            })
                        )
                    )

                    // prev
                    .then(ClientCommands.literal("prev")
                        .executes(context -> {
                            if (player == null) {
                                context.getSource().sendError(Component.literal("§c播放器未初始化"));
                                return 1;
                            }
                            player.prev();
                            context.getSource().sendFeedback(Component.literal("§a切换到上一首"));
                            return 1;
                        })
                        .then(ClientCommands.literal("force")
                            .executes(context -> {
                                if (player == null) {
                                    context.getSource().sendError(Component.literal("§c播放器未初始化"));
                                    return 1;
                                }
                                player.prevForce();
                                context.getSource().sendFeedback(Component.literal("§a强制切换到上一首"));
                                return 1;
                            })
                        )
                    )

                    // pause
                    .then(ClientCommands.literal("pause")
                        .executes(context -> {
                            if (player == null) {
                                context.getSource().sendError(Component.literal("§c播放器未初始化"));
                                return 1;
                            }
                            player.pause();
                            context.getSource().sendFeedback(Component.literal("§a已暂停"));
                            return 1;
                        })
                    )

                    // continue
                    .then(ClientCommands.literal("continue")
                        .executes(context -> {
                            if (player == null) {
                                context.getSource().sendError(Component.literal("§c播放器未初始化"));
                                return 1;
                            }
                            player.resume();
                            context.getSource().sendFeedback(Component.literal("§a已继续"));
                            return 1;
                        })
                    )

                    // play
                    .then(ClientCommands.literal("play")
                        .executes(context -> {
                            if (player == null) {
                                context.getSource().sendError(Component.literal("§c播放器未初始化"));
                                return 1;
                            }
                            List<String> list = player.getList();
                            if (list.isEmpty()) {
                                context.getSource().sendError(Component.literal("§c播放列表为空"));
                                return 1;
                            }
                            StringBuilder sb = new StringBuilder("§6当前播放列表:\n");
                            for (int i = 0; i < list.size(); i++) {
                                String marker = (i == player.getCurrentIndex()) ? "§a▶ " : "§7  ";
                                sb.append(marker).append(i + 1).append(". ").append(list.get(i)).append("\n");
                            }
                            context.getSource().sendFeedback(Component.literal(sb.toString()));
                            context.getSource().sendFeedback(Component.literal(
                                "§e随机模式: " + (ConfigManager.get().shuffle ? "§a开启" : "§c关闭") +
                                "  |  音量: §e" + Math.round(ConfigManager.get().volume * 100) + "%"
                            ));
                            context.getSource().sendFeedback(Component.literal(
                                "§e/music play \"<歌曲名>\" 或 §e/music play <编号> 播放指定歌曲"
                            ));
                            return 1;
                        })
                        .then(ClientCommands.argument("songName", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                if (player == null) {
                                    return builder.buildFuture();
                                }
                                List<String> names = player.getList();
                                for (String name : names) {
                                    String displayName = name.replaceFirst("\\.(ogg|wav)$", "");
                                    if (displayName.contains(" ")) {
                                        builder.suggest("\"" + displayName + "\"", Component.literal("§7" + displayName));
                                    } else {
                                        builder.suggest(displayName, Component.literal("§7" + displayName));
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> {
                                if (player == null) {
                                    context.getSource().sendError(Component.literal("§c播放器未初始化"));
                                    return 1;
                                }
                                String rawSongName = StringArgumentType.getString(context, "songName");
                                if (rawSongName == null || rawSongName.isEmpty()) {
                                    context.getSource().sendError(Component.literal("§c请输入歌曲名"));
                                    return 1;
                                }

                                String songName = rawSongName;
                                if (songName.startsWith("\"") && songName.endsWith("\"")) {
                                    songName = songName.substring(1, songName.length() - 1);
                                }

                                // 尝试解析为数字（编号）
                                try {
                                    int index = Integer.parseInt(songName) - 1;
                                    if (index >= 0 && index < player.getList().size()) {
                                        player.playIndex(index);
                                        context.getSource().sendFeedback(Component.literal(
                                            "§a正在播放: " + player.getList().get(index)
                                        ));
                                        return 1;
                                    } else {
                                        context.getSource().sendError(Component.literal("§c无效的编号"));
                                        return 1;
                                    }
                                } catch (NumberFormatException ignored) {
                                    // 不是数字，按名称搜索
                                }

                                List<String> list = player.getList();
                                List<Integer> matches = new ArrayList<>();
                                for (int i = 0; i < list.size(); i++) {
                                    String name = list.get(i);
                                    String nameWithoutExt = name.replaceFirst("\\.(ogg|wav)$", "");
                                    if (nameWithoutExt.equalsIgnoreCase(songName) ||
                                        nameWithoutExt.toLowerCase().contains(songName.toLowerCase())) {
                                        matches.add(i);
                                    }
                                }

                                if (matches.isEmpty()) {
                                    context.getSource().sendError(Component.literal("§c未找到歌曲: " + songName));
                                    return 1;
                                }

                                if (matches.size() > 1) {
                                    context.getSource().sendFeedback(Component.literal("§e找到多个匹配，请选择:"));
                                    for (int idx : matches) {
                                        context.getSource().sendFeedback(Component.literal("§7- " + list.get(idx)));
                                    }
                                    context.getSource().sendFeedback(Component.literal(
                                        "§e使用 /music play \"完整文件名\" 或 /music play <编号>"
                                    ));
                                    return 1;
                                }

                                int index = matches.get(0);
                                player.playIndex(index);
                                context.getSource().sendFeedback(Component.literal("§a正在播放: " + list.get(index)));
                                return 1;
                            })
                        )
                    )

                    // help
                    .then(ClientCommands.literal("help")
                        .executes(context -> {
                            context.getSource().sendFeedback(Component.literal("§6=== /music 命令帮助 ==="));
                            context.getSource().sendFeedback(Component.literal("§e/music reload §7- 重新扫描音乐文件夹"));
                            context.getSource().sendFeedback(Component.literal("§e/music shuffle §7- 切换随机播放模式"));
                            context.getSource().sendFeedback(Component.literal("§e/music volume §7- 显示当前音量"));
                            context.getSource().sendFeedback(Component.literal("§e/music volume <0-100> §7- 设置音量"));
                            context.getSource().sendFeedback(Component.literal("§e/music next §7- 播放下一首"));
                            context.getSource().sendFeedback(Component.literal("§e/music next force §7- 强制播放下一首"));
                            context.getSource().sendFeedback(Component.literal("§e/music prev §7- 播放上一首"));
                            context.getSource().sendFeedback(Component.literal("§e/music prev force §7- 强制播放上一首"));
                            context.getSource().sendFeedback(Component.literal("§e/music pause §7- 暂停播放"));
                            context.getSource().sendFeedback(Component.literal("§e/music continue §7- 继续播放"));
                            context.getSource().sendFeedback(Component.literal("§e/music play §7- 显示播放列表"));
                            context.getSource().sendFeedback(Component.literal("§e/music play \"<歌曲名>\" §7- 播放指定歌曲"));
                            context.getSource().sendFeedback(Component.literal("§e/music play <编号> §7- 播放指定编号"));
                            return 1;
                        })
                    )
            );
        });
    }
}