package com.multitablemax;

import com.multitablemax.engine.GameMode;
import com.multitablemax.engine.QuizEngine;
import com.multitablemax.model.Progress;
import com.multitablemax.store.ProgressStore;
import com.multitablemax.ui.BossArt;
import com.multitablemax.ui.HomeView;
import com.multitablemax.ui.StatsView;
import com.multitablemax.ui.TrainerView;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Set;

/**
 * Точка входа JavaFX. Управляет навигацией между стартовым экраном (выбор
 * режима и области таблицы) и тренажёром. Прогресс сохраняется при каждом
 * ответе и при закрытии окна.
 */
public final class App extends Application {

    private final ProgressStore store = new ProgressStore();
    private Progress progress;
    private Scene scene;

    @Override
    public void start(Stage stage) {
        progress = store.load();

        scene = new Scene(showHomeRoot(), 560, 680);
        var css = App.class.getResource("/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        var icon = App.class.getResource("/bosses/boss-0.png");
        if (icon != null) {
            stage.getIcons().add(new javafx.scene.image.Image(icon.toExternalForm()));
        }

        stage.setTitle("Тренажёр для Макса — таблица умножения");
        stage.setScene(scene);
        stage.setMinWidth(480);
        stage.setMinHeight(600);
        stage.show();
    }

    /** Показывает стартовый экран и возвращает его корень. */
    private javafx.scene.layout.Region showHomeRoot() {
        HomeView home = new HomeView(progress, this::startGame, this::showStats,
                this::confirmReset, this::setBossArt);
        return home.getRoot();
    }

    private void showHome() {
        scene.setRoot(showHomeRoot());
    }

    private void showStats() {
        scene.setRoot(new StatsView(progress, this::showHome).getRoot());
    }

    /** Запускает тренажёр в выбранном режиме и области таблицы. */
    private void startGame(GameMode mode, Set<Integer> scope) {
        QuizEngine engine = new QuizEngine(progress, scope);
        TrainerView trainer = new TrainerView(
                engine,
                mode,
                BossArt.fromString(progress.bossArt()),
                this::saveQuietly,
                this::showHome,
                () -> startGame(mode, scope)); // «Ещё раз» — тот же режим и область
        scene.setRoot(trainer.getRoot());
        trainer.focusInput();
    }

    /** Сохраняет выбранный стиль картинок боссов. */
    private void setBossArt(BossArt art) {
        progress.setBossArt(art.name());
        store.save(progress);
    }

    /** Спрашивает подтверждение и сбрасывает весь прогресс. */
    private void confirmReset() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Сбросить весь прогресс? Звёзды и статистика будут удалены безвозвратно.",
                ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Сброс статистики");
        alert.setTitle("Подтверждение");
        alert.showAndWait().ifPresent(button -> {
            if (button == ButtonType.YES) {
                progress.reset();
                store.save(progress);
                showHome();   // обновить показанные звёзды/уровень
            }
        });
    }

    private void saveQuietly() {
        store.save(progress);
    }

    @Override
    public void stop() {
        if (progress != null) {
            store.save(progress);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
