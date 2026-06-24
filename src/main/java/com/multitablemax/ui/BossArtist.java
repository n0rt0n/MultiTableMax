package com.multitablemax.ui;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;

import java.util.Map;

/**
 * Рисует картинку босса выбранным способом ({@link BossArt}).
 * Босс привязан к множителю-«хозяину» (bossId 2..9), либо 0 — общий босс
 * (когда тренируется несколько таблиц или вся таблица).
 *
 * Для каждого множителя — свой архетип монстра, поэтому боссы разные.
 * Archetype/палитра здесь и в генераторе PNG совпадают по смыслу.
 */
final class BossArtist {

    private BossArtist() {
    }

    enum Arc { SLIME, CYCLOPS, ROBOT, GHOST, DEVIL, SPIKY, FLUFF, DRAGON, KING }

    private static Arc arc(int id) {
        return switch (id) {
            case 2 -> Arc.SLIME;
            case 3 -> Arc.CYCLOPS;
            case 4 -> Arc.ROBOT;
            case 5 -> Arc.GHOST;
            case 6 -> Arc.DEVIL;
            case 7 -> Arc.SPIKY;
            case 8 -> Arc.FLUFF;
            case 9 -> Arc.DRAGON;
            default -> Arc.KING;
        };
    }

    // {base, light, dark} в формате {r,g,b}
    private static int[][] palette(Arc a) {
        return switch (a) {
            case SLIME   -> new int[][]{{120, 210, 90}, {175, 240, 150}, {70, 160, 50}};
            case CYCLOPS -> new int[][]{{150, 90, 200}, {195, 145, 235}, {105, 55, 160}};
            case ROBOT   -> new int[][]{{150, 165, 180}, {200, 212, 224}, {95, 110, 128}};
            case GHOST   -> new int[][]{{120, 180, 235}, {180, 216, 250}, {80, 140, 205}};
            case DEVIL   -> new int[][]{{225, 80, 70}, {252, 135, 122}, {170, 42, 38}};
            case SPIKY   -> new int[][]{{40, 195, 182}, {120, 228, 218}, {20, 140, 132}};
            case FLUFF   -> new int[][]{{245, 150, 192}, {255, 198, 222}, {208, 108, 150}};
            case DRAGON  -> new int[][]{{140, 200, 60}, {188, 232, 118}, {92, 150, 34}};
            case KING    -> new int[][]{{242, 198, 70}, {255, 226, 132}, {196, 156, 38}};
        };
    }

    private static final Map<Integer, String> EMOJI = Map.of(
            0, "👑", 2, "🐸", 3, "🐲", 4, "🦖", 5, "👾",
            6, "🐙", 7, "🦂", 8, "🐉", 9, "👹");

    static Node create(BossArt art, int bossId, double size) {
        return switch (art) {
            case CANVAS -> canvas(bossId, size);
            case IMAGE -> image(bossId, size);
            case EMOJI -> emoji(bossId, size);
        };
    }

    private static Node emoji(int bossId, double size) {
        Label label = new Label(EMOJI.getOrDefault(bossId, "🤖"));
        label.setStyle("-fx-font-size: " + Math.round(size * 0.8) + "px;");
        return label;
    }

    private static Node image(int bossId, double size) {
        int id = EMOJI.containsKey(bossId) ? bossId : 0;
        var url = BossArtist.class.getResource("/bosses/boss-" + id + ".png");
        if (url == null) {
            return emoji(bossId, size);   // картинки нет — мягкий откат на emoji
        }
        return new ImageView(new Image(url.toExternalForm(), size, size, true, true));
    }

    // ---------- Canvas: тот же набор архетипов, что и в PNG ----------

    private static Node canvas(int bossId, double size) {
        Arc a = arc(bossId);
        int[][] p = palette(a);
        Canvas canvas = new Canvas(size, size);
        GraphicsContext g = canvas.getGraphicsContext2D();
        Color base = col(p[0]), light = col(p[1]), dark = col(p[2]);
        double s = size;

        // тень
        g.setFill(Color.rgb(0, 0, 0, 0.15));
        g.fillOval(s * 0.24, s * 0.86, s * 0.52, s * 0.06);

        switch (a) {
            case SLIME   -> slime(g, s, base, light, dark);
            case CYCLOPS -> cyclops(g, s, base, light, dark);
            case ROBOT   -> robot(g, s, base, light, dark);
            case GHOST   -> ghost(g, s, base, light, dark);
            case DEVIL   -> devil(g, s, base, light, dark);
            case SPIKY   -> spiky(g, s, base, light, dark);
            case FLUFF   -> fluff(g, s, base, light, dark);
            case DRAGON  -> dragon(g, s, base, light, dark);
            case KING    -> king(g, s, base, light, dark);
        }
        return canvas;
    }

