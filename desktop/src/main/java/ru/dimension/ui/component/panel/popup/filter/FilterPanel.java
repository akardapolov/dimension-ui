package ru.dimension.ui.component.panel.popup.filter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.tt.api.TT;
import ru.dimension.tt.api.TTRegistry;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.tt.swing.TableUi;
import ru.dimension.tt.swingx.JXTableTables;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.panel.popup.base.ConfigPopupPanel;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.date.DateLocale;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.view.table.icon.ModelIconProviders;
import ru.dimension.ui.view.table.row.Rows.ColumnRow;

@Log4j2
@Data
@EqualsAndHashCode(callSuper = true)
public class FilterPanel extends ConfigPopupPanel {
  private final MessageBroker.Component component;

  private final JTextField columnsSearch;
  private final JTextField filterSearch;

  private final TTTable<ColumnRow, JXTable> columnsTable;
  private TableRowSorter<?> columnSorter;

  private final GanttPopupPanel filtersPanel;

  private TableInfo tableInfo;
  private Metric metric;
  private SeriesType seriesType = SeriesType.COMMON;
  protected Map<String, Color> seriesColorMap = new HashMap<>();
  private DStore dStore;
  private long begin;
  private long end;

  private List<CProfile> allColumns = new ArrayList<>();

  private CProfile selectedColumn;
  private RealtimeStateProvider realtimeStateProvider;

  private JSlider opacitySlider;
  private JButton closeButton;
  private JButton clearButton;

  public FilterPanel(MessageBroker.Component component) {
    super(JPanel::new, "Filter >>", "Filter <<");

    this.component = component;

    columnsSearch = new JTextField();
    filterSearch = new JTextField();

    filtersPanel = new GanttPopupPanel(component);
    filtersPanel.setOnFilterChanged(this::updateClearButtonState);

    TTRegistry registry = TT.builder()
        .scanPackages("ru.dimension.ui.view.table.row")
        .build();

    this.columnsTable = createColumnTable(registry);

    this.opacitySlider = createOpacitySlider();
    this.closeButton = createCloseButton();
    this.clearButton = createClearButton();

    initializeListeners();

    updateContent(this::createPopupContent);
  }

  private JButton createCloseButton() {
    JButton btn = new JButton("✕");
    btn.setMargin(new Insets(0, 4, 0, 4));
    btn.setFocusable(false);
    btn.setToolTipText("Close filter panel");
    btn.addActionListener(e -> closePopup());
    return btn;
  }

  private JButton createClearButton() {
    JButton btn = new JButton("Clear");
    btn.setMargin(new Insets(0, 8, 0, 8));
    btn.setFocusable(false);
    btn.setEnabled(false);
    btn.setToolTipText("Clear all filters");
    btn.addActionListener(e -> filtersPanel.clearAllFilters());
    return btn;
  }

  private JSlider createOpacitySlider() {
    JSlider slider = new JSlider(JSlider.HORIZONTAL, 10, 100, 100);
    slider.setPreferredSize(new Dimension(150, 40));
    slider.setMajorTickSpacing(30);
    slider.setMinorTickSpacing(10);
    slider.setPaintTicks(true);
    slider.setSnapToTicks(false);

    Hashtable<Integer, JLabel> labels = new Hashtable<>();
    labels.put(10, new JLabel("10%"));
    labels.put(40, new JLabel("40%"));
    labels.put(70, new JLabel("70%"));
    labels.put(100, new JLabel("100%"));
    slider.setLabelTable(labels);

    slider.addChangeListener(e -> {
      float opacity = slider.getValue() / 100.0f;
      setPopupOpacity(opacity);
      slider.setToolTipText("Opacity: " + slider.getValue() + "%");
    });

    return slider;
  }

