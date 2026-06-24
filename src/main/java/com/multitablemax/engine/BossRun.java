package com.multitablemax.engine;

import com.multitablemax.model.Fact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Раунд «босс-таблица»: нужно верно ответить на ВСЕ примеры области.
 * Есть ограниченный запас жизней; ошибка отнимает жизнь, а сам пример
 * возвращается в конец очереди (его ещё спросят). Кончились жизни — поражение,
 * решены все примеры — победа.
 *
 * Класс независим от UI и от {@link QuizEngine} (тот лишь начисляет звёзды).
 */
public final class BossRun {

    private final List<Fact> remaining;  // примеры, которые ещё не решены верно
    private final int total;
    private int lives;

    public BossRun(List<Fact> facts) {
        this.remaining = new ArrayList<>(facts);
        Collections.shuffle(this.remaining);
        this.total = facts.size();
        this.lives = GameMode.BOSS_LIVES;
    }

    /** Текущий пример (тот, что нужно показать), или null, если раунд окончен. */
    public Fact current() {
        return remaining.isEmpty() ? null : remaining.get(0);
    }

    /** Состояние раунда после очередного ответа. */
    public record State(boolean correct, boolean won, boolean lost, int lives,
                        int solved, int total) {
    }

    /** Регистрирует ответ на текущий пример и возвращает новое состояние. */
    public State record(boolean correct) {
        if (remaining.isEmpty()) {
            throw new IllegalStateException("Раунд уже окончен");
        }
        Fact f = remaining.remove(0);
        if (!correct) {
            lives--;
            remaining.add(f);             // вернётся позже
        }
        boolean lost = lives <= 0;
        boolean won = !lost && remaining.isEmpty();
        int solved = total - remaining.size();
        return new State(correct, won, lost, Math.max(0, lives), solved, total);
    }

    public int lives() {
        return lives;
    }

    public int total() {
        return total;
    }

    public int solved() {
        return total - remaining.size();
    }
}
