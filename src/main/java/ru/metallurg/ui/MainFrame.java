package ru.metallurg.ui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import ru.metallurg.db.DatabaseManager;
import ru.metallurg.io.ChartBuilder;
import ru.metallurg.io.FileExporter;
import ru.metallurg.io.FileImporter;
import ru.metallurg.model.ConverterResult;
import ru.metallurg.model.DeoxidationResult;
import ru.metallurg.service.ConverterCalculator;
import ru.metallurg.service.DeoxidationCalculator;
import ru.metallurg.service.RandomDataGenerator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Главное окно приложения «Калькулятор металлурга» (Java Swing).
 */
public class MainFrame extends JFrame {
    private final String currentUser;
    private final ConverterCalculator converterCalculator = new ConverterCalculator();
    private final DeoxidationCalculator deoxidationCalculator = new DeoxidationCalculator();
    private final RandomDataGenerator randomGenerator = new RandomDataGenerator();

    // Компоненты таблицы журнала расчетов
    private DefaultTableModel journalModel;
    private JTable journalTable;
    private TableRowSorter<DefaultTableModel> rowSorter;

    // Последний построенный график для вставки в PDF и отображения на вкладке
    private JFreeChart lastChart;
    private JPanel chartContainer;

    public MainFrame(String username) {
        this.currentUser = username;
        setTitle("Калькулятор металлурга | Пользователь: " + currentUser);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 720);
        setLocationRelativeTo(null);

        setJMenuBar(createMenuBar());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("1. Конвертерная плавка", createConverterTab());
        tabbedPane.addTab("2. Обработка на АКОС", createAkosTab());
        tabbedPane.addTab("3. Журнал расчетов (БД)", createJournalTab());
        tabbedPane.addTab("4. Витрина оборудования", createEquipmentTab());
        tabbedPane.addTab("5. Справка и формулы", createHelpTab());

        add(tabbedPane, BorderLayout.CENTER);

