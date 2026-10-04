package ru.dimension.ui.component.chart;

import static ru.dimension.ui.laf.LafColorGroup.CHART_HISTORY_DAY_FONT;
import static ru.dimension.ui.laf.LafColorGroup.CHART_HISTORY_HOUR_FONT;
import static ru.dimension.ui.laf.LafColorGroup.CHART_HISTORY_MONTH_FONT;
import static ru.dimension.ui.laf.LafColorGroup.CHART_HISTORY_YEAR_FONT;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.SelectionWheelHandler;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.PeriodAxis;
import org.jfree.chart.axis.PeriodAxisLabelInfo;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.panel.selectionhandler.EntitySelectionManager;
import org.jfree.chart.panel.selectionhandler.MouseClickSelectionHandler;
import org.jfree.chart.panel.selectionhandler.RectangularHeightRegionSelectionHandler;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.item.IRSUtilities;
import org.jfree.chart.renderer.xy.SmoothedStackedXYAreaRenderer;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer3;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.util.IDetailPanel;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.extension.DatasetIterator;
import org.jfree.data.extension.DatasetSelectionExtension;
import org.jfree.data.extension.impl.DatasetExtensionManager;
import org.jfree.data.extension.impl.XYCursor;
import org.jfree.data.extension.impl.XYDatasetSelectionExtension;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.SelectionChangeEvent;
import org.jfree.data.general.SelectionChangeListener;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Month;
import org.jfree.data.time.Year;
import ru.dimension.ui.helper.ColorHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.config.ChartUISettings;
import ru.dimension.ui.model.data.CategoryTableXYDatasetRealTime;

@Log4j2
public class StackedChart implements SelectionChangeListener<XYCursor>, DynamicChart, DetailChart {

  private final ChartPanel chartPanel;
  private final JFreeChart jFreeChart;
  private final XYPlot xyPlot;
  private final DateAxis dateAxis;

  private final CategoryTableXYDatasetRealTime chartDataset;
  private StackedXYAreaRenderer3 stackedXYAreaRenderer3;

  @Getter
  @Setter
  private LegendTitle legendTitle;
  @Getter
  @Setter
  private int legendFontSize;

  @Setter
  private boolean selectionWheelEnabled = true;

  private static final int DEFAULT_LEGEND_FIXED_WIDTH = 150;

  private int legendFixedWidth = DEFAULT_LEGEND_FIXED_WIDTH;

  private static final double LEGEND_VERTICAL_TRIM = 12.0;

  private static final double LEGEND_ITEM_HEIGHT_MARGIN = 10.0;

  private static final int LEGEND_SCROLL_ITEMS_PER_NOTCH = 3;

  private static final int LEGEND_DIMMED_FILL_ALPHA = 100;
  private static final int LEGEND_DIMMED_LABEL_ALPHA = 120;

  private static final int PLOT_DIMMED_ALPHA = 90;

  private boolean legendFixedSize = false;

  private LegendItemSource fixedLegendItemSource;

  private LegendItemSource[] defaultLegendSources;

  private ChartMouseListener legendHoverListener;
  private MouseWheelListener legendScrollWheelListener;

  private int legendScrollOffset = 0;

  private int legendTotalCount = 0;
  private int visibleLegendCount = 0;

  private final Map<String, Color> internalSeriesColor = new ConcurrentHashMap<>();
  private final Map<String, Color> externalSeriesColor = new ConcurrentHashMap<>();

  private final Map<String, Integer> seriesIndexMap = new ConcurrentHashMap<>();

  private AtomicInteger counter;
  private RectangularHeightRegionSelectionHandler selectionHandler;
  private DatasetExtensionManager dExManager;
  private EntitySelectionManager selectionManager;
  private DatasetSelectionExtension<XYCursor> datasetExtension;

  private final ColorHelper colorHelper;

  private Double selDomainStart = null;
  private Double selDomainEnd = null;

  private volatile String hoveredSeriesKey = null;

  public StackedChart(ChartPanel chartPanel,
                      ColorHelper colorHelper) {
    this.chartPanel = chartPanel;
    this.jFreeChart = this.chartPanel.getChart();
    this.xyPlot = (XYPlot) this.jFreeChart.getPlot();
    this.dateAxis = (DateAxis) this.xyPlot.getDomainAxis();
    this.chartDataset = (CategoryTableXYDatasetRealTime) this.xyPlot.getDataset();

    this.colorHelper = colorHelper;
  }

