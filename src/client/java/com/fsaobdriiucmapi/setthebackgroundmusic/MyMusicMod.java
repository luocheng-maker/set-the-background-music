package com.fsaobdriiucmapi.setthebackgroundmusic;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public class MyMusicMod implements ClientModInitializer {
    public static final String MOD_ID = "setthebackgroundmusic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static MusicPlayer player;
    private static MusicTickHandler tickHandler;

    public static MusicPlayer getPlayer() { return player; }

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        List<Path> musicFiles = MusicFileScanner.scan();
        if (musicFiles.isEmpty()) {
            LOGGER.warn("No music files found. Use /music reload after adding songs.");
        } else if (!ConfigManager.get().modEnabled) {
            LOGGER.info("Mod disabled in config.");
        } else {
            activatePlayer(musicFiles);
        }

        MusicHud.register();

        // ========== 新增：注册播放失败回调 ==========
        AudioPlayer.setOnPlaybackFailed(file -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                String fileName = file.getFileName().toString();
                int dot = fileName.lastIndexOf('.');
                String ext = dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "未知";
                String msg = "§c此歌曲无法播放，请尝试转换格式。目前的音乐格式为 §e" + ext
                        + "§c，推荐使用 §e.ogg / .wav / .mp3 / .aiff§c。";
                mc.player.sendSystemMessage(Component.literal(msg));
            }
            MusicPlayer p = getPlayer();
            if (p != null) p.replayPrevious();
        });
        // ==========================================

        registerCommands();
        LOGGER.info("Registered /music commands.");
    }

    private static void activatePlayer(List<Path> files) {
        player = new MusicPlayer(files);
        tickHandler = new MusicTickHandler(player);
        tickHandler.register();

        String savedCat = ConfigManager.get().activeCategory;
        if (savedCat != null && !savedCat.isEmpty()) {
            player.setCategory(savedCat);
        }
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(
                ClientCommands.literal("music")
                    .then(ClientCommands.literal("reload").executes(ctx -> {
                        List<Path> files = MusicFileScanner.scan();
                        if (files.isEmpty()) {
                            ctx.getSource().sendError(Component.literal(
                                    "§c没有找到任何音乐文件，请把歌放进 config/setthebackgroundmusic/music/"));
                            return 0;
                        }
                        if (player == null) {
                            activatePlayer(files);
                            ctx.getSource().sendFeedback(Component.literal(
                                    "§a播放器已激活，加载 " + files.size() + " 首歌曲！"));
                        } else {
                            player.reload();
                            ctx.getSource().sendFeedback(Component.literal(
                                    "§a音乐列表已刷新！共 " + player.getList().size() + " 首。"));
                        }
                        return 1;
                    }))
                    .then(ClientCommands.literal("shuffle").executes(ctx -> {
                        if (requirePlayer(ctx)) return 0;
                        ConfigManager.toggleShuffle();
                        ctx.getSource().sendFeedback(Component.literal(
                                "§a随机播放: " + (ConfigManager.get().shuffle ? "开启" : "关闭")));
                        return 1;
                    }))
                    .then(ClientCommands.literal("shufflemode")
                        .then(ClientCommands.argument("mode", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                builder.suggest("TRUE_RANDOM");
                                builder.suggest("NO_REPEAT");
                                builder.suggest("WEIGHTED");
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String mode = StringArgumentType.getString(ctx, "mode").toUpperCase();
                                if (!mode.equals("TRUE_RANDOM") && !mode.equals("NO_REPEAT") && !mode.equals("WEIGHTED")) {
                                    ctx.getSource().sendError(Component.literal(
                                            "§c未知模式，可选: TRUE_RANDOM / NO_REPEAT / WEIGHTED"));
                                    return 0;
                                }
                                ConfigManager.get().shuffleMode = mode;
                                ConfigManager.save();
                                ctx.getSource().sendFeedback(Component.literal("§a随机模式: §e" + mode));
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommands.literal("volume")
                        .then(ClientCommands.argument("value", FloatArgumentType.floatArg(0.0f, 100.0f))
                            .executes(ctx -> {
                                float percent = FloatArgumentType.getFloat(ctx, "value");
                                ConfigManager.setVolume(percent / 100.0f);
                                ctx.getSource().sendFeedback(Component.literal(
                                        "§a音量已设置为: §e" + Math.round(percent) + "%"));
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommands.literal("next").executes(ctx -> {
                        if (requirePlayer(ctx)) return 0;
                        player.next();
                        return 1;
                    }))
                    .then(ClientCommands.literal("prev").executes(ctx -> {
                        if (requirePlayer(ctx)) return 0;
                        player.prev();
                        return 1;
                    }))
                    .then(ClientCommands.literal("pause").executes(ctx -> {
                        if (requirePlayer(ctx)) return 0;
                        player.pause();
                        return 1;
                    }))
                    .then(ClientCommands.literal("continue").executes(ctx -> {
                        if (requirePlayer(ctx)) return 0;
                        player.resume();
                        return 1;
                    }))
                    .then(ClientCommands.literal("category")
                        .executes(ctx -> {
                            List<String> cats = MusicFileScanner.listCategories();
                            StringBuilder sb = new StringBuilder("§6分类列表:\n");
                            sb.append("§7  - (all) 全部分类\n");
                            for (String c : cats) sb.append("§7  - ").append(c).append("\n");
                            if (cats.isEmpty()) sb.append("§7  （还没有子文件夹）\n");
                            ctx.getSource().sendFeedback(Component.literal(sb.toString()));
                            return 1;
                        })
                        .then(ClientCommands.argument("name", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                builder.suggest("all");
                                for (String c : MusicFileScanner.listCategories()) builder.suggest(c);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                if (requirePlayer(ctx)) return 0;
                                String cat = StringArgumentType.getString(ctx, "name");
                                if (cat.equalsIgnoreCase("all")) {
                                    player.setCategory("");
                                    ctx.getSource().sendFeedback(Component.literal("§a已切换到: 全部分类"));
                                } else {
                                    player.setCategory(cat);
                                    ctx.getSource().sendFeedback(Component.literal("§a已切换到分类: §e" + cat));
                                }
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommands.literal("fav")
                        .executes(ctx -> {
                            if (requirePlayer(ctx)) return 0;
                            String track = player.getCurrentTrackName();
                            if (track.isEmpty()) {
                                ctx.getSource().sendError(Component.literal("§c当前没有播放歌曲"));
                                return 0;
                            }
                            boolean added = player.toggleFavoriteCurrent();
                            ctx.getSource().sendFeedback(Component.literal(
                                    added ? "§a已收藏: §e" + track : "§7已取消收藏: §e" + track));
                            return 1;
                        })
                        .then(ClientCommands.literal("list").executes(ctx -> {
                            if (requirePlayer(ctx)) return 0;
                            List<String> favs = player.getFavorites();
                            if (favs.isEmpty()) {
                                ctx.getSource().sendFeedback(Component.literal("§7没有收藏的歌曲"));
                                return 1;
                            }
                            StringBuilder sb = new StringBuilder("§6收藏列表 (" + favs.size() + "):\n");
                            for (String f : favs) sb.append("§7  ★ ").append(f).append("\n");
                            ctx.getSource().sendFeedback(Component.literal(sb.toString()));
                            return 1;
                        }))
                    )
                    .then(ClientCommands.literal("history")
                        .executes(ctx -> {
                            if (requirePlayer(ctx)) return 0;
                            List<String> h = player.getHistory();
                            if (h.isEmpty()) {
                                ctx.getSource().sendFeedback(Component.literal("§7暂无播放历史"));
                                return 1;
                            }
                            StringBuilder sb = new StringBuilder("§6最近播放 (" + h.size() + "):\n");
                            int limit = Math.min(10, h.size());
                            for (int i = 0; i < limit; i++) {
                                sb.append("§7  ").append(i + 1).append(". ").append(h.get(i)).append("\n");
                            }
                            ctx.getSource().sendFeedback(Component.literal(sb.toString()));
                            return 1;
                        })
                    )
                    .then(ClientCommands.literal("hud").executes(ctx -> {
                        ModConfig cfg = ConfigManager.get();
                        cfg.hudEnabled = !cfg.hudEnabled;
                        ConfigManager.save();
                        ctx.getSource().sendFeedback(Component.literal(
                                "§aHUD: " + (cfg.hudEnabled ? "开启" : "关闭")));
                        return 1;
                    }))
                    .then(ClientCommands.literal("fade").executes(ctx -> {
                        ModConfig cfg = ConfigManager.get();
                        cfg.fadeEnabled = !cfg.fadeEnabled;
                        ConfigManager.save();
                        ctx.getSource().sendFeedback(Component.literal(
                                "§a淡入淡出: " + (cfg.fadeEnabled ? "开启" : "关闭")));
                        return 1;
                    }))
                    .then(ClientCommands.literal("play")
                        .executes(ctx -> {
                            if (requirePlayer(ctx)) return 0;
                            List<String> list = player.getList();
                            StringBuilder sb = new StringBuilder("§6当前播放列表 (" + list.size() + "):\n");
                            for (int i = 0; i < list.size(); i++) {
                                String marker = (i == player.getCurrentIndex()) ? "§a▶ " : "§7  ";
                                sb.append(marker).append(i + 1).append(". ").append(list.get(i)).append("\n");
                            }
                            ctx.getSource().sendFeedback(Component.literal(sb.toString()));
                            return 1;
                        })
                        .then(ClientCommands.argument("songName", StringArgumentType.string())
                            .suggests((ctx, builder) -> {
                                if (player == null) return builder.buildFuture();
                                for (String name : player.getList()) {
                                    builder.suggest(name.replaceFirst(MusicFileScanner.EXT_REGEX, ""));
                                }
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                if (requirePlayer(ctx)) return 0;
                                String songName = StringArgumentType.getString(ctx, "songName");
                                int idx = player.findIndexByName(songName);
                                if (idx >= 0) {
                                    player.playIndex(idx);
                                    ctx.getSource().sendFeedback(Component.literal(
                                            "§a正在播放: " + player.getList().get(idx)));
                                } else {
                                    ctx.getSource().sendError(Component.literal("§c未找到歌曲: " + songName));
                                }
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommands.literal("testfx").executes(ctx -> {
                        JavaFXTestScreen.show();
                        ctx.getSource().sendFeedback(Component.literal("§a正在打开 JavaFX 测试窗口..."));
                        return 1;
                    }))
                    .then(ClientCommands.literal("help").executes(ctx -> {
                        String[] lines = {
                            "§6=== /music 命令帮助 ===",
                            "§e/music reload §7- 刷新歌曲列表（首次放歌后先用这个）",
                            "§e/music shuffle §7- 开关随机播放",
                            "§e/music shufflemode <mode> §7- TRUE_RANDOM/NO_REPEAT/WEIGHTED",
                            "§e/music volume <0-100> §7- 设置音量",
                            "§e/music next / prev §7- 上/下一首",
                            "§e/music pause / continue §7- 暂停/继续",
                            "§e/music category [name|all] §7- 切换分类",
                            "§e/music fav §7- 收藏/取消当前歌曲",
                            "§e/music fav list §7- 查看收藏",
                            "§e/music history §7- 查看播放历史",
                            "§e/music hud §7- 开关 HUD",
                            "§e/music fade §7- 开关淡入淡出",
                            "§e/music play [歌名] §7- 播放指定歌曲",
                            "§e/music testfx §7- 测试 JavaFX"
                        };
                        for (String line : lines) {
                            ctx.getSource().sendFeedback(Component.literal(line));
                        }
                        return 1;
                    }))
            );
        });
    }

    private static boolean requirePlayer(com.mojang.brigadier.context.CommandContext<
            net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> ctx) {
        if (player == null) {
            ctx.getSource().sendError(Component.literal(
                    "§c播放器未初始化，请先执行 §e/music reload"));
            return true;
        }
        return false;
    }
}