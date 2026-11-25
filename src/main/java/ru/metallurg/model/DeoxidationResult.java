package ru.metallurg.model;

/**
 * Результат расчета раскисления и легирования стали на агрегате ковш-печь (АКОС).
 */
public class DeoxidationResult {
    public final double steelMassTons;     // Масса обрабатываемой стали, т
    public final double feMnKg;            // Расход ферромарганца (ФМн78), кг
    public final double feSiKg;            // Расход ферросилиция (ФС65), кг
    public final double aluminumKg;        // Расход первичного алюминия (АВ97), кг
    public final double totalAlloysKg;     // Суммарная масса вводимых добавок, кг
    public final double tempDropCelsius;   // Ожидаемое снижение температуры стали от присадок, °C

    public DeoxidationResult(double steelMassTons, double feMnKg, double feSiKg,
                             double aluminumKg, double totalAlloysKg, double tempDropCelsius) {
        this.steelMassTons = steelMassTons;
        this.feMnKg = feMnKg;
        this.feSiKg = feSiKg;
        this.aluminumKg = aluminumKg;
        this.totalAlloysKg = totalAlloysKg;
        this.tempDropCelsius = tempDropCelsius;
    }

    public String toInputSummary() {
        return String.format("Масса стали: %.1f т", steelMassTons);
    }

    public String toResultSummary() {
        return String.format("ФМн78: %.1f кг, ФС65: %.1f кг, Al: %.1f кг (ΔT: -%.1f °C)",
                feMnKg, feSiKg, aluminumKg, tempDropCelsius);
    }
}
