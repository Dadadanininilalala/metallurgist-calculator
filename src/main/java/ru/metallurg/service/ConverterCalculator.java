package ru.metallurg.service;

import ru.metallurg.model.ConverterResult;

/**
 * Расчет материального и теплового баланса кислородно-конвертерной плавки
 * (по методике О.К. Токового, ЮУрГУ, 2013).
 */
public class ConverterCalculator {

    /**
     * Выполняет расчет плавки.
     *
     * @param hotMetalTons   масса чугуна, т
     * @param scrapTons      масса металлолома, т
     * @param cPercent       содержание углерода в чугуне, % (обычно 4.0 - 4.5)
     * @param siPercent      содержание кремния в чугуне, % (обычно 0.5 - 0.9)
     * @param mnPercent      содержание марганца в чугуне, % (обычно 0.3 - 0.8)
     * @param hotMetalTempC  температура заливаемого чугуна, °C (обычно 1320 - 1400)
     * @param targetSteelTempC целевая температура стали на выпуске, °C (обычно 1630 - 1670)
     */
    public ConverterResult calculate(double hotMetalTons, double scrapTons,
                                     double cPercent, double siPercent, double mnPercent,
                                     double hotMetalTempC, double targetSteelTempC) {
        validatePositive(hotMetalTons, "Масса чугуна");
        if (scrapTons < 0) throw new IllegalArgumentException("Масса лома не может быть отрицательной");

        double totalChargeTons = hotMetalTons + scrapTons;

        // 1. Расчет извести на ошлакование кремния (основность шлака B = 3.2, содержание CaO в извести 90%)
        double siMassTons = hotMetalTons * (siPercent / 100.0);
        double sio2MassTons = siMassTons * 2.14; // Стехиометрия Si -> SiO2
        double limeTons = round((sio2MassTons * 3.2) / 0.90 + totalChargeTons * 0.015, 2);

        // 2. Масса конечного шлака (SiO2 + CaO + оксиды железа и марганца)
        double slagMassTons = round(limeTons * 0.90 + sio2MassTons + totalChargeTons * 0.025, 2);

        // 3. Выход жидкой стали (угар примесей C, Si, Mn + угар железа со шлаком и пылью ~7-9%)
        double impuritiesBurnTons = hotMetalTons * ((cPercent + siPercent + mnPercent) / 100.0);
        double ironLossTons = totalChargeTons * 0.032;
        double steelYieldTons = round(totalChargeTons - impuritiesBurnTons - ironLossTons, 2);

        // 4. Расход технического кислорода (99.5% O2) на окисление C, Si, Mn и Fe, м3
        double cMassKg = hotMetalTons * 10.0 * cPercent;
        double siMassKg = hotMetalTons * 10.0 * siPercent;
        double mnMassKg = hotMetalTons * 10.0 * mnPercent;
        // Удельный расход O2: C -> 0.933 м3/кг (до CO), Si -> 0.80 м3/кг, Mn -> 0.20 м3/кг + на Fe
        double oxygenVolumeM3 = round((cMassKg * 0.98 + siMassKg * 0.80 + mnMassKg * 0.20 + ironLossTons * 200.0) / 0.995, 1);

        // 5. Тепловой баланс (МДж)
        // Приход: физическое тепло чугуна + тепло окисления примесей (C, Si, Mn, Fe)
        double physHeatHotMetal = hotMetalTons * 1000.0 * (0.84 * hotMetalTempC + 210.0) / 1000.0; // МДж
        double chemHeat = (cMassKg * 10.5 + siMassKg * 31.0 + mnMassKg * 7.0 + ironLossTons * 1000.0 * 4.8) / 1000.0;
        double heatInputMJ = round(physHeatHotMetal + chemHeat, 1);

        // Расход: физическое тепло жидкой стали + тепло шлака + нагрев лома и извести + тепловые потери (8%)
        double steelHeat = steelYieldTons * 1000.0 * (0.82 * targetSteelTempC + 270.0) / 1000.0;
        double slagHeat = slagMassTons * 1000.0 * (1.25 * targetSteelTempC + 300.0) / 1000.0;
        double lossesHeat = heatInputMJ * 0.08;
        double heatOutputMJ = round(steelHeat + slagHeat + lossesHeat, 1);

        double heatBalanceMJ = round(heatInputMJ - heatOutputMJ, 1);

        return new ConverterResult(hotMetalTons, scrapTons, limeTons, oxygenVolumeM3,
                steelYieldTons, slagMassTons, heatInputMJ, heatOutputMJ, heatBalanceMJ);
    }

    private void validatePositive(double val, String name) {
        if (val <= 0) throw new IllegalArgumentException(name + " должна быть больше нуля");
    }

    private double round(double val, int places) {
        double scale = Math.pow(10, places);
        return Math.round(val * scale) / scale;
    }
}
