package com.multitablemax.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Весь сохраняемый прогресс игрока: статистика по примерам + звёзды.
 * Уровень вычисляется из звёзд.
 */
public final class Progress {

    private final Map<String, FactStats> stats = new HashMap<>();
    private int stars;
    private int bestTimedScore;     // лучший счёт в режиме «на время»
    private int bestBossTimeSec;    // лучшее (наименьшее) время победы над боссом, сек; 0 = нет
    private String bossArt = "EMOJI"; // настройка отрисовки боссов (не сбрасывается со статистикой)

    /** Статистика по примеру; создаётся пустой при первом обращении. */
    public FactStats statsFor(Fact fact) {
        return stats.computeIfAbsent(fact.key(), k -> new FactStats());
    }

    /** Только чтение: статистика по примеру или null, если его ещё не было. */
    public FactStats peek(Fact fact) {
        return stats.get(fact.key());
    }

    /** Для восстановления из хранилища. */
    public void putStats(String key, FactStats s) {
        stats.put(key, s);
    }

    public Map<String, FactStats> allStats() {
        return stats;
    }

    public int stars() {
        return stars;
    }

    public void addStars(int delta) {
        stars = Math.max(0, stars + delta);
    }

    public void setStars(int stars) {
        this.stars = Math.max(0, stars);
    }

    public int bestTimedScore() {
        return bestTimedScore;
    }

    public int bestBossTimeSec() {
        return bestBossTimeSec;
    }

    public void setBestTimedScore(int v) {
        this.bestTimedScore = Math.max(0, v);
    }

    public void setBestBossTimeSec(int v) {
        this.bestBossTimeSec = Math.max(0, v);
    }

    public String bossArt() {
        return bossArt;
    }

    public void setBossArt(String bossArt) {
        if (bossArt != null && !bossArt.isBlank()) {
            this.bossArt = bossArt;
        }
    }

    /** Сохраняет результат «на время». @return true, если это новый рекорд. */
    public boolean recordTimedScore(int correct) {
        if (correct > bestTimedScore) {
            bestTimedScore = correct;
            return true;
        }
        return false;
    }

    /** Сохраняет время победы над боссом. @return true, если это новый рекорд. */
    public boolean recordBossWin(int seconds) {
        if (seconds > 0 && (bestBossTimeSec == 0 || seconds < bestBossTimeSec)) {
            bestBossTimeSec = seconds;
            return true;
        }
        return false;
    }

    /** Полный сброс прогресса: статистика, звёзды и рекорды. */
    public void reset() {
        stats.clear();
        stars = 0;
        bestTimedScore = 0;
        bestBossTimeSec = 0;
    }

    /** Звание растёт каждые 50 звёзд: 1 — Новичок, дальше Ученик/Мастер... */
    public int level() {
        return stars / 50 + 1;
    }

    public String levelTitle() {
        return switch (level()) {
            case 1 -> "Новичок";
            case 2 -> "Ученик";
            case 3 -> "Знаток";
            case 4 -> "Мастер";
            default -> "Гроссмейстер";
        };
    }
}
