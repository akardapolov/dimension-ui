package ru.dimension.ui.view.analyze.panel;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.view.analyze.AnalyzeComponent;
import ru.dimension.ui.view.analyze.CustomAction;
import ru.dimension.ui.view.analyze.router.Message;
import ru.dimension.ui.view.analyze.router.MessageRouter;
import ru.dimension.ui.view.analyze.router.MessageRouter.Action;
import ru.dimension.ui.view.analyze.router.MessageRouter.Destination;
import ru.dimension.ui.model.SourceConfig;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.AnalyzeType;
import ru.dimension.ui.model.view.SeriesType;

@Log4j2
public class AnalyzeAdHocPanel extends JPanel implements IDetailPanel, CustomAction {

  private final MessageRouter router;

  private final Metric metric;
  private final Map<String, Color> seriesColorMap;
  private final SeriesType seriesType;

  @Getter
  @Setter
  private Entry<CProfile, List<String>> filter;

  private final DStore dStore;

  public AnalyzeAdHocPanel(QueryInfo queryInfo,
                           TableInfo tableInfo,
                           ChartInfo chartInfo,
                           SourceConfig sourceConfig,
                           Metric metric,
                           Map<String, Color> seriesColorMap,
                           SeriesType seriesType,
                           DStore dStore) {

    this.router = new MessageRouter();

    this.metric = metric;
    this.seriesColorMap = seriesColorMap;
    this.seriesType = seriesType;

    this.dStore = dStore;

    AnalyzeComponent analyzeComponent =
        new AnalyzeComponent(null, null, dStore, router,
                             queryInfo, tableInfo, chartInfo, null, sourceConfig, AnalyzeType.AD_HOC,
                             metric, seriesColorMap, seriesType);

    PGHelper.cellXYRemainder(this, analyzeComponent.getMainSplitPane(), false);
  }

  @Override
  public void loadDataToDetail(long begin,
                               long end) {
    sendLoadTopMessage(begin, end);
    sendRemoveAllChartsMessage();
    sendClearAllCheckboxesMessage();
    sendSetCheckboxMessage(metric.getYAxis());
    sendSetBeginEndMessage(begin, end);

    if (filter != null) {
      sendAddChartFilterMessage(metric, filter.getKey(), filter.getValue(), seriesColorMap);
    } else {
      sendAddChartMessage(metric.getYAxis(), seriesColorMap);
    }
  }

  @Override
  public void setCustomSeriesFilter(CProfile cProfileFilter,
                                    List<String> filter) {
    sendClearTopMessage();
    sendRemoveAllChartsMessage();
    sendClearAllCheckboxesMessage();
    sendSetCheckboxMessage(metric.getYAxis());
    sendAddChartFilterMessage(metric, cProfileFilter, filter, seriesColorMap);
    sendSetFilterMessage(cProfileFilter, filter);

    this.filter = Map.entry(cProfileFilter, filter);
  }

  @Override
  public void setCustomSeriesFilter(CProfile cProfileFilter,
                                    List<String> filter,
                                    Map<String, Color> seriesColorMap) {

  }

  @Override
  public void setBeginEnd(long begin,
                          long end) {
    sendSetBeginEndMessage(begin, end);
  }

  private void sendLoadTopMessage(long begin,
                                  long end) {
    router.sendMessage(Message.builder()
                           .destination(Destination.TOP)
                           .action(Action.LOAD_TOP)
                           .parameter("dStore", dStore)
                           .parameter("begin", begin)
                           .parameter("end", end)
                           .parameter("seriesType", seriesType)
                           .build());
  }

  private void sendClearTopMessage() {
    router.sendMessage(Message.builder()
                           .destination(Destination.TOP)
                           .action(Action.CLEAR_TOP)
                           .build());
  }

  private void sendRemoveAllChartsMessage() {
    router.sendMessage(Message.builder()
                           .destination(Destination.CHART_LIST)
                           .action(Action.REMOVE_ALL_CHART)
                           .build());
  }

  private void sendClearAllCheckboxesMessage() {
    router.sendMessage(Message.builder()
                           .destination(Destination.DIMENSION)
                           .action(Action.CLEAR_ALL_CHECKBOX_CHART)
                           .build());
  }

  private void sendSetCheckboxMessage(CProfile cProfile) {
    router.sendMessage(Message.builder()
                           .destination(Destination.DIMENSION)
                           .action(Action.SET_CHECKBOX_CHART)
                           .parameter("cProfile", cProfile)
                           .build());
  }

  private void sendSetBeginEndMessage(long begin,
                                      long end) {
    router.sendMessage(Message.builder()
                           .destination(Destination.CHART_LIST)
                           .action(Action.SET_BEGIN_END)
                           .parameter("begin", begin)
                           .parameter("end", end)
                           .build());
  }

  private void sendAddChartMessage(CProfile cProfile,
                                   Map<String, Color> seriesColorMap) {
    router.sendMessage(Message.builder()
                           .destination(Destination.CHART_LIST)
                           .action(Action.ADD_CHART)
                           .parameter("cProfile", cProfile)
                           .parameter("seriesColorMap", seriesColorMap)
                           .build());
  }

  private void sendAddChartFilterMessage(Metric metric,
                                         CProfile cProfile,
                                         List<String> filter,
                                         Map<String, Color> seriesColorMap) {
    router.sendMessage(Message.builder()
                           .destination(Destination.CHART_LIST)
                           .action(Action.ADD_CHART_FILTER)
                           .parameter("metric", metric)
                           .parameter("cProfileFilter", cProfile)
                           .parameter("filter", filter)
                           .parameter("seriesColorMap", seriesColorMap)
                           .build());
  }

  private void sendSetFilterMessage(CProfile cProfile,
                                    List<String> filter) {
    router.sendMessage(Message.builder()
                           .destination(Destination.DIMENSION)
                           .action(Action.SET_FILTER)
                           .parameter("cProfile", cProfile)
                           .parameter("filter", filter)
                           .build());
  }
}
