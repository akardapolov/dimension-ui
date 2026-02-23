package ru.dimension.ui.component.module.preview.zoom.internal;

import static ru.dimension.ui.laf.LafColorGroup.CONFIG_PANEL;
import static ru.dimension.ui.laf.LafColorGroup.ZOOM_PANEL;
import static ru.dimension.ui.laf.LafColorGroup.ZOOM_PANEL_ACCENT;
import static ru.dimension.ui.laf.LafColorGroup.ZOOM_PANEL_BORDER;
import static ru.dimension.ui.laf.LafColorGroup.ZOOM_PANEL_TEXT;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import lombok.extern.log4j.Log4j2;
import org.jfree.chart.JFreeChart;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.module.chart.PRChartModule;
import ru.dimension.ui.component.module.preview.zoom.RenderMode;
import ru.dimension.ui.component.module.preview.zoom.ViewMode;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TaskInfo;

@Log4j2
public class ZoomCanvasPanel extends JPanel {

  private static final String PREF_TILE_W = "zoom.canvas.tileW";
  private static final String PREF_TILE_H = "zoom.canvas.tileH";
  private static final String PREF_TILES_PER_PAGE = "zoom.canvas.tilesPerPage";
  private static final String PREF_VIEW_MODE = "zoom.canvas.viewMode";
  private static final Preferences prefs = Preferences.userNodeForPackage(ZoomCanvasPanel.class);

  private static final int DEFAULT_TILE_W = 800;
  private static final int DEFAULT_TILE_H = 300;
  private static final int TILE_W_MIN = 400;
  private static final int TILE_W_MAX = 3200;
  private static final int TILE_H_MIN = 150;
  private static final int TILE_H_MAX = 1200;
  private static final int DEFAULT_TILES_PER_PAGE = 10;
  private static final int SLIDER_PANEL_SIZE = 36;
  private static final int STATUS_BAR_H = 35;
  private static final int HOVER_INFO_H = 50;
  private static final int GAP = 20;
  private static final int HEADER_H = 80;
  private static final int HEADER_GAP = 10;
  private static final double ANIM_SPEED = 0.03;
  private static final double FOCUS_FILL_RATIO = 0.75;
  private static final int DIRTY_CHECK_MS = 300;
  private static final int SLIDER_DEBOUNCE_MS = 300;
  private static final int ZOOM_IDLE_MS = 300;

  private static final int PLAIN_HEADER_MIN_W = 160;
  private static final int PLAIN_HEADER_MAX_W = 900;
  private static final Font HEADER_MEASURE_FONT = new Font("SansSerif", Font.BOLD, 44);

  private static final Color DEFAULT_BG = new Color(20, 20, 25);
  private static final Color DEFAULT_TOOLBAR_BG = new Color(30, 30, 38);
  private static final Color DEFAULT_ZOOM_PANEL_BG = new Color(38, 40, 48);
  private static final Color DEFAULT_ZOOM_BORDER = new Color(72, 78, 96);
  private static final Color DEFAULT_ZOOM_TEXT = new Color(195, 205, 225);
  private static final Color DEFAULT_ZOOM_ACCENT = new Color(110, 180, 245);

  private static final Color PROFILE_BG = new Color(40, 55, 80);
  private static final Color PROFILE_BG_HL = new Color(55, 75, 115);
  private static final Color PROFILE_BORDER = new Color(60, 100, 160);
  private static final Color PROFILE_BORDER_HL = new Color(90, 150, 230);
  private static final Color PROFILE_TEXT = new Color(150, 190, 240);
  private static final Color PROFILE_TEXT_HL = new Color(210, 230, 255);

  private static final Color TASK_BG = new Color(40, 65, 55);
  private static final Color TASK_BG_HL = new Color(55, 90, 75);
  private static final Color TASK_BORDER = new Color(60, 130, 90);
  private static final Color TASK_BORDER_HL = new Color(90, 180, 130);
  private static final Color TASK_TEXT = new Color(150, 220, 180);
  private static final Color TASK_TEXT_HL = new Color(210, 255, 230);

  private static final Color QUERY_BG = new Color(55, 40, 70);
  private static final Color QUERY_BG_HL = new Color(75, 55, 95);
  private static final Color QUERY_BORDER = new Color(100, 60, 140);
  private static final Color QUERY_BORDER_HL = new Color(150, 90, 210);
  private static final Color QUERY_TEXT = new Color(190, 150, 230);
  private static final Color QUERY_TEXT_HL = new Color(230, 210, 255);

  private ViewMode viewMode;
  private int tilesPerPage;
  private int currentTabIndex = 0;
  private JPanel centerWrapper;
  private JComponent tabBarWrapper;
  private final List<JButton> tabButtons = new ArrayList<>();
  private JComboBox<String> pageSizeCombo;
  private JRadioButton plainRadio, tilesRadio;

  private final RenderMode renderMode;
  private final ProfileManager profileManager;
  private final List<TileEntry> allTiles = new ArrayList<>();
  private final List<DisplayItem> displayItems = new ArrayList<>();
  private final ExecutorService snapshotExecutor;
  private final Runnable onCloseCallback;

  private double viewX = 0, viewY = 0;
  private double scale = 1.0;

  private int lastMouseX = -1, lastMouseY = -1;
  private boolean panning = false;
  private int hoveredChartIndex = -1;
  private int hoveredHeaderIndex = -1;
  private volatile boolean interacting = false;

  private boolean animating = false;
  private double animStartViewX, animStartViewY, animStartScale;
  private double animTargetViewX, animTargetViewY, animTargetScale;
  private double animProgress = 0.0;

  private int focusedItemIndex = -1;
  private double preFocusViewX, preFocusViewY, preFocusScale;
  private boolean hasSavedPreFocus = false;

  private int tileW, tileH;
  private int snapshotW, snapshotH;
  private boolean initialFitDone = false;
  private int plainCols = 2;

  private final Timer renderTimer;
  private final Timer dirtyCheckTimer;
  private Timer sliderDebounceTimer;
  private Timer zoomIdleTimer;

  private JButton refreshButton;
  private JCheckBox autoRefreshCheckbox;
  private JComboBox<String> intervalCombo;
  private Timer autoRefreshTimer;
  private boolean autoRefreshEnabled = true;
  private int autoRefreshPeriodMs = 5000;
  private boolean autoRunInFlight = false;
  private int pendingRunTasks = 0;
  private long lastRepaintTime = 0;

  private final JSlider widthSlider;
  private final JSlider heightSlider;
  private final CanvasArea canvasArea;

  private final Color panelBgColor;
  private final Color toolbarBgColor;
  private final Color zoomPanelBg;
  private final Color zoomBorderColor;
  private final Color zoomTextColor;
  private final Color zoomAccentColor;

