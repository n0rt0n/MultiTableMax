package com.multitablemax.ui;

/** Способ отрисовки картинки босса. */
public enum BossArt {
    /** Готовый emoji-монстр. */
    EMOJI,
    /** Монстр, нарисованный фигурами на JavaFX Canvas. */
    CANVAS;

    /** Безопасный разбор из строки (для хранилища); по умолчанию EMOJI. */
    public static BossArt fromString(String s) {
        if (s != null) {
            try {
                return valueOf(s);
            } catch (IllegalArgumentException ignored) {
                // непонятное значение — берём значение по умолчанию
            }
        }
        return EMOJI;
    }
}
