package ru.dimension.ui.view.structure.workspace.handler;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.exception.EmptySearchException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.view.chart.DetailChart;
import ru.dimension.ui.view.chart.search.SearchSCP;
import ru.dimension.ui.cache.AppCache;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.detail.DetailPanel;
import ru.dimension.ui.view.panel.QuerySearchButtonPanel;

@Log4j2
public class QuerySearchHandler implements ActionListener {

  private final QuerySearchButtonPanel querySearchButtonPanel;

  private LocalDateTime begin;
  private LocalDateTime end;

  private JSplitPane chartPanelSearch;

  @Inject
  @Named("eventListener")
  EventListener eventListener;

  @Inject
  @Named("appCache")
  AppCache appCache;
  private final ProfileTaskQueryKey profileTaskQueryKey;
  private final QueryInfo queryInfo;
  private final TableInfo tableInfo;
  private final ChartInfo chartInfo;
  private final WorkspaceQueryComponent workspaceQueryComponent;

  private final ScheduledExecutorService executorService;

  public QuerySearchHandler(JSplitPane chartPanelSearch,
                            QuerySearchButtonPanel querySearchButtonPanel,
                            ProfileTaskQueryKey profileTaskQueryKey,
                            QueryInfo queryInfo,
                            TableInfo tableInfo,
                            ChartInfo chartInfo,
                            WorkspaceQueryComponent workspaceQueryComponent) {
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.queryInfo = queryInfo;
    this.tableInfo = tableInfo;
    this.chartInfo = chartInfo;
    this.workspaceQueryComponent = workspaceQueryComponent;

    this.begin = LocalDateTime.now();
    this.end = LocalDateTime.now();

    this.executorService = Executors.newScheduledThreadPool(1);

    this.chartPanelSearch = chartPanelSearch;

    GUIHelper.addToJSplitPane(this.chartPanelSearch, querySearchButtonPanel, JSplitPane.TOP, 40);
    GUIHelper.addToJSplitPane(this.chartPanelSearch, new JPanel(), JSplitPane.BOTTOM, 40);

    this.querySearchButtonPanel = querySearchButtonPanel;
    this.querySearchButtonPanel.getJButtonSearch().addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    log.info("Go button for ad-hoc search has been pressed..");

    String searchString = querySearchButtonPanel.getJTextFieldSearch().getText();

    if (searchString.length() < 3) {
      throw new EmptySearchException("The search string must not be empty or have a length greater than 3");
    }

    executorService.submit(() -> {
      GUIHelper.addToJSplitPane(chartPanelSearch, ProgressBarHelper.createProgressBar("Loading, please wait..."), JSplitPane.BOTTOM, 40);

      JSplitPane chartRawPanelSearch = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 200);

      CategoryTableXYDatasetRealTime chartDataset = new CategoryTableXYDatasetRealTime();

      SearchSCP stackChartPanel = new SearchSCP(workspaceQueryComponent, chartDataset,
                                                profileTaskQueryKey, queryInfo, tableInfo, chartInfo, ProcessType.SEARCH, searchString);
      stackChartPanel.initialize();
      stackChartPanel.loadData();

      DetailPanel detailPanel = getDetailPanel(stackChartPanel.getSeriesColorMap(), stackChartPanel, ProcessType.SEARCH);

      chartRawPanelSearch.add(stackChartPanel, JSplitPane.TOP);
      chartRawPanelSearch.add(detailPanel, JSplitPane.BOTTOM);
      chartRawPanelSearch.setDividerLocation(210);

      GUIHelper.addToJSplitPane(chartPanelSearch, chartRawPanelSearch, JSplitPane.BOTTOM, 40);
    });

  }

  protected DetailPanel getDetailPanel(Map<String, Color> seriesColorMap,
                                       DetailChart dynamicChart,
                                       ProcessType processType) {
    Metric metric = new Metric(-1, "None", false, null, null, null,
                               MetricFunction.NONE, ChartType.NONE, new ArrayList<>());
    DetailPanel detailPanel =
        new DetailPanel(workspaceQueryComponent, queryInfo, tableInfo, metric, seriesColorMap, processType, SeriesType.COMMON);

    dynamicChart.addChartListenerReleaseMouse(detailPanel);

    return detailPanel;
  }
}