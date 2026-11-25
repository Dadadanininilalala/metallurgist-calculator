package ru.metallurg.service;

import ru.metallurg.model.DeoxidationResult;

/**
 * Расчет раскисления и легирования стали на агрегате комплексной обработки стали (АКОС)
 * (по методике О.К. Токового, ЮУрГУ, 2013).
 */
public class DeoxidationCalculator {

    // Стандартные коэффициенты усвоения элементов (%) и содержание ведущего элемента в ферросплавах (%)
    private static final double MN_CONTENT_FEMN = 78.0; // Ферромарганец ФМн78
    private static final double MN_ASSIMILATION = 85.0; // Усвоение Mn, %

    private static final double SI_CONTENT_FESI = 65.0; // Ферросилиций ФС65
    private static final double SI_ASSIMILATION = 80.0; // Усвоение Si, %

    private static final double AL_CONTENT_PURE = 97.0; // Первичный алюминий АВ97
    private static final double AL_ASSIMILATION = 45.0; // Усвоение Al при глубинном раскислении, %

    /**
     * Рассчитывает массу ферросплавов и алюминия (кг) для попадания в заданный марочный состав.
     */
    public DeoxidationResult calculate(double steelMassTons,
                                       double initMn, double targetMn,
                                       double initSi, double targetSi,
                                       double targetAl) {
        if (steelMassTons <= 0) {
            throw new IllegalArgumentException("Масса стали должна быть больше нуля");
        }
        if (targetMn < initMn || targetSi < initSi || targetAl < 0) {
            throw new IllegalArgumentException("Целевое содержание элемента не может быть ниже исходного");
        }

        double steelMassKg = steelMassTons * 1000.0;

        // Формула: G_фс = G_ст * (C_кон - C_нач) * 100 / (C_фс * K_усв)
        double feMnKg = round((steelMassKg * (targetMn - initMn) * 100.0) / (MN_CONTENT_FEMN * MN_ASSIMILATION), 1);
        double feSiKg = round((steelMassKg * (targetSi - initSi) * 100.0) / (SI_CONTENT_FESI * SI_ASSIMILATION), 1);
        double aluminumKg = round((steelMassKg * targetAl * 100.0) / (AL_CONTENT_PURE * AL_ASSIMILATION), 1);

        double totalAlloysKg = round(feMnKg + feSiKg + aluminumKg, 1);

        // Охлаждающий эффект присадок (в среднем ~1.3 °C на каждые 10 кг/т ферросплавов)
        double specificAdditivesKgPerTon = totalAlloysKg / steelMassTons;
        double tempDropCelsius = round(specificAdditivesKgPerTon * 1.35, 1);

        return new DeoxidationResult(steelMassTons, feMnKg, feSiKg, aluminumKg, totalAlloysKg, tempDropCelsius);
    }

    private double round(double val, int places) {
        double scale = Math.pow(10, places);
        return Math.round(val * scale) / scale;
    }
}