  public ZoomCanvasPanel(
      ConcurrentMap<ProfileTaskQueryKey, ConcurrentMap<CProfile, PRChartModule>> chartPanes,
      Runnable onCloseCallback,
      RenderMode renderMode,
      ProfileManager profileManager) {

    this.onCloseCallback = onCloseCallback;
    this.renderMode = renderMode;
    this.profileManager = profileManager;
    this.snapshotExecutor = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

    Color configPanelColor = LaF.getBackgroundColor(CONFIG_PANEL, LaF.getLafType());
    this.panelBgColor = configPanelColor != null ? configPanelColor : DEFAULT_BG;
    this.toolbarBgColor = configPanelColor != null ? configPanelColor : DEFAULT_TOOLBAR_BG;

    Color zpBg = LaF.getBackgroundColor(ZOOM_PANEL, LaF.getLafType());
    this.zoomPanelBg = zpBg != null ? zpBg : DEFAULT_ZOOM_PANEL_BG;

    Color zpBorder = LaF.getBackgroundColor(ZOOM_PANEL_BORDER, LaF.getLafType());
    this.zoomBorderColor = zpBorder != null ? zpBorder : DEFAULT_ZOOM_BORDER;

    Color zpText = LaF.getBackgroundColor(ZOOM_PANEL_TEXT, LaF.getLafType());
    this.zoomTextColor = zpText != null ? zpText : DEFAULT_ZOOM_TEXT;

    Color zpAccent = LaF.getBackgroundColor(ZOOM_PANEL_ACCENT, LaF.getLafType());
    this.zoomAccentColor = zpAccent != null ? zpAccent : DEFAULT_ZOOM_ACCENT;

    setLayout(new BorderLayout());
    setBackground(panelBgColor);

    tileW = clamp(prefs.getInt(PREF_TILE_W, DEFAULT_TILE_W), TILE_W_MIN, TILE_W_MAX);
    tileH = clamp(prefs.getInt(PREF_TILE_H, DEFAULT_TILE_H), TILE_H_MIN, TILE_H_MAX);
    tilesPerPage = sanitizePageSize(prefs.getInt(PREF_TILES_PER_PAGE, DEFAULT_TILES_PER_PAGE));
    viewMode = loadViewMode();
    snapshotW = tileW;
    snapshotH = tileH;

    canvasArea = new CanvasArea();
    widthSlider = createWidthSlider();
    heightSlider = createHeightSlider();

    buildLayoutPanels();
    rebuildTiles(chartPanes);

    canvasArea.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        if (!initialFitDone && canvasArea.getWidth() > 0
            && canvasArea.getHeight() > 0 && !allTiles.isEmpty()) {
          rebuildDisplayForCurrentMode();
          fitAllToView();
          initialFitDone = true;
          if (renderMode == RenderMode.HYBRID) {
            SwingUtilities.invokeLater(() -> startManagedRun());
          }
        } else if (viewMode == ViewMode.PLAIN && canvasArea.getWidth() > 0 && canvasArea.getHeight() > 0) {
          rebuildDisplayForCurrentMode();
          canvasArea.repaint();
        }
      }
    });

    renderTimer = new Timer(16, e -> tickAnimation());
    renderTimer.start();

    dirtyCheckTimer = new Timer(DIRTY_CHECK_MS, e -> processDirtyTiles());
    if (renderMode == RenderMode.CURRENT) {
      dirtyCheckTimer.start();
    }

    if (renderMode == RenderMode.HYBRID && autoRefreshEnabled) {
      restartAutoTimer();
    }
  }

  private ViewMode loadViewMode() {
    String s = prefs.get(PREF_VIEW_MODE, ViewMode.TILES.name());
    try {
      return ViewMode.valueOf(s);
    } catch (Exception e) {
      return ViewMode.TILES;
    }
  }

  private boolean needsTabs() {
    return allTiles.size() > tilesPerPage;
  }

  private int getTotalTabs() {
    if (allTiles.isEmpty()) return 1;
    return (int) Math.ceil((double) allTiles.size() / tilesPerPage);
  }

  private int getPageStart() {
    return currentTabIndex * tilesPerPage;
  }

  private int getPageEnd() {
    return Math.min((currentTabIndex + 1) * tilesPerPage, allTiles.size());
  }

  private int getVisibleTileCount() {
    return getPageEnd() - getPageStart();
  }

  private static int sanitizePageSize(int val) {
    if (val == 30 || val == 50) return val;
    return DEFAULT_TILES_PER_PAGE;
  }

  private int pageSizeToComboIndex(int ps) {
    return switch (ps) { case 30 -> 1; case 50 -> 2; default -> 0; };
  }

  private void buildLayoutPanels() {
    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.setBackground(toolbarBgColor);
    topPanel.setPreferredSize(new Dimension(0, SLIDER_PANEL_SIZE));

    JLabel widthLabel = new JLabel(" W:");
    widthLabel.setForeground(zoomTextColor);
    widthLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

    JPanel widthPanel = new JPanel(new BorderLayout(4, 0));
    widthPanel.setBackground(toolbarBgColor);
    widthPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    widthPanel.add(widthLabel, BorderLayout.WEST);
    widthPanel.add(widthSlider, BorderLayout.CENTER);

    JPanel cornerFiller = new JPanel();
    cornerFiller.setBackground(toolbarBgColor);
    cornerFiller.setPreferredSize(new Dimension(SLIDER_PANEL_SIZE, SLIDER_PANEL_SIZE));

    topPanel.add(cornerFiller, BorderLayout.WEST);
    topPanel.add(widthPanel, BorderLayout.CENTER);
    topPanel.add(buildRightToolbar(), BorderLayout.EAST);

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.setBackground(toolbarBgColor);
    leftPanel.setPreferredSize(new Dimension(SLIDER_PANEL_SIZE, 0));

    JLabel heightLabel = new JLabel("H");
    heightLabel.setForeground(zoomTextColor);
    heightLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
    heightLabel.setHorizontalAlignment(SwingConstants.CENTER);

    JPanel heightPanel = new JPanel(new BorderLayout(0, 4));
    heightPanel.setBackground(toolbarBgColor);
    heightPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    heightPanel.add(heightLabel, BorderLayout.NORTH);
    heightPanel.add(heightSlider, BorderLayout.CENTER);

    leftPanel.add(heightPanel, BorderLayout.CENTER);

    centerWrapper = new JPanel(new BorderLayout());
    centerWrapper.setBackground(panelBgColor);
    centerWrapper.add(canvasArea, BorderLayout.CENTER);

    add(topPanel, BorderLayout.NORTH);
    add(leftPanel, BorderLayout.WEST);
    add(centerWrapper, BorderLayout.CENTER);
  }

  private JPanel buildRightToolbar() {
    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
    toolbar.setBackground(toolbarBgColor);

    JLabel viewLabel = new JLabel("View:");
    viewLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
    viewLabel.setForeground(zoomTextColor);

    plainRadio = makeViewRadio("Plain", ViewMode.PLAIN);
    tilesRadio = makeViewRadio("Tiles", ViewMode.TILES);

    ButtonGroup viewGroup = new ButtonGroup();
    viewGroup.add(plainRadio);
    viewGroup.add(tilesRadio);

    switch (viewMode) {
      case PLAIN -> plainRadio.setSelected(true);
      default -> tilesRadio.setSelected(true);
    }

    toolbar.add(viewLabel);
    toolbar.add(plainRadio);
    toolbar.add(tilesRadio);

    toolbar.add(Box.createHorizontalStrut(8));
    toolbar.add(makeSep());
    toolbar.add(Box.createHorizontalStrut(6));

    JLabel pageLabel = new JLabel("Per page:");
    pageLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
    pageLabel.setForeground(zoomTextColor);

    pageSizeCombo = new JComboBox<>(new String[]{"10", "30", "50"});
    pageSizeCombo.setSelectedIndex(pageSizeToComboIndex(tilesPerPage));
    pageSizeCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));
    pageSizeCombo.setBackground(new Color(40, 40, 48));
    pageSizeCombo.setForeground(new Color(200, 200, 220));
    pageSizeCombo.setFocusable(false);
    pageSizeCombo.addActionListener(e -> onPageSizeChanged());

    toolbar.add(pageLabel);
    toolbar.add(pageSizeCombo);

    if (renderMode == RenderMode.HYBRID) {
      toolbar.add(Box.createHorizontalStrut(8));
      toolbar.add(makeSep());
      toolbar.add(Box.createHorizontalStrut(6));

      JLabel ml = new JLabel("\u2744 HYBRID");
      ml.setFont(new Font("SansSerif", Font.BOLD, 11));
      ml.setForeground(zoomAccentColor);

      refreshButton = new JButton("\u27F3 Refresh");
      refreshButton.setFont(new Font("SansSerif", Font.BOLD, 11));
      refreshButton.setBackground(new Color(50, 120, 180));
      refreshButton.setForeground(Color.WHITE);
      refreshButton.setFocusPainted(false);
      refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      refreshButton.addActionListener(e -> {
        if (!autoRunInFlight) startManagedRun();
        canvasArea.requestFocusInWindow();
      });

      autoRefreshCheckbox = new JCheckBox("Auto");
      autoRefreshCheckbox.setFont(new Font("SansSerif", Font.PLAIN, 11));
      autoRefreshCheckbox.setForeground(zoomTextColor);
      autoRefreshCheckbox.setBackground(toolbarBgColor);
      autoRefreshCheckbox.setFocusPainted(false);
      autoRefreshCheckbox.setSelected(true);
      autoRefreshCheckbox.addActionListener(e -> {
        toggleAutoRefresh();
        canvasArea.requestFocusInWindow();
      });

      intervalCombo = new JComboBox<>(new String[]{"5s", "10s", "20s"});
      intervalCombo.setSelectedIndex(0);
      intervalCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));
      intervalCombo.setBackground(new Color(40, 40, 48));
      intervalCombo.setForeground(new Color(200, 200, 220));
      intervalCombo.setFocusable(false);
      intervalCombo.addActionListener(e -> {
        String sel = (String) intervalCombo.getSelectedItem();
        if ("10s".equals(sel)) autoRefreshPeriodMs = 10000;
        else if ("20s".equals(sel)) autoRefreshPeriodMs = 20000;
        else autoRefreshPeriodMs = 5000;
        if (autoRefreshEnabled) restartAutoTimer();
        canvasArea.requestFocusInWindow();
      });

      toolbar.add(ml);
      toolbar.add(Box.createHorizontalStrut(8));
      toolbar.add(autoRefreshCheckbox);
      toolbar.add(intervalCombo);
      toolbar.add(refreshButton);
    }

    return toolbar;
  }

  private JRadioButton makeViewRadio(String label, ViewMode mode) {
    JRadioButton rb = new JRadioButton(label);
    rb.setFont(new Font("SansSerif", Font.PLAIN, 11));
    rb.setForeground(zoomTextColor);
    rb.setBackground(toolbarBgColor);
    rb.setFocusPainted(false);
    rb.addActionListener(e -> {
      if (viewMode != mode) {
        viewMode = mode;
        prefs.put(PREF_VIEW_MODE, mode.name());
        onViewModeChanged();
      }
      canvasArea.requestFocusInWindow();
    });
    return rb;
  }

  private JLabel makeSep() {
    JLabel s = new JLabel("|");
    s.setForeground(zoomBorderColor);
    s.setFont(new Font("SansSerif", Font.PLAIN, 14));
    return s;
  }

  private void onViewModeChanged() {
    focusedItemIndex = -1;
    hoveredChartIndex = -1;
    hoveredHeaderIndex = -1;
    hasSavedPreFocus = false;
    animating = false;

    buildTabBar();
    rebuildDisplayForCurrentMode();

    if (canvasArea.getWidth() > 0 && canvasArea.getHeight() > 0 && !allTiles.isEmpty()) {
      fitAllToView();
    }

    if (renderMode == RenderMode.HYBRID) {
      SwingUtilities.invokeLater(this::startManagedRun);
    }
    canvasArea.repaint();
  }

  private void onPageSizeChanged() {
    String sel = (String) pageSizeCombo.getSelectedItem();
    int ns = switch (sel) { case "30" -> 30; case "50" -> 50; default -> 10; };
    if (ns == tilesPerPage) return;

    for (TileEntry e : allTiles) { e.clearVolatile(); e.bufferedSnapshot = null; e.dirty = true; }

    tilesPerPage = ns;
    currentTabIndex = 0;
    focusedItemIndex = -1;
    hoveredChartIndex = -1;
    hoveredHeaderIndex = -1;
    hasSavedPreFocus = false;
    animating = false;
    prefs.putInt(PREF_TILES_PER_PAGE, tilesPerPage);

    buildTabBar();
    rebuildDisplayForCurrentMode();

    if (canvasArea.getWidth() > 0 && canvasArea.getHeight() > 0 && !allTiles.isEmpty()) {
      fitAllToView();
    }
    if (renderMode == RenderMode.HYBRID) {
      SwingUtilities.invokeLater(this::startManagedRun);
    }
    canvasArea.requestFocusInWindow();
    canvasArea.repaint();
  }

  private void buildTabBar() {
    if (tabBarWrapper != null) {
      centerWrapper.remove(tabBarWrapper);
      tabBarWrapper = null;
      tabButtons.clear();
    }
    if (!needsTabs()) {
      centerWrapper.revalidate();
      centerWrapper.repaint();
      return;
    }
    int total = getTotalTabs();
    JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 4));
    tabPanel.setBackground(zoomPanelBg);
    for (int i = 0; i < total; i++) {
      int rs = i * tilesPerPage + 1;
      int re = Math.min((i + 1) * tilesPerPage, allTiles.size());
      JButton btn = new JButton((i + 1) + " (" + rs + "\u2013" + re + ")");
      btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
      btn.setFocusPainted(false);
      btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      btn.setMargin(new Insets(4, 10, 4, 10));
      btn.setBorderPainted(false);
      final int idx = i;
      btn.addActionListener(e -> { switchToTab(idx); canvasArea.requestFocusInWindow(); });
      tabButtons.add(btn);
      tabPanel.add(btn);
    }
    JScrollPane sp = new JScrollPane(tabPanel,
                                     JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    sp.setPreferredSize(new Dimension(0, 38));
    sp.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, zoomBorderColor));
    sp.getViewport().setBackground(zoomPanelBg);
    tabBarWrapper = sp;
    centerWrapper.add(tabBarWrapper, BorderLayout.NORTH);
    updateTabBarSelection();
    centerWrapper.revalidate();
    centerWrapper.repaint();
  }

  private void updateTabBarSelection() {
    for (int i = 0; i < tabButtons.size(); i++) {
      JButton b = tabButtons.get(i);
      if (i == currentTabIndex) {
        b.setBackground(zoomAccentColor);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
      } else {
        b.setBackground(new Color(45, 45, 55));
        b.setForeground(zoomTextColor);
        b.setFont(b.getFont().deriveFont(Font.PLAIN));
      }
    }
  }

  private void switchToTab(int idx) {
    if (idx < 0 || idx >= getTotalTabs() || idx == currentTabIndex) return;
    currentTabIndex = idx;
    focusedItemIndex = -1;
    hoveredChartIndex = -1;
    hoveredHeaderIndex = -1;
    hasSavedPreFocus = false;
    animating = false;
    rebuildDisplayForCurrentMode();
    if (canvasArea.getWidth() > 0 && canvasArea.getHeight() > 0) fitAllToView();
    updateTabBarSelection();
    if (renderMode == RenderMode.HYBRID) startManagedRun();
    canvasArea.repaint();
  }

  private void restartAutoTimer() {
    if (autoRefreshTimer != null) autoRefreshTimer.stop();
    autoRefreshTimer = new Timer(autoRefreshPeriodMs, e -> {
      if (!autoRunInFlight && !interacting) startManagedRun();
    });
    autoRefreshTimer.start();
  }

  private void toggleAutoRefresh() {
    autoRefreshEnabled = autoRefreshCheckbox.isSelected();
    if (autoRefreshEnabled) restartAutoTimer();
    else if (autoRefreshTimer != null) autoRefreshTimer.stop();
  }

  private void startManagedRun() {
    if (renderMode != RenderMode.HYBRID || autoRunInFlight) return;
    int cw = canvasArea.getWidth(), ch = canvasArea.getHeight();
    if (cw <= 0 || ch <= 0) return;
    autoRunInFlight = true;
    pendingRunTasks = 0;
    for (DisplayItem item : displayItems) {
      if (item.type != DType.CHART) continue;
      item.tile.dirty = true;
      if (isVisible(item, cw, ch) && !item.tile.renderInFlight) {
        pendingRunTasks++;
        renderSnapshotAsync(item.tile, this::onRunTaskFinished);
      }
    }
    if (pendingRunTasks == 0) autoRunInFlight = false;
  }

  private void onRunTaskFinished() {
    pendingRunTasks = Math.max(0, pendingRunTasks - 1);
    if (pendingRunTasks == 0) autoRunInFlight = false;
  }

  public void rebuildTiles(
      ConcurrentMap<ProfileTaskQueryKey, ConcurrentMap<CProfile, PRChartModule>> chartPanes) {
    clearTiles();
    currentTabIndex = 0;
    focusedItemIndex = -1;
    hoveredChartIndex = -1;
    hoveredHeaderIndex = -1;
    hasSavedPreFocus = false;
    animating = false;
    initialFitDone = false;

    if (chartPanes != null && !chartPanes.isEmpty()) {
      chartPanes.forEach((key, map) -> map.forEach((cp, mod) ->
                                                       allTiles.add(new TileEntry(mod, key, cp, buildTitle(cp),
                                                                                  key.getProfileId(), resolveName(key.getProfileId(), 'P'),
                                                                                  key.getTaskId(), resolveName(key.getTaskId(), 'T'),
                                                                                  key.getQueryId(), resolveName(key.getQueryId(), 'Q'),
                                                                                  resolveColName(cp), resolveDataType(cp)))));

      allTiles.sort(Comparator.comparingInt((TileEntry t) -> t.profileId)
                        .thenComparingInt(t -> t.taskId)
                        .thenComparingInt(t -> t.queryId)
                        .thenComparing(t -> t.title != null ? t.title : ""));
    }

    buildTabBar();
    rebuildDisplayForCurrentMode();

    if (canvasArea.getWidth() > 0 && canvasArea.getHeight() > 0 && !allTiles.isEmpty()) {
      fitAllToView();
      initialFitDone = true;
      if (renderMode == RenderMode.HYBRID) SwingUtilities.invokeLater(this::startManagedRun);
    }
    canvasArea.repaint();
  }

  private void rebuildDisplayForCurrentMode() {
    displayItems.clear();
    switch (viewMode) {
      case PLAIN -> buildPlainLayout();
      default -> buildTilesLayout();
    }
  }

  private double plainChartsStartY() {
    return GAP + 3.0 * (HEADER_H + HEADER_GAP);
  }

  private void computeNodeWidths(Map<Integer, PNode> pm) {
    for (PNode p : pm.values()) {
      for (TNode t : p.tasks.values()) {
        for (QNode q : t.queries.values()) {
          q.width = Math.max(PLAIN_HEADER_MIN_W, Math.min(PLAIN_HEADER_MAX_W, measureHeaderWidth(q.name)));
        }
        double children = 0;
        int k = 0;
        for (QNode q : t.queries.values()) {
          if (k++ > 0) children += GAP;
          children += q.width;
        }
        t.width = Math.max(measureHeaderWidth(t.name), Math.max(PLAIN_HEADER_MIN_W, Math.min(PLAIN_HEADER_MAX_W, children)));
      }
      double children = 0;
      int k = 0;
      for (TNode t : p.tasks.values()) {
        if (k++ > 0) children += GAP;
        children += t.width;
      }
      p.width = Math.max(measureHeaderWidth(p.name), Math.max(PLAIN_HEADER_MIN_W, Math.min(PLAIN_HEADER_MAX_W * 2.0, children)));
    }
  }

  private void buildPlainLayout() {
    int start = getPageStart(), end = getPageEnd();
    if (start >= end) return;

    List<TileEntry> page = new ArrayList<>(allTiles.subList(start, end));
    Map<Integer, PNode> pm = new LinkedHashMap<>();
    for (TileEntry t : page) {
      PNode p = pm.computeIfAbsent(t.profileId, k -> new PNode(t.profileId, t.profileName));
      TNode tn = p.tasks.computeIfAbsent(t.taskId, k -> new TNode(t.taskId, t.taskName));
      QNode qn = tn.queries.computeIfAbsent(t.queryId, k -> new QNode(t.queryId, t.queryName));
      qn.tiles.add(t);
    }

    computeNodeWidths(pm);

    double profileY = GAP;
    double taskY = profileY + HEADER_H + HEADER_GAP;
    double queryY = taskY + HEADER_H + HEADER_GAP;
    double chartsY = queryY + HEADER_H + HEADER_GAP;

    double cx = GAP;
    for (PNode p : pm.values()) {
      double pStart = cx;
      for (TNode tn : p.tasks.values()) {
        double tStart = cx;
        for (QNode qn : tn.queries.values()) {
          addItem(DType.QUERY_HEADER, p.id, tn.id, qn.id, qn.name, null, cx, queryY, qn.width, HEADER_H);
          cx += qn.width + GAP;
        }
        double tw = Math.max(tn.width, Math.max(cx - tStart - GAP, PLAIN_HEADER_MIN_W));
        addItem(DType.TASK_HEADER, p.id, tn.id, -1, tn.name, null, tStart, taskY, tw, HEADER_H);
      }
      double pw = Math.max(p.width, Math.max(cx - pStart - GAP, PLAIN_HEADER_MIN_W));
      addItem(DType.PROFILE_HEADER, p.id, -1, -1, p.name, null, pStart, profileY, pw, HEADER_H);
    }

    int n = end - start;
    int cw = canvasArea.getWidth(), ch = canvasArea.getHeight();
    if (cw > 0 && ch > 0) {
      double headersW = Math.max(PLAIN_HEADER_MIN_W, cx);
      plainCols = computeOptimalColsPlain(n, cw, ch, headersW, chartsY);
    }

    for (int i = start; i < end; i++) {
      int li = i - start;
      int col = li % plainCols;
      int row = li / plainCols;
      DisplayItem di = new DisplayItem();
      di.type = DType.CHART;
      TileEntry te = allTiles.get(i);
      di.profileId = te.profileId;
      di.taskId = te.taskId;
      di.queryId = te.queryId;
      di.label = te.title;
      di.tile = te;
      di.rect = new Rectangle2D.Double(
          col * (tileW + GAP) + GAP, chartsY + row * (tileH + GAP), tileW, tileH);
      displayItems.add(di);
    }
  }

  private int computeOptimalColsPlain(int n, int cw, int ch, double headerW, double chartsStartY) {
    if (n <= 0) return 1;
    int best = 1;
    double bestS = 0;
    double availH = ch - STATUS_BAR_H - HOVER_INFO_H;
    for (int c = 1; c <= n; c++) {
      int rows = (int) Math.ceil((double) n / c);

      double gridW = c * tileW + (c + 1) * GAP;
      double gridH = rows * tileH + Math.max(0, rows - 1) * GAP;

      double contentW = Math.max(headerW, gridW) + GAP * 2;
      double contentH = chartsStartY + gridH + GAP;

      double s = Math.min((double) cw / contentW, availH / contentH);
      if (s > bestS) {
        bestS = s;
        best = c;
      }
    }
    return best;
  }

  private void buildTilesLayout() {
    int start = getPageStart(), end = getPageEnd();
    if (start >= end) return;

    List<TileEntry> page = new ArrayList<>(allTiles.subList(start, end));
    Map<Integer, PNode> pm = new LinkedHashMap<>();
    for (TileEntry t : page) {
      PNode p = pm.computeIfAbsent(t.profileId, k -> new PNode(t.profileId, t.profileName));
      TNode tn = p.tasks.computeIfAbsent(t.taskId, k -> new TNode(t.taskId, t.taskName));
      QNode qn = tn.queries.computeIfAbsent(t.queryId, k -> new QNode(t.queryId, t.queryName));
      qn.tiles.add(t);
    }

    double profileY = GAP;
    double taskY = profileY + HEADER_H + HEADER_GAP;
    double queryY = taskY + HEADER_H + HEADER_GAP;
    double chartsY = queryY + HEADER_H + HEADER_GAP;
    double cx = GAP;

    for (PNode p : pm.values()) {
      double pStart = cx;
      for (TNode tn : p.tasks.values()) {
        double tStart = cx;
        for (QNode qn : tn.queries.values()) {
          double qStart = cx;
          int cnt = qn.tiles.size();
          double qw = Math.max(tileW, 120);
          addItem(DType.QUERY_HEADER, p.id, tn.id, qn.id, qn.name, null, qStart, queryY, qw, HEADER_H);
          for (int i = 0; i < cnt; i++) {
            double cy = chartsY + i * (tileH + GAP);
            addItem(DType.CHART, p.id, tn.id, qn.id, qn.tiles.get(i).title, qn.tiles.get(i),
                    qStart, cy, tileW, tileH);
          }
          cx += qw + GAP;
        }
        double tw = Math.max(cx - tStart - GAP, 80);
        addItem(DType.TASK_HEADER, p.id, tn.id, -1, tn.name, null, tStart, taskY, tw, HEADER_H);
      }
      double pw = Math.max(cx - pStart - GAP, 80);
      addItem(DType.PROFILE_HEADER, p.id, -1, -1, p.name, null, pStart, profileY, pw, HEADER_H);
    }
  }

  private void addItem(DType type, int pid, int tid, int qid,
                       String label, TileEntry tile,
                       double x, double y, double w, double h) {
    DisplayItem di = new DisplayItem();
    di.type = type;
    di.profileId = pid;
    di.taskId = tid;
    di.queryId = qid;
    di.label = label;
    di.tile = tile;
    di.rect = new Rectangle2D.Double(x, y, w, h);
    displayItems.add(di);
  }

  public void markDirty(ProfileTaskQueryKey key) {
    for (TileEntry e : allTiles) if (key.equals(e.key)) e.dirty = true;
  }

  private void markAllDirty() {
    for (TileEntry e : allTiles) e.dirty = true;
  }

  public int getTileCount() { return allTiles.size(); }

  private void clearTiles() {
    for (TileEntry e : allTiles) e.clearVolatile();
    allTiles.clear();
    displayItems.clear();
  }

  private void fitAllToView() {
    if (displayItems.isEmpty()) return;
    int cw = canvasArea.getWidth(), ch = canvasArea.getHeight();
    if (cw <= 0 || ch <= 0) return;

    double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

    for (DisplayItem it : displayItems) {
      if (it.rect == null) continue;
      minX = Math.min(minX, it.rect.x);
      minY = Math.min(minY, it.rect.y);
      maxX = Math.max(maxX, it.rect.x + it.rect.width);
      maxY = Math.max(maxY, it.rect.y + it.rect.height);
    }
    if (minX == Double.MAX_VALUE) return;

    double contentW = maxX - minX + GAP * 2;
    double contentH = maxY - minY + GAP * 2;
    double sw = cw / contentW;
    double sh = (ch - STATUS_BAR_H - HOVER_INFO_H) / contentH;
    double fs = Math.max(0.05, Math.min(5.0, Math.min(sw, sh)));

    scale = fs;
    viewX = (cw - contentW * fs) / 2.0 - minX * fs + GAP * fs;
    viewY = HOVER_INFO_H + ((ch - STATUS_BAR_H - HOVER_INFO_H) - contentH * fs) / 2.0 - minY * fs + GAP * fs;
    canvasArea.repaint();
  }

  private void animateFitAll() {
    if (displayItems.isEmpty()) return;
    int cw = canvasArea.getWidth(), ch = canvasArea.getHeight();
    if (cw <= 0 || ch <= 0) return;

    double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
    for (DisplayItem it : displayItems) {
      if (it.rect == null) continue;
      minX = Math.min(minX, it.rect.x);
      minY = Math.min(minY, it.rect.y);
      maxX = Math.max(maxX, it.rect.x + it.rect.width);
      maxY = Math.max(maxY, it.rect.y + it.rect.height);
    }
    if (minX == Double.MAX_VALUE) return;
    double cW = maxX - minX + GAP * 2, cH = maxY - minY + GAP * 2;
    double fs = Math.max(0.05, Math.min(5.0, Math.min(cw / cW, (ch - STATUS_BAR_H - HOVER_INFO_H) / cH)));
    double tvx = (cw - cW * fs) / 2.0 - minX * fs + GAP * fs;
    double tvy = HOVER_INFO_H + ((ch - STATUS_BAR_H - HOVER_INFO_H) - cH * fs) / 2.0 - minY * fs + GAP * fs;
    startCameraAnimation(tvx, tvy, fs);
  }

  private void focusOnItem(int idx) {
    if (idx < 0 || idx >= displayItems.size()) return;
    DisplayItem it = displayItems.get(idx);
    if (it.type != DType.CHART || it.rect == null) return;
    int cw = canvasArea.getWidth(), ch = canvasArea.getHeight();
    if (cw <= 0 || ch <= 0) return;
    if (focusedItemIndex < 0) {
      preFocusViewX = viewX; preFocusViewY = viewY; preFocusScale = scale;
      hasSavedPreFocus = true;
    }
    Rectangle2D.Double r = it.rect;
    double ts = Math.max(0.1, Math.min(5.0, Math.min(
        cw * FOCUS_FILL_RATIO / r.width, (ch - HOVER_INFO_H) * FOCUS_FILL_RATIO / r.height)));
    focusedItemIndex = idx;
    startCameraAnimation(cw / 2.0 - (r.x + r.width / 2.0) * ts,
                         (ch + HOVER_INFO_H) / 2.0 - (r.y + r.height / 2.0) * ts, ts);
  }

  private void unfocus() {
    if (!hasSavedPreFocus) return;
    focusedItemIndex = -1;
    startCameraAnimation(preFocusViewX, preFocusViewY, preFocusScale);
    hasSavedPreFocus = false;
  }

  private void startCameraAnimation(double tvx, double tvy, double ts) {
    animStartViewX = viewX; animStartViewY = viewY; animStartScale = scale;
    animTargetViewX = tvx; animTargetViewY = tvy; animTargetScale = ts;
    animProgress = 0.0; animating = true;
  }

  private void tickAnimation() {
    if (animating) {
      animProgress = Math.min(1.0, animProgress + ANIM_SPEED);
      double t = animProgress < 0.5 ? 4 * animProgress * animProgress * animProgress
          : 1 - Math.pow(-2 * animProgress + 2, 3) / 2;
      viewX = animStartViewX + (animTargetViewX - animStartViewX) * t;
      viewY = animStartViewY + (animTargetViewY - animStartViewY) * t;
      scale = animStartScale + (animTargetScale - animStartScale) * t;
      canvasArea.repaint();
      if (animProgress >= 1.0) animating = false;
    } else if (renderMode == RenderMode.HYBRID) {
      long now = System.currentTimeMillis();
      if (now - lastRepaintTime > 1000) { lastRepaintTime = now; canvasArea.repaint(); }
    }
  }

  private void processDirtyTiles() {
    if (interacting) return;
    int cw = canvasArea.getWidth(), ch = canvasArea.getHeight();
    if (cw <= 0 || ch <= 0) return;
    for (DisplayItem it : displayItems) {
      if (it.type != DType.CHART) continue;
      if (!it.tile.dirty || it.tile.renderInFlight) continue;
      if (!isVisible(it, cw, ch)) continue;
      renderSnapshotAsync(it.tile);
    }
  }

  private boolean isVisible(DisplayItem it, int cw, int ch) {
    if (it.rect == null) return false;
    double sx = viewX + it.rect.x * scale, sy = viewY + it.rect.y * scale;
    double sw = it.rect.width * scale, sh = it.rect.height * scale;
    return !(sx + sw < 0 || sx > cw || sy + sh < 0 || sy > ch);
  }

  private void renderSnapshotAsync(TileEntry entry) { renderSnapshotAsync(entry, null); }

  private void renderSnapshotAsync(TileEntry entry, Runnable onFinish) {
    if (entry.renderInFlight) { if (onFinish != null) SwingUtilities.invokeLater(onFinish); return; }
    entry.renderInFlight = true;
    int w = snapshotW, h = Math.max(1, snapshotH);
    snapshotExecutor.submit(() -> {
      try {
        JFreeChart jfc = extractChart(entry.originalChart);
        if (jfc == null) return;
        BufferedImage img = jfc.createBufferedImage(w, h);
        entry.bufferedSnapshot = img;
        entry.clearVolatile();
        entry.dirty = false;
        entry.lastRenderTime = System.currentTimeMillis();
        SwingUtilities.invokeLater(canvasArea::repaint);
      } catch (Exception e) {
        log.trace("Snapshot failed: {}", e.getMessage());
      } finally {
        entry.renderInFlight = false;
        if (onFinish != null) SwingUtilities.invokeLater(onFinish);
      }
    });
  }

  private JFreeChart extractChart(PRChartModule mod) {
    try {
      Object c = mod.getPresenter().getChart();
      if (c instanceof SCP scp) return scp.getjFreeChart();
    } catch (Exception ignored) {}
    return null;
  }

  private Image getImage(TileEntry entry) {
    BufferedImage bi = entry.bufferedSnapshot;
    if (bi == null) return null;
    GraphicsConfiguration gc = canvasArea.getGraphicsConfiguration();
    if (gc == null) return bi;
    VolatileImage vi = entry.volatileSnapshot;
    boolean need = false;
    if (vi == null || vi.getWidth() != bi.getWidth() || vi.getHeight() != bi.getHeight()) {
      if (vi != null) vi.flush();
      vi = null;
      need = true;
    } else {
      int st = vi.validate(gc);
      if (st == VolatileImage.IMAGE_INCOMPATIBLE) {
        vi.flush();
        vi = null;
        need = true;
      } else if (st == VolatileImage.IMAGE_RESTORED) need = true;
    }
    if (need) {
      try {
        if (vi == null) vi = gc.createCompatibleVolatileImage(bi.getWidth(), bi.getHeight(), Transparency.OPAQUE);
        Graphics2D g = vi.createGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();
        entry.volatileSnapshot = vi;
      } catch (Exception e) { return bi; }
    }
    return vi;
  }

  private void updateHover(int mx, int my) {
    int oldC = hoveredChartIndex;
    int oldH = hoveredHeaderIndex;
    hoveredChartIndex = -1;
    hoveredHeaderIndex = -1;

    for (int i = 0; i < displayItems.size(); i++) {
      DisplayItem it = displayItems.get(i);
      if (it.rect == null) continue;
      double sx = viewX + it.rect.x * scale, sy = viewY + it.rect.y * scale;
      double sw = it.rect.width * scale, sh = it.rect.height * scale;
      if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh) {
        if (it.type == DType.CHART) hoveredChartIndex = i;
        else hoveredHeaderIndex = i;
        break;
      }
    }

    if (oldC != hoveredChartIndex || oldH != hoveredHeaderIndex) {
      boolean hand = hoveredChartIndex >= 0 || hoveredHeaderIndex >= 0;
      canvasArea.setCursor(hand ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
      canvasArea.repaint();
    }
  }

  private void drawGrid(Graphics2D g, int w, int h) {
    double gs = 50 * scale;
    if (gs < 10) gs *= Math.ceil(10 / gs);
    g.setColor(new Color(40, 40, 50));
    for (double x = viewX % gs; x < w; x += gs)
      for (double y = viewY % gs; y < h; y += gs)
        g.fillRect((int) x, (int) y, 1, 1);
  }

  private JSlider createWidthSlider() {
    JSlider s = new JSlider(JSlider.HORIZONTAL, TILE_W_MIN, TILE_W_MAX, tileW);
    s.setBackground(toolbarBgColor);
    s.setForeground(zoomTextColor);
    s.addChangeListener(this::onWidthChanged);
    return s;
  }

  private JSlider createHeightSlider() {
    JSlider s = new JSlider(JSlider.VERTICAL, TILE_H_MIN, TILE_H_MAX, tileH);
    s.setBackground(toolbarBgColor);
    s.setForeground(zoomTextColor);
    s.setInverted(true);
    s.addChangeListener(this::onHeightChanged);
    return s;
  }

  private void onWidthChanged(ChangeEvent e) {
    tileW = widthSlider.getValue();
    snapshotW = tileW;
    rebuildDisplayForCurrentMode();
    canvasArea.repaint();
    debounce();
  }

  private void onHeightChanged(ChangeEvent e) {
    tileH = heightSlider.getValue();
    snapshotH = tileH;
    rebuildDisplayForCurrentMode();
    canvasArea.repaint();
    debounce();
  }

  private void debounce() {
    if (sliderDebounceTimer != null) sliderDebounceTimer.stop();
    sliderDebounceTimer = new Timer(SLIDER_DEBOUNCE_MS, ev -> {
      markAllDirty();
      if (renderMode == RenderMode.HYBRID) startManagedRun();
    });
    sliderDebounceTimer.setRepeats(false);
    sliderDebounceTimer.start();
  }

  private void beginInteraction() {
    interacting = true;
    if (zoomIdleTimer != null) zoomIdleTimer.stop();
  }

  private void endInteractionDeferred() {
    if (zoomIdleTimer != null) zoomIdleTimer.stop();
    zoomIdleTimer = new Timer(ZOOM_IDLE_MS, ev -> interacting = false);
    zoomIdleTimer.setRepeats(false);
    zoomIdleTimer.start();
  }

  public void savePreferences() {
    prefs.putInt(PREF_TILE_W, tileW);
    prefs.putInt(PREF_TILE_H, tileH);
    prefs.putInt(PREF_TILES_PER_PAGE, tilesPerPage);
    prefs.put(PREF_VIEW_MODE, viewMode.name());
  }

  public void stopEngine() {
    if (renderTimer != null) renderTimer.stop();
    if (dirtyCheckTimer != null) dirtyCheckTimer.stop();
    if (sliderDebounceTimer != null) sliderDebounceTimer.stop();
    if (zoomIdleTimer != null) zoomIdleTimer.stop();
    if (autoRefreshTimer != null) autoRefreshTimer.stop();
    snapshotExecutor.shutdownNow();
    savePreferences();
    clearTiles();
  }

  private String fmtAge(long s) { return s < 60 ? s + "s" : s < 3600 ? (s / 60) + "m" : (s / 3600) + "h"; }
  private static int clamp(int v, int mn, int mx) { return Math.max(mn, Math.min(mx, v)); }

  private int measureHeaderWidth(String label) {
    String s = label != null ? label : "";
    BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    g.setFont(HEADER_MEASURE_FONT);
    FontMetrics fm = g.getFontMetrics();
    int w = fm.stringWidth(s) + 40;
    g.dispose();
    return w;
  }

  private String buildTitle(CProfile p) {
    return p != null && p.getColName() != null ? p.getColName() : "Chart";
  }

  private String resolveColName(CProfile p) {
    return p != null && p.getColName() != null ? p.getColName() : "";
  }

  private String resolveDataType(CProfile p) {
    if (p == null) return "";
    try {
      String dt = p.getColDbTypeName();
      if (dt != null && !dt.isEmpty()) return dt;
      dt = String.valueOf(p.getColSizeDisplay());
      if (dt != null && !dt.isEmpty()) return dt;
    } catch (Exception ignored) {}
    return "";
  }

  private String resolveName(int id, char type) {
    try {
      return switch (type) {
        case 'P' -> {
          ProfileInfo i = profileManager.getProfileInfoById(id);
          yield i != null && i.getName() != null ? i.getName() : "Profile #" + id;
        }
        case 'T' -> {
          TaskInfo i = profileManager.getTaskInfoById(id);
          yield i != null && i.getName() != null ? i.getName() : "Task #" + id;
        }
        default -> {
          QueryInfo i = profileManager.getQueryInfoById(id);
          yield i != null && i.getName() != null ? i.getName() : "Query #" + id;
        }
      };
    } catch (Exception e) {
      return type + " #" + id;
    }
  }

  private enum DType { PROFILE_HEADER, TASK_HEADER, QUERY_HEADER, CHART }

  private static class DisplayItem {
    DType type;
    int profileId, taskId, queryId;
    String label;
    Rectangle2D.Double rect;
    TileEntry tile;
  }

  private static class PNode {
    final int id;
    final String name;
    final Map<Integer, TNode> tasks = new LinkedHashMap<>();
    double width;

    PNode(int id, String n) { this.id = id; this.name = n; }
  }

  private static class TNode {
    final int id;
    final String name;
    final Map<Integer, QNode> queries = new LinkedHashMap<>();
    double width;

    TNode(int id, String n) { this.id = id; this.name = n; }
  }

  private static class QNode {
    final int id;
    final String name;
    final List<TileEntry> tiles = new ArrayList<>();
    double width;

    QNode(int id, String n) { this.id = id; this.name = n; }
  }

  private static class TileEntry {
    final PRChartModule originalChart;
    final ProfileTaskQueryKey key;
    final CProfile profile;
    final String title;
    final int profileId;
    final String profileName;
    final int taskId;
    final String taskName;
    final int queryId;
    final String queryName;
    final String colName;
    final String dataType;
    volatile BufferedImage bufferedSnapshot;
    volatile VolatileImage volatileSnapshot;
    volatile boolean dirty = true;
    volatile long lastRenderTime = 0;
    volatile boolean renderInFlight = false;

    TileEntry(PRChartModule oc, ProfileTaskQueryKey k, CProfile p, String t,
              int pid, String pn, int tid, String tn, int qid, String qn,
              String colName, String dataType) {
      this.originalChart = oc;
      this.key = k;
      this.profile = p;
      this.title = t;
      this.profileId = pid;
      this.profileName = pn;
      this.taskId = tid;
      this.taskName = tn;
      this.queryId = qid;
      this.queryName = qn;
      this.colName = colName;
      this.dataType = dataType;
    }

    void clearVolatile() {
      VolatileImage v = volatileSnapshot;
      if (v != null) {
        v.flush();
        volatileSnapshot = null;
      }
    }
  }

  private class CanvasArea extends JPanel {
    CanvasArea() {
      setBackground(panelBgColor);
      setFocusable(true);
      setupMouse();
    }

    private void setupMouse() {
      addMouseWheelListener(e -> {
        if (animating) return;
        double mx = (e.getX() - viewX) / scale, my = (e.getY() - viewY) / scale;
        double f = e.getWheelRotation() < 0 ? 1.15 : 1.0 / 1.15;
        double ns = Math.max(0.05, Math.min(5.0, scale * f));
        viewX = e.getX() - mx * ns;
        viewY = e.getY() - my * ns;
        scale = ns;
        if (focusedItemIndex >= 0) { focusedItemIndex = -1; hasSavedPreFocus = false; }
        beginInteraction();
        endInteractionDeferred();
        repaint();
      });

      addMouseMotionListener(new MouseMotionAdapter() {
        @Override public void mouseDragged(MouseEvent e) {
          if (animating || !panning) return;
          viewX += e.getX() - lastMouseX;
          viewY += e.getY() - lastMouseY;
          lastMouseX = e.getX();
          lastMouseY = e.getY();
          if (focusedItemIndex >= 0) { focusedItemIndex = -1; hasSavedPreFocus = false; }
          repaint();
        }
        @Override public void mouseMoved(MouseEvent e) { updateHover(e.getX(), e.getY()); }
      });

      addMouseListener(new MouseAdapter() {
        @Override public void mousePressed(MouseEvent e) {
          requestFocusInWindow();
          if (animating) return;
          if (e.getButton() == MouseEvent.BUTTON1) {
            if (e.getClickCount() == 2 && hoveredChartIndex >= 0) {
              if (focusedItemIndex == hoveredChartIndex) unfocus(); else focusOnItem(hoveredChartIndex);
              return;
            }
            panning = true;
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            beginInteraction();
          }
        }
        @Override public void mouseReleased(MouseEvent e) { panning = false; interacting = false; }
      });

      addKeyListener(new KeyAdapter() {
        @Override public void keyPressed(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (focusedItemIndex >= 0) unfocus(); else if (onCloseCallback != null) onCloseCallback.run();
          }
          if (e.getKeyCode() == KeyEvent.VK_HOME && !animating) {
            focusedItemIndex = -1;
            hasSavedPreFocus = false;
            animateFitAll();
          }
          if (e.getKeyCode() == KeyEvent.VK_R && renderMode == RenderMode.HYBRID && !autoRunInFlight) startManagedRun();
          if (needsTabs()) {
            if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
              int n = Math.min(currentTabIndex + 1, getTotalTabs() - 1);
              if (n != currentTabIndex) switchToTab(n);
            }
            if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
              int p = Math.max(currentTabIndex - 1, 0);
              if (p != currentTabIndex) switchToTab(p);
            }
          }
        }
      });
    }

    private int selectionDepthFromType(DType t) {
      return switch (t) {
        case PROFILE_HEADER -> 1;
        case TASK_HEADER -> 2;
        case QUERY_HEADER -> 3;
        case CHART -> 3;
      };
    }

    private boolean headerHighlighted(DisplayItem it, int depth, int selP, int selT, int selQ) {
      if (depth <= 0) return false;
      return switch (it.type) {
        case PROFILE_HEADER -> it.profileId == selP;
        case TASK_HEADER -> it.profileId == selP && (depth == 1 || it.taskId == selT);
        case QUERY_HEADER -> it.profileId == selP && (depth == 1 || (it.taskId == selT && (depth == 2 || it.queryId == selQ)));
        default -> false;
      };
    }

    private boolean chartGroupHighlighted(DisplayItem it, int depth, int selP, int selT, int selQ) {
      if (depth <= 0 || it.type != DType.CHART) return false;
      if (it.profileId != selP) return false;
      if (depth == 1) return true;
      if (it.taskId != selT) return false;
      if (depth == 2) return true;
      return it.queryId == selQ;
    }

    @Override
    protected void paintComponent(Graphics g0) {
      super.paintComponent(g0);
      Graphics2D g = (Graphics2D) g0;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      int w = getWidth(), h = getHeight();
      drawGrid(g, w, h);
      if (allTiles.isEmpty()) { paintEmpty(g, w, h); return; }

      int selP = -1, selT = -1, selQ = -1, selDepth = 0;
      if (hoveredChartIndex >= 0 && hoveredChartIndex < displayItems.size()) {
        DisplayItem hc = displayItems.get(hoveredChartIndex);
        selP = hc.profileId;
        selT = hc.taskId;
        selQ = hc.queryId;
        selDepth = 3;
      } else if (hoveredHeaderIndex >= 0 && hoveredHeaderIndex < displayItems.size()) {
        DisplayItem hh = displayItems.get(hoveredHeaderIndex);
        if (hh.type != DType.CHART) {
          selP = hh.profileId;
          selT = hh.taskId;
          selQ = hh.queryId;
          selDepth = selectionDepthFromType(hh.type);
        }
      }

      paintFlatItems(g, w, h, selDepth, selP, selT, selQ);
      paintHoverInfo(g, w);
      paintStatusBar(g, w, h);
    }

    private void paintEmpty(Graphics2D g, int w, int h) {
      g.setColor(zoomTextColor);
      g.setFont(new Font("SansSerif", Font.BOLD, 18));
      String m = "No charts available.";
      FontMetrics fm = g.getFontMetrics();
      g.drawString(m, (w - fm.stringWidth(m)) / 2, h / 2);
    }

    private void paintHoverInfo(Graphics2D g, int w) {
      g.setColor(zoomPanelBg);
      g.fillRect(0, 0, w, HOVER_INFO_H);

      g.setColor(zoomBorderColor);
      g.setStroke(new BasicStroke(1.5f));
      g.drawLine(0, HOVER_INFO_H - 1, w, HOVER_INFO_H - 1);

      Color accentDim = new Color(
          zoomAccentColor.getRed(), zoomAccentColor.getGreen(), zoomAccentColor.getBlue(), 40);
      g.setColor(accentDim);
      g.drawLine(0, 0, w, 0);

      if (hoveredChartIndex >= 0 && hoveredChartIndex < displayItems.size()) {
        DisplayItem hi = displayItems.get(hoveredChartIndex);
        TileEntry te = hi.tile;
        if (te != null) {
          int tx = 14, ty = 20;

          g.setFont(new Font("SansSerif", Font.BOLD, 18));
          g.setColor(zoomAccentColor);
          String nameStr = te.colName != null && !te.colName.isEmpty() ? te.colName : (te.title != null ? te.title : "Chart");
          g.drawString(nameStr, tx, ty);
          int nameW = g.getFontMetrics().stringWidth(nameStr);

          if (te.dataType != null && !te.dataType.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(
                Math.min(255, zoomAccentColor.getRed() + 30),
                Math.min(255, zoomAccentColor.getGreen() + 20),
                255));
            g.drawString("  [" + te.dataType + "]", tx + nameW, ty);
          }

          g.setFont(new Font("SansSerif", Font.PLAIN, 13));
          g.setColor(zoomTextColor);
          String ctx = "\u25C6 " + te.profileName + "  \u25B8 " + te.taskName + "  \u25AB " + te.queryName;
          g.drawString(ctx, tx, ty + 22);
        }
      } else {
        g.setFont(new Font("SansSerif", Font.ITALIC, 13));
        g.setColor(new Color(zoomTextColor.getRed(), zoomTextColor.getGreen(), zoomTextColor.getBlue(), 120));
        g.drawString("Hover over a chart to see details", 14, 30);
      }
    }

    private void paintFlatItems(Graphics2D g, int w, int h, int selDepth, int selP, int selT, int selQ) {
      long now = System.currentTimeMillis();
      boolean groupHover = hoveredHeaderIndex >= 0 && hoveredChartIndex < 0;
      for (int i = 0; i < displayItems.size(); i++) {
        DisplayItem it = displayItems.get(i);
        if (it.rect == null) continue;
        double sx = viewX + it.rect.x * scale, sy = viewY + it.rect.y * scale;
        double sw = it.rect.width * scale, sh = it.rect.height * scale;
        if (sx + sw < 0 || sx > w || sy + sh < 0 || sy > h) continue;

        switch (it.type) {
          case PROFILE_HEADER -> {
            boolean hl = headerHighlighted(it, selDepth, selP, selT, selQ);
            boolean hov = i == hoveredHeaderIndex;
            drawHeader(g, sx, sy, sw, sh, (hl || hov) ? PROFILE_BG_HL : PROFILE_BG,
                       (hl || hov) ? PROFILE_BORDER_HL : PROFILE_BORDER, (hl || hov) ? PROFILE_TEXT_HL : PROFILE_TEXT,
                       it.label, (hl || hov) ? 2.5f : 1.5f);
          }
          case TASK_HEADER -> {
            boolean hl = headerHighlighted(it, selDepth, selP, selT, selQ);
            boolean hov = i == hoveredHeaderIndex;
            drawHeader(g, sx, sy, sw, sh, (hl || hov) ? TASK_BG_HL : TASK_BG,
                       (hl || hov) ? TASK_BORDER_HL : TASK_BORDER, (hl || hov) ? TASK_TEXT_HL : TASK_TEXT,
                       it.label, (hl || hov) ? 2.5f : 1.5f);
          }
          case QUERY_HEADER -> {
            boolean hl = headerHighlighted(it, selDepth, selP, selT, selQ);
            boolean hov = i == hoveredHeaderIndex;
            drawHeader(g, sx, sy, sw, sh, (hl || hov) ? QUERY_BG_HL : QUERY_BG,
                       (hl || hov) ? QUERY_BORDER_HL : QUERY_BORDER, (hl || hov) ? QUERY_TEXT_HL : QUERY_TEXT,
                       it.label, (hl || hov) ? 2.5f : 1.5f);
          }
          case CHART -> {
            boolean hovered = i == hoveredChartIndex;
            boolean focused = i == focusedItemIndex;
            boolean groupHl = groupHover && chartGroupHighlighted(it, selDepth, selP, selT, selQ);
            drawChart(g, it, sx, sy, sw, sh, hovered, focused, groupHl, now);
          }
        }
      }
      paintFocusHint(g);
    }

    private void drawHeader(Graphics2D g, double sx, double sy, double sw, double sh,
                            Color bg, Color border, Color text, String label, float stroke) {
      g.setColor(bg);
      g.fillRoundRect((int) sx, (int) sy, (int) sw, (int) sh, 10, 10);
      g.setColor(border);
      g.setStroke(new BasicStroke(stroke));
      g.drawRoundRect((int) sx, (int) sy, (int) sw, (int) sh, 10, 10);

      int fs = Math.max(15, (int) (44 * scale));
      g.setFont(new Font("SansSerif", Font.BOLD, fs));
      g.setColor(text);

      FontMetrics fm = g.getFontMetrics();
      float textY = (float) (sy + (sh - (fm.getAscent() + fm.getDescent())) / 2 + fm.getAscent());
      g.drawString(label != null ? label : "", (int) sx + (int) (15 * scale), (int) textY);
    }

    private void drawChart(Graphics2D g, DisplayItem it,
                           double sx, double sy, double sw, double sh,
                           boolean hovered, boolean focused, boolean groupHighlighted, long now) {

      TileEntry e = it.tile;
      boolean effectiveHover = hovered || groupHighlighted;

      g.setColor(new Color(0, 0, 0, focused ? 80 : 40));
      g.fillRoundRect((int) sx + 3, (int) sy + 3, (int) sw, (int) sh, 10, 10);

      Color bg = focused ? new Color(45, 45, 60) : effectiveHover ? new Color(55, 55, 70) : new Color(35, 35, 45);
      g.setColor(bg);
      g.fillRoundRect((int) sx, (int) sy, (int) sw, (int) sh, 10, 10);

      Color ac = renderMode == RenderMode.HYBRID ? new Color(80, 200, 220) : zoomAccentColor;
      Color bc = focused ? ac : effectiveHover ? ac : ac.darker().darker();
      g.setColor(bc);
      g.setStroke(new BasicStroke(focused ? 2.5f : effectiveHover ? 2f : 1f));
      g.drawRoundRect((int) sx, (int) sy, (int) sw, (int) sh, 10, 10);

      Graphics2D tg = (Graphics2D) g.create();
      tg.setClip((int) sx + 1, (int) sy + 1, (int) sw - 2, (int) sh - 2);

      int titleFontSize = Math.max(12, (int) (28 * scale));
      tg.setFont(new Font("SansSerif", Font.BOLD, titleFontSize));
      FontMetrics titleFm = tg.getFontMetrics();

      int titleAreaH = titleFm.getHeight() + (int) (12 * scale);

      int titleBarY = (int) sy;
      Color titleBarBg = new Color(
          Math.max(0, zoomPanelBg.getRed() - 5),
          Math.max(0, zoomPanelBg.getGreen() - 5),
          Math.max(0, zoomPanelBg.getBlue() + 8), 200);
      tg.setColor(titleBarBg);
      tg.fillRect((int) sx + 1, titleBarY + 1, (int) sw - 2, titleAreaH);

      tg.setColor(zoomBorderColor);
      tg.setStroke(new BasicStroke(1f));
      tg.drawLine((int) sx + 1, titleBarY + titleAreaH, (int) (sx + sw) - 1, titleBarY + titleAreaH);

      String titleText = e.colName != null && !e.colName.isEmpty() ? e.colName : (e.title != null ? e.title : "Chart");
      String dtSuffix = e.dataType != null && !e.dataType.isEmpty() ? "  [" + e.dataType + "]" : "";

      tg.setFont(new Font("SansSerif", Font.BOLD, titleFontSize));
      tg.setColor(zoomAccentColor);
      int titleTextY = (int) sy + (titleAreaH - (titleFm.getAscent() + titleFm.getDescent())) / 2 + titleFm.getAscent() + (int) (2 * scale);

      tg.drawString(titleText, (int) sx + (int) (10 * scale), titleTextY);

      if (!dtSuffix.isEmpty()) {
        int titleTextW = titleFm.stringWidth(titleText);
        Font dtFont = new Font("SansSerif", Font.PLAIN, Math.max(10, (int) (22 * scale)));
        tg.setFont(dtFont);
        tg.setColor(new Color(
            Math.min(255, zoomAccentColor.getRed() + 30),
            Math.min(255, zoomAccentColor.getGreen() + 20),
            255));
        tg.drawString(dtSuffix, (int) sx + (int) (10 * scale) + titleTextW, titleTextY);
      }

      int imgX = (int) sx + 1, imgY = (int) sy + titleAreaH + 1;
      int imgW = (int) sw - 2, imgH = (int) sh - titleAreaH - 2;

      Image img = getImage(e);
      if (img != null) {
        tg.drawImage(img, imgX, imgY, imgW, Math.max(1, imgH), null);
      } else {
        tg.setColor(new Color(50, 50, 60));
        tg.fillRect(imgX, imgY, imgW, imgH);
        tg.setColor(zoomTextColor);
        tg.setFont(new Font("SansSerif", Font.ITALIC, Math.max(10, (int) (14 * scale))));
        FontMetrics fm = tg.getFontMetrics();
        String lt = renderMode == RenderMode.HYBRID ? "Awaiting..." : "Rendering...";
        tg.drawString(lt, imgX + (imgW - fm.stringWidth(lt)) / 2, imgY + (imgH + fm.getAscent()) / 2);
      }

      if (renderMode == RenderMode.HYBRID && e.lastRenderTime > 0) {
        long age = (now - e.lastRenderTime) / 1000;
        String bl = "\u2744" + fmtAge(age);
        Font bf = new Font("SansSerif", Font.BOLD, Math.max(10, (int) (14 * scale)));
        tg.setFont(bf);
        FontMetrics bfm = tg.getFontMetrics();
        int bw = bfm.stringWidth(bl) + 8, bh = bfm.getHeight() + 4;
        int bx = (int) (sx + sw) - bw - 4, by = (int) sy + 4;
        Color bbg = e.dirty ? new Color(180, 60, 20, 200) : age < 10 ? new Color(30, 120, 50, 200) : new Color(140, 110, 20, 200);
        tg.setColor(bbg);
        tg.fillRoundRect(bx, by, bw, bh, 6, 6);
        tg.setColor(new Color(240, 240, 250));
        tg.drawString(bl, bx + 4, by + bfm.getAscent() + 2);
      }

      if (hovered && !focused) {
        tg.setColor(new Color(0, 0, 0, 80));
        tg.fillRoundRect((int) sx, (int) sy, (int) sw, (int) sh, 10, 10);
        tg.setColor(new Color(255, 255, 255, 200));
        tg.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, (int) (16 * scale))));
        String hint = "Double-click to focus";
        FontMetrics fm = tg.getFontMetrics();
        tg.drawString(hint, (int) (sx + (sw - fm.stringWidth(hint)) / 2), (int) (sy + (sh + fm.getAscent()) / 2));
      }
      tg.dispose();
    }

    private void paintFocusHint(Graphics2D g) {
      if (focusedItemIndex < 0 || focusedItemIndex >= displayItems.size() || animating) return;
      DisplayItem fi = displayItems.get(focusedItemIndex);
      if (fi.rect == null) return;
      double sx = viewX + fi.rect.x * scale, sw = fi.rect.width * scale, sy = viewY + fi.rect.y * scale;
      String h = "DblClick/ESC to return";
      g.setFont(new Font("SansSerif", Font.PLAIN, 11));
      FontMetrics fm = g.getFontMetrics();
      int tw = fm.stringWidth(h), bw = tw + 16, bh = fm.getHeight() + 8;
      int bx = (int) (sx + sw) - bw - 6, by = (int) sy + 6;
      g.setColor(new Color(0, 0, 0, 160));
      g.fillRoundRect(bx, by, bw, bh, 8, 8);
      g.setColor(zoomTextColor);
      g.drawString(h, bx + 8, by + 4 + fm.getAscent());
    }

    private void paintStatusBar(Graphics2D g, int w, int h) {
      g.setColor(zoomPanelBg);
      g.fillRect(0, h - STATUS_BAR_H, w, STATUS_BAR_H);

      g.setColor(zoomBorderColor);
      g.setStroke(new BasicStroke(1.5f));
      g.drawLine(0, h - STATUS_BAR_H, w, h - STATUS_BAR_H);

      Color accentDim = new Color(
          zoomAccentColor.getRed(), zoomAccentColor.getGreen(), zoomAccentColor.getBlue(), 40);
      g.setColor(accentDim);
      g.drawLine(0, h - 1, w, h - 1);

      g.setColor(zoomTextColor);
      g.setFont(new Font("SansSerif", Font.PLAIN, 12));
      int vc = getVisibleTileCount();
      String vm = viewMode.getDisplayName();
      String pi = needsTabs() ? String.format("  |  Page %d/%d", currentTabIndex + 1, getTotalTabs()) : "";
      String fi = focusedItemIndex >= 0 && focusedItemIndex < displayItems.size()
          ? "  |  Focus: " + displayItems.get(focusedItemIndex).label
          : "";
      g.drawString(String.format(
          "%s  |  Charts: %d/%d%s  |  Zoom: %.0f%%%s  |  Scroll=zoom Drag=pan DblClick=focus Home=fit ESC=close",
          vm, vc, allTiles.size(), pi, scale * 100, fi), 15, h - 12);
    }
  }
}