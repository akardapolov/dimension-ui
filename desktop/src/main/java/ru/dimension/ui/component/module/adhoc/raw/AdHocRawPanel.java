package ru.dimension.ui.component.module.adhoc.raw;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
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
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTaskPane;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.db.model.profile.table.TType;
import ru.dimension.db.service.mapping.Mapper;
import ru.dimension.db.sql.BatchResultSet;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.gantt.DrawingScale;
import ru.dimension.ui.model.gantt.GanttColumn;
import ru.dimension.ui.view.detail.GanttCommon;
import ru.dimension.ui.view.detail.raw.RawDataTransposePanel;
import ext.egantt.swing.GanttTable;

import static ru.dimension.ui.laf.LafColorGroup.TABLE_BACKGROUND;
import static ru.dimension.ui.laf.LafColorGroup.TABLE_FONT;

@Log4j2
public class AdHocRawPanel extends JXTaskPane implements GanttFilterHeaderRenderer.GanttFilterCallback {

  private static final int FETCH_SIZE = 1000;
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
  private static final int TRIANGLE_SIZE = 8;

  private final DStore dStore;
  private final TProfile tProfile;

  @Getter
  private final CProfile cProfile;

  private final JXTable table;
  private final DefaultTableModel tableModel;
  private final JButton fetchNextButton;
  private final JLabel rowCountLabel;
  private final TitledBorder titledBorder;

  private BatchResultSet batchResultSet;

  private int rowCount = 0;
  private boolean hasData = false;

  private int pivotModelIndex = -1;
  private CProfile pivotCProfile = null;

  private final LinkedHashSet<String> activeGanttFilters = new LinkedHashSet<>();
  private final Map<String, Color> ganttSeriesColorMap = new LinkedHashMap<>();

  private TableRowSorter<DefaultTableModel> rowSorter;

  private JXTable currentGanttTable;
  private AdHocGanttHelper ganttHelper;

  private final String panelKey;

  private JScrollPane tableScrollPane;

  private final Map<Integer, JXTable> columnGanttTables = new HashMap<>();
  private final Map<Integer, Map<String, Color>> columnColorMaps = new HashMap<>();
  private final Map<Integer, List<GanttColumn>> columnGanttData = new HashMap<>();

  private final ColorStripViewportOverlay colorStripOverlay;
  private JPanel tableWithStripPanel;
  private JPanel contentPanel;

  private GanttFilterHeaderRenderer headerRenderer;
  private GanttFilterPopup filterPopup;
  private int activePopupModelIndex = -1;

  private final JCheckBox transposeCheckBox;
  private final RawDataTransposePanel transposePanel;
  private JSplitPane splitPane;

  public AdHocRawPanel(DStore dStore,
                       TProfile tProfile,
                       CProfile cProfile) {
    this(dStore, tProfile, cProfile, null);
  }

  public AdHocRawPanel(DStore dStore,
                       TProfile tProfile,
                       CProfile cProfile,
                       String panelKey) {
    this.dStore = dStore;
    this.tProfile = tProfile;
    this.cProfile = cProfile;
    this.panelKey = panelKey != null ? panelKey : tProfile.getTableName();

    this.setAnimated(false);
    ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder());

    this.ganttHelper = new AdHocGanttHelper();

