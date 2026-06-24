package com.multitablemax.model;

/**
 * Один пример таблицы умножения, например 7 × 8.
 * Identity по паре (a, b), поэтому это record.
 */
public record Fact(int a, int b) {

    public int answer() {
        return a * b;
    }

    /** Стабильный строковый ключ для хранения статистики и сериализации. */
    public String key() {
        return a + "x" + b;
    }

    @Override
    public String toString() {
        return a + " × " + b;
    }
}
