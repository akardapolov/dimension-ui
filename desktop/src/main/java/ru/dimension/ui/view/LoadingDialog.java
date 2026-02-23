package ru.dimension.ui.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class LoadingDialog extends JDialog {

  private final JLabel lblLoading;
  private final JButton btnCancel;
  private final AnimationPanel animationPanel;
  private final JPanel titlePanel;

  private static final Color BG_COLOR = new Color(245, 247, 250);
  private static final Color BORDER_COLOR = new Color(200, 200, 200);
  private static final Color BUTTON_COLOR_NORMAL = new Color(229, 115, 115);
  private static final Color BUTTON_COLOR_HOVER = new Color(239, 83, 80);
  private static final Color BUTTON_TEXT_COLOR = Color.WHITE;
  private static final Color TITLE_TEXT_COLOR = new Color(45, 50, 60);

  private static Color titleBarColor = new Color(185, 205, 225);

  public LoadingDialog() {
    setUndecorated(true);
    setModal(false);
    setAlwaysOnTop(true);
    setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    setType(Type.UTILITY);

    JPanel contentPane = new JPanel(new BorderLayout());
    contentPane.setBackground(BG_COLOR);
    contentPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

    titlePanel = new JPanel(new BorderLayout());
    titlePanel.setBackground(titleBarColor);
    titlePanel.setBorder(BorderFactory.createEmptyBorder(9, 0, 9, 0));

    JLabel titleLabel = new JLabel("Dimension-UI", SwingConstants.CENTER);
    titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 19));
    titleLabel.setForeground(TITLE_TEXT_COLOR);
    titlePanel.add(titleLabel, BorderLayout.CENTER);

    animationPanel = new AnimationPanel();
    animationPanel.setPreferredSize(new Dimension(420, 160));

    lblLoading = new JLabel("Loading components...", SwingConstants.CENTER);
    lblLoading.setFont(new Font("Segoe UI", Font.PLAIN, 16));
    lblLoading.setForeground(new Color(80, 80, 90));
    lblLoading.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.setBackground(BG_COLOR);
    centerPanel.add(animationPanel, BorderLayout.CENTER);
    centerPanel.add(lblLoading, BorderLayout.SOUTH);

    btnCancel = new JButton("Cancel");
    btnCancel.setFocusPainted(false);
    btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 14));
    btnCancel.setForeground(BUTTON_TEXT_COLOR);
    btnCancel.setBackground(BUTTON_COLOR_NORMAL);
    btnCancel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
    btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    btnCancel.addMouseListener(new MouseAdapter() {
      public void mouseEntered(MouseEvent e) { btnCancel.setBackground(BUTTON_COLOR_HOVER); }
      public void mouseExited(MouseEvent e) { if(btnCancel.isEnabled()) btnCancel.setBackground(BUTTON_COLOR_NORMAL); }
    });

    btnCancel.addActionListener(e -> {
      System.out.println("Cancel by user");
      System.exit(0);
    });

    contentPane.add(titlePanel, BorderLayout.NORTH);
    contentPane.add(centerPanel, BorderLayout.CENTER);
    contentPane.add(btnCancel, BorderLayout.SOUTH);

    setContentPane(contentPane);
    pack();

    animationPanel.start();
  }

  public static void setTitleBarColor(Color color) {
    titleBarColor = color;
  }

  public static Color getTitleBarColor() {
    return titleBarColor;
  }

  public void updateTitleBarColor(Color color) {
    titleBarColor = color;
    titlePanel.setBackground(color);
    titlePanel.repaint();
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      if (isOpacitySupported()) {
        setOpacity(0f);
        super.setVisible(true);
        centerOnScreen();
        fadeIn();
      } else {
        setLocation(-10000, -10000);
        super.setVisible(true);
        SwingUtilities.invokeLater(this::centerOnScreen);
      }
    } else {
      super.setVisible(false);
    }
  }

  private boolean isOpacitySupported() {
    try {
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      GraphicsDevice gd = ge.getDefaultScreenDevice();
      return gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
    } catch (Exception e) {
      return false;
    }
  }

  private void fadeIn() {
    Timer fadeInTimer = new Timer(16, null);
    final float[] opacity = {0f};
    fadeInTimer.addActionListener(e -> {
      opacity[0] += 0.1f;
      if (opacity[0] >= 1f) {
        setOpacity(1f);
        fadeInTimer.stop();
      } else {
        setOpacity(opacity[0]);
      }
    });
    fadeInTimer.start();
  }

  private void centerOnScreen() {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gd = ge.getDefaultScreenDevice();
    java.awt.GraphicsConfiguration gc = gd.getDefaultConfiguration();

    java.awt.Rectangle screenBounds = gc.getBounds();
    java.awt.Insets screenInsets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(gc);

    int screenWidth = screenBounds.width - screenInsets.left - screenInsets.right;
    int screenHeight = screenBounds.height - screenInsets.top - screenInsets.bottom;

    int x = screenBounds.x + screenInsets.left + (screenWidth - getWidth()) / 2;
    int y = screenBounds.y + screenInsets.top + (screenHeight - getHeight()) / 2;

    setLocation(x, y);
  }

  public void setMessage(String text) {
    SwingUtilities.invokeLater(() -> lblLoading.setText(text));
  }

  public void lockCancel(String text) {
    SwingUtilities.invokeLater(() -> {
      lblLoading.setText(text);
      btnCancel.setEnabled(false);
      btnCancel.setBackground(new Color(200, 200, 200));
      btnCancel.setForeground(Color.GRAY);
    });
  }

  @Override
  public void dispose() {
    animationPanel.stop();
    super.dispose();
  }

  private static class AnimationPanel extends JPanel {
    private static final Color COLOR_DISK_BODY = new Color(70, 130, 180);
    private static final Color COLOR_RAM_BODY = new Color(60, 179, 113);
    private static final Color COLOR_WIRE = new Color(200, 200, 210);

    private final Timer timer;
    private long startTime;
    private static final long CYCLE_DURATION_MS = 1500;

    public AnimationPanel() {
      setBackground(BG_COLOR);
      startTime = System.nanoTime();

      timer = new Timer(16, e -> repaint());
    }

    public void start() {
      if (!timer.isRunning()) {
        startTime = System.nanoTime();
        timer.start();
      }
    }
    public void stop() { timer.stop(); }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      long now = System.nanoTime();
      float globalProgress = (float) ((now - startTime) / 1_000_000.0 % CYCLE_DURATION_MS) / CYCLE_DURATION_MS;

      int w = getWidth();
      int h = getHeight();

      int iconSize = 60;
      int hMargin = 70;
      int startX = hMargin;
      int endX = w - hMargin - iconSize;
      int centerY = h / 2;

      g2.setColor(COLOR_WIRE);
      g2.setStroke(new BasicStroke(3));
      g2.drawLine(startX + iconSize, centerY, endX, centerY);

      drawDataStream(g2, startX + iconSize, endX, centerY, globalProgress);

      drawHardDisk(g2, startX, centerY, iconSize);
      drawRamStick(g2, endX, centerY, iconSize);
    }

    private void drawDataStream(Graphics2D g2, int x1, int x2, int y, float progress) {
      int distance = x2 - x1;
      int packetCount = 3;
      float spacing = 1.0f / packetCount;

      for (int i = 0; i < packetCount; i++) {
        float t = progress + (i * spacing);
        if (t > 1.0f) t -= 1.0f;

        int currentX = x1 + (int) (distance * t);

        Color packetColor = interpolateColor(t);

        AffineTransform old = g2.getTransform();
        g2.translate(currentX, y);
        g2.rotate(t * Math.PI * 4);

        g2.setColor(packetColor);
        int size = 18;
        g2.fill(new RoundRectangle2D.Float(-size/2f, -size/2f, size, size, 5, 5));

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new RoundRectangle2D.Float(-size/2f, -size/2f, size, size, 5, 5));

        g2.setTransform(old);
      }
    }

    private Color interpolateColor(float t) {
      if (t < 0.5f) {
        return blend(COLOR_DISK_BODY, BUTTON_COLOR_NORMAL, t * 2);
      } else {
        return blend(BUTTON_COLOR_NORMAL, COLOR_RAM_BODY, (t - 0.5f) * 2);
      }
    }

    private Color blend(Color c1, Color c2, float ratio) {
      if (ratio > 1f) ratio = 1f;
      if (ratio < 0f) ratio = 0f;
      int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
      int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
      int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
      return new Color(r, g, b);
    }

    private void drawHardDisk(Graphics2D g2, int x, int cy, int size) {
      int platterCount = 4;
      int platterHeight = size / 4;
      int stepY = 12;

      int totalHeight = (platterCount - 1) * stepY + platterHeight;
      int startY = cy - totalHeight / 2;

      g2.setColor(COLOR_DISK_BODY.darker());
      g2.fillRect(x + size/2 - 5, startY, 10, totalHeight);

      for (int i = platterCount - 1; i >= 0; i--) {
        int yPos = startY + (i * stepY);

        g2.setColor(COLOR_DISK_BODY.darker());
        g2.fillOval(x, yPos + 3, size, platterHeight);

        g2.setColor(COLOR_DISK_BODY);
        g2.fillOval(x, yPos, size, platterHeight);

        g2.setColor(new Color(255, 255, 255, 40));
        g2.drawOval(x, yPos, size, platterHeight);
      }

      drawLabel(g2, "HDD", x + size/2, cy + totalHeight/2 + 20);
    }

    private void drawRamStick(Graphics2D g2, int x, int cy, int size) {
      int w = size / 2 + 10;
      int h = size + 20;
      int startY = cy - h / 2;

      GradientPaint gp = new GradientPaint(x, startY, COLOR_RAM_BODY, x + w, startY + h, COLOR_RAM_BODY.darker());
      g2.setPaint(gp);
      g2.fillRoundRect(x, startY, w, h, 6, 6);

      g2.setColor(new Color(30, 30, 30));
      int chips = 4;
      int chipMargin = 4;
      int areaH = h - 15;
      int chipH = (areaH / chips) - chipMargin;

      for (int i = 0; i < chips; i++) {
        g2.fillRect(x + 5, startY + 5 + (i * (chipH + chipMargin)), w - 10, chipH);
      }

      g2.setColor(new Color(218, 165, 32));
      g2.fillRect(x + 3, startY + h - 8, w - 6, 6);
      g2.setColor(COLOR_RAM_BODY.darker());
      for(int i = 6; i < w - 6; i += 4) {
        g2.drawLine(x + i, startY + h - 8, x + i, startY + h - 2);
      }

      drawLabel(g2, "Memory", x + w/2, cy + h/2 + 20);
    }

    private void drawLabel(Graphics2D g2, String text, int cx, int y) {
      g2.setColor(new Color(120, 120, 130));
      g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
      FontMetrics fm = g2.getFontMetrics();
      g2.drawString(text, cx - fm.stringWidth(text) / 2, y);
    }
  }
}