package com.multitablemax.ui;

import com.multitablemax.engine.GameMode;
import com.multitablemax.engine.QuizEngine;
import com.multitablemax.model.Progress;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Стартовый экран: выбор режима (обычный / на время / босс), выбор области
 * таблицы (вся или произвольный набор множителей), сброс статистики и запуск.
 */
public final class HomeView {

    private final VBox root = new VBox(20);
    private final ToggleGroup modeGroup = new ToggleGroup();

    private ToggleButton allButton;
    private final Set<ToggleButton> factorButtons = new LinkedHashSet<>();

    /**
     * @param progress    для показа звёзд/уровня и текущей настройки боссов
     * @param onStart     старт игры: (режим, набор множителей; пусто = вся таблица)
     * @param onStats     открыть экран статистики
     * @param onReset     сброс всей статистики
     * @param onSetBossArt сохранить выбранный стиль картинок боссов
     */
    public HomeView(Progress progress, BiConsumer<GameMode, Set<Integer>> onStart,
                    Runnable onStats, Runnable onReset, Consumer<BossArt> onSetBossArt) {
        build(progress, onStart, onStats, onReset, onSetBossArt);
    }

    public Region getRoot() {
        return root;
    }

    private void build(Progress progress, BiConsumer<GameMode, Set<Integer>> onStart,
                       Runnable onStats, Runnable onReset, Consumer<BossArt> onSetBossArt) {
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("root-pane");

        Label title = new Label("Тренажёр для Макса");
        title.getStyleClass().add("home-title");

        Label subtitle = new Label("★ " + progress.stars()
                + "   ·   Уровень " + progress.level() + " · " + progress.levelTitle());
        subtitle.getStyleClass().add("level");

        root.getChildren().addAll(
                title,
                subtitle,
                section("Режим", buildModeChooser()),
                section("Что учим (можно выбрать несколько)", buildScopeChooser()),
                section("Картинки боссов", buildBossArtChooser(progress, onSetBossArt)),
                buildStartButton(onStart),
                buildBottomBar(onStats, onReset));
    }

    private HBox buildBossArtChooser(Progress progress, Consumer<BossArt> onSetBossArt) {
        BossArt current = BossArt.fromString(progress.bossArt());
        ToggleGroup group = new ToggleGroup();

        ToggleButton emoji = bossArtButton("😀 Emoji", BossArt.EMOJI, group, current);
        ToggleButton canvas = bossArtButton("🎨 Рисунок", BossArt.CANVAS, group, current);
        ToggleButton image = bossArtButton("🖼 Картинки", BossArt.IMAGE, group, current);

        // Сохраняем выбор сразу; не даём снять обе кнопки.
        group.selectedToggleProperty().addListener((obs, was, now) -> {
            if (now == null) {
                if (was != null) {
                    was.setSelected(true);
                }
            } else {
                onSetBossArt.accept((BossArt) now.getUserData());
            }
        });

        HBox box = new HBox(12, emoji, canvas, image);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private ToggleButton bossArtButton(String text, BossArt art, ToggleGroup group, BossArt current) {
        ToggleButton b = new ToggleButton(text);
        b.setToggleGroup(group);
        b.setUserData(art);
        b.setSelected(art == current);
        b.getStyleClass().add("chooser");
        return b;
    }

    private VBox section(String caption, Region content) {
        Label label = new Label(caption);
        label.getStyleClass().add("panel-title");
        VBox box = new VBox(10, label, content);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private HBox buildModeChooser() {
        HBox box = new HBox(12,
                modeButton("Обычный", GameMode.PRACTICE, true),
                modeButton("На время · " + GameMode.TIMED_SECONDS + " сек", GameMode.TIMED, false),
                modeButton("Босс-таблица", GameMode.BOSS, false));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private ToggleButton modeButton(String text, GameMode mode, boolean selected) {
        ToggleButton b = new ToggleButton(text);
        b.setToggleGroup(modeGroup);
        b.setUserData(mode);
        b.setSelected(selected);
        b.getStyleClass().add("chooser");
        return b;
    }

    private FlowPane buildScopeChooser() {
        FlowPane pane = new FlowPane(10, 10);
        pane.setAlignment(Pos.CENTER);
        pane.setMaxWidth(440);

        // «Вся» — это особый переключатель: включён, когда не выбрано ни одной таблицы.
        allButton = new ToggleButton("Вся");
        allButton.getStyleClass().add("chooser");
        allButton.setSelected(true);
        allButton.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                factorButtons.forEach(b -> b.setSelected(false));
            } else if (noFactorSelected()) {
                // Нельзя снять «Вся», ничего не выбрав — оставляем включённой.
                allButton.setSelected(true);
            }
        });
        pane.getChildren().add(allButton);

        for (int f = QuizEngine.MIN_FACTOR; f <= QuizEngine.MAX_FACTOR; f++) {
            ToggleButton b = new ToggleButton("× " + f);
            b.setUserData(f);
            b.getStyleClass().add("chooser");
            b.selectedProperty().addListener((obs, was, now) -> {
                if (now) {
                    allButton.setSelected(false);
                } else if (noFactorSelected()) {
                    allButton.setSelected(true);
                }
            });
            factorButtons.add(b);
            pane.getChildren().add(b);
        }
        return pane;
    }

    private boolean noFactorSelected() {
        return factorButtons.stream().noneMatch(ToggleButton::isSelected);
    }

    private Button buildStartButton(BiConsumer<GameMode, Set<Integer>> onStart) {
        Button start = new Button("Начать ▶");
        start.getStyleClass().add("start-button");
        start.setDefaultButton(true);
        start.setOnAction(e -> onStart.accept(selectedMode(), selectedScope()));
        return start;
    }

    private HBox buildBottomBar(Runnable onStats, Runnable onReset) {
        Button stats = new Button("📊 Статистика");
        stats.getStyleClass().add("menu-button");
        stats.setOnAction(e -> onStats.run());

        Button reset = new Button("Сбросить статистику");
        reset.getStyleClass().add("reset-button");
        reset.setOnAction(e -> onReset.run());

        HBox bar = new HBox(16, stats, reset);
        bar.setAlignment(Pos.CENTER);
        return bar;
    }

    private GameMode selectedMode() {
        var sel = modeGroup.getSelectedToggle();
        return sel == null ? GameMode.PRACTICE : (GameMode) sel.getUserData();
    }

    /** Выбранные множители; пустой набор означает «вся таблица». */
    private Set<Integer> selectedScope() {
        Set<Integer> scope = new LinkedHashSet<>();
        if (!allButton.isSelected()) {
            for (ToggleButton b : factorButtons) {
                if (b.isSelected()) {
                    scope.add((Integer) b.getUserData());
                }
            }
        }
        return scope;
    }
}
