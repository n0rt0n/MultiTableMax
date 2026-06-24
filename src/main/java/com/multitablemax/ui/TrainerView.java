package com.multitablemax.ui;

import com.multitablemax.engine.BossRun;
import com.multitablemax.engine.GameMode;
import com.multitablemax.engine.QuizEngine;
import com.multitablemax.model.Fact;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Экран-тренажёр. Три режима:
 *  - PRACTICE: без таймера, играем сколько угодно;
 *  - TIMED:    обратный отсчёт, в конце — счёт верных ответов;
 *  - BOSS:     ответить без ошибок на все примеры области (ограниченные жизни).
 * Поле ввода — цифрами, подтверждение по Enter; мгновенная обратная связь.
 */
public final class TrainerView {

    private static final double BOSS_IMAGE_SIZE = 120;

    private final QuizEngine engine;
    private final GameMode mode;
    private final BossArt bossArt;
    private final Runnable onProgressChanged;
    private final Runnable onExitToMenu;
    private final Runnable onReplay;

    private final VBox root = new VBox(16);
    private final Label starsLabel = new Label();
    private final Label levelLabel = new Label();
    private final Label statusLabel = new Label();   // серия / таймер / жизни — по режиму
    private final Label questionLabel = new Label();
    private final TextField answerField = new TextField();
    private final Label feedbackLabel = new Label();
    private final Map<Integer, ProgressBar> tableBars = new LinkedHashMap<>();
    private final HBox actionBar = new HBox(12);     // кнопки в конце раунда

    private Timeline timer;
    private int timeLeft = GameMode.TIMED_SECONDS;
    private BossRun boss;
    private long bossStartNanos;
    private int sessionCorrect;
    private int sessionTotal;
    private boolean finished;

    public TrainerView(QuizEngine engine, GameMode mode, BossArt bossArt,
                       Runnable onProgressChanged, Runnable onExitToMenu, Runnable onReplay) {
        this.engine = engine;
        this.mode = mode;
        this.bossArt = bossArt;
        this.onProgressChanged = onProgressChanged;
        this.onExitToMenu = onExitToMenu;
        this.onReplay = onReplay;
        build();
        if (mode == GameMode.BOSS) {
            boss = new BossRun(engine.scopeFacts());
            bossStartNanos = System.nanoTime();
            updateBossStatus();
        }
        nextQuestion();
        if (mode == GameMode.TIMED) {
            startTimer();
        }
    }

    public Region getRoot() {
        return root;
    }

    public void focusInput() {
        answerField.requestFocus();
    }

