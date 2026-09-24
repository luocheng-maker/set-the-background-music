package com.fsaobdriiucmapi.setthebackgroundmusic;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
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

        AudioPlayer.setOnPlaybackFailed(file -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                String fileName = file.getFileName().toString();
                int dot = fileName.lastIndexOf('.');
                String ext = dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "unknown";
                mc.player.sendSystemMessage(Component.translatable("stbm.playback.failed", ext));
            }
            MusicPlayer p = getPlayer();
            if (p != null) p.replayPrevious();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            LOGGER.info("Client stopping, releasing audio resources...");
            try {
                AudioPlayer.stop();
            } catch (Throwable t) {
                LOGGER.warn("Error stopping AudioPlayer", t);
            }
            JavaFXHelper.shutdown();
        });

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
                            ctx.getSource().sendError(Component.translatable("stbm.cmd.no_music"));
                            return 0;
                        }
                        if (player == null) {
                            activatePlayer(files);
                            ctx.getSource().sendFeedback(Component.translatable(
                                    "stbm.cmd.reload_new", String.valueOf(files.size())));
                        } else {
                            player.reload();
                            ctx.getSource().sendFeedback(Component.translatable(
                                    "stbm.cmd.reload_done", String.valueOf(player.getList().size())));
                        }
                        return 1;
                    }))
                    .then(ClientCommands.literal("shuffle").executes(ctx -> {
                        if (requirePlayer(ctx)) return 0;
                        ConfigManager.toggleShuffle();
                        ctx.getSource().sendFeedback(Component.translatable(
                                ConfigManager.get().shuffle ? "stbm.cmd.shuffle_on" : "stbm.cmd.shuffle_off"));
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
                                    ctx.getSource().sendError(Component.translatable("stbm.cmd.shufflemode.invalid"));
                                    return 0;
                                }
                                ConfigManager.get().shuffleMode = mode;
                                ConfigManager.save();
                                ctx.getSource().sendFeedback(Component.translatable("stbm.cmd.shufflemode.set", mode));
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommands.literal("volume")
                        .then(ClientCommands.argument("value", FloatArgumentType.floatArg(0.0f, 100.0f))
                            .executes(ctx -> {
                                float percent = FloatArgumentType.getFloat(ctx, "value");
                                ConfigManager.setVolume(percent / 100.0f);
                                ctx.getSource().sendFeedback(Component.translatable(
                                        "stbm.cmd.volume.set", String.valueOf(Math.round(percent))));
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
                            StringBuilder sb = new StringBuilder(
                                    Component.translatable("stbm.cmd.category.list_header").getString()).append("\n");
                            sb.append(Component.translatable("stbm.cmd.category.list_all").getString()).append("\n");
                            for (String c : cats) sb.append("§7  - ").append(c).append("\n");
                            if (cats.isEmpty()) {
                                sb.append(Component.translatable("stbm.cmd.category.list_empty").getString()).append("\n");
                            }
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
                                    ctx.getSource().sendFeedback(Component.translatable("stbm.cmd.category.all"));
                                } else {
                                    player.setCategory(cat);
                                    ctx.getSource().sendFeedback(Component.translatable("stbm.cmd.category.switched", cat));
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
                                ctx.getSource().sendError(Component.translatable("stbm.cmd.fav.no_track"));
                                return 0;
                            }
                            boolean added = player.toggleFavoriteCurrent();
                            ctx.getSource().sendFeedback(Component.translatable(
                                    added ? "stbm.cmd.fav.added" : "stbm.cmd.fav.removed", track));
                            return 1;
                        })
                        .then(ClientCommands.literal("list").executes(ctx -> {
                            if (requirePlayer(ctx)) return 0;
                            List<String> favs = player.getFavorites();
                            if (favs.isEmpty()) {
                                ctx.getSource().sendFeedback(Component.translatable("stbm.cmd.fav.empty"));
                                return 1;
                            }
                            StringBuilder sb = new StringBuilder(
                                    Component.translatable("stbm.cmd.fav.list_header",
                                            String.valueOf(favs.size())).getString()).append("\n");
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
                                ctx.getSource().sendFeedback(Component.translatable("stbm.cmd.history.empty"));
                                return 1;
                            }
                            StringBuilder sb = new StringBuilder(
                                    Component.translatable("stbm.cmd.history.header",
                                            String.valueOf(h.size())).getString()).append("\n");
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
                        ctx.getSource().sendFeedback(Component.translatable(
                                cfg.hudEnabled ? "stbm.cmd.hud.on" : "stbm.cmd.hud.off"));
                        return 1;
                    }))
                    .then(ClientCommands.literal("fade").executes(ctx -> {
                        ModConfig cfg = ConfigManager.get();
                        cfg.fadeEnabled = !cfg.fadeEnabled;
                        ConfigManager.save();
                        ctx.getSource().sendFeedback(Component.translatable(
                                cfg.fadeEnabled ? "stbm.cmd.fade.on" : "stbm.cmd.fade.off"));
                        return 1;
                    }))
                    .then(ClientCommands.literal("play")
                        .executes(ctx -> {
                            if (requirePlayer(ctx)) return 0;
                            List<String> list = player.getList();
                            StringBuilder sb = new StringBuilder(
                                    Component.translatable("stbm.cmd.play.header",
                                            String.valueOf(list.size())).getString()).append("\n");
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
                                    ctx.getSource().sendFeedback(Component.translatable(
                                            "stbm.cmd.play.now", player.getList().get(idx)));
                                } else {
                                    ctx.getSource().sendError(Component.translatable(
                                            "stbm.cmd.play.not_found", songName));
                                }
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommands.literal("testfx").executes(ctx -> {
                        JavaFXTestScreen.show();
                        ctx.getSource().sendFeedback(Component.translatable("stbm.cmd.testfx.opening"));
                        return 1;
                    }))
                    .then(ClientCommands.literal("help").executes(ctx -> {
                        String[] keys = {
                            "stbm.cmd.help.header",
                            "stbm.cmd.help.line1", "stbm.cmd.help.line2", "stbm.cmd.help.line3",
                            "stbm.cmd.help.line4", "stbm.cmd.help.line5", "stbm.cmd.help.line6",
                            "stbm.cmd.help.line7", "stbm.cmd.help.line8", "stbm.cmd.help.line9",
                            "stbm.cmd.help.line10", "stbm.cmd.help.line11", "stbm.cmd.help.line12",
                            "stbm.cmd.help.line13", "stbm.cmd.help.line14"
                        };
                        for (String k : keys) {
                            ctx.getSource().sendFeedback(Component.translatable(k));
                        }
                        return 1;
                    }))
            );
        });
    }

    private static boolean requirePlayer(com.mojang.brigadier.context.CommandContext<
            net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> ctx) {
        if (player == null) {
            ctx.getSource().sendError(Component.translatable("stbm.cmd.player_not_ready"));
            return true;
        }
        return false;
    }
}