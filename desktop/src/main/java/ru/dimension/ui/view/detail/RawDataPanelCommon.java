package ru.dimension.ui.view.detail;

import static ru.dimension.ui.laf.LafColorGroup.TABLE_BACKGROUND;
import static ru.dimension.ui.laf.LafColorGroup.TABLE_FONT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXFindBar;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.action.AbstractActionExt;
import org.jdesktop.swingx.search.AbstractSearchable;
import org.jdesktop.swingx.search.SearchFactory;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.service.mapping.Mapper;
import ru.dimension.db.sql.BatchResultSet;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.view.detail.raw.RawDataFilterStrip;
import ru.dimension.ui.view.detail.raw.RawDataTransposePanel;
import ru.dimension.ui.view.detail.raw.searchable.DecoratorFactory;
import ru.dimension.ui.view.detail.raw.searchable.MatchingTextHighlighter;
import ru.dimension.ui.view.detail.raw.searchable.XMatchingTextHighlighter;

@Log4j2
public abstract class RawDataPanelCommon extends JPanel {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

  private static final int TRIANGLE_SIZE = 8;

  private final TableInfo tableInfo;
  private final CProfile cProfile;

  private final JXTable table;
  private final JXFindBar findBar;
  private final JButton findNextButton;
  private final DefaultTableModel tableModel;

  private final RawDataFilterStrip filterStrip;

  private final TitledBorder titledBorder;

  private TableRowSorter<DefaultTableModel> rowSorter;

  private int cProfileColumnIndex = -1;

  private Map<String, Color> currentSeriesColorMap;

  private int rowCount = 0;

  protected JLabel jLabelRowCount;
  protected int fetchSize = 1000;
  protected final boolean useFetchSize;
  protected boolean hasData = false;

  protected BatchResultSet batchResultSet;

  private final JCheckBox transposeCheckBox;
  private final RawDataTransposePanel transposePanel;
  private JSplitPane splitPane;
  private final JScrollPane tableRawDataPane;
  private final JPanel searchablePanel;

  private class TriangleCellRenderer extends DefaultTableCellRenderer {
    private Color triangleColor = null;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      triangleColor = null;

      if (currentSeriesColorMap != null && !currentSeriesColorMap.isEmpty()) {
        int modelCol = ((JXTable) table).convertColumnIndexToModel(column);
        if (modelCol == cProfileColumnIndex && cProfileColumnIndex >= 0 && value != null) {
          triangleColor = currentSeriesColorMap.get(value.toString());
        }
      }

      return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);

      if (triangleColor != null) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(triangleColor);

        int[] xPoints = {getWidth() - TRIANGLE_SIZE, getWidth(), getWidth()};
        int[] yPoints = {0, 0, TRIANGLE_SIZE};
        g2.fillPolygon(xPoints, yPoints, 3);