    private void build() {
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20, 24, 24, 24));
        root.getStyleClass().add("root-pane");

        VBox tables = buildTablesPanel();
        actionBar.setAlignment(Pos.CENTER);
        actionBar.setVisible(false);
        actionBar.setManaged(false);

        root.getChildren().addAll(
                buildHeader(),
                buildQuestionArea(),
                feedbackLabel,
                actionBar,
                tables);

        feedbackLabel.getStyleClass().add("feedback");
        VBox.setVgrow(tables, Priority.ALWAYS);
        refreshStats();
    }

    private HBox buildHeader() {
        Button menu = new Button("☰ Меню");
        menu.getStyleClass().add("menu-button");
        menu.setOnAction(e -> exitToMenu());

        starsLabel.getStyleClass().add("stars");
        levelLabel.getStyleClass().add("level");
        statusLabel.getStyleClass().add(switch (mode) {
            case TIMED -> "timer";
            case BOSS -> "lives";
            case PRACTICE -> "combo";
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(14, menu, starsLabel, levelLabel, spacer, statusLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private VBox buildQuestionArea() {
        questionLabel.getStyleClass().add("question");

        answerField.getStyleClass().add("answer");
        answerField.setMaxWidth(180);
        answerField.setAlignment(Pos.CENTER);
        answerField.setPromptText("?");
        // Только цифры, максимум 2 знака (9×9=81).
        answerField.textProperty().addListener((obs, old, val) -> {
            String digits = val.replaceAll("\\D", "");
            if (digits.length() > 2) {
                digits = digits.substring(0, 2);
            }
            if (!digits.equals(val)) {
                answerField.setText(digits);
            }
        });
        answerField.setOnAction(e -> submit());

        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(16, 0, 8, 0));
        if (mode == GameMode.BOSS) {
            Node bossImage = BossArtist.create(bossArt, bossId(), BOSS_IMAGE_SIZE);
            box.getChildren().add(bossImage);
        }
        box.getChildren().addAll(questionLabel, answerField);
        return box;
    }

    /** Множитель-«хозяин» босса: сам множитель, если область — одна таблица, иначе 0 (общий). */
    private int bossId() {
        Set<Integer> scope = engine.scope();
        return scope.size() == 1 ? scope.iterator().next() : 0;
    }

    private VBox buildTablesPanel() {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("tables-panel");

        Label title = new Label("Освоение таблиц");
        title.getStyleClass().add("panel-title");
        panel.getChildren().add(title);

        boolean highlightScope = !engine.isFullScope();
        for (int f = QuizEngine.MIN_FACTOR; f <= QuizEngine.MAX_FACTOR; f++) {
            Label name = new Label("× " + f);
            name.getStyleClass().add("table-name");
            name.setMinWidth(40);
            // Подсвечиваем таблицы, входящие в выбранную область (если это не «вся»).
            if (highlightScope && engine.scope().contains(f)) {
                name.getStyleClass().add("table-name-active");
            }

            ProgressBar bar = new ProgressBar(0);
            bar.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(bar, Priority.ALWAYS);
            tableBars.put(f, bar);

            HBox row = new HBox(10, name, bar);
            row.setAlignment(Pos.CENTER_LEFT);
            panel.getChildren().add(row);
        }
        return panel;
    }

    private void startTimer() {
        timeLeft = GameMode.TIMED_SECONDS;
        updateTimerLabel();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            updateTimerLabel();
            if (timeLeft <= 0) {
                endTimedSession();
            }
        }));
        timer.setCycleCount(GameMode.TIMED_SECONDS);
        timer.play();
    }

    private void updateTimerLabel() {
        statusLabel.setText("⏱ " + timeLeft);
        statusLabel.getStyleClass().remove("timer-low");
        if (timeLeft <= 10) {
            statusLabel.getStyleClass().add("timer-low");
        }
    }

    private void updateBossStatus() {
        String hearts = "♥".repeat(boss.lives())
                + "♡".repeat(Math.max(0, GameMode.BOSS_LIVES - boss.lives()));
        statusLabel.setText(hearts + "   решено " + boss.solved() + "/" + boss.total());
    }

    private void nextQuestion() {
        Fact fact;
        if (mode == GameMode.BOSS) {
            fact = boss.current();
            engine.present(fact);
        } else {
            fact = engine.nextQuestion();
        }
        questionLabel.setText(fact.toString() + " =");
        answerField.clear();
        answerField.requestFocus();
    }

    private void submit() {
        if (finished) {
            return;
        }
        String text = answerField.getText().strip();
        if (text.isEmpty()) {
            return;
        }
        QuizEngine.Result result = engine.submit(Integer.parseInt(text));
        sessionTotal++;
        if (result.correct()) {
            sessionCorrect++;
            feedbackLabel.setText(praise(result));
            setFeedbackState("correct");
        } else {
            feedbackLabel.setText("Почти! Правильно: " + result.correctAnswer());
            setFeedbackState("wrong");
        }
        onProgressChanged.run();

        if (mode == GameMode.BOSS) {
            handleBossAnswer(result.correct());
        } else {
            refreshStats();
            nextQuestion();
        }
    }

    private void handleBossAnswer(boolean correct) {
        BossRun.State state = boss.record(correct);
        updateBossStatus();
        refreshStats();
        if (state.won()) {
            endBoss(true);
        } else if (state.lost()) {
            endBoss(false);
        } else {
            nextQuestion();
        }
    }

    private String praise(QuizEngine.Result result) {
        String base = "Верно! +" + result.starsEarned() + " ★";
        if (result.combo() > 0 && result.combo() % 5 == 0) {
            base += "  🔥 Серия " + result.combo() + "!";
        }
        return base;
    }

    private void setFeedbackState(String state) {
        feedbackLabel.getStyleClass().removeAll("correct", "wrong");
        feedbackLabel.getStyleClass().add(state);
    }

    private void endTimedSession() {
        finished = true;
        if (timer != null) {
            timer.stop();
        }
        statusLabel.setText("⏱ 0");
        boolean record = engine.recordTimedScore(sessionCorrect);
        onProgressChanged.run();
        String msg = "Время вышло! Верных ответов: " + sessionCorrect + " из " + sessionTotal;
        if (record) {
            msg += "   🏆 Новый рекорд!";
        }
        showResult("✓ " + sessionCorrect, msg, "correct");
    }

    private void endBoss(boolean won) {
        finished = true;
        if (won) {
            int seconds = (int) Math.round((System.nanoTime() - bossStartNanos) / 1_000_000_000.0);
            boolean record = engine.recordBossWin(seconds);
            onProgressChanged.run();
            String msg = "Победа! Все примеры решены за " + seconds + " сек.";
            if (record) {
                msg += "   🏆 Рекорд!";
            }
            showResult("🏆", msg, "correct");
        } else {
            showResult("💥", "Босс победил. Решено " + boss.solved() + " из " + boss.total()
                    + ". Попробуй ещё!", "wrong");
        }
    }

    /** Завершает раунд: показывает итог и кнопки «Ещё раз» / «Меню». */
    private void showResult(String bigText, String message, String feedbackState) {
        answerField.setDisable(true);
        questionLabel.setText(bigText);
        feedbackLabel.setText(message);
        setFeedbackState(feedbackState);

        Button again = new Button("▶ Ещё раз");
        again.getStyleClass().add("start-button");
        again.setDefaultButton(true);
        again.setOnAction(e -> onReplay.run());

        Button menu = new Button("☰ Меню");
        menu.getStyleClass().add("menu-button");
        menu.setOnAction(e -> exitToMenu());

        actionBar.getChildren().setAll(again, menu);
        actionBar.setVisible(true);
        actionBar.setManaged(true);
    }

    private void exitToMenu() {
        if (timer != null) {
            timer.stop();
        }
        onExitToMenu.run();
    }

    private void refreshStats() {
        starsLabel.setText("★ " + engine.stars());
        levelLabel.setText("Уровень " + engine.level() + " · " + engine.levelTitle());
        if (mode == GameMode.PRACTICE) {
            int combo = engine.combo();
            statusLabel.setText(combo >= 2 ? "Серия: " + combo : "");
        }
        for (var entry : tableBars.entrySet()) {
            entry.getValue().setProgress(engine.masteryOfTable(entry.getKey()));
        }
    }
}
