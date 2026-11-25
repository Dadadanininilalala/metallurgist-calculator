package ru.metallurg.model;

/**
 * Результат расчета материального и теплового баланса конвертерной плавки.
 */
public class ConverterResult {
    public final double hotMetalTons;      // Масса жидкого чугуна, т
    public final double scrapTons;         // Масса металлолома, т
    public final double limeTons;          // Расход извести, т
    public final double oxygenVolumeM3;    // Расход дутьевого кислорода, м3
    public final double steelYieldTons;    // Выход жидкой стали, т
    public final double slagMassTons;      // Масса конечного шлака, т
    public final double heatInputMJ;       // Приход тепла (физическое + экзотермия), МДж
    public final double heatOutputMJ;      // Расход тепла (нагрев стали, шлака, потери), МДж
    public final double heatBalanceMJ;     // Тепловой баланс (избыток/дефицит), МДж

    public ConverterResult(double hotMetalTons, double scrapTons, double limeTons,
                           double oxygenVolumeM3, double steelYieldTons, double slagMassTons,
                           double heatInputMJ, double heatOutputMJ, double heatBalanceMJ) {
        this.hotMetalTons = hotMetalTons;
        this.scrapTons = scrapTons;
        this.limeTons = limeTons;
        this.oxygenVolumeM3 = oxygenVolumeM3;
        this.steelYieldTons = steelYieldTons;
        this.slagMassTons = slagMassTons;
        this.heatInputMJ = heatInputMJ;
        this.heatOutputMJ = heatOutputMJ;
        this.heatBalanceMJ = heatBalanceMJ;
    }

    public String toInputSummary() {
        return String.format("Чугун: %.1f т, Лом: %.1f т, Известь: %.1f т", hotMetalTons, scrapTons, limeTons);
    }

    public String toResultSummary() {
        return String.format("Выход стали: %.1f т, O2: %.0f м3, Баланс тепла: %+.1f МДж",
                steelYieldTons, oxygenVolumeM3, heatBalanceMJ);
    }
}