    String[] columnHeaders = buildColumnHeaders();
    this.tableModel = new DefaultTableModel(columnHeaders, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    this.table = new JXTable(tableModel);
    this.table.setEditable(false);
    this.table.setColumnControlVisible(true);
    this.table.setHorizontalScrollEnabled(true);
    this.table.setShowVerticalLines(true);
    this.table.setShowHorizontalLines(true);
    this.table.setGridColor(Color.GRAY);
    this.table.setIntercellSpacing(new Dimension(1, 1));
    this.table.setBackground(LaF.getBackgroundColor(TABLE_BACKGROUND, LaF.getLafType()));
    this.table.setForeground(LaF.getBackgroundColor(TABLE_FONT, LaF.getLafType()));

    this.rowSorter = new TableRowSorter<>(tableModel);
    this.table.setRowSorter(rowSorter);

    this.headerRenderer = new GanttFilterHeaderRenderer(this);
    this.headerRenderer.installMouseListener(table);
    applyHeaderRenderer();

    this.table.getTableHeader().setPreferredSize(
        new Dimension(0, table.getRowHeight() * 2));

    this.filterPopup = new GanttFilterPopup(() -> {
      if (activePopupModelIndex >= 0) {
        handlePopupClosed();
      }
    });

    this.rowCountLabel = new JLabel("Rows: 0");

    this.fetchNextButton = new JButton("Next " + FETCH_SIZE + " rows");
    this.fetchNextButton.addActionListener(this::onFetchNext);

    this.titledBorder = new TitledBorder("Rows: 0");
    this.titledBorder.setTitleJustification(TitledBorder.RIGHT);

    this.colorStripOverlay = new ColorStripViewportOverlay();
    this.colorStripOverlay.setVisible(false);
    this.colorStripOverlay.setActive(false);

    this.transposePanel = new RawDataTransposePanel();
    this.transposeCheckBox = new JCheckBox("Transpose");
    this.transposeCheckBox.setSelected(false);
    this.transposeCheckBox.addActionListener(e -> toggleTranspose());

    this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && transposeCheckBox.isSelected()) {
          fireTransposeUpdate();
        }
      }
    });

    this.contentPanel = buildContentPanel();
  }

  public Runnable initializeUI() throws InterruptedException {
    loadInitialData();
    return () -> PGHelper.cellXYRemainder(this, contentPanel, 1, false);
  }

  private void applyHeaderRenderer() {
    for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
      table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
    }
  }

  @Override
  public void onFilterButtonClicked(int modelColumnIndex, int screenX, int screenY, int columnWidth) {
    if (activePopupModelIndex == modelColumnIndex && filterPopup.isVisible()) {
      filterPopup.close();
      return;
    }

    if (filterPopup.isVisible()) {
      filterPopup.dispose();
      handleFilterReset();
    }

    handlePivotSelectedData(modelColumnIndex);

    JXTable ganttTable = columnGanttTables.get(modelColumnIndex);
    activePopupModelIndex = modelColumnIndex;

    filterPopup.show(table, ganttTable, screenX, screenY, columnWidth);

    table.getTableHeader().repaint();
  }

  @Override
  public boolean isFilterActiveForColumn(int modelColumnIndex) {
    return activePopupModelIndex == modelColumnIndex && filterPopup.isVisible();
  }

  private void handlePopupClosed() {
    handleFilterReset();
    activePopupModelIndex = -1;
    table.getTableHeader().repaint();
  }

  private void handleFilterReset() {
    resetGanttSelection();
    clearCurrentPivot();
    pivotModelIndex = -1;
    pivotCProfile = null;
    table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer());
    deactivateColorStrip();
    updateRowCountDisplay();
  }

  private void resetGanttSelection() {
    if (pivotModelIndex >= 0) {
      JXTable ganttTable = columnGanttTables.get(pivotModelIndex);
      if (ganttTable != null) {
        for (int i = 0; i < ganttTable.getModel().getRowCount(); i++) {
          ganttTable.getModel().setValueAt(false, i, 2);
        }
        ganttTable.repaint();
      }
    }
  }

  private JPanel buildContentPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createCompoundBorder(
        titledBorder,
        BorderFactory.createCompoundBorder(
            new EmptyBorder(4, 4, 4, 4),
            BorderFactory.createMatteBorder(0, 0, 2, 0, Color.GRAY)
        )
    ));

    JPanel topPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 4, 0));
    topPanel.add(rowCountLabel);
    topPanel.add(transposeCheckBox);
    topPanel.add(fetchNextButton);

    tableScrollPane = new JScrollPane(table,
                                      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    tableScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

    tableScrollPane.getViewport().addChangeListener(e -> refreshColorStrip());

    colorStripOverlay.linkTable(table, tableScrollPane);

    tableWithStripPanel = new JPanel(new BorderLayout());
    tableWithStripPanel.add(tableScrollPane, BorderLayout.CENTER);
    tableWithStripPanel.add(colorStripOverlay, BorderLayout.EAST);

    panel.add(topPanel, BorderLayout.NORTH);
    panel.add(tableWithStripPanel, BorderLayout.CENTER);

    return panel;
  }

  private String[] buildColumnHeaders() {
    List<CProfile> profiles = tProfile.getCProfiles();
    String[] headers = new String[profiles.size()];
    for (int i = 0; i < profiles.size(); i++) {
      headers[i] = profiles.get(i).getColName();
    }
    return headers;
  }

  private void loadInitialData() {
    String tableName = tProfile.getTableName();
    log.info("Loading initial raw data via DStore for table: {}", tableName);

    boolean regular = TType.REGULAR.equals(tProfile.getTableType());

    if (regular) {
      batchResultSet = dStore.getBatchResultSet(tableName, FETCH_SIZE);
    } else {
      batchResultSet = dStore.getBatchResultSet(tableName, 0, Long.MAX_VALUE, FETCH_SIZE);
    }

    fetchNextBatch();
  }

  private void onFetchNext(ActionEvent e) {
    if (batchResultSet == null) {
      JOptionPane.showMessageDialog(this,
                                    "No data source available", "Info", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    log.info("Fetching next batch of {} rows for table: {}", FETCH_SIZE, tProfile.getTableName());
    fetchNextBatch();
  }

  private void fetchNextBatch() {
    List<List<Object>> rawData = batchResultSet.next()
        ? batchResultSet.getObject()
        : Collections.emptyList();

    if (rawData == null || rawData.isEmpty()) {
      if (!hasData) {
        log.warn("No raw data found for table: {}", tProfile.getTableName());
        JOptionPane.showMessageDialog(this,
                                      "No data found in table: " + tProfile.getTableName(),
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
      } else {
        JOptionPane.showMessageDialog(this,
                                      "No more data available",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
      }
      fetchNextButton.setEnabled(false);
      return;
    }

    hasData = true;

    loadToModel(rawData);

    if (rawData.size() < FETCH_SIZE) {
      fetchNextButton.setEnabled(false);
    }

    table.packAll();
    refreshColorStrip();

    computeGanttFromTableModel();
    applyHeaderRenderer();

    log.info("Loaded {} rows, total: {} for table: {}",
             rawData.size(), rowCount, tProfile.getTableName());
  }

  private void loadToModel(List<List<Object>> rawData) {
    List<CProfile> timestampProfiles = tProfile.getCProfiles()
        .stream()
        .filter(f -> f.getCsType().isTimeStamp()
            || f.getColDbTypeName().contains("TIMESTAMP"))
        .toList();

    rawData.forEach(row -> {
      Object[] rawObj = row.toArray();

      timestampProfiles.forEach(cp -> {
        if (cp.getCsType().isTimeStamp()) {
          long tsValue = Mapper.convertRawToLong(rawObj[cp.getColId()], cp);
          rawObj[cp.getColId()] = formatDate(tsValue);
        }
      });

      tableModel.addRow(rawObj);
    });

    rowCount += rawData.size();
    updateRowCountDisplay();
  }

  private String formatDate(long epochMillis) {
    return LocalDateTime.ofInstant(
        Instant.ofEpochMilli(epochMillis),
        ZoneId.systemDefault()
    ).format(FORMATTER);
  }

  private void updateRowCountDisplay() {
    String text;
    if (!activeGanttFilters.isEmpty()) {
      int visibleRows = table.getRowCount();
      text = "Rows: " + visibleRows + " / " + rowCount;
    } else {
      text = "Rows: " + rowCount;
    }
    rowCountLabel.setText(text);
    titledBorder.setTitle(text);
    repaint();
  }

  private void refreshColorStrip() {
    if (!colorStripOverlay.isActive() || pivotModelIndex < 0) return;

    SwingUtilities.invokeLater(() -> {
      List<ColorStripViewportOverlay.RowColorEntry> entries = buildColorStripEntries();
      colorStripOverlay.setRowEntries(entries);
      colorStripOverlay.repaint();
    });
  }

  private List<ColorStripViewportOverlay.RowColorEntry> buildColorStripEntries() {
    List<ColorStripViewportOverlay.RowColorEntry> entries = new ArrayList<>();

    if (pivotModelIndex < 0 || ganttSeriesColorMap.isEmpty()) {
      return entries;
    }

    int viewRowCount = table.getRowCount();
    for (int viewRow = 0; viewRow < viewRowCount; viewRow++) {
      int modelRow = table.convertRowIndexToModel(viewRow);
      Object value = tableModel.getValueAt(modelRow, pivotModelIndex);
      String key = value != null ? value.toString() : "(null)";

      Color color = ganttSeriesColorMap.getOrDefault(key, Color.DARK_GRAY);
      entries.add(new ColorStripViewportOverlay.RowColorEntry(color, key));
    }

    return entries;
  }

  private void activateColorStrip() {
    colorStripOverlay.setActive(true);
    colorStripOverlay.setVisible(true);
    refreshColorStrip();

    tableWithStripPanel.revalidate();
    tableWithStripPanel.repaint();
  }

  private void deactivateColorStrip() {
    colorStripOverlay.setActive(false);
    colorStripOverlay.setVisible(false);
    colorStripOverlay.setRowEntries(Collections.emptyList());

    tableWithStripPanel.revalidate();
    tableWithStripPanel.repaint();
  }

  private void handlePivotSelectedData(int modelColumnIndex) {
    pivotModelIndex = modelColumnIndex;

    List<CProfile> profiles = tProfile.getCProfiles();
    if (modelColumnIndex >= 0 && modelColumnIndex < profiles.size()) {
      pivotCProfile = profiles.get(modelColumnIndex);
    } else {
      pivotCProfile = null;
    }

    activateColumnGantt(modelColumnIndex);

    table.setDefaultRenderer(Object.class, new PivotTriangleCellRenderer());
    activateColorStrip();

    if (transposePanel != null) {
      transposePanel.setSeriesColorMap(ganttSeriesColorMap);
      if (pivotCProfile != null) {
        transposePanel.setHighlightColumnName(pivotCProfile.getColName());
      }
    }
  }

  private void clearCurrentPivot() {
    activeGanttFilters.clear();
    ganttSeriesColorMap.clear();
    currentGanttTable = null;
    rowSorter.setRowFilter(null);

    if (transposePanel != null) {
      transposePanel.setSeriesColorMap(null);
      transposePanel.setHighlightColumnName(null);
    }
  }

  private void computeGanttFromTableModel() {
    columnGanttTables.clear();
    columnColorMaps.clear();
    columnGanttData.clear();

    for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
      int modelIdx = table.getColumnModel().getColumn(i).getModelIndex();
      computeGanttForColumn(modelIdx);
    }
  }

  private void computeGanttForColumn(int modelIndex) {
    Map<String, Long> countMap = new LinkedHashMap<>();
    for (int row = 0; row < tableModel.getRowCount(); row++) {
      Object value = tableModel.getValueAt(row, modelIndex);
      String key = value != null ? value.toString() : "(null)";
      countMap.merge(key, 1L, Long::sum);
    }

    if (countMap.isEmpty()) return;

    Map<String, Color> colorMap = new LinkedHashMap<>();
    Random rnd = new Random(42 + modelIndex);
    for (String key : countMap.keySet()) {
      Color color = new Color(
          50 + rnd.nextInt(150),
          50 + rnd.nextInt(150),
          50 + rnd.nextInt(150)
      );
      colorMap.put(key, color);
    }

    List<GanttColumn> ganttColumnList = new ArrayList<>();
    for (Map.Entry<String, Long> entry : countMap.entrySet()) {
      GanttColumn gc = new GanttColumn();
      gc.setKey(entry.getKey());
      gc.setGantt(new HashMap<>(Map.of(entry.getKey(), entry.getValue().doubleValue())));
      ganttColumnList.add(gc);
    }

    List<CProfile> profiles = tProfile.getCProfiles();
    CProfile colProfile = (modelIndex >= 0 && modelIndex < profiles.size())
        ? profiles.get(modelIndex) : pivotCProfile;

    DrawingScale drawingScale = new DrawingScale();

    JXTable ganttTable = ganttHelper.loadGantt(
        colProfile,
        ganttColumnList,
        colorMap,
        drawingScale,
        5,
        20
    );

    if (ganttTable == null) return;

    setupGanttCheckboxEditor(ganttTable, modelIndex);

    columnGanttTables.put(modelIndex, ganttTable);
    columnColorMaps.put(modelIndex, colorMap);
    columnGanttData.put(modelIndex, ganttColumnList);
  }

  private void setupGanttCheckboxEditor(JXTable ganttTable, int sourceModelIndex) {
    ganttTable.getColumnExt(ColumnNames.NAME.ordinal()).setVisible(true);

    DefaultCellEditor pickEditor = new DefaultCellEditor(new JCheckBox());
    TableColumn pickColumn = null;
    for (int i = 0; i < ganttTable.getColumnCount(); i++) {
      TableColumn col = ganttTable.getColumnModel().getColumn(i);
      if (col.getModelIndex() == 2) {
        pickColumn = col;
        break;
      }
    }

    if (pickColumn != null) {
      pickColumn.setCellEditor(pickEditor);
      pickColumn.setMinWidth(30);
      pickColumn.setMaxWidth(35);

      pickEditor.addCellEditorListener(new CellEditorListener() {
        @Override
        public void editingStopped(ChangeEvent e) {
          TableCellEditor editor = (TableCellEditor) e.getSource();
          Boolean checked = (Boolean) editor.getCellEditorValue();

          int selRow = ganttTable.getSelectedRow();
          if (selRow < 0) return;
          int modelRow = ganttTable.convertRowIndexToModel(selRow);

          Object val = ganttTable.getModel().getValueAt(modelRow, 1);
          String key = val != null ? val.toString() : "";

          if (checked) {
            activeGanttFilters.add(key);
          } else {
            activeGanttFilters.remove(key);
          }

          applyGanttFilterForColumn(sourceModelIndex);
          refreshColorStrip();

          SwingUtilities.invokeLater(() -> {
            ganttTable.repaint();
            table.repaint();
            if (currentGanttTable != null) {
              currentGanttTable.repaint();
            }
          });
        }

        @Override
        public void editingCanceled(ChangeEvent e) { }
      });
    }
  }

  private void activateColumnGantt(int modelIndex) {
    Map<String, Color> colorMap = columnColorMaps.get(modelIndex);
    if (colorMap != null) {
      ganttSeriesColorMap.clear();
      ganttSeriesColorMap.putAll(colorMap);
    }

    JXTable ganttTable = columnGanttTables.get(modelIndex);
    if (ganttTable != null) {
      currentGanttTable = ganttTable;
    }

    updateRowCountDisplay();
    table.repaint();
    refreshColorStrip();
  }

  private void applyGanttFilterForColumn(int filterModelIndex) {
    if (activeGanttFilters.isEmpty()) {
      rowSorter.setRowFilter(null);
    } else {
      rowSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
        @Override
        public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
          Object value = entry.getValue(filterModelIndex);
          if (value == null) return false;
          return activeGanttFilters.contains(value.toString());
        }
      });
    }
    updateRowCountDisplay();
    table.repaint();
  }

  private void toggleTranspose() {
    if (transposeCheckBox.isSelected()) {
      if (table.getRowCount() > 0 && table.getSelectedRow() == -1) {
        JViewport viewport = tableScrollPane.getViewport();
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

      contentPanel.remove(tableWithStripPanel);

      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableWithStripPanel, transposePanel);
      splitPane.setResizeWeight(0.7);
      splitPane.setDividerSize(5);
      splitPane.setOneTouchExpandable(true);

      contentPanel.add(splitPane, BorderLayout.CENTER);

      fireTransposeUpdate();
    } else {
      if (splitPane != null) {
        contentPanel.remove(splitPane);
        splitPane = null;
      }

      transposePanel.clear();
      contentPanel.add(tableWithStripPanel, BorderLayout.CENTER);
    }

    contentPanel.revalidate();
    contentPanel.repaint();
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

  private class AdHocGanttHelper extends GanttCommon {

    public AdHocGanttHelper() {
      super();
    }

    @Override
    protected void initUI() {
    }

    @Override
    public JXTable loadGantt(CProfile firstLevelGroupBy,
                             List<GanttColumn> ganttColumnList,
                             Map<String, Color> seriesColorMap,
                             DrawingScale drawingScale,
                             int visibleRowCount,
                             int rowHeightForJTable) {

      if (ganttColumnList == null || ganttColumnList.isEmpty()) {
        return null;
      }

      String[][] columnNames = {{"Activity %", firstLevelGroupBy.getColName(), ColumnNames.PICK.getColName()}};

      sortGanttColumns(ganttColumnList);

      Object[][] data = createData(firstLevelGroupBy, columnNames, ganttColumnList, drawingScale, seriesColorMap);

      GanttTable ganttTable = new GanttTable(
          data,
          columnNames,
          getBasicJTableList(),
          seriesColorMap
      );

      setGanttTableParameters(visibleRowCount, rowHeightForJTable, ganttTable);
      setTableHeaderFont(ganttTable, new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
      setTooltipAndPercent(ganttTable);

      JXTable jxTable = ganttTable.getJXTable();
      if (jxTable == null) return null;

      jxTable.setShowGrid(true);
      jxTable.setShowVerticalLines(true);
      jxTable.setShowHorizontalLines(true);
      jxTable.setGridColor(Color.GRAY);
      jxTable.setIntercellSpacing(new Dimension(1, 1));
      jxTable.setBackground(LaF.getBackgroundColor(LafColorGroup.TABLE_BACKGROUND, LaF.getLafType()));
      jxTable.setForeground(LaF.getBackgroundColor(LafColorGroup.TABLE_FONT, LaF.getLafType()));

      return jxTable;
    }

    private Object[][] createData(CProfile firstLevelGroupBy,
                                  String[][] columnNames,
                                  List<GanttColumn> ganttColumnList,
                                  DrawingScale drawingScale,
                                  Map<String, Color> seriesColorMap) {

      Object[][] data = new Object[ganttColumnList.size()][columnNames[0].length];
      final ext.egantt.swing.GanttDrawingPartHelper partHelper = new ext.egantt.swing.GanttDrawingPartHelper();

      double countOfAllRowsId = ganttColumnList.stream()
          .map(GanttColumn::getGantt)
          .filter(Objects::nonNull)
          .map(Map::values)
          .flatMap(Collection::stream)
          .mapToDouble(Double::doubleValue)
          .sum();

      int rowNumber = 0;
      for (GanttColumn ganttColumn : ganttColumnList) {
        data[rowNumber][0] = createDrawingState(drawingScale, partHelper, ganttColumn, countOfAllRowsId);
        data[rowNumber][1] = ganttColumn.getKey();
        data[rowNumber][2] = activeGanttFilters.contains(ganttColumn.getKey());

        rowNumber++;
      }

      return data;
    }
  }

  private class PivotTriangleCellRenderer extends DefaultTableCellRenderer {
    private Color triangleColor = null;

    @Override
    public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
      Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);

      triangleColor = null;

      if (pivotModelIndex >= 0 && !ganttSeriesColorMap.isEmpty()) {
        int modelCol = ((JXTable) tbl).convertColumnIndexToModel(column);
        if (modelCol == pivotModelIndex && value != null) {
          triangleColor = ganttSeriesColorMap.get(value.toString());
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
}