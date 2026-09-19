package com.fsaobdriiucmapi.setthebackgroundmusic;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaFXTestScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger("JavaFXTestScreen");
    private static Stage stage;

    public static void show() {
        JavaFXHelper.init();
        JavaFXHelper.runLater(() -> {
            try {
                if (stage != null && stage.isShowing()) {
                    stage.toFront();
                    return;
                }
                stage = new Stage();
                stage.setTitle("JavaFX in Fabric - Test");

                Label label = new Label("JavaFX 25 运行正常！");
                label.setStyle("-fx-font-size: 18px; -fx-text-fill: #00ff88;");

                Button closeBtn = new Button("关闭");
                closeBtn.setOnAction(e -> {
                    stage.close();
                    stage = null;
                });

                VBox root = new VBox(20, label, closeBtn);
                root.setStyle("-fx-padding: 30px; -fx-alignment: center; -fx-background-color: #1a1a2e;");

                Scene scene = new Scene(root, 350, 200);
                stage.setScene(scene);
                stage.show();
                LOGGER.info("JavaFX test window shown.");
            } catch (Exception e) {
                LOGGER.error("Failed to show JavaFX window", e);
            }
        });
    }

    public static void close() {
        if (stage != null) {
            JavaFXHelper.runLater(() -> {
                stage.close();
                stage = null;
            });
        }
    }
}