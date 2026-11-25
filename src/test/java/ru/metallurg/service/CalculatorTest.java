package ru.metallurg.service;

import org.junit.jupiter.api.Test;
import ru.metallurg.model.ConverterResult;
import ru.metallurg.model.DeoxidationResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты расчетного ядра приложения (п. 16 ТЗ).
 */
class CalculatorTest {

    @Test
    void testConverterBalanceCalculation() {
        ConverterCalculator calc = new ConverterCalculator();
        ConverterResult res = calc.calculate(130.0, 30.0, 4.3, 0.7, 0.5, 1360.0, 1650.0);

        assertTrue(res.steelYieldTons > 140.0 && res.steelYieldTons < 155.0,
                "Выход годной стали должен составлять ~90-93% от металлозавалки");
        assertTrue(res.oxygenVolumeM3 > 7000.0,
                "Расход кислорода на 160 т садки должен превышать 7000 м3");
        assertTrue(res.limeTons > 5.0,
                "Расход извести должен быть положительным");
    }

    @Test
    void testConverterInvalidInputThrowsException() {
        ConverterCalculator calc = new ConverterCalculator();
        assertThrows(IllegalArgumentException.class, () ->
                calc.calculate(-10.0, 20.0, 4.2, 0.6, 0.5, 1350.0, 1650.0));
    }

    @Test
    void testDeoxidationCalculation() {
        DeoxidationCalculator calc = new DeoxidationCalculator();
        DeoxidationResult res = calc.calculate(150.0, 0.10, 0.60, 0.01, 0.25, 0.03);

        // Для 150 т стали и прироста Mn на 0.50%: (150000 * 0.5 * 100) / (78 * 85) = 1131.2 кг
        assertEquals(1131.2, res.feMnKg, 0.5);
        assertTrue(res.feSiKg > 600.0);
        assertTrue(res.aluminumKg > 90.0);
    }
}
