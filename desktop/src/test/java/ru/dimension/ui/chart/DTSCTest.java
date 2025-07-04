package ru.dimension.ui.chart;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.Timer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.jfree.data.xy.XYDataset;
import org.jfree.chart.ui.ApplicationFrame;

/**
 * @see http://stackoverflow.com/a/15521956/230513
 * @see http://stackoverflow.com/questions/5048852
 */
public class DTSCTest extends ApplicationFrame {

  private static final String TITLE = "Dynamic Series";
  private static final String START = "Start";
  private static final String STOP = "Stop";
  private static final float MINMAX = 100;
  private static final int COUNT = 2 * 60;
  private static final int FAST = 100;
  private static final int SLOW = FAST * 5;
  private static final Random random = new Random();
  private Timer timer;

  public DTSCTest(final String title) {
    super(title);
    final DynamicTimeSeriesCollection dataset =
        new DynamicTimeSeriesCollection(1, COUNT, new Second());
    dataset.setTimeBase(new Second(0, 0, 0, 1, 1, 2011));
    dataset.addSeries(gaussianData(), 0, "Gaussian data");
    JFreeChart chart = createChart(dataset);

    final JButton run = new JButton(STOP);
    run.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (STOP.equals(cmd)) {
          timer.stop();
          run.setText(START);
        } else {
          timer.start();
          run.setText(STOP);
        }
      }
    });

    final JComboBox combo = new JComboBox();
    combo.addItem("Fast");
    combo.addItem("Slow");
    combo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if ("Fast".equals(combo.getSelectedItem())) {
          timer.setDelay(FAST);
        } else {
          timer.setDelay(SLOW);
        }
      }
    });

    this.add(new ChartPanel(chart) {
      @Override
      public Dimension getPreferredSize() {
        return new Dimension(640, 480);
      }
    }, BorderLayout.CENTER);
    JPanel btnPanel = new JPanel(new FlowLayout());
    btnPanel.add(run);
    btnPanel.add(combo);
    this.add(btnPanel, BorderLayout.SOUTH);

    timer = new Timer(FAST, new ActionListener() {
      float[] newData = new float[1];

      @Override
      public void actionPerformed(ActionEvent e) {
        newData[0] = randomValue();
        dataset.advanceTime();
        dataset.appendData(newData);
      }
    });
  }

  private float randomValue() {
    return (float) (random.nextGaussian() * MINMAX / 3);
  }

  private float[] gaussianData() {
    float[] a = new float[COUNT];
    for (int i = 0; i < a.length; i++) {
      a[i] = randomValue();
    }
    return a;
  }

  private JFreeChart createChart(final XYDataset dataset) {
    final JFreeChart result = ChartFactory.createTimeSeriesChart(
        TITLE, "hh:mm:ss", "milliVolts", dataset, true, true, false);
    final XYPlot plot = (XYPlot) result.getPlot();
    ValueAxis domain = plot.getDomainAxis();
    domain.setAutoRange(true);
    ValueAxis range = plot.getRangeAxis();
    range.setRange(-MINMAX, MINMAX);
    return result;
  }

  public void start() {
    timer.start();
  }

  public static void main(final String[] args) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        DTSCTest demo = new DTSCTest(TITLE);
        demo.pack();
        positionFrameOnScreen(demo, 0.5, 0.5);
        demo.setVisible(true);
        demo.start();
      }
    });
  }

  public static void positionFrameOnScreen(final Window frame,
                                           final double horizontalPercent,
                                           final double verticalPercent) {

    final Rectangle s = frame.getGraphicsConfiguration().getBounds();
    final Dimension f = frame.getSize();
    final int w = Math.max(s.width - f.width, 0);
    final int h = Math.max(s.height - f.height, 0);
    final int x = (int) (horizontalPercent * w) + s.x;
    final int y = (int) (verticalPercent * h) + s.y;
    frame.setBounds(x, y, f.width, f.height);

  }
}