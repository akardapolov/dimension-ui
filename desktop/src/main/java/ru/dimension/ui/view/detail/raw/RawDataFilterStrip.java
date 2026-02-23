package ru.dimension.ui.view.detail.raw;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RawDataFilterStrip extends JPanel {

  private static final int STRIP_HEIGHT = 28;
  private static final int BUTTON_H_GAP = 1;
  private static final int OVERFLOW_BUTTON_WIDTH = 36;
  private static final int POPUP_MAX_HEIGHT = 300;
  private static final int POPUP_WIDTH = 220;
  private static final int MIN_PERCENT_BUTTON_WIDTH = 32;

  private final LinkedHashMap<String, Color> seriesColorMap = new LinkedHashMap<>();
  private final LinkedHashMap<String, Double> seriesPercentMap = new LinkedHashMap<>();

  @Getter
  private final LinkedHashSet<String> activeFilters = new LinkedHashSet<>();

  private final List<FilterToggleButton> allButtons = new ArrayList<>();

  private final JPanel buttonsPanel;
  private final JButton overflowButton;
  private final List<FilterToggleButton> overflowButtons = new ArrayList<>();

  private Popup currentPopup;
  private Consumer<Set<String>> filterChangeListener;

  public RawDataFilterStrip() {
    super(new BorderLayout());
    setOpaque(false);
    setBorder(new EmptyBorder(2, 0, 2, 0));

    setPreferredSize(new Dimension(0, STRIP_HEIGHT));
    setMinimumSize(new Dimension(100, STRIP_HEIGHT));
    setMaximumSize(new Dimension(Integer.MAX_VALUE, STRIP_HEIGHT));

    buttonsPanel = new JPanel(null);
    buttonsPanel.setOpaque(false);

    overflowButton = createOverflowButton();
    overflowButton.setVisible(false);

    add(buttonsPanel, BorderLayout.CENTER);
    add(overflowButton, BorderLayout.EAST);

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        relayout();
      }
    });
  }

  public void setFilterChangeListener(Consumer<Set<String>> listener) {
    this.filterChangeListener = listener;
  }

  public void loadSeries(Map<String, Color> seriesColors) {
    loadSeries(seriesColors, null);
  }

  public void loadSeries(Map<String, Color> seriesColors, Map<String, Double> percentages) {
    seriesColorMap.clear();
    seriesPercentMap.clear();
    if (seriesColors != null) {
      seriesColorMap.putAll(seriesColors);
    }
    if (percentages != null) {
      seriesPercentMap.putAll(percentages);
    }

    activeFilters.clear();
    rebuildButtons();
  }

  public void updatePercentages(Map<String, Double> percentages) {
    seriesPercentMap.clear();
    if (percentages != null) {
      seriesPercentMap.putAll(percentages);
    }
    allButtons.forEach(btn -> {
      Double pct = seriesPercentMap.getOrDefault(btn.getSeriesKey(), 0.0);
      btn.setPercent(pct);
      btn.updateTooltip();
    });
    relayout();
  }

  public void updateColors(Map<String, Color> seriesColors) {
    if (seriesColors != null) {
      seriesColorMap.putAll(seriesColors);
    }
    allButtons.forEach(btn -> {
      Color c = seriesColorMap.get(btn.getSeriesKey());
      if (c != null) {
        btn.setSeriesColor(c);
      }
    });
    repaint();
  }

  public boolean hasActiveFilters() {
    return !activeFilters.isEmpty();
  }

  public void clearFilters() {
    activeFilters.clear();
    allButtons.forEach(btn -> btn.setSelected(false));
    fireFilterChanged();
  }

  private void rebuildButtons() {
    allButtons.clear();
    buttonsPanel.removeAll();

    double totalCount = seriesPercentMap.values().stream().mapToDouble(Double::doubleValue).sum();

    for (Map.Entry<String, Color> entry : seriesColorMap.entrySet()) {
      String key = entry.getKey();
      Color color = entry.getValue();
      double pct = 0.0;
      if (totalCount > 0) {
        pct = seriesPercentMap.getOrDefault(key, 0.0);
      } else {
        int size = seriesColorMap.size();
        pct = size > 0 ? 100.0 / size : 0;
      }

      FilterToggleButton btn = new FilterToggleButton(key, color, pct, true);
      btn.setSelected(activeFilters.contains(key));
      btn.addActionListener(e -> onToggle(btn));
      allButtons.add(btn);
    }

    allButtons.sort((a, b) -> Double.compare(b.getPercent(), a.getPercent()));

    relayout();
  }

  private void relayout() {
    buttonsPanel.removeAll();
    overflowButtons.clear();
    dismissPopup();

    if (allButtons.isEmpty()) {
      overflowButton.setVisible(false);
      revalidate();
      repaint();
      return;
    }

    List<FilterToggleButton> sorted = new ArrayList<>(allButtons);
    sorted.sort((a, b) -> Double.compare(b.getPercent(), a.getPercent()));

    Insets insets = getInsets();
    int totalWidth = getWidth() - insets.left - insets.right - 4;
    if (totalWidth <= 0) {
      totalWidth = 400;
    }

    FontMetrics fm = getFontMetrics(getFont().deriveFont(11f));

    double totalPercent = sorted.stream().mapToDouble(FilterToggleButton::getPercent).sum();
    if (totalPercent <= 0) {
      totalPercent = 100.0;
    }

    int totalGaps = Math.max(0, sorted.size() - 1) * BUTTON_H_GAP;
    int availableForButtons = totalWidth - totalGaps;

    int totalMinWidths = 0;
    for (FilterToggleButton btn : sorted) {
      totalMinWidths += btn.computeMinWidth(fm);
    }
    totalMinWidths += totalGaps;

    boolean needOverflow = totalMinWidths > totalWidth;

    if (needOverflow) {
      int overflowReserve = OVERFLOW_BUTTON_WIDTH + BUTTON_H_GAP;
      int availWithOverflow = totalWidth - overflowReserve;

      int usedWidth = 0;
      List<FilterToggleButton> visible = new ArrayList<>();

      for (FilterToggleButton btn : sorted) {
        int minW = btn.computeMinWidth(fm);
        int gapNeeded = visible.isEmpty() ? 0 : BUTTON_H_GAP;
        if (usedWidth + minW + gapNeeded <= availWithOverflow) {
          visible.add(btn);
          usedWidth += minW + gapNeeded;
        } else {
          overflowButtons.add(btn);
        }
      }

      if (visible.isEmpty() && !sorted.isEmpty()) {
        visible.add(sorted.get(0));
        for (int i = 1; i < sorted.size(); i++) {
          overflowButtons.add(sorted.get(i));
        }
      }

      overflowButtons.sort((a, b) -> Double.compare(b.getPercent(), a.getPercent()));

      int visibleGaps = Math.max(0, visible.size() - 1) * BUTTON_H_GAP;
      int remainingWidth = availWithOverflow - visibleGaps;
      double visibleTotalPercent = visible.stream().mapToDouble(FilterToggleButton::getPercent).sum();

      int[] widths = new int[visible.size()];
      int allocatedWidth = 0;

      for (int i = 0; i < visible.size(); i++) {
        FilterToggleButton btn = visible.get(i);
        int minW = btn.computeMinWidth(fm);
        int w;
        if (visibleTotalPercent > 0) {
          w = (int) Math.floor((btn.getPercent() / visibleTotalPercent) * remainingWidth);
        } else {
          w = remainingWidth / visible.size();
        }
        w = Math.max(w, minW);
        widths[i] = w;
        allocatedWidth += w;
      }

      int leftover = remainingWidth - allocatedWidth;
      if (leftover > 0 && !visible.isEmpty()) {
        widths[visible.size() - 1] += leftover;
      } else if (leftover < 0 && !visible.isEmpty()) {
        for (int i = visible.size() - 1; i >= 0 && leftover < 0; i--) {
          int minW = visible.get(i).computeMinWidth(fm);
          int canShrink = widths[i] - minW;
          if (canShrink > 0) {
            int shrink = Math.min(canShrink, -leftover);
            widths[i] -= shrink;
            leftover += shrink;
          }
        }
      }

      int x = 0;
      for (int i = 0; i < visible.size(); i++) {
        FilterToggleButton btn = visible.get(i);
        btn.setShowLabel(false);
        int w = widths[i];
        btn.setPreferredSize(new Dimension(w, STRIP_HEIGHT - 4));
        btn.setBounds(x, 0, w, STRIP_HEIGHT - 4);
        buttonsPanel.add(btn);
        x += w + BUTTON_H_GAP;
      }

      overflowButton.setVisible(true);
      overflowButton.setText("+" + overflowButtons.size());
    } else {
      boolean showLabels = canFitLabels(fm, availableForButtons, totalPercent, sorted);
      sorted.forEach(btn -> btn.setShowLabel(showLabels));

      int[] widths = new int[sorted.size()];
      int allocatedWidth = 0;

      for (int i = 0; i < sorted.size(); i++) {
        FilterToggleButton btn = sorted.get(i);
        double ratio = btn.getPercent() / totalPercent;
        int w = (int) Math.floor(ratio * availableForButtons);
        int minW = btn.computeMinWidth(fm);
        w = Math.max(w, minW);
        widths[i] = w;
        allocatedWidth += w;
      }

      int leftover = availableForButtons - allocatedWidth;
      if (leftover > 0 && !sorted.isEmpty()) {
        widths[sorted.size() - 1] += leftover;
      } else if (leftover < 0 && !sorted.isEmpty()) {
        for (int i = sorted.size() - 1; i >= 0 && leftover < 0; i--) {
          int minW = sorted.get(i).computeMinWidth(fm);
          int canShrink = widths[i] - minW;
          if (canShrink > 0) {
            int shrink = Math.min(canShrink, -leftover);
            widths[i] -= shrink;
            leftover += shrink;
          }
        }
      }

      int x = 0;
      for (int i = 0; i < sorted.size(); i++) {
        FilterToggleButton btn = sorted.get(i);
        int w = widths[i];
        btn.setPreferredSize(new Dimension(w, STRIP_HEIGHT - 4));
        btn.setBounds(x, 0, w, STRIP_HEIGHT - 4);
        buttonsPanel.add(btn);
        x += w + BUTTON_H_GAP;
      }

      overflowButton.setVisible(false);
    }

    int actualContentWidth = 0;
    for (Component c : buttonsPanel.getComponents()) {
      actualContentWidth = Math.max(actualContentWidth, c.getX() + c.getWidth());
    }
    buttonsPanel.setPreferredSize(new Dimension(actualContentWidth, STRIP_HEIGHT - 4));

    revalidate();
    repaint();
  }

  private boolean canFitLabels(FontMetrics fm, int availableWidth, double totalPercent,
                               List<FilterToggleButton> buttons) {
    if (totalPercent <= 0) return false;
    int totalNeeded = 0;
    for (FilterToggleButton btn : buttons) {
      double ratio = btn.getPercent() / totalPercent;
      int proportionalW = (int) Math.round(ratio * availableWidth);
      int fullW = btn.computeFullWidth(fm);
      if (proportionalW < fullW) {
        return false;
      }
      totalNeeded += fullW;
    }
    totalNeeded += (buttons.size() - 1) * BUTTON_H_GAP;
    return totalNeeded <= availableWidth;
  }

  private void onToggle(FilterToggleButton btn) {
    String key = btn.getSeriesKey();
    if (btn.isSelected()) {
      activeFilters.add(key);
    } else {
      activeFilters.remove(key);
    }
    log.debug("Filter toggled: {} = {}, active filters: {}", key, btn.isSelected(), activeFilters);
    fireFilterChanged();
  }

  private void fireFilterChanged() {
    if (filterChangeListener != null) {
      filterChangeListener.accept(new LinkedHashSet<>(activeFilters));
    }
  }

  private JButton createOverflowButton() {
    JButton btn = new JButton("…");
    btn.setMargin(new Insets(2, 4, 2, 4));
    btn.setFocusPainted(false);
    btn.setPreferredSize(new Dimension(OVERFLOW_BUTTON_WIDTH, STRIP_HEIGHT - 4));
    btn.setToolTipText("Show more filters");
    btn.addActionListener(e -> togglePopup());
    return btn;
  }

  private void togglePopup() {
    if (currentPopup != null) {
      dismissPopup();
    } else {
      showPopup();
    }
  }

  private void showPopup() {
    if (overflowButtons.isEmpty()) return;

    JPanel popupContent = new JPanel();
    popupContent.setLayout(new java.awt.GridLayout(0, 1, 2, 2));
    popupContent.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.GRAY, 1),
        BorderFactory.createEmptyBorder(4, 4, 4, 4)
    ));
    popupContent.setBackground(getBackground() != null ? getBackground() : Color.WHITE);

    for (FilterToggleButton original : overflowButtons) {
      FilterToggleButton popupBtn = new FilterToggleButton(
          original.getSeriesKey(), original.getSeriesColor(), original.getPercent(), true);
      popupBtn.setSelected(original.isSelected());
      popupBtn.setPreferredSize(new Dimension(POPUP_WIDTH - 16, 24));
      popupBtn.addActionListener(ev -> {
        original.setSelected(popupBtn.isSelected());
        onToggle(original);
      });
      popupContent.add(popupBtn);
    }

    JScrollPane scrollPane = new JScrollPane(popupContent);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setBorder(null);

    int popupHeight = Math.min(overflowButtons.size() * 28 + 12, POPUP_MAX_HEIGHT);
    scrollPane.setPreferredSize(new Dimension(POPUP_WIDTH, popupHeight));

    java.awt.Point loc = overflowButton.getLocationOnScreen();
    int popupX = loc.x + overflowButton.getWidth() - POPUP_WIDTH;
    int popupY = loc.y + overflowButton.getHeight() + 2;

    PopupFactory factory = PopupFactory.getSharedInstance();
    currentPopup = factory.getPopup(this, scrollPane, popupX, popupY);
    currentPopup.show();

    SwingUtilities.invokeLater(() -> {
      java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
          new ClickAwayListener(), java.awt.AWTEvent.MOUSE_EVENT_MASK);
    });
  }

  private void dismissPopup() {
    if (currentPopup != null) {
      currentPopup.hide();
      currentPopup = null;
    }
  }

  private class ClickAwayListener implements java.awt.event.AWTEventListener {
    @Override
    public void eventDispatched(java.awt.AWTEvent event) {
      if (event instanceof java.awt.event.MouseEvent me) {
        if (me.getID() == java.awt.event.MouseEvent.MOUSE_PRESSED) {
          Component source = me.getComponent();
          if (source != null && !SwingUtilities.isDescendingFrom(source, RawDataFilterStrip.this)) {
            dismissPopup();
            java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(this);
          }
        }
      }
    }
  }
}