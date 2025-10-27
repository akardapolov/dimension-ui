package ru.dimension.ui.component.module.analyze;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import lombok.extern.log4j.Log4j2;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.util.IDetailPanel;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.forecast.ARIMAlgorithm;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.data.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.column.DimensionValuesNames;
import ru.dimension.ui.component.module.analyze.handler.TablePopupCellEditorHandler;
import ru.dimension.ui.component.module.analyze.handler.TableSelectionHandler;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.AlgorithmType;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.TimeSeriesAlgorithm;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.anomaly.MatrixProfileAlgorithm;
import ru.dimension.ui.component.module.analyze.timeseries.popup.PopupPanel;

@Log4j2
public abstract class AnalyzeTimeSeriesPanel extends JPanel implements IDetailPanel {

  private JXTableCase tableSeries;
  private Map<String, Color> seriesColorMap;
  private JSplitPane jspSeriesSettingsChart;
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

  private String value;

  private boolean hideSettings = false;

  public AnalyzeTimeSeriesPanel(JXTableCase tableSeries,
                                Map<String, Color> seriesColorMap,
                                CategoryTableXYDatasetRealTime chartDataset) {
    this.tableSeries = tableSeries;
    this.seriesColorMap = seriesColorMap;
    this.chartDataset = chartDataset;

    this.popupPanel = new PopupPanel();

    popupPanel.getTable().getJxTable().getDefaultEditor(Object.class)
        .addCellEditorListener(new TablePopupCellEditorHandler(popupPanel, listAlgorithm) {
        });

    this.jspSettingsChart = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 45);
    this.jspSeriesSettingsChart = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 120);

    this.jrbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    this.btnSettings = new JButton("Settings");
    this.buttonGroup = new ButtonGroup();

    JPanel jPanelTop = new JPanel();
    JPanel jPanelSettings = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    jPanelSettings.add(popupPanel);

    PainlessGridBag gblTop = new PainlessGridBag(jPanelTop, PGHelper.getPGConfig(), false);
    gblTop.row().cell(jrbPanel).cell(jPanelSettings).fillX();
    gblTop.done();

    this.jspSettingsChart.add(jPanelTop, JSplitPane.TOP);
    this.jspSettingsChart.add(new JPanel(), JSplitPane.BOTTOM);

    this.jspSeriesSettingsChart.add(jspSettingsChart, JSplitPane.RIGHT);

    Consumer<String> runAction = this::runAction;
    new TableSelectionHandler(this.tableSeries, DimensionValuesNames.VALUE.getColName(), runAction);

    this.jspSeriesSettingsChart.add(tableSeries.getJScrollPane(), JSplitPane.LEFT);

    DefaultTableCellRenderer analyzeTableCellRenderer = new AnalyzeDefaultTableCellRenderer();

    tableSeries.getJxTable().setDefaultRenderer(Object.class, analyzeTableCellRenderer);
    tableSeries.getJxTable().getColumnExt(0).setVisible(false);

    this.loadData();

    this.fillAlgorithm();

    this.viewRadioButton();

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    gbl.row().cellXYRemainder(jspSeriesSettingsChart).fillXY();
    gbl.done();
  }

  protected void runAction(String value) {
    log.info("Run action fired {}", value);

    if (hideSettings) jspSettingsChart.setDividerLocation(0);

    listAlgorithm.stream()
        .filter(f -> getAlgorithmType().equals(f.getType()))
        .forEach(algorithm -> {
          if (jrbListAlgorithm.stream()
              .anyMatch(jrb -> jrb.getText().equals(algorithm.getName()) & jrb.isSelected())) {
            log.info("Radio button for algorithm: {} loaded and selected", algorithm.getName());

            processAlgorithmWithHandling(algorithm, value);
            this.value = value;
          } else {
            log.warn("Algorithm {} in UI radio button has not selected", algorithm.getName());
          }
        });
  }

  protected void processAlgorithmWithHandling(TimeSeriesAlgorithm algorithm,
                                              String value) {
    try {
      processAlgorithm(algorithm, value);
    } catch (IllegalArgumentException | NegativeArraySizeException iae) {
      JOptionPane.showMessageDialog(this, "Too small dataset, try to select more data",
                                    algorithm.getName() + " Error", JOptionPane.ERROR_MESSAGE);
    } catch (Exception e) {
      DialogHelper.showErrorDialog(this, e.getMessage(), "General Connection Error", e);
    }
  }

  protected abstract void processAlgorithm(TimeSeriesAlgorithm algorithm,
                                           String value);

  protected abstract AlgorithmType getAlgorithmType();

  private void loadData() {
    for (Map.Entry<String, Color> entry : seriesColorMap.entrySet()) {
      tableSeries.getDefaultTableModel().addRow(new Object[]{entry.getValue(), entry.getKey()});
    }
  }

  static class AnalyzeDefaultTableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, 0);
      if (value instanceof Color) {
        Color color = (Color) value;
        c.setBackground(color);
      }
      return c;
    }
  }

  private void fillAlgorithm() {
    Map<String, String> matrixProfileParams = new HashMap<>();
    matrixProfileParams.put("Window", "10");

    MatrixProfileAlgorithm matrixProfileAlgorithm =
        new MatrixProfileAlgorithm("STAMP", matrixProfileParams, chartDataset);
    this.listAlgorithm.add(matrixProfileAlgorithm);

    Map<String, String> ARIMAParams = new HashMap<>();
    ARIMAParams.put("Steps", "10");
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

            // Fire processing of algorithm with saved value
            processAlgorithmWithHandling(algorithm, value);
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

    JFreeChart jFreeChart = ChartFactory.createTimeSeriesChart(algorithm.getName(), xAxisLabel, yAxisLabel, chartDataset,
                                                          isShowLegend, true, false);

    XYPlot plot = (XYPlot) jFreeChart.getPlot();
    plot.getRenderer().setSeriesPaint(0, getColor(value));
    plot.getRenderer().setSeriesPaint(1, getColor(value));
    plot.getRenderer().setSeriesStroke(1,
                                       new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                                                       1.0f, new float[]{10.0f, 6.0f}, 0.0f));

    ChartPanel chartPanel = new ChartPanel(jFreeChart, true, true, true, false, true);

    chartPanel.setPreferredSize(new Dimension(100, 200));
    chartPanel.setMaximumSize(new Dimension(100, 200));

    LaF.setBackgroundAndTextColorForChartPanel(CHART_PANEL, chartPanel);

    return chartPanel;
  }

  public void hideSettings() {
    hideSettings = true;
    jspSettingsChart.setDividerLocation(0);
  }

  private Color getColor(String value) {
    for (Map.Entry<String, Color> entry : seriesColorMap.entrySet()) {
      if (entry.getKey().equals(value)) {
        return entry.getValue();
      }
    }
    return null;
  }

  @Override
  public void loadDataToDetail(long begin,
                               long end) {
    this.begin = begin;
    this.end = end;

    log.info("Begin: {} End: {}", this.begin, this.end);
  }
}
