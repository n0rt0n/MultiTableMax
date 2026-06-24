package com.multitablemax.model;

/**
 * Статистика по одному примеру. В основе — система Лейтнера ("коробки"):
 * правильный ответ повышает уровень (показываем реже),
 * ошибка — понижает (показываем чаще). Это и есть адаптивное повторение.
 */
public final class FactStats {

    /** Максимальный уровень "коробки": достигнут — пример считается выученным. */
    public static final int MAX_BOX = 5;

    private int box;          // 0..MAX_BOX
    private int attempts;     // всего попыток
    private int correct;      // из них верных
    private long bestTimeMs;  // лучшее время верного ответа, мс (0 = ещё не было)
    private int streak;       // текущая серия верных ответов подряд

    public FactStats() {
    }

    /** Конструктор для восстановления из хранилища. */
    public FactStats(int box, int attempts, int correct, long bestTimeMs, int streak) {
        this.box = clampBox(box);
        this.attempts = Math.max(0, attempts);
        this.correct = Math.max(0, correct);
        this.bestTimeMs = Math.max(0, bestTimeMs);
        this.streak = Math.max(0, streak);
    }

    public void recordCorrect(long timeMs) {
        attempts++;
        correct++;
        streak++;
        box = clampBox(box + 1);
        if (timeMs > 0 && (bestTimeMs == 0 || timeMs < bestTimeMs)) {
            bestTimeMs = timeMs;
        }
    }

    public void recordWrong() {
        attempts++;
        streak = 0;
        // Ошибка отбрасывает на пару уровней назад — пример быстро вернётся в ротацию.
        box = clampBox(box - 2);
    }

    /**
     * Вес для случайного взвешенного выбора: чем ниже уровень, тем чаще
     * показываем. box 0 → 32, box 5 → 1.
     */
    public int weight() {
        return 1 << (MAX_BOX - box);
    }

    public boolean isLearned() {
        return box >= MAX_BOX;
    }

    /** Доля освоенности 0..1 — для полосок прогресса. */
    public double mastery() {
        return (double) box / MAX_BOX;
    }

    private static int clampBox(int v) {
        return Math.max(0, Math.min(MAX_BOX, v));
    }

    public int box() {
        return box;
    }

    public int attempts() {
        return attempts;
    }

    public int correct() {
        return correct;
    }

    public long bestTimeMs() {
        return bestTimeMs;
    }

    public int streak() {
        return streak;
    }
}
