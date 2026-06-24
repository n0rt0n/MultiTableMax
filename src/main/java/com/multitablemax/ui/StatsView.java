package com.multitablemax.ui;

import com.multitablemax.engine.QuizEngine;
import com.multitablemax.model.Fact;
import com.multitablemax.model.FactStats;
import com.multitablemax.model.Progress;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Экран статистики: сводка ответов, число освоенных примеров, самые трудные
 * примеры и освоенность каждой таблицы. Считается из {@link Progress}
 * без побочных эффектов (через {@link Progress#peek}).
 */
public final class StatsView {

    /** Сколько трудных примеров показывать. */
    private static final int HARDEST_LIMIT = 8;

    private final Progress progress;
    private final Runnable onBack;
    private final VBox root = new VBox(16);

    public StatsView(Progress progress, Runnable onBack) {
        this.progress = progress;
        this.onBack = onBack;
        build();
    }

    public Region getRoot() {
        return root;
    }

    private void build() {
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20, 24, 24, 24));
        root.getStyleClass().add("root-pane");

        VBox content = new VBox(18);
        content.setAlignment(Pos.TOP_CENTER);
        content.getChildren().addAll(
                buildSummary(),
                buildHardest(),
                buildTables());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("stats-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(buildHeader(), scroll);
    }

    private HBox buildHeader() {
        Button back = new Button("☰ Меню");
        back.getStyleClass().add("menu-button");
        back.setOnAction(e -> onBack.run());

        Label title = new Label("Статистика");
        title.getStyleClass().add("stats-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(14, back, title, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private VBox buildSummary() {
        int totalAttempts = 0;
        int totalCorrect = 0;
        int learned = 0;
        long fastestMs = 0;
        int totalFacts = 0;

        for (int a = QuizEngine.MIN_FACTOR; a <= QuizEngine.MAX_FACTOR; a++) {
            for (int b = QuizEngine.MIN_FACTOR; b <= QuizEngine.MAX_FACTOR; b++) {
                totalFacts++;
                FactStats s = progress.peek(new Fact(a, b));
                if (s == null) {
                    continue;
                }
                totalAttempts += s.attempts();
                totalCorrect += s.correct();
                if (s.isLearned()) {
                    learned++;
                }
                if (s.bestTimeMs() > 0 && (fastestMs == 0 || s.bestTimeMs() < fastestMs)) {
                    fastestMs = s.bestTimeMs();
                }
            }
        }

        int accuracy = totalAttempts == 0 ? 0 : Math.round(100f * totalCorrect / totalAttempts);

        VBox box = new VBox(10);
        box.getStyleClass().add("tables-panel");

        box.getChildren().add(panelTitle(
                "★ " + progress.stars() + "  ·  Уровень " + progress.level()
                        + " · " + progress.levelTitle()));

        box.getChildren().addAll(
                statRow("Всего ответов", String.valueOf(totalAttempts)),
                statRow("Из них верно", totalCorrect + "  (" + accuracy + "%)"),
                statRow("Самый быстрый ответ",
                        fastestMs == 0 ? "—" : String.format("%.1f сек", fastestMs / 1000.0)),
                statRow("🏆 Рекорд «на время»",
                        progress.bestTimedScore() == 0 ? "—" : progress.bestTimedScore() + " верных"),
                statRow("🏆 Быстрейший босс",
                        progress.bestBossTimeSec() == 0 ? "—" : progress.bestBossTimeSec() + " сек"));

        Label learnedLabel = new Label("Освоено примеров: " + learned + " из " + totalFacts);
        learnedLabel.getStyleClass().add("table-name");
        ProgressBar learnedBar = new ProgressBar(totalFacts == 0 ? 0 : (double) learned / totalFacts);
        learnedBar.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(learnedLabel, learnedBar);
        return box;
    }

    private VBox buildHardest() {
        record Hard(Fact fact, FactStats stats, int errors) {
        }

        List<Hard> hard = new ArrayList<>();
        for (int a = QuizEngine.MIN_FACTOR; a <= QuizEngine.MAX_FACTOR; a++) {
            for (int b = QuizEngine.MIN_FACTOR; b <= QuizEngine.MAX_FACTOR; b++) {
                Fact f = new Fact(a, b);
                FactStats s = progress.peek(f);
                if (s == null || s.attempts() == 0) {
                    continue;
                }
                int errors = s.attempts() - s.correct();
                if (errors > 0) {
                    hard.add(new Hard(f, s, errors));
                }
            }
        }
        // Больше ошибок — выше; при равенстве — менее освоенные выше.
        hard.sort(Comparator.<Hard>comparingInt(h -> h.errors()).reversed()
                .thenComparingInt(h -> h.stats().box()));

        VBox box = new VBox(8);
        box.getStyleClass().add("tables-panel");
        box.getChildren().add(panelTitle("Самые трудные примеры"));

        if (hard.isEmpty()) {
            Label none = new Label("Ошибок пока нет — отличная работа! 🎉");
            none.getStyleClass().add("table-name");
            box.getChildren().add(none);
            return box;
        }

        hard.stream().limit(HARDEST_LIMIT).forEach(h -> {
            int masteryPct = Math.round(100f * (float) h.stats().mastery());
            Label row = new Label(h.fact() + "   ошибок: " + h.errors()
                    + "  ·  освоено " + masteryPct + "%");
            row.getStyleClass().add("table-name");
            box.getChildren().add(row);
        });
        return box;
    }

    private VBox buildTables() {
        VBox box = new VBox(8);
        box.getStyleClass().add("tables-panel");
        box.getChildren().add(panelTitle("Освоение таблиц"));

        for (int f = QuizEngine.MIN_FACTOR; f <= QuizEngine.MAX_FACTOR; f++) {
            Label name = new Label("× " + f);
            name.getStyleClass().add("table-name");
            name.setMinWidth(40);

            ProgressBar bar = new ProgressBar(masteryOfTable(f));
            bar.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(bar, Priority.ALWAYS);

            HBox row = new HBox(10, name, bar);
            row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
        }
        return box;
    }

    private double masteryOfTable(int factor) {
        double sum = 0;
        int n = 0;
        for (int b = QuizEngine.MIN_FACTOR; b <= QuizEngine.MAX_FACTOR; b++) {
            FactStats s = progress.peek(new Fact(factor, b));
            sum += (s == null) ? 0 : s.mastery();
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }

    private Label panelTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("panel-title");
        return label;
    }

    private HBox statRow(String caption, String value) {
        Label c = new Label(caption);
        c.getStyleClass().add("table-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        HBox row = new HBox(10, c, spacer, v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
