package com.smartsociety.ui;

import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AnimationUtils {

    public static Scene buildScaledScene(Parent root, Stage stage, double baseWidth, double baseHeight) {
        StackPane scaleRoot = new StackPane(root);
        scaleRoot.setPickOnBounds(false);
        double sceneWidth = stage.getWidth();
        double sceneHeight = stage.getHeight();
        if (sceneWidth <= 0 || sceneHeight <= 0) {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            sceneWidth = bounds.getWidth();
            sceneHeight = bounds.getHeight();
        }

        Scene scene = new Scene(scaleRoot, sceneWidth, sceneHeight);
        DoubleBinding safeScale = Bindings.createDoubleBinding(() -> {
            double w = scene.getWidth();
            double h = scene.getHeight();
            double padX = Math.max(36, w * 0.045);
            double padY = Math.max(24, h * 0.03);
            double sw = (w - padX * 2) / baseWidth;
            double sh = (h - padY * 2) / baseHeight;
            return Math.min(1.0, Math.min(sw, sh));
        }, scene.widthProperty(), scene.heightProperty());
        root.scaleXProperty().bind(safeScale);
        root.scaleYProperty().bind(root.scaleXProperty());
        return scene;
    }

    public static StackPane createScaledContent(Parent content, Scene scene, double baseWidth, double baseHeight) {
        StackPane wrapper = new StackPane(content);
        wrapper.setPickOnBounds(false);
        DoubleBinding safeScale = Bindings.createDoubleBinding(() -> {
            double w = scene.getWidth();
            double h = scene.getHeight();
            double padX = Math.max(36, w * 0.045);
            double padY = Math.max(24, h * 0.03);
            double sw = (w - padX * 2) / baseWidth;
            double sh = (h - padY * 2) / baseHeight;
            return Math.min(1.0, Math.min(sw, sh));
        }, scene.widthProperty(), scene.heightProperty());
        content.scaleXProperty().bind(safeScale);
        content.scaleYProperty().bind(content.scaleXProperty());
        return wrapper;
    }

    public static void sizeStageToScreen(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    public static void applyFullScreen(Stage stage) {
        sizeStageToScreen(stage);
        stage.setResizable(false);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        javafx.application.Platform.runLater(() -> stage.setFullScreen(true));
    }

    public static void fadeIn(Node node, int durationMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setInterpolator(Interpolator.EASE_OUT);
        ft.play();
    }

    public static void fadeOut(Node node, int durationMs, Runnable onDone) {
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setInterpolator(Interpolator.EASE_IN);
        if (onDone != null) ft.setOnFinished(e -> onDone.run());
        ft.play();
    }

    public static void switchSection(Node from, Node to, int direction) {
        double slide = direction * 22.0;

        FadeTransition fo = new FadeTransition(Duration.millis(140), from);
        fo.setFromValue(1.0);
        fo.setToValue(0.0);
        TranslateTransition so = new TranslateTransition(Duration.millis(140), from);
        so.setToX(-slide * 0.5);
        ParallelTransition out = new ParallelTransition(fo, so);

        out.setOnFinished(e -> {
            from.setVisible(false);
            from.setManaged(false);
            from.setTranslateX(0);
            from.setOpacity(1.0);

            to.setOpacity(0);
            to.setTranslateX(slide);
            to.setVisible(true);
            to.setManaged(true);

            FadeTransition fi = new FadeTransition(Duration.millis(220), to);
            fi.setFromValue(0.0);
            fi.setToValue(1.0);
            fi.setInterpolator(Interpolator.EASE_OUT);
            TranslateTransition si = new TranslateTransition(Duration.millis(220), to);
            si.setFromX(slide);
            si.setToX(0);
            si.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fi, si).play();
        });

        out.play();
    }

    public static void introAnimation(Node node) {
        node.setOpacity(0);
        node.setTranslateY(18);
        node.setScaleX(0.985);
        node.setScaleY(0.985);
        FadeTransition fi = new FadeTransition(Duration.millis(480), node);
        fi.setFromValue(0.0);
        fi.setToValue(1.0);
        fi.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition si = new TranslateTransition(Duration.millis(480), node);
        si.setFromY(18);
        si.setToY(0);
        si.setInterpolator(Interpolator.EASE_OUT);
        ScaleTransition sc = new ScaleTransition(Duration.millis(520), node);
        sc.setFromX(0.985);
        sc.setFromY(0.985);
        sc.setToX(1.0);
        sc.setToY(1.0);
        sc.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fi, si, sc).play();
    }

    public static void sceneTransition(Node currentRoot, Runnable loadNewScene) {
        FadeTransition fo = new FadeTransition(Duration.millis(200), currentRoot);
        fo.setFromValue(1.0);
        fo.setToValue(0.0);
        fo.setInterpolator(Interpolator.EASE_IN);
        fo.setOnFinished(e -> loadNewScene.run());
        fo.play();
    }

    public static void addHoverScale(Node node) {
        node.setOnMouseEntered(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(90), node);
            s.setToX(1.025); s.setToY(1.025);
            s.setInterpolator(Interpolator.EASE_OUT);
            s.play();
        });
        node.setOnMouseExited(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(90), node);
            s.setToX(1.0); s.setToY(1.0);
            s.setInterpolator(Interpolator.EASE_OUT);
            s.play();
        });
    }

    public static void addHoverLift(Node node) {
        node.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(130), node);
            scale.setToX(1.012);
            scale.setToY(1.012);
            scale.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition lift = new TranslateTransition(Duration.millis(130), node);
            lift.setToY(-2);
            lift.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(scale, lift).play();
        });
        node.setOnMousePressed(e -> node.setTranslateY(0));
        node.setOnMouseReleased(e -> node.setTranslateY(node.isHover() ? -2 : 0));
        node.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), node);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition lift = new TranslateTransition(Duration.millis(150), node);
            lift.setToY(0);
            lift.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(scale, lift).play();
        });
    }

    public static void installAmbientMotion(Pane layer) {
        if (layer == null) return;
        int index = 0;
        for (Node node : layer.getChildren()) {
            node.setMouseTransparent(true);
            node.setManaged(false);

            TranslateTransition drift = new TranslateTransition(Duration.seconds(6 + index), node);
            drift.setByX(index % 2 == 0 ? 58 : -50);
            drift.setByY(index % 2 == 0 ? -38 : 44);
            drift.setAutoReverse(true);
            drift.setCycleCount(Animation.INDEFINITE);
            drift.setInterpolator(Interpolator.EASE_BOTH);

            ScaleTransition breathe = new ScaleTransition(Duration.seconds(5 + index), node);
            breathe.setToX(1.12);
            breathe.setToY(1.12);
            breathe.setAutoReverse(true);
            breathe.setCycleCount(Animation.INDEFINITE);
            breathe.setInterpolator(Interpolator.EASE_BOTH);

            RotateTransition rotate = new RotateTransition(Duration.seconds(13 + index * 2), node);
            rotate.setByAngle(index % 2 == 0 ? 16 : -18);
            rotate.setAutoReverse(true);
            rotate.setCycleCount(Animation.INDEFINITE);
            rotate.setInterpolator(Interpolator.EASE_BOTH);

            new ParallelTransition(drift, breathe, rotate).play();
            index++;
        }
    }

    public static void shakeNode(Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(55), node);
        shake.setFromX(0);
        shake.setByX(9);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setInterpolator(Interpolator.EASE_BOTH);
        shake.setOnFinished(e -> node.setTranslateX(0));
        shake.play();
    }

    public static void showAutoFadeStatus(Label label, String msg, String styleClass) {
        label.setText(msg);
        label.getStyleClass().setAll(styleClass);
        label.setOpacity(0);
        FadeTransition fi = new FadeTransition(Duration.millis(180), label);
        fi.setFromValue(0.0);
        fi.setToValue(1.0);
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        FadeTransition fo = new FadeTransition(Duration.millis(280), label);
        fo.setFromValue(1.0);
        fo.setToValue(0.0);
        new SequentialTransition(fi, pause, fo).play();
    }
}
