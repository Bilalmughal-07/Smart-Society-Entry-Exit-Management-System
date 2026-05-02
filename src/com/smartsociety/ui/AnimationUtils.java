package com.smartsociety.ui;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

public class AnimationUtils {

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
        node.setRotationAxis(Rotate.Y_AXIS);
        node.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(160), node);
            scale.setToX(1.025);
            scale.setToY(1.025);
            scale.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition lift = new TranslateTransition(Duration.millis(160), node);
            lift.setToY(-4);
            lift.setInterpolator(Interpolator.EASE_OUT);

            RotateTransition tilt = new RotateTransition(Duration.millis(160), node);
            tilt.setToAngle(-2.5);
            tilt.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(scale, lift, tilt).play();
        });
        node.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(180), node);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition lift = new TranslateTransition(Duration.millis(180), node);
            lift.setToY(0);
            lift.setInterpolator(Interpolator.EASE_OUT);

            RotateTransition tilt = new RotateTransition(Duration.millis(180), node);
            tilt.setToAngle(0);
            tilt.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(scale, lift, tilt).play();
        });
    }

    public static void installAmbientMotion(Pane layer) {
        if (layer == null) return;
        int index = 0;
        for (Node node : layer.getChildren()) {
            node.setMouseTransparent(true);
            node.setManaged(false);

            TranslateTransition drift = new TranslateTransition(Duration.seconds(9 + index * 2), node);
            drift.setByX(index % 2 == 0 ? 34 : -28);
            drift.setByY(index % 2 == 0 ? -22 : 26);
            drift.setAutoReverse(true);
            drift.setCycleCount(Animation.INDEFINITE);
            drift.setInterpolator(Interpolator.EASE_BOTH);

            ScaleTransition breathe = new ScaleTransition(Duration.seconds(7 + index), node);
            breathe.setToX(1.08);
            breathe.setToY(1.08);
            breathe.setAutoReverse(true);
            breathe.setCycleCount(Animation.INDEFINITE);
            breathe.setInterpolator(Interpolator.EASE_BOTH);

            RotateTransition rotate = new RotateTransition(Duration.seconds(20 + index * 3), node);
            rotate.setByAngle(index % 2 == 0 ? 9 : -11);
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
