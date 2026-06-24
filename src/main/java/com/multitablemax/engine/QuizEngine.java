package com.multitablemax.engine;

import com.multitablemax.model.Fact;
import com.multitablemax.model.FactStats;
import com.multitablemax.model.Progress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Ядро тренажёра, полностью независимое от UI.
 * Выбирает следующий пример взвешенно-случайно (сложные — чаще),
 * принимает ответ и начисляет звёзды с бонусом за серию (combo).
 */
public final class QuizEngine {

    /** Классическая таблица: множители от 2 до 9. */
    public static final int MIN_FACTOR = 2;
    public static final int MAX_FACTOR = 9;

    private final Progress progress;
    private final Set<Integer> scope;   // множители, которые тренируем (нормализованы)
    private final List<Fact> facts = new ArrayList<>();
    private final Random random = new Random();

    private Fact current;
    private Fact previous;          // чтобы не повторять один пример дважды подряд
    private long questionStartNanos;
    private int combo;              // текущая серия верных ответов

    /** Тренировка по всей таблице. */
    public QuizEngine(Progress progress) {
        this(progress, Set.of());
    }

    /**
     * @param scopeFactors множители для тренировки (например {3, 4, 8}).
     *                     Пустой набор — вся таблица (2..9).
     */
    public QuizEngine(Progress progress, Set<Integer> scopeFactors) {
        this.progress = progress;
        this.scope = normalizeScope(scopeFactors);
        for (int a : scope) {
            for (int b = MIN_FACTOR; b <= MAX_FACTOR; b++) {
                facts.add(new Fact(a, b));
            }
        }
    }

    private static Set<Integer> normalizeScope(Set<Integer> requested) {
        Set<Integer> result = new LinkedHashSet<>();
        if (requested != null) {
            for (int f = MIN_FACTOR; f <= MAX_FACTOR; f++) {
                if (requested.contains(f)) {
                    result.add(f);
                }
            }
        }
        if (result.isEmpty()) {           // ничего не выбрано → вся таблица
            for (int f = MIN_FACTOR; f <= MAX_FACTOR; f++) {
                result.add(f);
            }
        }
        return result;
    }

    /** Множители, входящие в текущую область (нормализованные). */
    public Set<Integer> scope() {
        return Collections.unmodifiableSet(scope);
    }

    /** true, если тренируется вся таблица (а не подмножество). */
    public boolean isFullScope() {
        return scope.size() == (MAX_FACTOR - MIN_FACTOR + 1);
    }

    /** Все примеры выбранной области — для режима «босс». */
    public List<Fact> scopeFacts() {
        return new ArrayList<>(facts);
    }

    /** Результат проверки ответа — то, что нужно UI для реакции. */
    public record Result(boolean correct, int correctAnswer, int starsEarned, int combo) {
    }

    /** Выбирает (взвешенно) и запоминает следующий вопрос. */
    public Fact nextQuestion() {
        Fact pick = weightedPick();
        // Избегаем немедленного повтора, если примеров больше одного.
        if (pick.equals(previous) && facts.size() > 1) {
            pick = weightedPick();
        }
        present(pick);
        return current;
    }

    /**
     * Делает указанный пример текущим (для режимов с собственным порядком,
     * например «босс»). Дальше работает обычный {@link #submit(int)}.
     */
    public void present(Fact fact) {
        current = fact;
        questionStartNanos = System.nanoTime();
    }

    private Fact weightedPick() {
        long total = 0;
        for (Fact f : facts) {
            total += progress.statsFor(f).weight();
        }
        long r = (long) (random.nextDouble() * total);
        for (Fact f : facts) {
            r -= progress.statsFor(f).weight();
            if (r < 0) {
                return f;
            }
        }
        return facts.get(facts.size() - 1); // страховка от погрешностей double
    }

    /**
     * Принимает ответ на текущий вопрос.
     * @param answer введённое ребёнком число
     */
    public Result submit(int answer) {
        if (current == null) {
            throw new IllegalStateException("Нет активного вопроса");
        }
        long elapsedMs = (System.nanoTime() - questionStartNanos) / 1_000_000;
        FactStats s = progress.statsFor(current);
        boolean correct = answer == current.answer();

        int earned = 0;
        if (correct) {
            s.recordCorrect(elapsedMs);
            combo++;
            earned = 1 + comboBonus();   // бонус за серию
            progress.addStars(earned);
        } else {
            s.recordWrong();
            combo = 0;
        }

        previous = current;
        Result result = new Result(correct, current.answer(), earned, combo);
        current = null;
        return result;
    }

    /** +1 звезда за каждые 5 верных подряд (на 5-м, 10-м, ... ответе). */
    private int comboBonus() {
        return (combo % 5 == 0) ? 1 : 0;
    }

    public Fact current() {
        return current;
    }

    public int combo() {
        return combo;
    }

    public int stars() {
        return progress.stars();
    }

    public int level() {
        return progress.level();
    }

    public String levelTitle() {
        return progress.levelTitle();
    }

    /** Сохраняет результат «на время». @return true, если новый рекорд. */
    public boolean recordTimedScore(int correct) {
        return progress.recordTimedScore(correct);
    }

    /** Сохраняет время победы над боссом. @return true, если новый рекорд. */
    public boolean recordBossWin(int seconds) {
        return progress.recordBossWin(seconds);
    }

    /** Освоенность "таблицы на f" 0..1 — среднее по примерам f×(2..9). */
    public double masteryOfTable(int factor) {
        double sum = 0;
        int n = 0;
        for (int b = MIN_FACTOR; b <= MAX_FACTOR; b++) {
            sum += progress.statsFor(new Fact(factor, b)).mastery();
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }
}
