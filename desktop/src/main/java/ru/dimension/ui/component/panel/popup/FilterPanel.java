package ru.dimension.ui.component.panel.popup;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.date.DateLocale;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.ChartKey;

@Log4j2
@Data
public class FilterPanel extends ConfigPopupPanel {
  private final MessageBroker.Component component;

  private final JTextField columnsSearch;
  private final JTextField filterSearch;
  private final JXTableCase columns;
  private final GanttPopupPanel filtersPanel;

  private TableInfo tableInfo;
  private Metric metric;
  private SeriesType seriesType = SeriesType.COMMON;
  protected Map<String, Color> seriesColorMap = new HashMap<>();
  private DStore dStore;
  private long begin;
  private long end;

  private List<CProfile> allColumns = new ArrayList<>();
  private List<CProfile> filteredColumns = new ArrayList<>();
  private List<String> allFilters = new ArrayList<>();
  private List<String> filteredFilters = new ArrayList<>();

  private CProfile selectedColumn;

  private RealtimeStateProvider realtimeStateProvider;

  public FilterPanel(MessageBroker.Component component) {
    super(JPanel::new, "Filter >>", "Filter <<");

    this.component = component;

    columnsSearch = new JTextField();
    filterSearch = new JTextField();

    String[] colNames = {MetricsColumnNames.ID.getColName(), MetricsColumnNames.COLUMN_NAME.getColName()};

    columns = GUIHelper.getJXTableCase(10, colNames);
    filtersPanel = new GanttPopupPanel(component);

    configureTableColumns(columns);

    initializeListeners();

    updateContent(this::createPopupContent);
  }

  private void configureTableColumns(JXTableCase table) {
    table.getJxTable().getColumnExt(0).setVisible(false);
  }

  private JPanel createPopupContent() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setPreferredSize(new Dimension(500, 300));
    panel.setBorder(new EtchedBorder());

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.add(columnsSearch, BorderLayout.NORTH);
    leftPanel.add(columns.getJScrollPane(), BorderLayout.CENTER);

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

    columns.getJxTable().getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        int selectedRow = columns.getJxTable().getSelectedRow();
        if (selectedRow >= 0) {
          selectedColumn = filteredColumns.get(selectedRow);
          filterSearch.setText("");

          loadFiltersForColumn(selectedColumn);
        } else {
          clearFilters();
        }
      }
    });
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
    columns.getJxTable().setEnabled(panelEnabled);
    filterSearch.setEnabled(panelEnabled);
    filtersPanel.setEnabled(panelEnabled);
  }

  public void clearFilterPanel() {
    columns.getJxTable().clearSelection();
    filtersPanel.clear();

    columnsSearch.setText("");
    filterSearch.setText("");
  }

  private void loadColumns() {
    if (tableInfo == null) return;

    allColumns = tableInfo.getCProfiles().stream()
        .filter(profile -> !profile.getCsType().isTimeStamp())
        .collect(Collectors.toList());

    filterColumns();
  }

  private void filterColumns() {
    String searchText = columnsSearch.getText();
    filteredColumns = allColumns.stream()
        .filter(profile -> matchesRegex(profile.getColName(), searchText))
        .collect(Collectors.toList());

    updateColumnsTable();
  }

  private void updateColumnsTable() {
    DefaultTableModel model = columns.getDefaultTableModel();
    model.setRowCount(0);

    for (CProfile profile : filteredColumns) {
      model.addRow(new Object[]{profile.getColId(), profile.getColName()});
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

  private boolean matchesRegex(String input, String pattern) {
    if (pattern == null || pattern.isEmpty()) return true;
    try {
      return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
          .matcher(input)
          .find();
    } catch (Exception e) {
      return false;
    }
  }
}