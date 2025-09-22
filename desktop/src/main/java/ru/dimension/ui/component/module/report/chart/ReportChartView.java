package ru.dimension.ui.component.module.report.chart;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.block.HistoryConfigBlock;
import ru.dimension.ui.component.panel.FunctionPanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.function.NormFunctionPanel;
import ru.dimension.ui.component.panel.function.TimeRangeFunctionPanel;
import ru.dimension.ui.component.panel.popup.ActionPanel;
import ru.dimension.ui.component.panel.popup.DescriptionPanel;
import ru.dimension.ui.component.panel.popup.FilterPanel;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
import ru.dimension.ui.helper.GUIHelper;

@Data
@Log4j2
public class ReportChartView {
  private final ReportChartModel reportChartModel;

  private Dimension dimension = new Dimension(100, 600);

  private int configDividerLocation = 32;
  private int chartDividerLocation = 250;

  private int lastHistoryConfigDividerLocation = 32;
  private int lastHistoryChartDividerLocation = 250;

  private FunctionPanel historyFunctionPanel;
  private TimeRangeFunctionPanel historyTimeRangeFunctionPanel;
  private NormFunctionPanel historyNormFunctionPanel;
  private HistoryRangePanel historyRangePanel;
  private LegendPanel historyLegendPanel;
  private FilterPanel historyFilterPanel;
  private ActionPanel historyActionPanel;
  private DescriptionPanel historyDescriptionPanel;

  private HistoryConfigBlock historyConfigBlock;

  @Getter
  private JPanel historyChartPanel;
  private JPanel historyDetailPanel;

  private JSplitPane historyChartDetailSplitPane;

  @Getter
  private JSplitPane historyConfigChartDetail;

  public ReportChartView(ReportChartModel reportChartModel) {
    this.reportChartModel = reportChartModel;

    historyTimeRangeFunctionPanel = new TimeRangeFunctionPanel();
    historyNormFunctionPanel = new NormFunctionPanel();
    historyFunctionPanel = new FunctionPanel(getLabel("Group: "),
                                             reportChartModel.getComponent(),
                                             reportChartModel.getKey(),
                                             reportChartModel.getChartKey().getCProfile(),
                                             historyTimeRangeFunctionPanel,
                                             historyNormFunctionPanel);
    historyRangePanel = new HistoryRangePanel(getLabel("Range: "));
    historyLegendPanel = new LegendPanel(getLabel("Legend: "));
    historyFilterPanel = new FilterPanel(reportChartModel.getComponent());
    historyActionPanel = new ActionPanel(reportChartModel.getComponent());

    historyDescriptionPanel = new DescriptionPanel(reportChartModel.getComponent(),
                                                   reportChartModel.getChartKey().getProfileTaskQueryKey(),
                                                   reportChartModel.getChartKey().getCProfile(),
                                                   reportChartModel.getDescription());

    historyConfigBlock = new HistoryConfigBlock(historyFunctionPanel,
                                                historyRangePanel,
                                                historyLegendPanel,
                                                historyFilterPanel,
                                                historyActionPanel,
                                                historyDescriptionPanel);

    historyChartPanel = new JPanel(new BorderLayout());
    historyDetailPanel = new JPanel(new BorderLayout());

    historyConfigChartDetail = createBaseSplitPane();
    historyChartDetailSplitPane = createChartDetailSplitPane();

    historyChartDetailSplitPane.setTopComponent(historyChartPanel);
    historyChartDetailSplitPane.setBottomComponent(historyDetailPanel);

    historyConfigChartDetail.setTopComponent(historyConfigBlock);
    historyConfigChartDetail.setBottomComponent(historyChartDetailSplitPane);

    historyConfigChartDetail.setDividerLocation(lastHistoryConfigDividerLocation);
    historyChartDetailSplitPane.setDividerLocation(lastHistoryChartDividerLocation);
  }

  public void setDimension(Dimension dimension) {
    this.dimension = dimension;
    historyConfigChartDetail.setPreferredSize(dimension);
    historyConfigChartDetail.setMaximumSize(dimension);
    historyConfigChartDetail.revalidate();
    historyConfigChartDetail.repaint();
  }

  private JLabel getLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    return label;
  }

  private JSplitPane createBaseSplitPane() {
    JSplitPane splitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, configDividerLocation);
    splitPane.setDividerLocation(configDividerLocation);
    splitPane.setResizeWeight(0.5);
    splitPane.setPreferredSize(dimension);
    splitPane.setMaximumSize(dimension);
    return splitPane;
  }

  private JSplitPane createChartDetailSplitPane() {
    JSplitPane splitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, chartDividerLocation);
    splitPane.setDividerLocation(chartDividerLocation);
    splitPane.setResizeWeight(0.5);
    return splitPane;
  }

  public void setHistoryDetailVisible(boolean visible) {
    if (visible) {
      historyChartDetailSplitPane.getBottomComponent().setVisible(true);
      historyChartDetailSplitPane.setDividerLocation(lastHistoryChartDividerLocation);
    } else {
      historyChartDetailSplitPane.getBottomComponent().setVisible(false);
      historyChartDetailSplitPane.setDividerLocation(1.0);
    }

    historyChartDetailSplitPane.revalidate();
    historyChartDetailSplitPane.repaint();
    historyConfigChartDetail.revalidate();
    historyConfigChartDetail.repaint();
  }
}