        g2.dispose();
      }
    }
  }

  public RawDataPanelCommon(TableInfo tableInfo,
                            CProfile cProfile,
                            boolean useFetchSize) {
    this.tableInfo = tableInfo;
    this.cProfile = cProfile;

    this.setLayout(new BorderLayout());

    this.useFetchSize = useFetchSize;

    this.jLabelRowCount = new JLabel("Rows: " + rowCount);

    this.tableModel = new DefaultTableModel(getColumnHeaders(), 0);

    this.table = new JXTable(tableModel);

    this.rowSorter = new TableRowSorter<>(tableModel);
    this.table.setRowSorter(rowSorter);

    this.table.setColumnControlVisible(true);
    this.table.setHorizontalScrollEnabled(true);
    this.table.setShowVerticalLines(true);
    this.table.setShowHorizontalLines(true);
    this.table.setGridColor(java.awt.Color.GRAY);
    this.table.setIntercellSpacing(new java.awt.Dimension(1, 1));
    this.table.setBackground(LaF.getBackgroundColor(TABLE_BACKGROUND, LaF.getLafType()));
    this.table.setForeground(LaF.getBackgroundColor(TABLE_FONT, LaF.getLafType()));

    this.table.setDefaultRenderer(Object.class, new TriangleCellRenderer());

    this.table.packAll();

    this.findBar = SearchFactory.getInstance().createFindBar();

    this.findNextButton = new JButton("Next " + fetchSize + " rows");
    this.findNextButton.addActionListener(e -> {
      log.info("Fetch next batch of {} rows..", fetchSize);
      loadToModel(batchResultSet.next() ? batchResultSet.getObject() : Collections.emptyList());
    });

    this.transposeCheckBox = new JCheckBox("Transpose");
    this.transposeCheckBox.setSelected(false);
    this.transposeCheckBox.addActionListener(e -> toggleTranspose());

    this.transposePanel = new RawDataTransposePanel();

    this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && transposeCheckBox.isSelected()) {
          fireTransposeUpdate();
        }
      }
    });

    MatchingTextHighlighter matchingTextMarker = new XMatchingTextHighlighter();
    matchingTextMarker.setPainter(DecoratorFactory.createPlainPainter());
    ((AbstractSearchable) this.table.getSearchable()).setMatchHighlighter(matchingTextMarker);

    tableRawDataPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                       ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    tableRawDataPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
    tableRawDataPane.setViewportView(this.table);
    tableRawDataPane.setVerticalScrollBar(tableRawDataPane.getVerticalScrollBar());

    this.filterStrip = new RawDataFilterStrip();
    this.filterStrip.setFilterChangeListener(this::onFilterChanged);
    this.filterStrip.setVisible(false);

    this.cProfileColumnIndex = findCProfileColumnIndex();

    searchablePanel = new JPanel(new BorderLayout());
    final JXCollapsiblePane collapsible = connectCollapsibleFindBarWithTable();

    JPanel findBarRow = new JPanel(new BorderLayout());
    findBarRow.add(collapsible, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    buttonPanel.add(transposeCheckBox);
    if (useFetchSize) {
      buttonPanel.add(findNextButton);
    }
    findBarRow.add(buttonPanel, BorderLayout.EAST);

    JPanel topSection = new JPanel(new BorderLayout());
    topSection.add(findBarRow, BorderLayout.NORTH);
    topSection.add(filterStrip, BorderLayout.SOUTH);

    searchablePanel.add(topSection, BorderLayout.NORTH);
    searchablePanel.add(tableRawDataPane, BorderLayout.CENTER);

    this.titledBorder = new TitledBorder("Selected rows: " + rowCount);
    this.titledBorder.setTitleJustification(TitledBorder.RIGHT);
    searchablePanel.setBorder(BorderFactory.createCompoundBorder(
        titledBorder, new EmptyBorder(4, 4, 4, 4)));

    this.add(searchablePanel, BorderLayout.CENTER);
  }

  private void toggleTranspose() {
    if (transposeCheckBox.isSelected()) {
      if (table.getRowCount() > 0 && table.getSelectedRow() == -1) {
        JViewport viewport = tableRawDataPane.getViewport();
        Point viewPosition = viewport.getViewPosition();
        int viewportHeight = viewport.getExtentSize().height;
        Point centerPoint = new Point(viewPosition.x, viewPosition.y + viewportHeight / 2);
        int middleRow = table.rowAtPoint(centerPoint);
        if (middleRow < 0) {
          middleRow = table.rowAtPoint(viewPosition);
        }
        if (middleRow < 0) {
          middleRow = 0;
        }
        table.setRowSelectionInterval(middleRow, middleRow);
      }

      searchablePanel.remove(tableRawDataPane);

      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableRawDataPane, transposePanel);
      splitPane.setResizeWeight(0.7);
      splitPane.setDividerSize(5);
      splitPane.setOneTouchExpandable(true);

      searchablePanel.add(splitPane, BorderLayout.CENTER);

      syncTransposePanelState();
      fireTransposeUpdate();
    } else {
      if (splitPane != null) {
        searchablePanel.remove(splitPane);
        splitPane = null;
      }

      transposePanel.clear();
      searchablePanel.add(tableRawDataPane, BorderLayout.CENTER);
    }

    searchablePanel.revalidate();
    searchablePanel.repaint();
  }

  private void syncTransposePanelState() {
    if (currentSeriesColorMap != null) {
      transposePanel.setSeriesColorMap(currentSeriesColorMap);
    }
    if (cProfile != null && cProfileColumnIndex >= 0) {
      transposePanel.setHighlightColumnName(cProfile.getColName());
    }
  }

  private void fireTransposeUpdate() {
    int viewRow = table.getSelectedRow();
    if (viewRow < 0) {
      transposePanel.clear();
      return;
    }

    int modelRow = table.convertRowIndexToModel(viewRow);
    int colCount = tableModel.getColumnCount();

    String[] columnNames = new String[colCount];
    Object[] rowValues = new Object[colCount];

    for (int col = 0; col < colCount; col++) {
      columnNames[col] = tableModel.getColumnName(col);
      rowValues[col] = tableModel.getValueAt(modelRow, col);
    }

    transposePanel.updateFromRow(columnNames, rowValues);
  }

  public void initializeFilterStrip(Map<String, Color> seriesColorMap) {
    if (seriesColorMap != null && !seriesColorMap.isEmpty()) {
      this.currentSeriesColorMap = new LinkedHashMap<>(seriesColorMap);
      filterStrip.setVisible(true);
      computeAndUpdateFilterPercentages();

      transposePanel.setSeriesColorMap(currentSeriesColorMap);
      if (cProfile != null) {
        transposePanel.setHighlightColumnName(cProfile.getColName());
      }
    } else {
      this.currentSeriesColorMap = null;
      filterStrip.setVisible(false);

      transposePanel.setSeriesColorMap(null);
      transposePanel.setHighlightColumnName(null);
    }
    table.repaint();
  }

  protected Set<String> collectPresentSeriesKeys() {
    Set<String> presentKeys = new HashSet<>();
    if (cProfileColumnIndex < 0) {
      return presentKeys;
    }
    int totalRows = tableModel.getRowCount();
    for (int row = 0; row < totalRows; row++) {
      Object value = tableModel.getValueAt(row, cProfileColumnIndex);
      if (value != null) {
        presentKeys.add(value.toString());
      }
    }
    return presentKeys;
  }

  private void scrollToCProfileColumn() {
    if (cProfileColumnIndex < 0) return;

    int viewCol = table.convertColumnIndexToView(cProfileColumnIndex);
    if (viewCol < 0) return;

    SwingUtilities.invokeLater(() -> {
      Rectangle cellRect = table.getCellRect(0, viewCol, true);

      java.awt.Container parent = table.getParent();
      if (parent instanceof JViewport) {
        JViewport viewport = (JViewport) parent;
        int viewportWidth = viewport.getExtentSize().width;
        int columnCenter = cellRect.x + cellRect.width / 2;
        int scrollX = Math.max(0, columnCenter - viewportWidth / 2);
        viewport.setViewPosition(new java.awt.Point(scrollX, viewport.getViewPosition().y));
      }
    });
  }

  private void computeAndUpdateFilterPercentages() {
    if (currentSeriesColorMap == null || currentSeriesColorMap.isEmpty() || cProfileColumnIndex < 0) {
      return;
    }

    Map<String, Double> counts = new LinkedHashMap<>();
    for (String key : currentSeriesColorMap.keySet()) {
      counts.put(key, 0.0);
    }

    int totalRows = tableModel.getRowCount();
    for (int row = 0; row < totalRows; row++) {
      Object value = tableModel.getValueAt(row, cProfileColumnIndex);
      if (value != null) {
        String strValue = value.toString();
        if (counts.containsKey(strValue)) {
          counts.put(strValue, counts.get(strValue) + 1);
        }
      }
    }

    double total = counts.values().stream().mapToDouble(Double::doubleValue).sum();
    Map<String, Double> percentages = new LinkedHashMap<>();
    for (Map.Entry<String, Double> entry : counts.entrySet()) {
      double pct = total > 0 ? (entry.getValue() / total) * 100.0 : 0.0;
      percentages.put(entry.getKey(), pct);
    }

    filterStrip.loadSeries(currentSeriesColorMap, percentages);
  }

  private void onFilterChanged(Set<String> activeFilters) {
    applyVisualFilter(activeFilters);
  }

  private void applyVisualFilter(Set<String> activeFilters) {
    if (activeFilters == null || activeFilters.isEmpty()) {
      rowSorter.setRowFilter(null);
      log.debug("Filter cleared — showing all rows");
    } else if (cProfileColumnIndex >= 0) {
      rowSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
        @Override
        public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
          Object value = entry.getValue(cProfileColumnIndex);
          if (value == null) return false;
          String strValue = value.toString();
          return activeFilters.contains(strValue);
        }
      });
      log.debug("Filter applied — showing {} series: {}", activeFilters.size(), activeFilters);
      scrollToCProfileColumn();
    }

    updateFilteredRowCount();
    table.repaint();
  }

  private void updateFilteredRowCount() {
    int visibleRows = table.getRowCount();
    int totalRows = tableModel.getRowCount();

    if (filterStrip.hasActiveFilters()) {
      titledBorder.setTitle("Selected rows: " + visibleRows + " / " + totalRows);
    } else {
      titledBorder.setTitle("Selected rows: " + totalRows);
    }

    repaint();
  }

  private int findCProfileColumnIndex() {
    if (cProfile == null) return -1;

    List<CProfile> profiles = tableInfo.getCProfiles();
    for (int i = 0; i < profiles.size(); i++) {
      if (profiles.get(i).getColName().equals(cProfile.getColName())) {
        return i;
      }
    }
    return -1;
  }

  protected abstract void loadResultSet(String tableName,
                                        long begin,
                                        long end);

  protected abstract void loadRawData(String tableName,
                                      long begin,
                                      long end);

  protected void loadToModel(List<List<Object>> rawData) {
    List<CProfile> timeStampIndex = tableInfo.getCProfiles()
        .stream()
        .filter(f -> (f.getCsType().isTimeStamp() || f.getColDbTypeName().contains("TIMESTAMP")))
        .toList();

    if (rawData == null || rawData.isEmpty()) {
      if (useFetchSize) {
        if (hasData) {
          JOptionPane.showMessageDialog(this,
                                        "No raw data found", "Warning", JOptionPane.WARNING_MESSAGE);
        }
      } else {
        log.warn("No raw data found");
      }
    } else {
      rowCount = rowCount + rawData.size();
      jLabelRowCount.setText("Rows: " + rowCount);

      rawData.forEach(row -> {
        Object[] rawObj = row.toArray();

        timeStampIndex.forEach(cProfile -> {
          if (cProfile.getCsType().isTimeStamp()) {
            long tsValue = Mapper.convertRawToLong(rawObj[cProfile.getColId()], cProfile);
            rawObj[cProfile.getColId()] = getDate(tsValue);
          }
        });

        tableModel.addRow(rawObj);
      });
    }

    computeAndUpdateFilterPercentages();

    if (filterStrip.hasActiveFilters()) {
      applyVisualFilter(filterStrip.getActiveFilters());
    } else {
      updateFilteredRowCount();
    }

    table.packAll();
  }

  protected String getDate(long l) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneId.systemDefault()).format(FORMATTER);
  }

  private String[] getColumnHeaders() {
    String[] columnHeaders = new String[tableInfo.getCProfiles().size()];
    AtomicInteger at = new AtomicInteger(0);

    tableInfo.getCProfiles().forEach(e -> columnHeaders[at.getAndIncrement()] = e.getColName());

    return columnHeaders;
  }

  private JXCollapsiblePane connectCollapsibleFindBarWithTable() {
    final JXCollapsiblePane collapsible = new JXCollapsiblePane();
    this.table.putClientProperty(AbstractSearchable.MATCH_HIGHLIGHTER, Boolean.TRUE);
    this.findBar.setSearchable(this.table.getSearchable());
    collapsible.add(this.findBar);
    collapsible.setCollapsed(false);

    Action openFindBar = new AbstractActionExt() {
      public void actionPerformed(ActionEvent e) {
        collapsible.setCollapsed(false);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(findBar);
      }
    };
    Action closeFindBar = new AbstractActionExt() {
      public void actionPerformed(ActionEvent e) {
        collapsible.setCollapsed(true);
        table.requestFocusInWindow();
      }
    };

    this.table.getActionMap().put("find", openFindBar);
    this.findBar.getActionMap().put("close", closeFindBar);

    return collapsible;
  }
}