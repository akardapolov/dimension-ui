package ru.dimension.ui.component.module.analyze;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.util.IDetailPanel;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.block.InsightConfigBlock;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.module.analyze.handler.TablePopupCellEditorHandler;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.AlgorithmType;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.TimeSeriesAlgorithm;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.anomaly.MatrixProfileAlgorithm;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.forecast.ARIMAlgorithm;
import ru.dimension.ui.component.module.analyze.timeseries.popup.PopupPanel;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.column.SeriesNames;
import ru.dimension.ui.model.data.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.table.JXTableCase;

@Log4j2
public abstract class AnalyzeTimeSeriesPanel extends JPanel implements IDetailPanel {

  private static final Dimension DEFAULT_CHART_SIZE = new Dimension(100, 200);

  private final JXTableCase tableSeries;
  private final Map<String, Color> seriesColorMap;
  private final JSplitPane jspSeriesSettingsChart;
  protected JSplitPane jspSettingsChart;
  protected JPanel jrbPanel;
  protected JButton btnSettings;

  protected CategoryTableXYDatasetRealTime chartDataset;
  protected List<TimeSeriesAlgorithm<?>> listAlgorithm = new ArrayList<>();
  protected List<JRadioButton> jrbListAlgorithm = new ArrayList<>();
  protected ButtonGroup buttonGroup;
  private final PopupPanel popupPanel;

  protected Long begin;
  protected Long end;

  private boolean ignoreCheckboxEvents = false;
  private final Map<String, JXTaskPane> chartCards = new HashMap<>();

  private boolean hideSettings = false;

  private final JTextField columnSearch;
  private final JXTaskPaneContainer cardContainer;
  private final JScrollPane cardScrollPane;
  private final InsightConfigBlock insightConfigBlock;
  private TableRowSorter<?> sorter;