  private TTTable<ColumnRow, JXTable> createColumnTable(TTRegistry registry) {
    TTTable<ColumnRow, JXTable> tt = JXTableTables.create(
        registry,
        ColumnRow.class,
        TableUi.<ColumnRow>builder()
            .rowIcon(ModelIconProviders.forColumnRow())
            .rowIconInColumn("name")
            .build()
    );

    JXTable table = tt.table();
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(java.awt.Color.GRAY);
    table.setIntercellSpacing(new java.awt.Dimension(1, 1));

    table.setEditable(false);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    if (table.getColumnExt("ID") != null) {
      table.getColumnExt("ID").setVisible(false);
    }

    if (table.getColumnExt("Pick") != null) {
      table.getColumnExt("Pick").setVisible(false);
    } else if (table.getColumnExt("pick") != null) {
      table.getColumnExt("pick").setVisible(false);
    }

    columnSorter = new TableRowSorter<>(tt.model());
    table.setRowSorter(columnSorter);

    return tt;
  }

  private JPanel createPopupContent() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setPreferredSize(new Dimension(500, 300));
    panel.setBorder(new EtchedBorder());

    JPanel topBar = new JPanel(new BorderLayout());

    JPanel leftTopBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
    leftTopBar.add(new JLabel("Opacity:"));
    leftTopBar.add(opacitySlider);

    JPanel rightTopBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
    rightTopBar.add(clearButton);
    rightTopBar.add(closeButton);

    topBar.add(leftTopBar, BorderLayout.WEST);
    topBar.add(rightTopBar, BorderLayout.EAST);

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.add(columnsSearch, BorderLayout.NORTH);
    leftPanel.add(columnsTable.scrollPane(), BorderLayout.CENTER);

    JPanel rightPanel = new JPanel(new BorderLayout());
    rightPanel.add(filterSearch, BorderLayout.NORTH);
    rightPanel.add(filtersPanel, BorderLayout.CENTER);

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
    splitPane.setDividerLocation(200);
    splitPane.setResizeWeight(0.5);
    splitPane.setOneTouchExpandable(true);
    splitPane.setContinuousLayout(true);

    leftPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    rightPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

    PainlessGridBag gblTable = new PainlessGridBag(panel, PGHelper.getPGConfig(1), false);

    gblTable.row()
        .cellXRemainder(topBar).fillX();
    gblTable.row()
        .cellXYRemainder(splitPane).fillXY();

    gblTable.done();

