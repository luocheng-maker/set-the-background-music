package com.fsaobdriiucmapi.setthebackgroundmusic;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class JavaFXHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("JavaFXHelper");
    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        LOGGER.info("Initializing JavaFX Toolkit...");
        new JFXPanel();
        initialized = true;
        LOGGER.info("JavaFX Toolkit initialized.");
    }

    public static void runAndWait(Runnable task) {
        if (!initialized) init();
        if (Platform.isFxApplicationThread()) {
            task.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while waiting for JavaFX task", e);
            return;
        }
        if (error[0] != null) {
            LOGGER.error("Error in JavaFX task", error[0]);
            throw new RuntimeException(error[0]);
        }
    }

    public static void runLater(Runnable task) {
        if (!initialized) init();
        Platform.runLater(task);
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static synchronized void shutdown() {
        if (!initialized) return;
        try {
            LOGGER.info("Shutting down JavaFX Toolkit...");
            Platform.exit();
            initialized = false;
            LOGGER.info("JavaFX Toolkit shutdown requested.");
        } catch (Throwable t) {
            LOGGER.warn("Error shutting down JavaFX", t);
        }
    }
}