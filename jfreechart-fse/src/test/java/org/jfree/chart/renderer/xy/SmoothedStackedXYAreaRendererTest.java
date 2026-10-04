package org.jfree.chart.renderer.xy;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmoothedStackedXYAreaRendererTest {

    private DefaultTableXYDataset createSparseDataset() {
        DefaultTableXYDataset result = new DefaultTableXYDataset();
        XYSeries series1 = new XYSeries("Series 1", false, false);
        series1.add(1.0, 2.0);
        series1.add(2.0, 5.0);
        series1.add(3.0, 1.0);
        series1.add(4.0, 6.0);
        series1.add(5.0, 3.0);
        XYSeries series2 = new XYSeries("Series 2", false, false);
        series2.add(1.0, 4.0);
        series2.add(2.0, 3.0);
        series2.add(3.0, 7.0);
        series2.add(4.0, 2.0);
        series2.add(5.0, 5.0);
        result.addSeries(series1);
        result.addSeries(series2);
        return result;
    }

    @Test
    public void testDrawWithEmptyDataset() {
        JFreeChart chart = ChartFactory.createStackedXYAreaChart("title", "x",
                "y", new DefaultTableXYDataset());
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setRenderer(new SmoothedStackedXYAreaRenderer());

        BufferedImage image = new BufferedImage(200, 100,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        chart.draw(g2, new Rectangle2D.Double(0, 0, 200, 100), null, null);
        g2.dispose();
    }

    @Test
    public void testDrawWithSparseDataset() {
        JFreeChart chart = ChartFactory.createStackedXYAreaChart("title", "x",
                "y", createSparseDataset());
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setRenderer(new SmoothedStackedXYAreaRenderer());

        BufferedImage image = new BufferedImage(400, 200,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        chart.draw(g2, new Rectangle2D.Double(0, 0, 400, 200), null, null);
        g2.dispose();

        assertTrue(hasNonWhitePixels(image));
    }

    @Test
    public void testPrecision() {
        SmoothedStackedXYAreaRenderer r = new SmoothedStackedXYAreaRenderer();
        assertEquals(8, r.getPrecision());
        r.setPrecision(3);
        assertEquals(3, r.getPrecision());
        assertThrows(IllegalArgumentException.class,
                () -> r.setPrecision(0));
        assertThrows(IllegalArgumentException.class,
                () -> new SmoothedStackedXYAreaRenderer(null, null, -1));
    }

    @Test
    public void testEquals() {
        SmoothedStackedXYAreaRenderer r1 = new SmoothedStackedXYAreaRenderer();
        SmoothedStackedXYAreaRenderer r2 = new SmoothedStackedXYAreaRenderer();
        assertEquals(r1, r2);
        assertEquals(r2, r1);

        r1.setPrecision(20);
        assertTrue(!r1.equals(r2));
        r2.setPrecision(20);
        assertEquals(r1, r2);
    }

    @Test
    public void testCloning() throws CloneNotSupportedException {
        SmoothedStackedXYAreaRenderer r1 = new SmoothedStackedXYAreaRenderer();
        SmoothedStackedXYAreaRenderer r2 =
                (SmoothedStackedXYAreaRenderer) r1.clone();
        assertNotSame(r1, r2);
        assertSame(r1.getClass(), r2.getClass());
        assertEquals(r1, r2);
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        SmoothedStackedXYAreaRenderer r1 = new SmoothedStackedXYAreaRenderer();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(buffer);
        out.writeObject(r1);
        out.close();

        ObjectInput in = new ObjectInputStream(
                new ByteArrayInputStream(buffer.toByteArray()));
        SmoothedStackedXYAreaRenderer r2 =
                (SmoothedStackedXYAreaRenderer) in.readObject();
        in.close();

        assertEquals(r1, r2);
    }

    private boolean hasNonWhitePixels(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int white = 0xFFFFFFFF;
        for (int x = 0; x < w; x += 5) {
            for (int y = 0; y < h; y += 5) {
                if (image.getRGB(x, y) != white) {
                    return true;
                }
            }
        }
        return false;
    }

}