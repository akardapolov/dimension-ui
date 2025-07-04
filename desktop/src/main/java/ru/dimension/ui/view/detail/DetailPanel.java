package ru.dimension.ui.view.detail;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.ui.component.model.DetailTabType;
import ru.dimension.ui.config.prototype.detail.WorkspaceDetailModule;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.view.analyze.DetailAction;
import ru.dimension.ui.view.detail.pivot.MainPivotPanel;
import ru.dimension.ui.view.detail.raw.RawDataPanel;
import ru.dimension.ui.view.detail.top.MainTopPanel;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.router.event.EventListener;

@Log4j2
public class DetailPanel extends JPanel implements IDetailPanel, DetailAction {

  private static final String LOADING_MESSAGE = "Loading, please wait...";
  private static final Color DEFAULT_SERIES_COLOR = new Color(255, 93, 93);

  private JPanel mainPanel;

  private final QueryInfo queryInfo;
  private final TableInfo tableInfo;

  private final ProcessType processType;

  private final Metric metric;
  private final CProfile cProfile;
  private ChartType chartType;

  private final ExecutorService executorService;

  private final WorkspaceQueryComponent workspaceQueryComponent;

  @Inject
  @Named("eventListener")
  EventListener eventListener;

  private final Map<String, Color> seriesColorMap;

  private final SeriesType seriesType;

  public DetailPanel(WorkspaceQueryComponent workspaceQueryComponent,
                     QueryInfo queryInfo,
                     TableInfo tableInfo,
                     Metric metric,
                     Map<String, Color> seriesColorMap,
                     ProcessType processType,
                     SeriesType seriesType) {
    this.workspaceQueryComponent = workspaceQueryComponent;
    this.workspaceQueryComponent.initDetail(new WorkspaceDetailModule(this)).inject(this);

    this.queryInfo = queryInfo;
    this.tableInfo = tableInfo;
    this.metric = metric;
    this.cProfile = metric.getYAxis();
    this.chartType = metric.getChartType();
    this.seriesColorMap = seriesColorMap;
    this.processType = processType;
    this.seriesType = seriesType;

    this.executorService = Executors.newSingleThreadExecutor();

    this.setLayout(new BorderLayout());
    this.setBorder(new EtchedBorder());

    this.mainPanel = new JPanel();
    this.mainPanel.setLayout(new GridLayout(1, 1, 3, 3));

    this.add(this.mainPanel, BorderLayout.CENTER);
  }

  @Override
  public void cleanMainPanel() {
    executorService.submit(this::clearPanel);
  }

  @Override
  public void loadDataToDetail(long begin,
                               long end) {
    executorService.submit(() -> {

      showLoadingIndicator();

      try {
        JTabbedPane tabbedPane = new JTabbedPane();

        if (!MetricFunction.COUNT.equals(metric.getMetricFunction())) {
          setSeriesColor();
        }

        if (Objects.nonNull(metric.getYAxis())) {
          addTop(tabbedPane, begin, end);
        }

        if (MetricFunction.COUNT.equals(metric.getMetricFunction())) {
          if (Objects.isNull(seriesType) || SeriesType.COMMON.equals(seriesType)) {
            addPivot(tabbedPane, begin, end);
          }
        }

        addRaw(tabbedPane, begin, end);

        clearPanel();

        mainPanel.add(tabbedPane);

      } catch (Exception exception) {
        log.catching(exception);
      }

    });
  }

  private void addTop(JTabbedPane tabbedPane,
                      long begin,
                      long end) {
    MainTopPanel mainTopPanel = new MainTopPanel(workspaceQueryComponent, tableInfo, metric, cProfile, begin, end, seriesColorMap, seriesType);
    tabbedPane.add(DetailTabType.TOP.getName(), mainTopPanel);
  }

  private void addPivot(JTabbedPane tabbedPane,
                        long begin,
                        long end) {
    MainPivotPanel mainPivotPanel = new MainPivotPanel(workspaceQueryComponent, tableInfo, metric, cProfile, begin, end, seriesColorMap);
    tabbedPane.add(DetailTabType.PIVOT.getName(), mainPivotPanel);
  }

  private void addRaw(JTabbedPane tabbedPane,
                      long begin,
                      long end) {
    boolean useFetchSize = ProcessType.HISTORY.equals(processType);
    RawDataPanel rawDataPanel = new RawDataPanel(workspaceQueryComponent, tableInfo, cProfile, begin, end, useFetchSize);
    tabbedPane.add(DetailTabType.RAW.getName(), rawDataPanel);
  }

  private void setSeriesColor() {
    if (Objects.nonNull(metric.getYAxis())) {
      seriesColorMap.put(metric.getYAxis().getColName(), DEFAULT_SERIES_COLOR);
    }
  }

  private void showLoadingIndicator() {
    mainPanel.removeAll();
    mainPanel.add(ProgressBarHelper.createProgressBar(LOADING_MESSAGE));
    mainPanel.repaint();
    mainPanel.revalidate();
  }

  private void clearPanel() {
    mainPanel.removeAll();
    mainPanel.repaint();
    mainPanel.revalidate();
  }
}
