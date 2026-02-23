package ru.dimension.ui.component.module.adhoc.raw;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.jdesktop.swingx.JXTable;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;

public class GanttFilterPopup {

  private static final int POPUP_HEIGHT = 220;

  private JDialog dialog;
  private final Runnable onCloseCallback;

  public GanttFilterPopup(Runnable onCloseCallback) {
    this.onCloseCallback = onCloseCallback;
  }

  public void show(java.awt.Component owner, JXTable ganttTable,
                   int screenX, int screenY, int width) {
    close();

    JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(owner);
    if (frame == null) return;

    dialog = new JDialog(frame, false);
    dialog.setUndecorated(true);
    dialog.setAlwaysOnTop(false);

    JRootPane rootPane = dialog.getRootPane();
    KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    rootPane.registerKeyboardAction(e -> close(), esc, JComponent.WHEN_IN_FOCUSED_WINDOW);

    JPanel content = new JPanel(new BorderLayout()) {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg = LaF.getBackgroundColor(LafColorGroup.CONFIG_PANEL, LaF.getLafType());
        if (bg == null) {
          bg = UIManager.getColor("Panel.background");
          if (bg == null) bg = Color.WHITE;
        }

        Color border = LaF.getBackgroundColor(LafColorGroup.BORDER, LaF.getLafType());
        if (border == null) {
          border = Color.GRAY;
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        g2.setColor(border);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

        g2.dispose();
      }
    };
    content.setOpaque(false);
    content.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setOpaque(false);
    headerPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));

    JLabel titleLabel = new JLabel("Filter");
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

    // Цвет текста из LaF
    Color titleColor = LaF.getBackgroundColor(LafColorGroup.LABEL_FONT_COLOR, LaF.getLafType());
    if (titleColor == null) titleColor = Color.BLACK;
    titleLabel.setForeground(titleColor);

    headerPanel.add(titleLabel, BorderLayout.CENTER);

    CloseButton closeButton = new CloseButton();
    closeButton.addActionListener(e -> close());
    headerPanel.add(closeButton, BorderLayout.EAST);

    content.add(headerPanel, BorderLayout.NORTH);

    if (ganttTable != null) {
      JScrollPane sp = new JScrollPane(ganttTable);
      sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      Color borderColor = LaF.getBackgroundColor(LafColorGroup.BORDER, LaF.getLafType());
      if (borderColor == null) borderColor = Color.GRAY;
      sp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor));

      Color bg = LaF.getBackgroundColor(LafColorGroup.CHART_PANEL, LaF.getLafType());
      if (bg == null) bg = UIManager.getColor("Panel.background");
      sp.getViewport().setBackground(bg);

      JPanel tableContainer = new JPanel(new BorderLayout());
      tableContainer.setOpaque(false);
      tableContainer.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));
      tableContainer.add(sp);

      content.add(tableContainer, BorderLayout.CENTER);
    }

    dialog.setContentPane(content);

    int popupWidth = Math.max(width, 250);
    int popupY = screenY - POPUP_HEIGHT - 2;
    if (popupY < 0) {
      popupY = 0;
    }

    dialog.setBounds(screenX, popupY, popupWidth, POPUP_HEIGHT);

    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowDeactivated(WindowEvent e) {
      }
    });

    dialog.setVisible(true);
  }

  public void close() {
    if (dialog != null && dialog.isVisible()) {
      dialog.dispose();
      dialog = null;
      if (onCloseCallback != null) {
        onCloseCallback.run();
      }
    }
  }

  public boolean isVisible() {
    return dialog != null && dialog.isVisible();
  }

  public void dispose() {
    if (dialog != null) {
      dialog.dispose();
      dialog = null;
    }
  }

  private static class CloseButton extends JButton {
    private static final int SIZE = 20;
    private boolean hovered = false;

    private final Color crossColorNormal = new Color(200, 90, 90);
    private final Color crossColorHover = new Color(235, 70, 70);

    public CloseButton() {
      setPreferredSize(new Dimension(SIZE, SIZE));
      setContentAreaFilled(false);
      setBorderPainted(false);
      setFocusPainted(false);
      setRolloverEnabled(true);

      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
          hovered = true;
          repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
          hovered = false;
          repaint();
        }
      });
    }

    @Override
    protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      int w = getWidth();
      int h = getHeight();

      if (hovered) {
        g2.setColor(new Color(150, 150, 150, 40));
        g2.fillRoundRect(0, 0, w, h, 6, 6);
      }

      int padding = 6;
      int x1 = padding;
      int x2 = w - padding;
      int y1 = padding;
      int y2 = h - padding;

      g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

      if (hovered) {
        g2.setColor(crossColorHover);
      } else {
        g2.setColor(crossColorNormal);
      }

      g2.drawLine(x1, y1, x2, y2);
      g2.drawLine(x2, y1, x1, y2);

      g2.dispose();
    }
  }
}