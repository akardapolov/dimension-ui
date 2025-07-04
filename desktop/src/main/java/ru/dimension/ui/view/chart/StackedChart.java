package ru.dimension.ui.view.chart;

import static ru.dimension.ui.laf.LafColorGroup.CHART_HISTORY_DAY_FONT;
import static ru.dimension.ui.laf.LafColorGroup.CHART_HISTORY_HOUR_FONT;
import static ru.dimension.ui.laf.LafColorGroup.CHART_HISTORY_MONTH_FONT;
import static ru.dimension.ui.laf.LafColorGroup.CHART_HISTORY_YEAR_FONT;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.PeriodAxis;
import org.jfree.chart.axis.PeriodAxisLabelInfo;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.panel.selectionhandler.EntitySelectionManager;
import org.jfree.chart.panel.selectionhandler.MouseClickSelectionHandler;
import org.jfree.chart.panel.selectionhandler.RectangularHeightRegionSelectionHandler;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.item.IRSUtilities;
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
import ru.dimension.ui.model.chart.CategoryTableXYDatasetRealTime;

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

  private final Map<String, Color> internalSeriesColor = new ConcurrentHashMap<>();
  private final Map<String, Color> externalSeriesColor = new ConcurrentHashMap<>();
  private AtomicInteger counter;
  private RectangularHeightRegionSelectionHandler selectionHandler;
  private DatasetExtensionManager dExManager;
  private EntitySelectionManager selectionManager;
  private DatasetSelectionExtension<XYCursor> datasetExtension;

  public StackedChart(ChartPanel chartPanel) {
    this.chartPanel = chartPanel;
    this.jFreeChart = this.chartPanel.getChart();
    this.xyPlot = (XYPlot) this.jFreeChart.getPlot();
    this.dateAxis = (DateAxis) this.xyPlot.getDomainAxis();
    this.chartDataset = (CategoryTableXYDatasetRealTime) this.xyPlot.getDataset();
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
    this.chartPanel.addMouseHandler(selectionHandler);
    this.chartPanel.addMouseHandler(new MouseClickSelectionHandler());
    this.chartPanel.removeMouseHandler(this.chartPanel.getZoomHandler());

    this.dExManager = new DatasetExtensionManager();
    this.dExManager.registerDatasetExtension(datasetExtension);

    this.selectionManager = new EntitySelectionManager(this.chartPanel,
                                                       new Dataset[]{this.chartDataset},
                                                       dExManager);
    this.chartPanel.setSelectionManager(this.selectionManager);

    this.setLegendTitle();
    this.jFreeChart.addSubtitle(this.legendTitle);
    this.chartPanel.setRangeZoomable(false);
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
  public void loadSeriesColorInternal(String seriesName) {
    if (!this.internalSeriesColor.containsKey(seriesName)) {
      try {
        int cnt = counter.getAndIncrement();

        Color color;
        if (this.externalSeriesColor.containsKey(seriesName)) {
          color = externalSeriesColor.get(seriesName);
        } else {
          color = ColorHelper.getColor(seriesName);
        }

        this.stackedXYAreaRenderer3.setSeriesPaint(cnt, color);
        this.chartDataset.saveSeriesValues(cnt, seriesName);
        this.internalSeriesColor.put(seriesName, color);
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

    this.chartDataset.clear();
    this.stackedXYAreaRenderer3.clearSeriesPaints(true);
    this.stackedXYAreaRenderer3.clearSeriesStrokes(true);

    counter = new AtomicInteger(0);
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
    this.legendTitle = new LegendTitle(this.jFreeChart.getPlot());

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
    this.stackedXYAreaRenderer3 = new StackedXYAreaRenderer3(standardXYToolTipGenerator, null);
    this.stackedXYAreaRenderer3.setRoundXCoordinates(true);

    this.xyPlot.setDomainPannable(true);
    this.xyPlot.setRangePannable(true);
    this.xyPlot.setDomainCrosshairVisible(true);
    this.xyPlot.setRangeCrosshairVisible(true);
    datasetExtension.addChangeListener(this.xyPlot);

    IRSUtilities.setSelectedItemFillPaint(this.getStackedXYAreaRenderer3(), datasetExtension, Color.black);
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
}
