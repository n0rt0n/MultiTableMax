package com.multitablemax.ui;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.util.Map;

/**
 * Рисует картинку босса выбранным способом ({@link BossArt}).
 * Босс привязан к множителю-«хозяину» (bossId 2..9), либо 0 — общий босс
 * (когда тренируется несколько таблиц или вся таблица).
 */
final class BossArtist {

    private BossArtist() {
    }

    private static final Map<Integer, String> EMOJI = Map.of(
            0, "👑",
            2, "🐸",
            3, "🐲",
            4, "🦖",
            5, "👾",
            6, "🐙",
            7, "🦂",
            8, "🐉",
            9, "👹");

    static Node create(BossArt art, int bossId, double size) {
        return art == BossArt.CANVAS ? canvas(bossId, size) : emoji(bossId, size);
    }

    private static Node emoji(int bossId, double size) {
        Label label = new Label(EMOJI.getOrDefault(bossId, "🤖"));
        label.setStyle("-fx-font-size: " + Math.round(size * 0.8) + "px;");
        return label;
    }

    private static Node canvas(int bossId, double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext g = canvas.getGraphicsContext2D();

        // Цвет и количество рожек зависят от босса — каждый выглядит по-своему.
        double hue = ((bossId <= 0 ? 11 : bossId) * 40) % 360;
        Color body = Color.hsb(hue, 0.55, 0.92);
        Color dark = Color.hsb(hue, 0.6, 0.6);
        int horns = (bossId <= 0) ? 3 : (bossId % 3) + 1;

        double cx = size / 2;
        double bodyTop = size * 0.22;
        double bodyW = size * 0.74;
        double bodyH = size * 0.70;
        double bodyLeft = cx - bodyW / 2;

        // Рожки
        g.setFill(dark);
        for (int i = 0; i < horns; i++) {
            double hx = bodyLeft + bodyW * (i + 1) / (horns + 1);
            g.fillPolygon(
                    new double[]{hx - size * 0.05, hx + size * 0.05, hx},
                    new double[]{bodyTop + size * 0.05, bodyTop + size * 0.05, bodyTop - size * 0.12},
                    3);
        }

        // Тело
        g.setFill(body);
        g.fillRoundRect(bodyLeft, bodyTop, bodyW, bodyH, size * 0.4, size * 0.4);

        // Глаза
        double eyeY = bodyTop + bodyH * 0.32;
        double eyeR = size * 0.11;
        double eyeDx = bodyW * 0.20;
        for (int s = -1; s <= 1; s += 2) {
            double ex = cx + s * eyeDx;
            g.setFill(Color.WHITE);
            g.fillOval(ex - eyeR, eyeY - eyeR, eyeR * 2, eyeR * 2);
            g.setFill(Color.web("#243b53"));
            double pr = eyeR * 0.5;
            g.fillOval(ex - pr, eyeY - pr, pr * 2, pr * 2);
        }

        // Рот с зубами
        double mouthY = bodyTop + bodyH * 0.66;
        double mouthW = bodyW * 0.5;
        double mouthH = size * 0.12;
        g.setFill(dark);
        g.fillRoundRect(cx - mouthW / 2, mouthY, mouthW, mouthH, mouthH, mouthH);
        g.setFill(Color.WHITE);
        int teeth = 4;
        double toothW = mouthW / teeth;
        for (int i = 0; i < teeth; i++) {
            double tx = cx - mouthW / 2 + i * toothW + toothW * 0.15;
            g.fillRect(tx, mouthY, toothW * 0.7, mouthH * 0.45);
        }

        return canvas;
    }
}