    return panel;
  }

  private void initializeListeners() {
    columnsSearch.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) { filterColumns(); }
      @Override
      public void removeUpdate(DocumentEvent e) { filterColumns(); }
      @Override
      public void changedUpdate(DocumentEvent e) { filterColumns(); }
    });

    filterSearch.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) { filterFilters(); }
      @Override
      public void removeUpdate(DocumentEvent e) { filterFilters(); }
      @Override
      public void changedUpdate(DocumentEvent e) { filterFilters(); }
    });

    columnsTable.table().getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        int viewRow = columnsTable.table().getSelectedRow();
        if (viewRow >= 0) {
          int modelRow = columnsTable.table().convertRowIndexToModel(viewRow);
          ColumnRow rowItem = columnsTable.model().itemAt(modelRow);

          handleColumnSelection(rowItem);
        } else {
          clearFilters();
        }
      }
    });
  }

  private void handleColumnSelection(ColumnRow rowItem) {
    if (rowItem == null) return;

    this.selectedColumn = allColumns.stream()
        .filter(p -> p.getColName().equals(rowItem.getName()))
        .findFirst()
        .orElse(null);

    if (this.selectedColumn != null) {
      filterSearch.setText("");
      loadFiltersForColumn(this.selectedColumn);
    }
  }

  public void initializeChartPanel(ChartKey chartKey, TableInfo tableInfo, Panel panelType) {
    this.tableInfo = tableInfo;

    this.filtersPanel.setChartKey(chartKey);
    this.filtersPanel.setPanelType(panelType);

    loadColumns();
  }

  public void setDataSource(DStore dStore, Metric metric, long begin, long end) {
    this.dStore = dStore;
    this.metric = metric;
    this.begin = begin;
    this.end = end;
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    updateComponents();
  }

  private void updateComponents() {
    boolean panelEnabled = super.isEnabled();

    columnsSearch.setEnabled(panelEnabled);
    columnsTable.table().setEnabled(panelEnabled);
    filterSearch.setEnabled(panelEnabled);
    filtersPanel.setEnabled(panelEnabled);
    opacitySlider.setEnabled(panelEnabled);
    closeButton.setEnabled(panelEnabled);
    clearButton.setEnabled(panelEnabled && hasActiveFilters());
  }

  private void updateClearButtonState() {
    boolean panelEnabled = super.isEnabled();
    clearButton.setEnabled(panelEnabled && hasActiveFilters());
  }

  public void clearFilterPanel() {
    columnsTable.table().clearSelection();
    filtersPanel.clear();

    columnsSearch.setText("");
    filterSearch.setText("");
  }

  public boolean hasActiveFilters() {
    Map<CProfile, LinkedHashSet<String>> filterMap = filtersPanel.getFilterSelectedMap();
    return filterMap != null
        && !filterMap.isEmpty()
        && filterMap.values().stream().anyMatch(set -> set != null && !set.isEmpty());
  }

  public Map<CProfile, LinkedHashSet<String>> getActiveFilters() {
    return filtersPanel.getFilterSelectedMap();
  }

  private void loadColumns() {
    if (tableInfo == null) return;

    allColumns = tableInfo.getCProfiles().stream()
        .filter(profile -> !profile.getCsType().isTimeStamp())
        .collect(Collectors.toList());

    List<ColumnRow> rows = allColumns.stream()
        .map(cProfile -> new ColumnRow(cProfile, false))
        .collect(Collectors.toList());

    columnsTable.setItems(rows);
  }

  private void filterColumns() {
    String searchText = columnsSearch.getText();
    if (columnsTable == null || columnSorter == null) return;

    if (searchText == null || searchText.isEmpty()) {
      columnSorter.setRowFilter(null);
    } else {
      try {
        int nameIndex = columnsTable.model().schema().modelIndexOf("name");
        if (nameIndex >= 0) {
          columnSorter.setRowFilter(RowFilter.regexFilter("(?iu)" + searchText, nameIndex));
        }
      } catch (PatternSyntaxException e) {
        log.warn("Invalid regex pattern", e);
        columnSorter.setRowFilter(null);
      }
    }
  }

  private void loadFiltersForColumn(CProfile column) {
    if (dStore == null || tableInfo == null) return;

    long currentBegin = this.begin;
    long currentEnd = this.end;
    Map<String, Color> currentSeriesColorMap = this.seriesColorMap;

    if (realtimeStateProvider != null) {
      currentBegin = realtimeStateProvider.provideCurrentBegin();
      currentEnd = realtimeStateProvider.provideCurrentEnd();
      currentSeriesColorMap = realtimeStateProvider.provideCurrentSeriesColorMap();
    }

    log.info(DateHelper.getDateFormatted(DateLocale.RU, currentBegin));
    log.info(DateHelper.getDateFormatted(DateLocale.RU, currentEnd));

    filtersPanel.setTableInfo(tableInfo);
    filtersPanel.setMetric(metric);
    filtersPanel.setDStore(dStore);
    filtersPanel.setBegin(currentBegin);
    filtersPanel.setEnd(currentEnd);
    filtersPanel.setSeriesType(seriesType);
    filtersPanel.setSeriesColorMap(currentSeriesColorMap);

    filtersPanel.loadData(column, currentSeriesColorMap);
  }

  private void filterFilters() {
    filtersPanel.filter(filterSearch.getText());
  }

  private void clearFilters() {
    filterSearch.setText("");
    filtersPanel.clear();
  }
}