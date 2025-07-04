package ru.dimension.ui.view.analyze.timeseries;

import java.awt.Color;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import lombok.extern.log4j.Log4j2;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.column.DimensionValuesNames;
import ru.dimension.ui.view.chart.stacked.StackChartPanelCommon;

@Log4j2
public class AnalyzePanel extends JPanel {

  private JTabbedPane jtpAnalyze;

  private AnalyzeAnomalyPanel anomalyPanel;
  private AnalyzeForecastPanel forecastPanel;

  private Map<String, Color> seriesColorMap;
  private StackChartPanelCommon stackChartPanel;

  public AnalyzePanel(StackChartPanelCommon stackChartPanel) {
    this.jtpAnalyze = new JTabbedPane();
    this.seriesColorMap = stackChartPanel.getSeriesColorMap();

    //Data tab
    this.stackChartPanel = stackChartPanel;

    //Anomaly tab
    anomalyPanel = new AnalyzeAnomalyPanel(GUIHelper.getJXTableCase(6, getTableColumnNames()),
                                           seriesColorMap, stackChartPanel.getChartDataset());

    //Forecast tab
    forecastPanel = new AnalyzeForecastPanel(GUIHelper.getJXTableCase(6, getTableColumnNames()),
                                             seriesColorMap, stackChartPanel.getChartDataset());

    this.stackChartPanel.addChartListenerReleaseMouse(anomalyPanel);
    this.stackChartPanel.addChartListenerReleaseMouse(forecastPanel);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    this.jtpAnalyze.add(this.stackChartPanel, "Data");
    this.jtpAnalyze.add(this.anomalyPanel, "Anomaly");
    this.jtpAnalyze.add(this.forecastPanel, "Forecast");

    gbl.row().cellXYRemainder(jtpAnalyze).fillXY();
    gbl.done();
  }

  private String[] getTableColumnNames() {
    return new String[]{
        DimensionValuesNames.COLOR.getColName(),
        DimensionValuesNames.VALUE.getColName()
    };
  }
}