    private static Color col(int[] c) {
        return Color.rgb(c[0], c[1], c[2]);
    }

    private static Paint grad(double cx, double cy, double r, Color light, Color base) {
        return new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                new Stop(0, light), new Stop(1, base));
    }

    private static void eye(GraphicsContext g, double x, double y, double r, double lx, double ly) {
        g.setFill(Color.WHITE);
        g.fillOval(x - r, y - r, r * 2, r * 2);
        g.setFill(Color.rgb(40, 44, 52));
        double pr = r * 0.55;
        g.fillOval(x - pr + lx, y - pr + ly, pr * 2, pr * 2);
        g.setFill(Color.WHITE);
        double gr = pr * 0.4;
        g.fillOval(x - pr * 0.5 + lx, y - pr * 0.7 + ly, gr * 2, gr * 2);
    }

    private static void smile(GraphicsContext g, double s, double cx, double y, double w, double depth, Color color) {
        g.setStroke(color);
        g.setLineWidth(s * 0.028);
        g.setLineCap(StrokeLineCap.ROUND);
        g.beginPath();
        g.moveTo(cx - w / 2, y);
        g.quadraticCurveTo(cx, y + depth * 2, cx + w / 2, y);
        g.stroke();
    }

    private static void tri(GraphicsContext g, double x1, double y1, double x2, double y2, double x3, double y3) {
        g.fillPolygon(new double[]{x1, x2, x3}, new double[]{y1, y2, y3}, 3);
    }

    private static void slime(GraphicsContext g, double s, Color base, Color light, Color dark) {
        double cx = s * 0.5, cy = s * 0.56, rw = s * 0.34, rh = s * 0.24;
        double bot = cy + rh, left = cx - rw, right = cx + rw;
        g.setFill(Color.color(dark.getRed(), dark.getGreen(), dark.getBlue(), 0.43));
        g.fillOval(cx - rw * 1.25, bot - rh * 0.18, rw * 2.5, rh * 0.5);
        g.setFill(grad(cx, cy - rh * 0.4, rw * 1.2, light, base));
        g.beginPath();
        g.moveTo(left, bot);
        g.quadraticCurveTo(left - rw * 0.08, cy - rh * 0.4, cx - rw * 0.5, cy - rh);
        g.quadraticCurveTo(cx, cy - rh * 1.5, cx + rw * 0.5, cy - rh);
        g.quadraticCurveTo(right + rw * 0.08, cy - rh * 0.4, right, bot);
        g.quadraticCurveTo(cx + rw * 0.5, bot + rh * 0.18, cx, bot);
        g.quadraticCurveTo(cx - rw * 0.5, bot + rh * 0.18, left, bot);
        g.closePath();
        g.fill();
        g.setFill(grad(cx, cy, rw, light, base));
        g.fillOval(right - rw * 0.1, cy - rh * 0.2, rw * 0.28, rh * 0.7);
        g.setFill(Color.rgb(255, 255, 255, 0.35));
        g.fillOval(cx - rw * 0.55, cy - rh * 0.85, rw * 0.4, rh * 0.5);
        eye(g, cx - rw * 0.34, cy - rh * 0.1, s * 0.075, s * 0.008, s * 0.008);
        eye(g, cx + rw * 0.34, cy - rh * 0.1, s * 0.075, s * 0.008, s * 0.008);
        smile(g, s, cx, cy + rh * 0.42, rw * 0.6, s * 0.045, dark);
    }

    private static void cyclops(GraphicsContext g, double s, Color base, Color light, Color dark) {
        double cx = s * 0.5, top = s * 0.22, w = s * 0.50, h = s * 0.62;
        g.setFill(dark);
        tri(g, cx - w * 0.34, top + s * 0.02, cx - w * 0.18, top + s * 0.02, cx - w * 0.30, top - s * 0.13);
        tri(g, cx + w * 0.18, top + s * 0.02, cx + w * 0.34, top + s * 0.02, cx + w * 0.30, top - s * 0.13);
        g.setFill(grad(cx, top + h * 0.3, w * 0.8, light, base));
        g.fillRoundRect(cx - w / 2, top, w, h, w * 0.55, w * 0.55);
        eye(g, cx, top + h * 0.38, s * 0.15, s * 0.012, s * 0.008);
        smile(g, s, cx, top + h * 0.74, w * 0.4, s * 0.04, dark);
    }

    private static void robot(GraphicsContext g, double s, Color base, Color light, Color dark) {
        double cx = s * 0.5, top = s * 0.26, w = s * 0.60, h = s * 0.56;
        g.setStroke(dark);
        g.setLineWidth(s * 0.02);
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(cx, top, cx, top - s * 0.12);
        g.setFill(dark);
        g.fillOval(cx - s * 0.035, top - s * 0.16, s * 0.07, s * 0.07);
        g.setFill(grad(cx, top + h * 0.3, w * 0.85, light, base));
        g.fillRoundRect(cx - w / 2, top, w, h, w * 0.22, w * 0.22);
        g.setFill(dark);
        g.fillOval(cx - w / 2 - s * 0.02, top + h * 0.4, s * 0.05, s * 0.05);
        g.fillOval(cx + w / 2 - s * 0.03, top + h * 0.4, s * 0.05, s * 0.05);
        double ey = top + h * 0.32, ew = s * 0.14, eh = s * 0.11;
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            double ex = cx + sgn * w * 0.2 - ew / 2;
            g.setFill(Color.WHITE);
            g.fillRoundRect(ex, ey, ew, eh, eh * 0.6, eh * 0.6);
            g.setFill(Color.rgb(40, 44, 52));
            g.fillOval(ex + ew * 0.3, ey + eh * 0.25, eh * 0.5, eh * 0.5);
        }
        double my = top + h * 0.66, mw = w * 0.5;
        g.setFill(dark);
        g.fillRoundRect(cx - mw / 2, my, mw, s * 0.07, s * 0.03, s * 0.03);
        g.setFill(base);
        for (int i = 1; i < 5; i++) {
            g.fillRect(cx - mw / 2 + i * (mw / 5), my, s * 0.012, s * 0.07);
        }
    }

    private static void ghost(GraphicsContext g, double s, Color base, Color light, Color dark) {
        double cx = s * 0.5, top = s * 0.22, w = s * 0.58, h = s * 0.60;
        double left = cx - w / 2, right = cx + w / 2, bot = top + h;
        g.setFill(grad(cx, top + h * 0.3, w * 0.8, light, base));
        g.beginPath();
        g.moveTo(left, bot);
        g.lineTo(left, top + w * 0.5);
        g.bezierCurveTo(left, top, right, top, right, top + w * 0.5);
        g.lineTo(right, bot);
        int humps = 4;
        double hw = w / humps;
        for (int i = 0; i < humps; i++) {
            double x1 = right - (i + 1) * hw;
            g.quadraticCurveTo(right - (i + 0.5) * hw, bot + s * 0.06, x1, bot);
        }
        g.closePath();
        g.fill();
        eye(g, cx - w * 0.16, top + h * 0.34, s * 0.085, s * 0.008, s * 0.004);
        eye(g, cx + w * 0.16, top + h * 0.34, s * 0.085, s * 0.008, s * 0.004);
        g.setFill(dark);
        g.fillOval(cx - s * 0.04, top + h * 0.56, s * 0.08, s * 0.1);
    }

    private static void devil(GraphicsContext g, double s, Color base, Color light, Color dark) {
        double cx = s * 0.5, top = s * 0.26, w = s * 0.60, h = s * 0.56;
        g.setFill(dark);
        tri(g, cx - w * 0.42, top + s * 0.04, cx - w * 0.2, top + s * 0.08, cx - w * 0.5, top - s * 0.14);
        tri(g, cx + w * 0.42, top + s * 0.04, cx + w * 0.2, top + s * 0.08, cx + w * 0.5, top - s * 0.14);
        g.setFill(grad(cx, top + h * 0.3, w * 0.75, light, base));
        g.fillOval(cx - w / 2, top, w, h);
        eye(g, cx - w * 0.18, top + h * 0.4, s * 0.08, s * 0.008, 0);
        eye(g, cx + w * 0.18, top + h * 0.4, s * 0.08, s * 0.008, 0);
        g.setStroke(dark);
        g.setLineWidth(s * 0.03);
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(cx - w * 0.3, top + h * 0.24, cx - w * 0.08, top + h * 0.34);
        g.strokeLine(cx + w * 0.3, top + h * 0.24, cx + w * 0.08, top + h * 0.34);
        smile(g, s, cx, top + h * 0.66, w * 0.42, s * 0.05, dark);
        g.setFill(Color.WHITE);
        tri(g, cx - w * 0.12, top + h * 0.64, cx - w * 0.05, top + h * 0.64, cx - w * 0.085, top + h * 0.74);
        tri(g, cx + w * 0.12, top + h * 0.64, cx + w * 0.05, top + h * 0.64, cx + w * 0.085, top + h * 0.74);
    }

    private static void spiky(GraphicsContext g, double s, Color base, Color light, Color dark) {
        double cx = s * 0.5, cy = s * 0.52, r = s * 0.27;
        g.setFill(dark);
        int n = 12;
        for (int i = 0; i < n; i++) {
            double ang = 2 * Math.PI * i / n;
            double bx = cx + Math.cos(ang) * r, by = cy + Math.sin(ang) * r;
            double tx = cx + Math.cos(ang) * (r + s * 0.07), ty = cy + Math.sin(ang) * (r + s * 0.07);
            double ox = -Math.sin(ang), oy = Math.cos(ang);
            tri(g, bx + ox * s * 0.04, by + oy * s * 0.04, bx - ox * s * 0.04, by - oy * s * 0.04, tx, ty);
        }
        g.setFill(grad(cx, cy - r * 0.3, r * 1.1, light, base));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        eye(g, cx - r * 0.4, cy - r * 0.1, s * 0.07, s * 0.008, s * 0.004);
        eye(g, cx + r * 0.4, cy - r * 0.1, s * 0.07, s * 0.008, s * 0.004);
        eye(g, cx, cy - r * 0.5, s * 0.055, 0, s * 0.004);
        smile(g, s, cx, cy + r * 0.45, r * 0.7, s * 0.04, dark);
    }

    private static void fluff(GraphicsContext g, double s, Color base, Color light, Color dark) {
        double cx = s * 0.5, cy = s * 0.55, r = s * 0.28;
        g.setFill(grad(cx, cy, r, light, base));
        g.fillOval(cx - r * 0.95, cy - r * 1.25, r * 0.6, r * 0.7);
        g.fillOval(cx + r * 0.35, cy - r * 1.25, r * 0.6, r * 0.7);
        g.setFill(grad(cx, cy - r * 0.3, r * 1.2, light, base));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        eye(g, cx - r * 0.42, cy - r * 0.15, s * 0.085, s * 0.008, s * 0.008);
        eye(g, cx + r * 0.42, cy - r * 0.15, s * 0.085, s * 0.008, s * 0.008);
        smile(g, s, cx, cy + r * 0.35, r * 0.6, s * 0.04, dark);
        g.setFill(Color.WHITE);
        g.fillRoundRect(cx - r * 0.16, cy + r * 0.4, r * 0.14, r * 0.22, 4, 4);
        g.fillRoundRect(cx + r * 0.02, cy + r * 0.4, r * 0.14, r * 0.22, 4, 4);
    }

    private static void dragon(GraphicsContext g, double s, Color base, Color light, Color dark) {
        double cx = s * 0.5, top = s * 0.26, w = s * 0.58, h = s * 0.54;
        g.setFill(dark);
        tri(g, cx - w * 0.26, top + s * 0.02, cx - w * 0.12, top + s * 0.02, cx - w * 0.19, top - s * 0.1);
        tri(g, cx + w * 0.12, top + s * 0.02, cx + w * 0.26, top + s * 0.02, cx + w * 0.19, top - s * 0.1);
        g.setFill(grad(cx, top + h * 0.3, w * 0.8, light, base));
        g.fillOval(cx - w / 2, top, w, h);
        g.setFill(grad(cx, top + h * 0.6, w * 0.4, light, base));
        g.fillOval(cx - w * 0.26, top + h * 0.5, w * 0.52, h * 0.45);
        eye(g, cx - w * 0.17, top + h * 0.32, s * 0.075, s * 0.008, s * 0.004);
        eye(g, cx + w * 0.17, top + h * 0.32, s * 0.075, s * 0.008, s * 0.004);
        g.setFill(dark);
        g.fillOval(cx - w * 0.1, top + h * 0.7, s * 0.03, s * 0.025);
        g.fillOval(cx + w * 0.07, top + h * 0.7, s * 0.03, s * 0.025);
    }

    private static void king(GraphicsContext g, double s, Color base, Color light, Color dark) {
        double cx = s * 0.5, top = s * 0.30, w = s * 0.62, h = s * 0.54;
        double cl = cx - w * 0.34, cr = cx + w * 0.34, cb = top + s * 0.02, ctp = top - s * 0.16;
        g.setFill(dark);
        g.fillPolygon(
                new double[]{cl, cl, cl + w * 0.17, cx, cr - w * 0.17, cr, cr},
                new double[]{cb, ctp + s * 0.06, ctp + s * 0.13, ctp, ctp + s * 0.13, ctp + s * 0.06, cb}, 7);
        g.setFill(Color.WHITE);
        double[][] pts = {{cl, ctp + s * 0.06}, {cx, ctp}, {cr, ctp + s * 0.06}};
        for (double[] pt : pts) {
            g.fillOval(pt[0] - s * 0.018, pt[1] - s * 0.018, s * 0.036, s * 0.036);
        }
        g.setFill(grad(cx, top + h * 0.3, w * 0.8, light, base));
        g.fillRoundRect(cx - w / 2, top, w, h, w * 0.5, w * 0.5);
        eye(g, cx - w * 0.17, top + h * 0.4, s * 0.08, s * 0.008, s * 0.008);
        eye(g, cx + w * 0.17, top + h * 0.4, s * 0.08, s * 0.008, s * 0.008);
        smile(g, s, cx, top + h * 0.66, w * 0.4, s * 0.05, dark);
    }
}
