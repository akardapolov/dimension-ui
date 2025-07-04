package ru.dimension.ui.chart;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;

/**
 * @see https://stackoverflow.com/a/21307289/230513
 */
public class DynamicTimeSeriesChart extends JPanel {

  private final DynamicTimeSeriesCollection dataset;
  private final JFreeChart chart;

  public DynamicTimeSeriesChart(final String title) {
    dataset = new DynamicTimeSeriesCollection(1, 1000, new Second());
    dataset.setTimeBase(new Second(0, 0, 0, 23, 1, 2014));
    dataset.addSeries(new float[1], 0, title);
    chart = ChartFactory.createTimeSeriesChart(
        title, "Time", title, dataset, true, true, false);
    final XYPlot plot = (XYPlot) chart.getPlot();
    DateAxis axis = (DateAxis) plot.getDomainAxis();
    axis.setAutoRange(true);
    axis.setFixedAutoRange(10000);
    axis.setDateFormatOverride(new SimpleDateFormat("ss.SS"));
    final ChartPanel chartPanel = new ChartPanel(chart);
    add(chartPanel);
  }

  public void update(float value) {
    float[] newData = new float[1];
    newData[0] = value;
    dataset.advanceTime();
    dataset.appendData(newData);
  }

  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {

      @Override
      public void run() {
        JFrame frame = new JFrame("testing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final DynamicTimeSeriesChart chart
            = new DynamicTimeSeriesChart("Alternating data");
        frame.add(chart);
        frame.pack();
        Timer timer = new Timer(1000, new ActionListener() {
          private boolean b;

          @Override
          public void actionPerformed(ActionEvent e) {
            chart.update(b ? 1 : 0);
            b = !b;
          }
        });
        timer.start();
        frame.setVisible(true);
      }
    });
  }
}