  public AnalyzeTimeSeriesPanel(Map<String, Color> seriesColorMap,
                                CategoryTableXYDatasetRealTime chartDataset) {
    this.seriesColorMap = seriesColorMap;
    this.chartDataset = chartDataset;

    this.tableSeries = GUIHelper.getJXTableCaseCheckBoxAdHoc(6,
                                                             new String[]{
                                                                 SeriesNames.COLOR.getColName(),
                                                                 SeriesNames.SERIES.getColName(),
                                                                 SeriesNames.PICK.getColName()
                                                             }, ColumnNames.PICK.ordinal());

    this.tableSeries.getJxTable().getColumnExt(SeriesNames.COLOR.ordinal()).setVisible(false);

    TableColumn nameColumn = tableSeries.getJxTable().getColumnModel().getColumn(SeriesNames.SERIES.ordinal());
    nameColumn.setMinWidth(30);
    nameColumn.setMaxWidth(40);

    this.sorter = new TableRowSorter<>(tableSeries.getDefaultTableModel());
    this.tableSeries.getJxTable().setRowSorter(sorter);

    this.columnSearch = new JTextField(15);
    this.columnSearch.setToolTipText("Search series...");
    this.columnSearch.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) { updateFilter(); }
      @Override
      public void removeUpdate(DocumentEvent e) { updateFilter(); }
      @Override
      public void changedUpdate(DocumentEvent e) { updateFilter(); }
    });

    this.cardContainer = new JXTaskPaneContainer();
    LaF.setBackgroundColor(CHART_PANEL, cardContainer);
    cardContainer.setBackgroundPainter(null);

    this.cardScrollPane = new JScrollPane(cardContainer);
    GUIHelper.setScrolling(cardScrollPane);
    cardScrollPane.setViewportView(cardContainer);

    this.insightConfigBlock = new InsightConfigBlock();
    this.insightConfigBlock.getCollapseCardPanel().setStateChangeConsumer(state -> {
      boolean shouldCollapse = (state == ChartCardState.COLLAPSE_ALL);
      for (Component comp : cardContainer.getComponents()) {
        if (comp instanceof JXTaskPane) {
          ((JXTaskPane) comp).setCollapsed(shouldCollapse);
        }
      }
    });

    this.insightConfigBlock.getCollapseCardPanel().setState(ChartCardState.EXPAND_ALL);

    this.popupPanel = new PopupPanel();
    CellEditorListener popupHandler = new TablePopupCellEditorHandler(popupPanel, listAlgorithm);
    popupPanel.getTable().getJxTable().getDefaultEditor(Object.class).addCellEditorListener(popupHandler);
    popupPanel.getTable().getJxTable().getDefaultEditor(Object.class).addCellEditorListener(new CellEditorListener() {
      @Override
      public void editingStopped(ChangeEvent e) {
        updateAllCharts();
      }

      @Override
      public void editingCanceled(ChangeEvent e) {
      }
    });

    this.jspSettingsChart = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 40);
    this.jspSeriesSettingsChart = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 250);

    this.jrbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    this.buttonGroup = new ButtonGroup();

    JPanel topBarPanel = new JPanel();
    LaF.setBackgroundConfigPanel(CHART_PANEL, topBarPanel);
    PainlessGridBag gblTop = new PainlessGridBag(topBarPanel, PGHelper.getPGConfig(1), false);
    gblTop.row()
        .cell(jrbPanel).cell(popupPanel)
        .cellX(new JPanel(), 1).fillX()
        .cell(insightConfigBlock);
    PGHelper.setConstrainsInsets(gblTop, jrbPanel, 0, 5);
    PGHelper.setConstrainsInsets(gblTop, popupPanel, 0, 5);
    gblTop.done();

    this.jspSettingsChart.setTopComponent(topBarPanel);
    this.jspSettingsChart.setBottomComponent(cardScrollPane);

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.setBorder(BorderFactory.createTitledBorder("Series"));
    leftPanel.add(columnSearch, BorderLayout.NORTH);
    leftPanel.add(tableSeries.getJScrollPane(), BorderLayout.CENTER);

    this.jspSeriesSettingsChart.setRightComponent(jspSettingsChart);
    this.jspSeriesSettingsChart.setLeftComponent(leftPanel);

    tableSeries.getDefaultTableModel().addTableModelListener(e -> {
      if (ignoreCheckboxEvents || e.getType() != TableModelEvent.UPDATE || e.getColumn() != SeriesNames.PICK.ordinal()) {
        return;
      }

      int row = e.getFirstRow();
      String seriesName = (String) tableSeries.getDefaultTableModel().getValueAt(row, SeriesNames.SERIES.ordinal());
      boolean isSelected = (Boolean) tableSeries.getDefaultTableModel().getValueAt(row, SeriesNames.PICK.ordinal());

      if (isSelected) {
        addChartCard(seriesName);
      } else {
        removeChartCard(seriesName);
      }
    });

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXYRemainder(jspSeriesSettingsChart).fillXY();
    gbl.done();
  }

  public void init() {
    this.loadData();
    this.fillAlgorithm();
    this.viewRadioButton();
    this.loadAllCharts();
  }

  private void loadAllCharts() {
    ignoreCheckboxEvents = true;
    DefaultTableModel model = tableSeries.getDefaultTableModel();
    for (int i = 0; i < model.getRowCount(); i++) {
      model.setValueAt(true, i, SeriesNames.PICK.ordinal());
      String seriesName = (String) model.getValueAt(i, SeriesNames.SERIES.ordinal());
      addChartCard(seriesName);
    }
    ignoreCheckboxEvents = false;
  }

  private void addChartCard(String seriesName) {
    log.info("Requesting chart for series: {}", seriesName);
    listAlgorithm.stream()
        .filter(f -> getAlgorithmType().equals(f.getType()))
        .filter(algorithm -> jrbListAlgorithm.stream()
            .anyMatch(jrb -> jrb.getText().equals(algorithm.getName()) && jrb.isSelected()))
        .findFirst()
        .ifPresent(algorithm -> processAlgorithmWithHandling(algorithm, seriesName));
  }

  public void removeChartCard(String seriesName) {
    log.info("Removing chart for series: {}", seriesName);
    JXTaskPane card = chartCards.remove(seriesName);
    if (card != null) {
      cardContainer.remove(card);
      cardContainer.revalidate();
      cardContainer.repaint();
    }
  }

  private void updateFilter() {
    String text = columnSearch.getText();

    if (tableSeries == null) return;
    if (sorter == null) {
      sorter = new TableRowSorter<>(tableSeries.getJxTable().getModel());
      tableSeries.getJxTable().setRowSorter(sorter);
    }

    if (text == null || text.isEmpty()) {
      sorter.setRowFilter(null);
    } else {
      try {
        sorter.setRowFilter(RowFilter.regexFilter("(?iu)" + text, SeriesNames.SERIES.ordinal()));
      } catch (PatternSyntaxException e) {
        sorter.setRowFilter(RowFilter.regexFilter("$^", SeriesNames.SERIES.ordinal()));
      }
    }
  }

  protected void displayChart(String title, JPanel chartPanel) {
    removeChartCard(title);

    if (chartPanel != null) {
      chartPanel.setPreferredSize(DEFAULT_CHART_SIZE);

      JXTaskPane card = new JXTaskPane();
      card.setTitle(title);
      card.add(chartPanel);
      card.setCollapsed(insightConfigBlock.getCollapseCardPanel().getCurrentState() == ChartCardState.COLLAPSE_ALL);
      cardContainer.add(card);
      chartCards.put(title, card);
    }
    cardContainer.revalidate();
    cardContainer.repaint();
  }

  private void updateAllCharts() {
    log.info("Updating all visible charts due to parameter or algorithm change.");
    new ArrayList<>(chartCards.keySet()).forEach(this::addChartCard);
  }

  protected void processAlgorithmWithHandling(TimeSeriesAlgorithm algorithm, String value) {
    try {
      processAlgorithm(algorithm, value);
    } catch (IllegalArgumentException | NegativeArraySizeException iae) {
      JOptionPane.showMessageDialog(this, "Too small dataset for series '" + value + "', try to select more data",
                                    algorithm.getName() + " Error", JOptionPane.ERROR_MESSAGE);
      displayChart(value, null);
    } catch (Exception e) {
      DialogHelper.showErrorDialog(this, e.getMessage(), "General Connection Error", e);
      displayChart(value, null);
    }
  }

  protected abstract void processAlgorithm(TimeSeriesAlgorithm algorithm, String value);

  protected abstract AlgorithmType getAlgorithmType();

  private void loadData() {
    for (Map.Entry<String, Color> entry : seriesColorMap.entrySet()) {
      tableSeries.getDefaultTableModel().addRow(new Object[]{entry.getValue(), entry.getKey(), Boolean.FALSE});
    }
  }

  private void fillAlgorithm() {
    Map<String, String> matrixProfileParams = new HashMap<>();
    matrixProfileParams.put("Window", "10");

    MatrixProfileAlgorithm matrixProfileAlgorithm =
        new MatrixProfileAlgorithm("STAMP", matrixProfileParams, chartDataset);
    this.listAlgorithm.add(matrixProfileAlgorithm);

    Map<String, String> ARIMAParams = new HashMap<>();
    ARIMAParams.put("Steps", "15");
    ARIMAParams.put("Order of AR", "6");
    ARIMAParams.put("Order of MA", "3");
    ARIMAParams.put("Linear trend", "false");
    ARIMAlgorithm arimAlgorithm = new ARIMAlgorithm("ARIMA", ARIMAParams, chartDataset);
    this.listAlgorithm.add(arimAlgorithm);
  }

  protected void viewRadioButton() {
    listAlgorithm.stream()
        .filter(f -> getAlgorithmType().equals(f.getType()))
        .map(algorithm -> {
          JRadioButton jRadioButton = new JRadioButton(algorithm.getName());
          jRadioButton.addActionListener(event -> {
            popupPanel.getTable().getJxTable().clearSelection();
            popupPanel.getTable().getDefaultTableModel().setRowCount(0);

            JRadioButton selectedButton = (JRadioButton) event.getSource();
            String selectedText = selectedButton.getText();
            popupPanel.getTextField().setText(selectedText);

            for (Map.Entry<String, String> entry : algorithm.getParameters().entrySet()) {
              popupPanel.getTable().getDefaultTableModel()
                  .addRow(new Object[]{entry.getKey(), entry.getValue()});
            }

            updateAllCharts();
          });
          buttonGroup.add(jRadioButton);
          return jRadioButton;
        })
        .forEach(radioButton -> {
          jrbListAlgorithm.add(radioButton);
          jrbPanel.add(radioButton);
        });

    jrbListAlgorithm.stream().findFirst().ifPresent(radioButton -> {
      radioButton.setSelected(true);
      popupPanel.getTextField().setText(radioButton.getText());
      listAlgorithm.stream()
          .filter(algorithm -> algorithm.getName().equals(radioButton.getText()))
          .findFirst()
          .ifPresent(alg -> alg.getParameters()
              .forEach((key, val) -> popupPanel.getTable().getDefaultTableModel()
                  .addRow(new Object[]{key, val})));
    });
  }

  protected JPanel createChartPanel(TimeSeriesAlgorithm algorithm,
                                    String value,
                                    CategoryTableXYDatasetRealTime chartDataset,
                                    boolean isShowLegend) {
    String xAxisLabel = value.isEmpty() ? " " : value;
    String yAxisLabel = "Value";

    String title = "";

    JFreeChart jFreeChart = ChartFactory.createTimeSeriesChart(title, xAxisLabel, yAxisLabel, chartDataset,
                                                               isShowLegend, true, false);

    XYPlot plot = (XYPlot) jFreeChart.getPlot();

    plot.getRenderer().setSeriesPaint(0, getColor(value));
    plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));

    if (plot.getSeriesCount() > 1) {
      float width = 2.0f;
      float[] dash = { 2.0f, 5.0f };

      for (int series = 1; series < plot.getSeriesCount(); series++) {
        Color color = getColor(value).brighter();
        BasicStroke basicStroke = new BasicStroke(width,
                                                  BasicStroke.CAP_ROUND,
                                                  BasicStroke.JOIN_ROUND,
                                                  1.0f,
                                                  dash,
                                                  0.0f);

        plot.getRenderer().setSeriesPaint(series, color);
        plot.getRenderer().setSeriesStroke(series, basicStroke);
      }
    }

    ChartPanel chartPanel = new ChartPanel(jFreeChart, false, false, false, false, false);

    LaF.setBackgroundAndTextColorForChartPanel(CHART_PANEL, chartPanel);

    return chartPanel;
  }

  private Color getColor(String value) {
    return seriesColorMap.get(value);
  }

  @Override
  public void loadDataToDetail(long begin, long end) {
    this.begin = begin;
    this.end = end;
    log.info("Begin: {} End: {}", this.begin, this.end);
  }
}