        // Загружаем данные из БД при открытии окна
        refreshJournalTable();
    }

    /**
     * Верхнее меню приложения (JMenuBar по п. 13 ТЗ).
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Файл");
        JMenuItem importCsvItem = new JMenuItem("Импорт плавок из CSV...");
        JMenuItem exportCsvItem = new JMenuItem("Экспорт журнала в CSV...");
        JMenuItem exportXlsxItem = new JMenuItem("Экспорт журнала в Excel (.xlsx)...");
        JMenuItem exportPdfItem = new JMenuItem("Экспорт отчета и графика в PDF...");
        JMenuItem exitItem = new JMenuItem("Выход");

        importCsvItem.addActionListener(e -> handleImportCsv());
        exportCsvItem.addActionListener(e -> handleExport("csv"));
        exportXlsxItem.addActionListener(e -> handleExport("xlsx"));
        exportPdfItem.addActionListener(e -> handleExport("pdf"));
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(importCsvItem);
        fileMenu.addSeparator();
        fileMenu.add(exportCsvItem);
        fileMenu.add(exportXlsxItem);
        fileMenu.add(exportPdfItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu helpMenu = new JMenu("О программе");
        JMenuItem aboutItem = new JMenuItem("Методика и автор");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Калькулятор металлурга v1.0\n" +
                        "Расчет материального/теплового баланса конвертера и раскисления на АКОС.\n" +
                        "Методика: О.К. Токовой (ЮУрГУ, 2013).",
                "О программе", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    /**
     * Вкладка 1: Расчет материального и теплового баланса конвертерной плавки + график.
     */
    private JPanel createConverterTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Исходные параметры плавки"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> eqCombo = new JComboBox<>(new String[]{
                "1 - Кислородный конвертер ККЦ-1 (160 т)",
                "2 - Кислородный конвертер ККЦ-2 (300 т)"
        });
        JComboBox<String> gradeCombo = new JComboBox<>(new String[]{"Ст3сп", "09Г2С", "20К", "45", "10ХСНД"});

        JTextField hotMetalField = new JTextField("132.0", 10);
        JTextField scrapField = new JTextField("28.0", 10);
        JTextField cField = new JTextField("4.30", 10);
        JTextField siField = new JTextField("0.70", 10);
        JTextField mnField = new JTextField("0.50", 10);
        JTextField tHotField = new JTextField("1360", 10);
        JTextField tSteelField = new JTextField("1650", 10);

        // Переключатели JRadioButton (п. 13 ТЗ)
        JRadioButton topBlowRadio = new JRadioButton("Верхняя продувка O2", true);
        JRadioButton combBlowRadio = new JRadioButton("Комбинированная (O2 + Ar)");
        ButtonGroup blowGroup = new ButtonGroup();
        blowGroup.add(topBlowRadio);
        blowGroup.add(combBlowRadio);

        int row = 0;
        addFormRow(formPanel, gbc, row++, "Агрегат:", eqCombo);
        addFormRow(formPanel, gbc, row++, "Марка стали:", gradeCombo);
        addFormRow(formPanel, gbc, row++, "Масса жидкого чугуна, т:", hotMetalField);
        addFormRow(formPanel, gbc, row++, "Масса металлолома, т:", scrapField);
        addFormRow(formPanel, gbc, row++, "Углерод (C) в чугуне, %:", cField);
        addFormRow(formPanel, gbc, row++, "Кремний (Si) в чугуне, %:", siField);
        addFormRow(formPanel, gbc, row++, "Марганец (Mn) в чугуне, %:", mnField);
        addFormRow(formPanel, gbc, row++, "Температура чугуна, °C:", tHotField);
        addFormRow(formPanel, gbc, row++, "Целевая T стали, °C:", tSteelField);

        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(topBlowRadio, gbc);
        gbc.gridx = 1;
        formPanel.add(combBlowRadio, gbc);
        row++;

        JButton calcBtn = new JButton("Рассчитать и сохранить в БД");
        calcBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        formPanel.add(calcBtn, gbc);

        // Правая часть: текстовый отчет + график JFreeChart
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        JTextArea resultArea = new JTextArea(11, 40);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        resultArea.setBorder(BorderFactory.createTitledBorder("Результаты материального и теплового баланса"));

        chartContainer = new JPanel(new BorderLayout());
        chartContainer.setBorder(BorderFactory.createTitledBorder("Диаграмма баланса плавки"));

        // Стартовый график по умолчанию
        ConverterResult defaultRes = converterCalculator.calculate(132.0, 28.0, 4.3, 0.7, 0.5, 1360, 1650);
        lastChart = ChartBuilder.createBalanceChart(defaultRes);
        chartContainer.add(new ChartPanel(lastChart), BorderLayout.CENTER);

        rightPanel.add(new JScrollPane(resultArea), BorderLayout.NORTH);
        rightPanel.add(chartContainer, BorderLayout.CENTER);

        calcBtn.addActionListener(e -> {
            try {
                double hotMetal = Double.parseDouble(hotMetalField.getText().replace(",", "."));
                double scrap = Double.parseDouble(scrapField.getText().replace(",", "."));
                double c = Double.parseDouble(cField.getText().replace(",", "."));
                double si = Double.parseDouble(siField.getText().replace(",", "."));
                double mn = Double.parseDouble(mnField.getText().replace(",", "."));
                double tHot = Double.parseDouble(tHotField.getText().replace(",", "."));
                double tSteel = Double.parseDouble(tSteelField.getText().replace(",", "."));

                ConverterResult res = converterCalculator.calculate(hotMetal, scrap, c, si, mn, tHot, tSteel);
                String blowMode = topBlowRadio.isSelected() ? "Верхняя продувка" : "Комбинированная продувка";

                resultArea.setText(String.format(
                        "Режим: %s | Марка: %s%n" +
                                "--------------------------------------------------%n" +
                                "1. Расход извести:          %.2f т%n" +
                                "2. Расход кислорода (O2):   %.1f м3%n" +
                                "3. Выход жидкой стали:      %.2f т%n" +
                                "4. Масса конечного шлака:   %.2f т%n" +
                                "--------------------------------------------------%n" +
                                "5. Приход тепла:            %.1f МДж%n" +
                                "6. Расход тепла:            %.1f МДж%n" +
                                "7. Тепловой баланс:         %+.1f МДж (%s)",
                        blowMode, gradeCombo.getSelectedItem(),
                        res.limeTons, res.oxygenVolumeM3, res.steelYieldTons, res.slagMassTons,
                        res.heatInputMJ, res.heatOutputMJ, res.heatBalanceMJ,
                        res.heatBalanceMJ >= 0 ? "избыток тепла, плавка обеспечена" : "дефицит тепла, снизить долю лома"
                ));

                // Обновляем график
                lastChart = ChartBuilder.createBalanceChart(res);
                chartContainer.removeAll();
                chartContainer.add(new ChartPanel(lastChart), BorderLayout.CENTER);
                chartContainer.revalidate();
                chartContainer.repaint();

                // Сохраняем запись в БД
                int eqId = eqCombo.getSelectedIndex() == 0 ? 1 : 2;
                saveCalculationToDb(eqId, "Баланс конвертерной плавки",
                        (String) gradeCombo.getSelectedItem(),
                        res.toInputSummary(), res.toResultSummary(), res.steelYieldTons);

                JOptionPane.showMessageDialog(this, "Расчет выполнен и сохранен в журнал БД!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Проверьте корректность чисел: " + ex.getMessage(),
                        "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(formPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Вкладка 2: Расчет раскисления и легирования стали на АКОС.
     */
    private JPanel createAkosTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Параметры внепечной обработки (АКОС)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> eqCombo = new JComboBox<>(new String[]{
                "3 - АКОС-1 (УПК 160 т)",
                "4 - АКОС-2 (УПК 300 т)"
        });
        JComboBox<String> gradeCombo = new JComboBox<>(new String[]{"09Г2С", "Ст3сп", "20К", "45", "17Г1С"});
        JTextField massField = new JTextField("155.0", 10);
        JTextField initMnField = new JTextField("0.12", 10);
        JTextField targetMnField = new JTextField("1.45", 10);
        JTextField initSiField = new JTextField("0.01", 10);
        JTextField targetSiField = new JTextField("0.60", 10);
        JTextField targetAlField = new JTextField("0.035", 10);

        int r = 0;
        addFormRow(formPanel, gbc, r++, "Агрегат:", eqCombo);
        addFormRow(formPanel, gbc, r++, "Целевая марка стали:", gradeCombo);
        addFormRow(formPanel, gbc, r++, "Масса жидкой стали в ковше, т:", massField);
        addFormRow(formPanel, gbc, r++, "Исходный Mn в металле, %:", initMnField);
        addFormRow(formPanel, gbc, r++, "Требуемый Mn в марке, %:", targetMnField);
        addFormRow(formPanel, gbc, r++, "Исходный Si в металле, %:", initSiField);
        addFormRow(formPanel, gbc, r++, "Требуемый Si в марке, %:", targetSiField);
        addFormRow(formPanel, gbc, r++, "Требуемый остаточный Al, %:", targetAlField);

        JButton calcBtn = new JButton("Рассчитать присадки и сохранить");
        calcBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        gbc.gridx = 0; gbc.gridy = r; gbc.gridwidth = 2;
        formPanel.add(calcBtn, gbc);

        JTextArea akosOutput = new JTextArea();
        akosOutput.setEditable(false);
        akosOutput.setFont(new Font("Monospaced", Font.PLAIN, 14));
        akosOutput.setBorder(BorderFactory.createTitledBorder("Ведомость расхода ферросплавов и раскислителей"));

        calcBtn.addActionListener(e -> {
            try {
                double mass = Double.parseDouble(massField.getText().replace(",", "."));
                double iMn = Double.parseDouble(initMnField.getText().replace(",", "."));
                double tMn = Double.parseDouble(targetMnField.getText().replace(",", "."));
                double iSi = Double.parseDouble(initSiField.getText().replace(",", "."));
                double tSi = Double.parseDouble(targetSiField.getText().replace(",", "."));
                double tAl = Double.parseDouble(targetAlField.getText().replace(",", "."));

                DeoxidationResult res = deoxidationCalculator.calculate(mass, iMn, tMn, iSi, tSi, tAl);

                akosOutput.setText(String.format(
                        "Марка стали: %s | Масса плавки: %.1f т%n" +
                                "=====================================================%n" +
                                "1. Ферромарганец ФМн78 (усвоение 85%%):   %.1f кг%n" +
                                "2. Ферросилиций ФС65 (усвоение 80%%):    %.1f кг%n" +
                                "3. Алюминий первичный АВ97 (усв. 45%%):   %.1f кг%n" +
                                "-----------------------------------------------------%n" +
                                "Итого ферросплавов и раскислителей:      %.1f кг%n" +
                                "Охлаждение расплава от присадок:         -%.1f °C%n" +
                                "Рекомендация: включить дуговой подогрев на %.0f мин.",
                        gradeCombo.getSelectedItem(), res.steelMassTons,
                        res.feMnKg, res.feSiKg, res.aluminumKg,
                        res.totalAlloysKg, res.tempDropCelsius,
                        Math.ceil(res.tempDropCelsius / 3.5)
                ));

                int eqId = eqCombo.getSelectedIndex() == 0 ? 3 : 4;
                saveCalculationToDb(eqId, "Раскисление и легирование (АКОС)",
                        (String) gradeCombo.getSelectedItem(),
                        res.toInputSummary(), res.toResultSummary(), res.steelMassTons);

                JOptionPane.showMessageDialog(this, "Расчет АКОС сохранен в журнал БД!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка расчета: " + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(formPanel, BorderLayout.WEST);
        panel.add(new JScrollPane(akosOutput), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Вкладка 3: Журнал расчетов из БД (поиск, фильтрация, сортировка, удаление, генератор, импорт/экспорт).
     */
    private JPanel createJournalTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Панель поиска и фильтрации (п. 1 ТЗ)
        JPanel topControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topControls.add(new JLabel("Поиск / Фильтр (марка, тип, дата):"));
        JTextField searchField = new JTextField(22);
        topControls.add(searchField);

        JButton refreshBtn = new JButton("Обновить из БД");
        JButton deleteBtn = new JButton("Удалить выбранную строку");
        topControls.add(refreshBtn);
        topControls.add(deleteBtn);

        // Таблица с поддержкой сортировки по клику на заголовок (п. 1, 13 ТЗ)
        String[] columns = {"ID", "Дата и время", "Агрегат", "Тип расчета", "Марка стали", "Исходные данные", "Результат", "Масса стали, т"};
        journalModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        journalTable = new JTable(journalModel);
        rowSorter = new TableRowSorter<>(journalModel);
        journalTable.setRowSorter(rowSorter);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void filter() {
                String text = searchField.getText().trim();
                rowSorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + text));
            }
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });

        refreshBtn.addActionListener(e -> refreshJournalTable());
        deleteBtn.addActionListener(e -> deleteSelectedCalculation());

        // Нижняя панель: Генератор случайных данных (JCheckBox по п. 13 ТЗ) + Кнопки экспорта
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Генерация случайных плавок и обмен файлами"));

        bottomPanel.add(new JLabel("Кол-во:"));
        JTextField countField = new JTextField("5", 4);
        JCheckBox convCheck = new JCheckBox("Конвертер", true);
        JCheckBox akosCheck = new JCheckBox("АКОС", true);
        JButton generateBtn = new JButton("Сгенерировать");

        generateBtn.addActionListener(e -> {
            try {
                int count = Integer.parseInt(countField.getText().trim());
                int added = randomGenerator.generateAndSave(count, convCheck.isSelected(), akosCheck.isSelected());
                refreshJournalTable();
                JOptionPane.showMessageDialog(this, "Сгенерировано и добавлено в БД записей: " + added);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка генерации: " + ex.getMessage());
            }
        });

        JButton importBtn = new JButton("Импорт CSV");
        JButton csvBtn = new JButton("Экспорт CSV");
        JButton xlsxBtn = new JButton("Экспорт XLSX");
        JButton pdfBtn = new JButton("Экспорт PDF");

        importBtn.addActionListener(e -> handleImportCsv());
        csvBtn.addActionListener(e -> handleExport("csv"));
        xlsxBtn.addActionListener(e -> handleExport("xlsx"));
        pdfBtn.addActionListener(e -> handleExport("pdf"));

        bottomPanel.add(countField);
        bottomPanel.add(convCheck);
        bottomPanel.add(akosCheck);
        bottomPanel.add(generateBtn);
        bottomPanel.add(new JSeparator(SwingConstants.VERTICAL));
        bottomPanel.add(importBtn);
        bottomPanel.add(csvBtn);
        bottomPanel.add(xlsxBtn);
        bottomPanel.add(pdfBtn);

        panel.add(topControls, BorderLayout.NORTH);
        panel.add(new JScrollPane(journalTable), BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Вкладка 4: Витрина металлургических агрегатов из БД (п. 4 ТЗ).
     */
    private JPanel createEquipmentTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] cols = {"ID", "Наименование агрегата", "Тип", "Садка, т", "Мощность / Интенсивность", "Назначение"};
        DefaultTableModel eqModel = new DefaultTableModel(cols, 0);
        JTable eqTable = new JTable(eqModel);
        eqTable.setRowHeight(28);

        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, type, capacity_tons, power_or_blow, description FROM equipment")) {
            while (rs.next()) {
                eqModel.addRow(new Object[]{
                        rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getDouble(4), rs.getString(5), rs.getString(6)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки оборудования: " + e.getMessage());
        }

        panel.add(new JLabel("<html><h3>Технические характеристики металлургических агрегатов цеха</h3></html>"), BorderLayout.NORTH);
        panel.add(new JScrollPane(eqTable), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Вкладка 5: Справка с расчетными формулами и инструкцией (п. 1, 14 ТЗ).
     */
    private JPanel createHelpTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextArea helpText = new JTextArea();
        helpText.setEditable(false);
        helpText.setFont(new Font("SansSerif", Font.PLAIN, 14));
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        helpText.setText(
                "СПРАВКА ПО МЕТОДИКЕ РАСЧЕТОВ И РАБОТЕ С ПРОГРАММОЙ\n" +
                        "====================================================================\n\n" +
                        "1. МАТЕРИАЛЬНЫЙ И ТЕПЛОВОЙ БАЛАНС КОНВЕРТЕРНОЙ ПЛАВКИ:\n" +
                        "   • Расход извести: G_изв = (G_Si * 2.14 * B) / %CaO_изв + 0.015 * G_шихты,\n" +
                        "     где B = 3.2 — заданная основность шлака.\n" +
                        "   • Выход жидкой стали: G_ст = G_чуг + G_лом - G_примесей - G_угар_Fe.\n" +
                        "   • Расход кислорода: V_O2 = (0.98*m_C + 0.80*m_Si + 0.20*m_Mn + 200*G_угар_Fe) / 0.995.\n" +
                        "   • Тепловой баланс: Q_прих = Q_физ(чуг) + Q_хим(C, Si, Mn, Fe); Q_расх = Q_ст + Q_шлак + Q_потери.\n\n" +
                        "2. РАСЧЕТ РАСКИСЛЕНИЯ И ЛЕГИРОВАНИЯ НА АКОС:\n" +
                        "   • Масса ферросплава: G_фс = G_ст * (C_кон - C_нач) * 100 / (C_фс * K_усв),\n" +
                        "     где K_усв — коэффициент усвоения элемента (Mn: 85%, Si: 80%, Al: 45%).\n\n" +
                        "3. КРАТКАЯ ИНСТРУКЦИЯ ПОЛЬЗОВАТЕЛЯ:\n" +
                        "   • Вкладки 1 и 2 предназначены для ручного ввода параметров и расчета плавки.\n" +
                        "   • Все расчеты автоматически сохраняются в базу данных SQLite (Вкладка 3).\n" +
                        "   • Во вкладке 3 доступны поиск в реальном времени, сортировка по столбцам,\n" +
                        "     генерация случайных плавок, пакетный импорт из CSV и экспорт в CSV/XLSX/PDF."
        );

        panel.add(new JScrollPane(helpText), BorderLayout.CENTER);
        return panel;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private void saveCalculationToDb(int equipmentId, String calcType, String grade,
                                     String inputSummary, String resultSummary, double yieldTons) throws SQLException {
        String sql = """
                INSERT INTO calculations (calc_date, equipment_id, calc_type, steel_grade, input_summary, result_summary, total_yield_tons)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            ps.setInt(2, equipmentId);
            ps.setString(3, calcType);
            ps.setString(4, grade);
            ps.setString(5, inputSummary);
            ps.setString(6, resultSummary);
            ps.setDouble(7, yieldTons);
            ps.executeUpdate();
        }
        refreshJournalTable();
    }

    private void refreshJournalTable() {
        if (journalModel == null) return;
        journalModel.setRowCount(0);
        String sql = """
                SELECT c.id, c.calc_date, e.name, c.calc_type, c.steel_grade, c.input_summary, c.result_summary, c.total_yield_tons
                FROM calculations c
                LEFT JOIN equipment e ON c.equipment_id = e.id
                ORDER BY c.id DESC
                """;
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                journalModel.addRow(new Object[]{
                        rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getString(7), rs.getDouble(8)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка чтения БД: " + e.getMessage());
        }
    }

    private void deleteSelectedCalculation() {
        int viewRow = journalTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Выберите строку в таблице для удаления.");
            return;
        }
        int modelRow = journalTable.convertRowIndexToModel(viewRow);
        int id = (int) journalModel.getValueAt(modelRow, 0);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM calculations WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            refreshJournalTable();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка удаления: " + e.getMessage());
        }
    }

    private void handleImportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите CSV-файл (Марка;Чугун;Лом;C;Si;Mn)");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                int count = FileImporter.importConverterCsv(chooser.getSelectedFile());
                refreshJournalTable();
                JOptionPane.showMessageDialog(this, "Успешно импортировано и рассчитано плавок: " + count);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка импорта файла: " + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleExport(String format) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("metallurg_report." + format));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                switch (format) {
                    case "csv" -> FileExporter.exportToCsv(journalModel, file);
                    case "xlsx" -> FileExporter.exportToXlsx(journalModel, file);
                    case "pdf" -> FileExporter.exportToPdf(journalModel, lastChart, file);
                }
                JOptionPane.showMessageDialog(this, "Файл успешно сохранен:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка сохранения файла: " + ex.getMessage(),
                        "Ошибка экспорта", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
