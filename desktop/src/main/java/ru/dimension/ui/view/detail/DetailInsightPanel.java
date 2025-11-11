package ru.dimension.ui.view.detail;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPane;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.history.HistorySCP;
import ru.dimension.ui.component.module.analyze.DetailAction;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.column.DimensionValuesNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.view.detail.insight.InsightPanel;

@Log4j2
public class DetailInsightPanel extends JPanel implements IDetailPanel, DetailAction {
  private static final Dimension dimension = new Dimension(100, 200);

  private JPanel mainPanel;
  private final ExecutorService executorService;

  private final ChartKey chartKey;
  private final QueryInfo queryInfo;
  private final ChartInfo chartInfo;
  private final TableInfo tableInfo;
  private final Metric metric;
  private final CProfile cProfile;
  private final Map<String, Color> seriesColorMap;
  private final SeriesType seriesType;
  private final DStore dStore;
  private final Map<CProfile, LinkedHashSet<String>> topMapSelected;

  public DetailInsightPanel(ChartKey chartKey,
                            QueryInfo queryInfo,
                            ChartInfo chartInfo,
                            TableInfo tableInfo,
                            Metric metric,
                            Map<String, Color> seriesColorMap,
                            SeriesType seriesType,
                            DStore dStore,
                            Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    this.chartKey = chartKey;
    this.queryInfo = queryInfo;
    this.chartInfo = chartInfo;
    this.tableInfo = tableInfo;
    this.metric = metric;
    this.cProfile = metric.getYAxis();
    this.seriesColorMap = seriesColorMap;
    this.seriesType = seriesType;
    this.dStore = dStore;
    this.topMapSelected = topMapSelected;

    this.executorService = Executors.newSingleThreadExecutor();

    initializeUI();
  }

  private void initializeUI() {
    setLayout(new BorderLayout());
    setBorder(new EtchedBorder());

    mainPanel = new JPanel();
    mainPanel.setLayout(new GridLayout(1, 1, 3, 3));

    add(mainPanel, BorderLayout.CENTER);
  }

  @Override
  public void cleanMainPanel() {
    executorService.submit(this::clearPanel);
  }

  private void clearPanel() {
    mainPanel.removeAll();
    mainPanel.repaint();
    mainPanel.revalidate();
  }

  @Override
  public void loadDataToDetail(long begin, long end) {
    executorService.submit(() -> loadInsight(begin, end));
  }

  private void loadInsight(long begin, long end) {
    mainPanel.removeAll();
    mainPanel.add(ProgressBarHelper.createProgressBar("Loading, please wait..."));
    mainPanel.repaint();
    mainPanel.revalidate();

    try {
      Map<CProfile, LinkedHashSet<String>> actualTopMapSelected = resolveTopMapSelected();

      ChartInfo chartInfoCopy = chartInfo.copy();
      chartInfoCopy.setCustomBegin(begin);
      chartInfoCopy.setCustomEnd(end);

      ChartConfig config = buildChartConfig(chartInfoCopy);
      ProfileTaskQueryKey key = config.getChartKey().getProfileTaskQueryKey();

      SCP chart = new HistorySCP(dStore, config, key, actualTopMapSelected, true);
      chart.loadSeriesColor(metric, seriesColorMap);
      chart.initialize();

      InsightPanel insightPanel = new InsightPanel(chart, dimension);

      mainPanel.removeAll();
      mainPanel.add(insightPanel);
      mainPanel.revalidate();
      mainPanel.repaint();

    } catch (Exception ex) {
      log.catching(ex);
      mainPanel.removeAll();
      mainPanel.add(new JLabel("Error while loading insights: " + ex.getMessage()));
      mainPanel.revalidate();
      mainPanel.repaint();
    }
  }

  private Map<CProfile, LinkedHashSet<String>> resolveTopMapSelected() {
    Map<CProfile, LinkedHashSet<String>> actualTopMapSelected = topMapSelected;
    if (SeriesType.CUSTOM.equals(seriesType)) {
      if (actualTopMapSelected == null) {
        actualTopMapSelected = new HashMap<>();
      }
      actualTopMapSelected.computeIfAbsent(cProfile, k -> new LinkedHashSet<>(seriesColorMap.keySet()));
    }
    return actualTopMapSelected;
  }

  private JXTaskPane createTaskPane(String title, JComponent content) {
    JXTaskPane pane = new JXTaskPane();

    ((JComponent) pane.getContentPane()).setBorder(BorderFactory.createEmptyBorder());

    pane.setTitle(title);
    pane.setAnimated(false);
    pane.add(content);
    return pane;
  }

  private ChartConfig buildChartConfig(ChartInfo chartInfo) {
    ChartConfig config = new ChartConfig();
    config.setTitle("");
    config.setChartKey(chartKey);
    config.setXAxisLabel(metric.getYAxis().getColName());
    config.setYAxisLabel("Value");
    config.setMetric(metric);
    config.setChartInfo(chartInfo);
    config.setQueryInfo(queryInfo);
    return config;
  }

  private String[] getTableColumnNames() {
    return new String[]{
        DimensionValuesNames.COLOR.getColName(),
        DimensionValuesNames.VALUE.getColName()
    };
  }

  private static JComponent createTextSeparator(String text) {
    return new JPanel(new BorderLayout()) {
      {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JLabel label = new JLabel(text);
        label.setForeground(new Color(0x0A8D0A));
        label.setFont(scaleFontSize(label.getFont()));
        add(label, BorderLayout.WEST);

        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 20));
        add(separator, BorderLayout.EAST);
      }
    };
  }

  private static Font scaleFontSize(Font currentFont) {
    float newSize = currentFont.getSize() * 1.2f;
    return currentFont.deriveFont(newSize);
  }
}