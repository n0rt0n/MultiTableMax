package com.multitablemax.engine;

/** Режим тренировки. */
public enum GameMode {
    /** Обычный: без ограничения по времени, играем сколько хотим. */
    PRACTICE,
    /** На время: фиксированное число секунд, считаем верные ответы. */
    TIMED,
    /** Босс-таблица: ответить без ошибок на все примеры области. */
    BOSS;

    /** Длительность раунда «на время», секунд. */
    public static final int TIMED_SECONDS = 60;

    /** Сколько ошибок допускается в режиме «босс». */
    public static final int BOSS_LIVES = 3;
}
