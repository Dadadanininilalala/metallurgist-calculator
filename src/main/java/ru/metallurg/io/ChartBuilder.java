package ru.metallurg.io;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import ru.metallurg.model.ConverterResult;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Построение графиков и диаграмм с помощью JFreeChart (п. 2 ТЗ).
 */
public class ChartBuilder {

    /**
     * Создает столбчатую диаграмму материального и теплового баланса для последней плавки.
     */
    public static JFreeChart createBalanceChart(ConverterResult res) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(res.hotMetalTons, "Масса (т)", "Чугун");
        dataset.addValue(res.scrapTons, "Масса (т)", "Металлолом");
        dataset.addValue(res.limeTons, "Масса (т)", "Известь");
        dataset.addValue(res.steelYieldTons, "Масса (т)", "Выход стали");
        dataset.addValue(res.slagMassTons, "Масса (т)", "Шлак");

        JFreeChart chart = ChartFactory.createBarChart(
                "Материальный баланс конвертерной плавки",
                "Компоненты плавки",
                "Масса, тонн",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
        );
        chart.setBackgroundPaint(Color.WHITE);
        return chart;
    }

    /**
     * Возвращает готовую Swing-панель с графиком для встраивания в интерфейс.
     */
    public static ChartPanel createChartPanel(ConverterResult res) {
        ChartPanel panel = new ChartPanel(createBalanceChart(res));
        panel.setPreferredSize(new Dimension(520, 320));
        return panel;
    }

    /**
     * Сохраняет график в PNG-файл (для отчета и вставки в PDF).
     */
    public static void saveChartAsPng(JFreeChart chart, File file) throws IOException {
        ChartUtils.saveChartAsPNG(file, chart, 640, 400);
    }
}
