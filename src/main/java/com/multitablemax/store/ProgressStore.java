package com.multitablemax.store;

import com.multitablemax.model.FactStats;
import com.multitablemax.model.Progress;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Сохранение прогресса в простой текстовый файл в домашней папке пользователя
 * (~/.multitablemax/progress.txt). Формат намеренно тривиальный — без JSON-зависимостей.
 *
 * Строки:
 *   stars;42
 *   fact;7;8;5;12;11;1430;3      (a;b;box;attempts;correct;bestMs;streak)
 */
public final class ProgressStore {

    private final Path file;

    public ProgressStore() {
        this(defaultPath());
    }

    public ProgressStore(Path file) {
        this.file = file;
    }

    private static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".multitablemax", "progress.txt");
    }

    public Progress load() {
        Progress progress = new Progress();
        if (!Files.exists(file)) {
            return progress;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                parseLine(line.strip(), progress);
            }
        } catch (IOException | RuntimeException e) {
            // Повреждённый файл не должен ронять приложение — начинаем с чистого листа.
            System.err.println("Не удалось прочитать прогресс: " + e.getMessage());
        }
        return progress;
    }

    private void parseLine(String line, Progress progress) {
        if (line.isEmpty()) {
            return;
        }
        String[] p = line.split(";");
        switch (p[0]) {
            case "stars" -> progress.setStars(Integer.parseInt(p[1]));
            case "timedbest" -> progress.setBestTimedScore(Integer.parseInt(p[1]));
            case "bossbest" -> progress.setBestBossTimeSec(Integer.parseInt(p[1]));
            case "bossart" -> progress.setBossArt(p[1]);
            case "fact" -> {
                int a = Integer.parseInt(p[1]);
                int b = Integer.parseInt(p[2]);
                FactStats s = new FactStats(
                        Integer.parseInt(p[3]),
                        Integer.parseInt(p[4]),
                        Integer.parseInt(p[5]),
                        Long.parseLong(p[6]),
                        Integer.parseInt(p[7]));
                progress.putStats(a + "x" + b, s);
            }
            default -> { /* неизвестная строка — игнорируем */ }
        }
    }

    public void save(Progress progress) {
        List<String> lines = new ArrayList<>();
        lines.add("stars;" + progress.stars());
        lines.add("timedbest;" + progress.bestTimedScore());
        lines.add("bossbest;" + progress.bestBossTimeSec());
        lines.add("bossart;" + progress.bossArt());
        progress.allStats().forEach((key, s) -> {
            String[] ab = key.split("x");
            lines.add(String.join(";",
                    "fact", ab[0], ab[1],
                    String.valueOf(s.box()),
                    String.valueOf(s.attempts()),
                    String.valueOf(s.correct()),
                    String.valueOf(s.bestTimeMs()),
                    String.valueOf(s.streak())));
        });
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Не удалось сохранить прогресс: " + e.getMessage());
        }
    }
}