  @Override
  public void initialize() {
    this.counter = new AtomicInteger(0);

    this.datasetExtension = new XYDatasetSelectionExtension(this.chartDataset);
    datasetExtension.addChangeListener(this);

    this.setStackedXYAreaRenderer3(datasetExtension);

    this.xyPlot.setRenderer(this.getStackedXYAreaRenderer3());
    this.xyPlot.getRangeAxis().setLowerBound(0.0);
    this.xyPlot.getRangeAxis().setAutoRange(true);

    this.dateAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));

    this.selectionHandler = new RectangularHeightRegionSelectionHandler();
    this.selectionHandler.setDataset(this.chartDataset);
    this.chartPanel.addMouseHandler(selectionHandler);
    this.chartPanel.addMouseHandler(new MouseClickSelectionHandler());
    this.chartPanel.removeMouseHandler(this.chartPanel.getZoomHandler());

    this.dExManager = new DatasetExtensionManager();
    this.dExManager.registerDatasetExtension(datasetExtension);

    this.selectionManager = new EntitySelectionManager(this.chartPanel,
                                                       new Dataset[]{this.chartDataset},
                                                       dExManager);
    this.chartPanel.setSelectionManager(this.selectionManager);
    this.chartPanel.addComponentListener(new SelectionRefitListener());

    if (selectionWheelEnabled) {
      new SelectionWheelHandler(this.chartPanel);
    }

    this.setLegendTitle();
    this.jFreeChart.addSubtitle(this.legendTitle);
    this.chartPanel.setRangeZoomable(false);

    this.applyLegendFixedSize();
  }

  @Override
  public ChartPanel getChartPanel() {
    return this.chartPanel;
  }

  @Override
  public Map<String, Color> getSeriesColorMap() {
    return internalSeriesColor;
  }

  @Override
  public void setChartTitle(String titleText) {
    this.jFreeChart.setTitle(new TextTitle(titleText, new Font("SansSerif", Font.BOLD, 18)));
  }

  @Override
  public void loadSeriesColorInternal(String colorProfileName, String seriesName) {
    if (!this.internalSeriesColor.containsKey(seriesName)) {
      try {
        int cnt = counter.getAndIncrement();

        Color color;
        if (this.externalSeriesColor.containsKey(seriesName)) {
          color = externalSeriesColor.get(seriesName);
        } else {
          color = colorHelper.getColor(colorProfileName, seriesName);
        }

        this.stackedXYAreaRenderer3.setSeriesPaint(cnt, color);
        this.chartDataset.saveSeriesValues(cnt, seriesName);
        this.internalSeriesColor.put(seriesName, color);
        this.seriesIndexMap.put(seriesName, cnt);
      } catch (Exception e) {
        log.catching(e);
      }
    }
  }

  public void loadExternalSeriesColor(String seriesName,
                                      Color color) {
    this.externalSeriesColor.put(seriesName, color);
  }

  public void clearSeriesColor() {
    this.externalSeriesColor.clear();
    this.internalSeriesColor.clear();
    this.seriesIndexMap.clear();
    this.hoveredSeriesKey = null;
    this.legendScrollOffset = 0;

    this.chartDataset.clear();
    this.stackedXYAreaRenderer3.clearSeriesPaints(true);
    this.stackedXYAreaRenderer3.clearSeriesStrokes(true);

    counter = new AtomicInteger(0);
  }

  public void refreshSeriesColors(String colorProfileName) {
    if (this.internalSeriesColor.isEmpty()) {
      return;
    }

    this.stackedXYAreaRenderer3.clearSeriesPaints(true);
    this.stackedXYAreaRenderer3.clearSeriesStrokes(true);

    AtomicInteger newCounter = new AtomicInteger(0);
    Map<String, Color> refreshedColors = new ConcurrentHashMap<>();
    Map<String, Integer> refreshedIndex = new ConcurrentHashMap<>();

    this.internalSeriesColor.keySet().forEach(seriesName -> {
      Color color;
      if (this.externalSeriesColor.containsKey(seriesName)) {
        color = this.externalSeriesColor.get(seriesName);
      } else {
        color = this.colorHelper.getColor(colorProfileName, seriesName);
      }

      int cnt = newCounter.getAndIncrement();
      this.stackedXYAreaRenderer3.setSeriesPaint(cnt, color);
      refreshedColors.put(seriesName, color);
      refreshedIndex.put(seriesName, cnt);
    });

    this.internalSeriesColor.clear();
    this.internalSeriesColor.putAll(refreshedColors);
    this.seriesIndexMap.clear();
    this.seriesIndexMap.putAll(refreshedIndex);
    this.counter = newCounter;

    this.chartPanel.repaint();
  }

  @Override
  public void setDateAxis(long begin,
                          long end) {
    PeriodAxis domainAxis = new PeriodAxis(" ");
    domainAxis.setTimeZone(TimeZone.getDefault());

    long difference = end - begin;

    long hours = TimeUnit.MILLISECONDS.toHours(difference);
    long days = TimeUnit.MILLISECONDS.toDays(difference);

    RectangleInsets rectangleInsetsDay = new RectangleInsets(2, 2, 2, 2);
    Font fontDay = new Font("SansSerif", Font.BOLD, 8);
    Color colorHour = LaF.getBackgroundColor(CHART_HISTORY_HOUR_FONT, LaF.getLafType());
    Color colorDay = LaF.getBackgroundColor(CHART_HISTORY_DAY_FONT, LaF.getLafType());
    Color colorMonth = LaF.getBackgroundColor(CHART_HISTORY_MONTH_FONT, LaF.getLafType());
    Color colorYear = LaF.getBackgroundColor(CHART_HISTORY_YEAR_FONT, LaF.getLafType());
    BasicStroke basicStrokeDay = new BasicStroke(0.0f);

    PeriodAxisLabelInfo labelInfoHourMin =
        new PeriodAxisLabelInfo(Hour.class,
                                new SimpleDateFormat("HH:mm"),
                                rectangleInsetsDay,
                                fontDay,
                                colorHour,
                                false,
                                basicStrokeDay,
                                Color.lightGray);

    PeriodAxisLabelInfo labelInfoHour =
        new PeriodAxisLabelInfo(Hour.class,
                                new SimpleDateFormat("HH"),
                                rectangleInsetsDay,
                                fontDay,
                                colorHour,
                                false,
                                basicStrokeDay,
                                Color.lightGray);

    PeriodAxisLabelInfo labelInfoDay =
        new PeriodAxisLabelInfo(Day.class,
                                new SimpleDateFormat("d"),
                                rectangleInsetsDay,
                                fontDay,
                                colorDay,
                                false,
                                basicStrokeDay,
                                Color.lightGray);

    PeriodAxisLabelInfo labelInfoMonth =
        new PeriodAxisLabelInfo(Month.class,
                                new SimpleDateFormat("MMM"),
                                rectangleInsetsDay,
                                fontDay,
                                colorMonth,
                                false,
                                basicStrokeDay,
                                Color.lightGray);

    PeriodAxisLabelInfo labelInfoYear =
        new PeriodAxisLabelInfo(Year.class,
                                new SimpleDateFormat("yyyy"),
                                rectangleInsetsDay,
                                fontDay,
                                colorYear,
                                false,
                                basicStrokeDay,
                                Color.lightGray);

    PeriodAxisLabelInfo[] info;

    if (hours <= 1) {
      info = new PeriodAxisLabelInfo[]{
          labelInfoHourMin
      };
    } else if (hours <= 4) {
      info = new PeriodAxisLabelInfo[]{
          labelInfoHourMin,
          labelInfoDay
      };
    } else if (hours <= 12) {
      info = new PeriodAxisLabelInfo[]{
          labelInfoHourMin,
          labelInfoDay
      };
    } else if (days <= 8) {
      info = new PeriodAxisLabelInfo[]{
          labelInfoHour,
          labelInfoDay
      };
    } else if (days <= 120) {
      info = new PeriodAxisLabelInfo[]{
          labelInfoDay,
          labelInfoMonth
      };
    } else if (days <= 365) {
      info = new PeriodAxisLabelInfo[]{
          labelInfoDay,
          labelInfoMonth,
          labelInfoYear
      };
    } else {
      info = new PeriodAxisLabelInfo[]{
          labelInfoMonth,
          labelInfoYear
      };
    }

    if (hours <= 12) {
      this.dateAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
    } else {
      domainAxis.setLabelInfo(info);
      this.xyPlot.setDomainAxis(domainAxis);
    }
  }

  @Override
  public void addSeriesValue(double x,
                             double y,
                             String seriesName) {
    this.chartDataset.addSeriesValue(x, y, seriesName);
  }

  @Override
  public void deleteSeriesValue(double x,
                                String seriesName) {
    this.chartDataset.remove(x, seriesName);
  }

  @Override
  public void deleteAllSeriesData(int holdRange) {
    if (holdRange == 0) {
      this.chartDataset.clear();
    }
    this.chartDataset.deleteValuesFromDataset(holdRange);
  }

  @Override
  public double getEndXValue() {
    double endXValue;

    try {
      endXValue = this.chartDataset.getEndXValue(0, chartDataset.getItemCount() - 1);
    } catch (Exception e) {
      endXValue = 0D;
    }

    return endXValue;
  }

  @Override
  public void setNotifyPlot(boolean notify) {
    this.xyPlot.setNotify(notify);
  }

  @Override
  public boolean isNotifyPlot() {
    return this.xyPlot.isNotify();
  }

  private void setLegendTitle() {
    this.legendTitle = new FixedLegendTitle(this.jFreeChart.getPlot());

    BlockContainer blockContainerParent = new BlockContainer(new BorderArrangement());
    blockContainerParent.setFrame(new BlockBorder(1.0, 1.0, 1.0, 1.0));

    BlockContainer legendItemContainer = this.legendTitle.getItemContainer();
    legendItemContainer.setPadding(2, 10, 5, 2);

    blockContainerParent.add(legendItemContainer);
    this.legendTitle.setWrapper(blockContainerParent);

    this.legendTitle.setItemFont(new Font(LegendTitle.DEFAULT_ITEM_FONT.getFontName(),
                                          LegendTitle.DEFAULT_ITEM_FONT.getStyle(), this.getLegendFontSize()));

    this.legendTitle.setPosition(RectangleEdge.RIGHT);
    this.legendTitle.setHorizontalAlignment(HorizontalAlignment.LEFT);
    this.legendTitle.setSortOrder(SortOrder.DESCENDING);
  }

  public void setLegendFixedSize(boolean legendFixedSize) {
    this.legendFixedSize = legendFixedSize;
    applyLegendFixedSize();
  }

  public void setLegendFixedWidth(int legendFixedWidth) {
    this.legendFixedWidth = legendFixedWidth;

    if (legendTitle != null && legendFixedSize
        && legendTitle instanceof FixedLegendTitle fixedLegendTitle) {
      fixedLegendTitle.setFixedItemWidth(this.legendFixedWidth);
      jFreeChart.fireChartChanged();
    }
  }

  private void applyLegendFixedSize() {
    if (legendTitle == null) {
      return;
    }

    if (legendFixedSize) {
      if (fixedLegendItemSource == null) {
        fixedLegendItemSource = createFixedLegendItemSource();
      }
      if (defaultLegendSources == null) {
        defaultLegendSources = legendTitle.getSources();
      }
      legendTitle.setSources(new LegendItemSource[]{fixedLegendItemSource});
      setLegendNoWrap(true);
      setLegendFixedWidthEnabled(true);
    } else {
      if (defaultLegendSources != null) {
        legendTitle.setSources(defaultLegendSources);
      }
      setLegendNoWrap(false);
      setLegendFixedWidthEnabled(false);

      hoveredSeriesKey = null;
      legendScrollOffset = 0;
      applySeriesPaints(null);
      resetLegendScrollbar();
    }

    syncLegendInteractionState(legendTitle.isVisible());

    jFreeChart.fireChartChanged();
  }

  private void setLegendNoWrap(boolean noWrap) {
    if (legendTitle instanceof FixedLegendTitle fixedLegendTitle) {
      fixedLegendTitle.setNoWrap(noWrap);
    }
  }

  private void setLegendFixedWidthEnabled(boolean enabled) {
    if (legendTitle instanceof FixedLegendTitle fixedLegendTitle) {
      fixedLegendTitle.setFixedItemWidth(legendFixedWidth);
      fixedLegendTitle.setFixedWidthEnabled(enabled);
    }
  }

  private void resetLegendScrollbar() {
    if (legendTitle instanceof FixedLegendTitle fixedLegendTitle) {
      fixedLegendTitle.setScrollState(0, 0, 0);
    }
  }

  private void syncLegendInteractionState(boolean legendCurrentlyVisible) {
    if (legendFixedSize && legendCurrentlyVisible) {
      installLegendInteraction();
    } else {
      removeLegendInteraction();
    }
  }

  private void installLegendInteraction() {
    if (legendHoverListener == null) {
      legendHoverListener = createLegendHoverListener();
      chartPanel.addChartMouseListener(legendHoverListener);
    }
    if (legendScrollWheelListener == null) {
      legendScrollWheelListener = createLegendScrollWheelListener();
      chartPanel.addMouseWheelListener(legendScrollWheelListener);
    }
  }

  private void removeLegendInteraction() {
    if (legendHoverListener != null) {
      chartPanel.removeChartMouseListener(legendHoverListener);
      legendHoverListener = null;
    }
    if (legendScrollWheelListener != null) {
      chartPanel.removeMouseWheelListener(legendScrollWheelListener);
      legendScrollWheelListener = null;
    }
  }

  private LegendItemSource createFixedLegendItemSource() {
    return () -> {
      List<LegendItem> original = xyPlot.getLegendItems();
      if (original.isEmpty()) {
        legendTotalCount = 0;
        visibleLegendCount = 0;
        return new ArrayList<>();
      }

      Font baseFont = legendTitle.getItemFont();
      Paint defaultItemPaint = legendTitle.getItemPaint();

      FontMetrics fm = getFontMetrics(baseFont.deriveFont(Font.BOLD));

      int iconAndPaddingWidth = 24;
      int availableTextWidth = Math.max(20, legendFixedWidth - iconAndPaddingWidth);

      String hovered = hoveredSeriesKey;
      boolean anyHovered = hovered != null;

      List<LegendItem> displayOrder = new ArrayList<>(original);
      Collections.reverse(displayOrder);

      List<LegendItem> window = visibleLegendWindow(displayOrder);

      List<LegendItem> result = new ArrayList<>(window.size());
      for (int i = window.size() - 1; i >= 0; i--) {
        result.add(createWindowLegendItem(window.get(i), baseFont, defaultItemPaint,
                                          fm, availableTextWidth, hovered, anyHovered));
      }
      return result;
    };
  }

  private List<LegendItem> visibleLegendWindow(List<LegendItem> displayOrder) {
    int total = displayOrder.size();
    legendTotalCount = total;

    double itemHeight = estimateLegendItemHeight();
    double availableHeight = availableLegendHeight();

    int count;
    if (availableHeight <= 0) {
      count = total;
    } else {
      count = (int) Math.floor(availableHeight / itemHeight);
    }
    count = Math.max(1, Math.min(count, total));
    visibleLegendCount = count;

    legendScrollOffset = Math.max(0, Math.min(legendScrollOffset, total - count));

    if (legendTitle instanceof FixedLegendTitle fixedLegendTitle) {
      fixedLegendTitle.setScrollState(legendScrollOffset, visibleLegendCount, legendTotalCount);
    }

    return displayOrder.subList(legendScrollOffset, legendScrollOffset + count);
  }

  private double estimateLegendItemHeight() {
    if (legendTitle instanceof FixedLegendTitle fixedLegendTitle
        && fixedLegendTitle.getMeasuredItemHeight() > 0) {
      return fixedLegendTitle.getMeasuredItemHeight();
    }
    return getFontMetrics(legendTitle.getItemFont()).getHeight() + LEGEND_ITEM_HEIGHT_MARGIN;
  }

  private double availableLegendHeight() {
    if (legendTitle instanceof FixedLegendTitle fixedLegendTitle) {
      double constraintHint = fixedLegendTitle.getHeightConstraintHint();
      if (constraintHint > 0) {
        return constraintHint - LEGEND_VERTICAL_TRIM;
      }

      Rectangle2D lastArea = fixedLegendTitle.getLastDrawnArea();
      if (lastArea != null) {
        return lastArea.getHeight() - LEGEND_VERTICAL_TRIM;
      }
    }
    return chartPanel.getHeight() - LEGEND_VERTICAL_TRIM;
  }

  private LegendItem createWindowLegendItem(LegendItem item,
                                            Font baseFont,
                                            Paint defaultItemPaint,
                                            FontMetrics fm,
                                            int availableTextWidth,
                                            String hovered,
                                            boolean anyHovered) {
    String fullLabel = item.getLabel();
    String shortLabel = truncateToWidth(fullLabel, fm, availableTextWidth);

    String seriesKey = String.valueOf(item.getSeriesKey());
    boolean isHovered = anyHovered && hovered.equals(seriesKey);

    Color nativeColor = internalSeriesColor.get(seriesKey);
    Paint fillPaint;
    if (nativeColor != null) {
      fillPaint = anyHovered && !isHovered
          ? withAlpha(nativeColor, LEGEND_DIMMED_FILL_ALPHA)
          : nativeColor;
    } else {
      fillPaint = item.getFillPaint();
    }

    LegendItem shortItem = new LegendItem(shortLabel, item.getDescription(), fullLabel, item.getURLText(),
                                          item.isShapeVisible(), item.getShape(), item.isShapeFilled(), fillPaint,
                                          item.isShapeOutlineVisible(), item.getOutlinePaint(), item.getOutlineStroke(),
                                          item.isLineVisible(), item.getLine(), item.getLineStroke(), item.getLinePaint());

    shortItem.setSeriesKey(item.getSeriesKey());
    shortItem.setSeriesIndex(item.getSeriesIndex());
    shortItem.setDataset(item.getDataset());
    shortItem.setDatasetIndex(item.getDatasetIndex());

    Paint labelPaint = item.getLabelPaint() != null ? item.getLabelPaint() : defaultItemPaint;
    if (anyHovered && !isHovered && labelPaint instanceof Color labelColor) {
      labelPaint = withAlpha(labelColor, LEGEND_DIMMED_LABEL_ALPHA);
    }
    shortItem.setLabelFont(isHovered ? baseFont.deriveFont(Font.BOLD) : baseFont);
    shortItem.setLabelPaint(labelPaint);

    return shortItem;
  }

  private ChartMouseListener createLegendHoverListener() {
    return new ChartMouseListener() {
      @Override
      public void chartMouseClicked(ChartMouseEvent event) {
      }

      @Override
      public void chartMouseMoved(ChartMouseEvent event) {
        if (!legendFixedSize || legendTitle == null || !legendTitle.isVisible()) {
          return;
        }

        ChartEntity entity = event.getEntity();
        String newHovered = null;

        if (entity instanceof LegendItemEntity legendItemEntity
            && legendItemEntity.getSeriesKey() != null) {
          newHovered = String.valueOf(legendItemEntity.getSeriesKey());
        }

        if (!Objects.equals(newHovered, hoveredSeriesKey)) {
          hoveredSeriesKey = newHovered;
          applySeriesPaints(newHovered);

          jFreeChart.fireChartChanged();
        }
      }
    };
  }

  private MouseWheelListener createLegendScrollWheelListener() {
    return e -> {
      if (!legendFixedSize || !(legendTitle instanceof FixedLegendTitle fixedLegendTitle)) {
        return;
      }

      Rectangle2D legendArea = fixedLegendTitle.getLastDrawnArea();
      if (legendArea == null || !legendTitle.isVisible()) {
        return;
      }

      Point2D point = chartPanel.translateScreenToJava2D(e.getPoint());
      if (!legendArea.contains(point)) {
        return;
      }

      if (scrollLegendBy(e.getWheelRotation() * LEGEND_SCROLL_ITEMS_PER_NOTCH)) {
        jFreeChart.fireChartChanged();
      }
    };
  }

  private boolean scrollLegendBy(int delta) {
    int maxOffset = Math.max(0, legendTotalCount - visibleLegendCount);
    int newOffset = Math.max(0, Math.min(legendScrollOffset + delta, maxOffset));
    if (newOffset == legendScrollOffset) {
      return false;
    }
    legendScrollOffset = newOffset;
    return true;
  }

  private void applySeriesPaints(String highlightedSeriesKey) {
    seriesIndexMap.forEach((name, idx) -> {
      Color base = internalSeriesColor.get(name);
      if (base == null) {
        return;
      }
      Paint paint = highlightedSeriesKey != null && !name.equals(highlightedSeriesKey)
          ? withAlpha(base, PLOT_DIMMED_ALPHA)
          : base;
      stackedXYAreaRenderer3.setSeriesPaint(idx, paint);
    });
    chartPanel.repaint();
  }

  private static Color withAlpha(Color c, int alpha) {
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
  }

  private static FontMetrics getFontMetrics(Font font) {
    BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = tmp.createGraphics();
    try {
      return g2.getFontMetrics(font);
    } finally {
      g2.dispose();
    }
  }

  private static String truncateToWidth(String text, FontMetrics fm, int maxWidth) {
    if (text == null) {
      return "";
    }
    if (fm.stringWidth(text) <= maxWidth) {
      return text;
    }

    String ellipsis = "\u2026";
    int ellipsisWidth = fm.stringWidth(ellipsis);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      String candidate = sb.toString() + text.charAt(i);
      if (fm.stringWidth(candidate) + ellipsisWidth > maxWidth) {
        break;
      }
      sb.append(text.charAt(i));
    }

    return sb.length() == 0 ? ellipsis : sb + ellipsis;
  }

  public void setBackgroundAndTextColor(Color backgroundColor,
                                        Color legendBackgroundColor) {
    if (backgroundColor != null) {
      jFreeChart.setBackgroundPaint(backgroundColor);
      jFreeChart.getTitle().setPaint(Color.WHITE);
      XYPlot plot = (XYPlot) jFreeChart.getPlot();
      plot.getDomainAxis().setLabelPaint(Color.WHITE);
      plot.getRangeAxis().setLabelPaint(Color.WHITE);
      plot.getDomainAxis().setTickLabelPaint(Color.WHITE);
      plot.getRangeAxis().setTickLabelPaint(Color.WHITE);

      legendTitle.setBackgroundPaint(legendBackgroundColor);
    }
  }

  private StackedXYAreaRenderer3 getStackedXYAreaRenderer3() {
    return stackedXYAreaRenderer3;
  }

  private void setStackedXYAreaRenderer3(DatasetSelectionExtension<XYCursor> datasetExtension) {

    StandardXYToolTipGenerator standardXYToolTipGenerator = new StandardXYToolTipGenerator
        ("{0} ({1}, {2})",
         new SimpleDateFormat("HH:mm"),
         new DecimalFormat("0.0"));
    this.stackedXYAreaRenderer3 = new SmoothedStackedXYAreaRenderer(standardXYToolTipGenerator, null);
    this.stackedXYAreaRenderer3.setRoundXCoordinates(true);

    this.xyPlot.setDomainPannable(true);
    this.xyPlot.setRangePannable(true);
    this.xyPlot.setDomainCrosshairVisible(true);
    this.xyPlot.setRangeCrosshairVisible(true);
    datasetExtension.addChangeListener(this.xyPlot);

    IRSUtilities.setSelectedItemFillPaint(this.getStackedXYAreaRenderer3(), datasetExtension, Color.black);
  }

  public void setSmoothingPrecision(int precision) {
    if (this.stackedXYAreaRenderer3 instanceof SmoothedStackedXYAreaRenderer smooth) {
      smooth.setPrecision(precision);
    }
  }

  public void clearSelectionRegion() {
    chartPanel.setSelectionShape(null);

    if (datasetExtension != null) {
      datasetExtension.clearSelection();
    }

    if (selectionManager != null) {
      selectionManager.clearSelection();
    }

    chartPanel.repaint();
  }

  public void snapshotSelectionRegion() {
    try {
      Shape sel = chartPanel.getSelectionShape();
      if (sel == null) {
        selDomainStart = selDomainEnd = null;
        return;
      }

      Rectangle2D selBounds = sel.getBounds2D();
      Rectangle2D dataArea = chartPanel.getScreenDataArea();
      if (dataArea == null) {
        selDomainStart = selDomainEnd = null;
        return;
      }

      double left = Math.max(selBounds.getMinX(), dataArea.getMinX());
      double right = Math.min(selBounds.getMaxX(), dataArea.getMaxX());
      if (right < left) {
        left = selBounds.getMinX();
        right = selBounds.getMaxX();
      }

      ValueAxis domainAxis = xyPlot.getDomainAxis();
      RectangleEdge domainEdge = xyPlot.getDomainAxisEdge();

      double v1 = domainAxis.java2DToValue(left, dataArea, domainEdge);
      double v2 = domainAxis.java2DToValue(right, dataArea, domainEdge);

      selDomainStart = Math.min(v1, v2);
      selDomainEnd = Math.max(v1, v2);
    } catch (Exception e) {
      log.warn("Snapshot selection region failed", e);
      selDomainStart = selDomainEnd = null;
    }
  }

  public boolean hasSelectionSnapshot() {
    return selDomainStart != null && selDomainEnd != null;
  }

  public void restoreSelectionRegion() {
    try {
      if (!hasSelectionSnapshot()) {
        return;
      }
      Rectangle2D dataArea = chartPanel.getScreenDataArea();
      if (dataArea == null) {
        return;
      }

      ValueAxis domainAxis = xyPlot.getDomainAxis();
      RectangleEdge domainEdge = xyPlot.getDomainAxisEdge();

      double x1 = domainAxis.valueToJava2D(selDomainStart, dataArea, domainEdge);
      double x2 = domainAxis.valueToJava2D(selDomainEnd, dataArea, domainEdge);
      double minX = Math.min(x1, x2);
      double width = Math.abs(x2 - x1);
      if (width < 1) width = 1;

      Rectangle2D rect = new Rectangle2D.Double(minX, dataArea.getMinY(), width, dataArea.getHeight());

      chartPanel.setSelectionShape(rect);

      if (selectionManager != null) {
        selectionManager.select(rect);
      }

      chartPanel.repaint();
    } catch (Exception e) {
      log.warn("Restore selection region failed", e);
    }
  }

  public void restoreSelectionRegionAfterNextDraw() {
    if (!hasSelectionSnapshot()) return;

    ChartProgressListener once = new ChartProgressListener() {
      @Override
      public void chartProgress(ChartProgressEvent event) {
        if (event.getType() == ChartProgressEvent.DRAWING_FINISHED) {
          try {
            jFreeChart.removeProgressListener(this);
            restoreSelectionRegion();
          } catch (Exception e) {
            log.warn("Restore selection region after next draw failed", e);
          }
        }
      }
    };

    jFreeChart.addProgressListener(once);
    chartPanel.repaint();
  }

  public void setLegendTitleVisible(boolean visible) {
    setLegendTitleVisible(visible, null);
  }

  public void setLegendTitleVisible(boolean visible, ChartUISettings settings) {
    boolean hideCustomLegend = settings != null && settings.isHideCustomLegend();
    boolean hideBuiltInLegend = settings != null && settings.isHideBuiltInLegend();
    boolean hideYAxis = settings != null && settings.isHideYAxis();
    boolean hideXAxis = settings != null && settings.isHideXAxis();
    boolean hidePlotInsets = settings != null && settings.isHidePlotInsets();
    boolean hideChartPadding = settings != null && settings.isHideChartPadding();

    boolean customLegendVisible = visible || !hideCustomLegend;
    if (legendTitle != null) {
      legendTitle.setVisible(customLegendVisible);
    }

    if (jFreeChart.getLegend() != null) {
      jFreeChart.getLegend().setVisible(visible || !hideBuiltInLegend);
    }

    ValueAxis rangeAxis = this.xyPlot.getRangeAxis();
    if (rangeAxis != null) {
      rangeAxis.setVisible(visible || !hideYAxis);
    }

    ValueAxis domainAxis = this.xyPlot.getDomainAxis();
    if (domainAxis != null) {
      domainAxis.setVisible(visible || !hideXAxis);
    }

    if (visible) {
      this.xyPlot.setInsets(new RectangleInsets(4, 8, 4, 4));
      this.jFreeChart.setPadding(new RectangleInsets(4, 4, 4, 4));
    } else {
      if (hidePlotInsets) {
        this.xyPlot.setInsets(new RectangleInsets(0, 0, 0, 0));
      } else {
        this.xyPlot.setInsets(new RectangleInsets(4, 8, 4, 4));
      }
      if (hideChartPadding) {
        this.jFreeChart.setPadding(new RectangleInsets(0, 0, 0, 0));
      } else {
        this.jFreeChart.setPadding(new RectangleInsets(4, 4, 4, 4));
      }
    }

    syncLegendInteractionState(customLegendVisible);

    this.jFreeChart.fireChartChanged();
  }

  public void setRangeAxisVisible(boolean visible) {
    ValueAxis rangeAxis = this.xyPlot.getRangeAxis();
    if (rangeAxis != null) {
      rangeAxis.setVisible(visible);

      if (!visible) {
        this.xyPlot.setAxisOffset(new RectangleInsets(0, 0, 0, 0));
      }
    }

    this.jFreeChart.fireChartChanged();
  }

  @Override
  public void addChartListenerReleaseMouse(IDetailPanel l) {
    chartPanel.addListenerReleaseMouse(l);
  }

  @Override
  public void removeChartListenerReleaseMouse(IDetailPanel l) {
    chartPanel.removeListenerReleaseMouse(l);
  }

  @Override
  public void selectionChanged(SelectionChangeEvent<XYCursor> event) {
    XYDatasetSelectionExtension ext = (XYDatasetSelectionExtension) event.getSelectionExtension();
    DatasetIterator<XYCursor> iter = ext.getSelectionIterator(true);
  }

  private class SelectionRefitListener extends ComponentAdapter {
    @Override
    public void componentResized(ComponentEvent e) {
      if (chartPanel.getSelectionShape() != null) {
        snapshotSelectionRegion();
      }
      if (hasSelectionSnapshot()) {
        restoreSelectionRegionAfterNextDraw();
      }
    }
  }
}