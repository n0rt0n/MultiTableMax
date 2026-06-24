package com.multitablemax;

/**
 * Точка входа, НЕ наследующая Application.
 * Это нужно, чтобы приложение запускалось как обычный jar / нативный пакет
 * без ошибки "JavaFX runtime components are missing".
 */
public final class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
