package ru.dimension.ui.view.detail;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.chart.ChartType;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.component.module.analyze.DetailAction;
import ru.dimension.ui.view.detail.pivot.ReportPivotPanel;
import ru.dimension.ui.view.detail.raw.RawDataAdHocPanel;
import ru.dimension.ui.view.detail.top.ReportTopPanel;
import ru.dimension.ui.model.view.SeriesType;

@Log4j2
public class DetailAdHocPanel extends JPanel implements IDetailPanel, DetailAction {

  private JPanel mainPanel;
  private final TableInfo tableInfo;
  private final ChartType chartType;
  private final SeriesType seriesType;

  private final Metric metric;
  private final ExecutorService executorService;

  private final DStore dStore;

  private final Map<String, Color> seriesColorMap;

  public DetailAdHocPanel(DStore dStore,
                          TableInfo tableInfo,
                          Metric metric,
                          Map<String, Color> seriesColorMap,
                          ChartType chartType,
                          SeriesType seriesType) {

    this.dStore = dStore;
    this.tableInfo = tableInfo;
    this.metric = metric;
    this.seriesColorMap = seriesColorMap;
    this.chartType = chartType;
    this.seriesType = seriesType;

    this.executorService = Executors.newSingleThreadExecutor();

    this.setLayout(new BorderLayout());

    this.mainPanel = new JPanel();
    this.mainPanel.setLayout(new GridLayout(1, 1, 3, 3));

    this.add(this.mainPanel, BorderLayout.CENTER);
  }

  @Override
  public void loadDataToDetail(long begin,
                               long end) {
    executorService.submit(() -> {

      mainPanel.removeAll();
      mainPanel.add(ProgressBarHelper.createProgressBar("Loading, please wait..."));
      mainPanel.repaint();
      mainPanel.revalidate();

      try {
        JTabbedPane mainJTabbedPane = new JTabbedPane();

        if (ChartType.STACKED.equals(chartType)) {
          ReportTopPanel ganttDataPanel = new ReportTopPanel(dStore, tableInfo, metric.getYAxis(), seriesType, begin, end, seriesColorMap);
          mainJTabbedPane.add("Top", ganttDataPanel);
          ReportPivotPanel mainPivotPanel = new ReportPivotPanel(dStore, tableInfo, metric.getYAxis(), seriesType, begin, end, seriesColorMap);
          mainJTabbedPane.add("Pivot", mainPivotPanel);
        }

        RawDataAdHocPanel rawDataPanel = new RawDataAdHocPanel(dStore, tableInfo, metric.getYAxis(), begin, end, true);
        mainJTabbedPane.add("Raw", rawDataPanel);

        mainPanel.removeAll();
        mainPanel.repaint();
        mainPanel.revalidate();

        mainPanel.add(mainJTabbedPane);

      } catch (Exception exception) {
        log.catching(exception);
      }

    });
  }

  @Override
  public void cleanMainPanel() {
    executorService.submit(() -> {
      mainPanel.removeAll();
      mainPanel.repaint();
      mainPanel.revalidate();
    });
  }
}
