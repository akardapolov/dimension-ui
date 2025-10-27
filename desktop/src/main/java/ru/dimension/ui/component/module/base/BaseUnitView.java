package ru.dimension.ui.component.module.base;

import java.awt.*;
import java.util.Optional;
import javax.swing.*;
import lombok.Getter;
import ru.dimension.ui.component.module.api.UnitView;
import ru.dimension.ui.helper.GUIHelper;

public class BaseUnitView implements UnitView {

  public enum LayoutMode {
    CONFIG_CHART_DETAIL, // on top: config; on bottom: chart+detail
    CONFIG_CHART_ONLY    // on top: config; on bottom: chart (without detail)
  }

  private static final Dimension DEFAULT_DIMENSION = new Dimension(100, 600);

  @Getter
  private final JPanel configPanel = new JPanel(new BorderLayout());
  @Getter
  private final JPanel chartPanel = new JPanel(new BorderLayout());
  private final JPanel detailPanel = new JPanel(new BorderLayout());

  @Getter
  private final JSplitPane chartDetailSplitPane;
  @Getter
  private final JSplitPane configChartSplitPane;
  private final LayoutMode layoutMode;

  public BaseUnitView(LayoutMode layoutMode) {
    this.layoutMode = layoutMode;

    configChartSplitPane = createBaseSplitPane(32);
    chartDetailSplitPane = createChartDetailSplitPane(250);

    if (layoutMode == LayoutMode.CONFIG_CHART_DETAIL) {
      chartDetailSplitPane.setTopComponent(chartPanel);
      chartDetailSplitPane.setBottomComponent(detailPanel);
      configChartSplitPane.setTopComponent(configPanel);
      configChartSplitPane.setBottomComponent(chartDetailSplitPane);
    } else {
      configChartSplitPane.setTopComponent(configPanel);
      configChartSplitPane.setBottomComponent(chartPanel);
    }
  }

  private JSplitPane createBaseSplitPane(int divider) {
    JSplitPane splitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, divider);
    splitPane.setDividerLocation(divider);
    splitPane.setResizeWeight(0.5);
    splitPane.setPreferredSize(DEFAULT_DIMENSION);
    splitPane.setMaximumSize(DEFAULT_DIMENSION);
    return splitPane;
  }

  private JSplitPane createChartDetailSplitPane(int divider) {
    JSplitPane splitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, divider);
    splitPane.setDividerLocation(divider);
    splitPane.setResizeWeight(0.5);
    return splitPane;
  }

  @Override
  public Component getRootComponent() {
    return configChartSplitPane;
  }

  @Override
  public Optional<JPanel> getDetailPanel() {
    return layoutMode == LayoutMode.CONFIG_CHART_DETAIL ? Optional.of(detailPanel) : Optional.empty();
  }

  @Override
  public void setDetailVisible(boolean visible) {
    if (layoutMode == LayoutMode.CONFIG_CHART_DETAIL) {
      chartDetailSplitPane.getBottomComponent().setVisible(visible);
      chartDetailSplitPane.resetToPreferredSizes();
    }
  }
}