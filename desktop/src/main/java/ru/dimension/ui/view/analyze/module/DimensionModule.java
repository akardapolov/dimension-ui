package ru.dimension.ui.view.analyze.module;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.SourceConfig;
import ru.dimension.ui.model.column.ConnectionColumnNames;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.analyze.router.Message;
import ru.dimension.ui.view.analyze.router.MessageAction;
import ru.dimension.ui.view.analyze.router.MessageRouter;
import ru.dimension.ui.view.analyze.router.MessageRouter.Action;
import ru.dimension.ui.view.analyze.router.MessageRouter.Destination;

@Log4j2
public class DimensionModule extends JPanel implements MessageAction {

  private final MessageRouter router;
  private final QueryInfo queryInfo;
  private final TableInfo tableInfo;
  private final SourceConfig sourceConfig;
  private final Map<String, Color> seriesColorMap;
  private final Metric metric;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  protected JXTableCase analyzeColumnDTable;
  protected JXTableCase analyzeMetricDTable;

  protected Map<CProfile, List<String>> filterMap = new HashMap<>();

  public DimensionModule(MessageRouter router,
                         QueryInfo queryInfo,
                         TableInfo tableInfo,
                         Metric metric,
                         SourceConfig sourceConfig,
                         Map<String, Color> seriesColorMap) {
    this.router = router;
    this.queryInfo = queryInfo;
    this.tableInfo = tableInfo;
    this.metric = metric;
    this.sourceConfig = sourceConfig;
    this.seriesColorMap = seriesColorMap;

    initializeTables();
    populateTables();
    setupLayout();
    initMetricAndColumnSelector();
  }

  private void initializeTables() {
    String[] colNames = {ConnectionColumnNames.ID.getColName(), MetricsColumnNames.COLUMN_NAME.getColName(), MetricsColumnNames.PICK.getColName()};
    String[] metricNames = {ConnectionColumnNames.ID.getColName(), MetricsColumnNames.METRIC_NAME.getColName(), MetricsColumnNames.PICK.getColName()};

    analyzeColumnDTable = createTable(colNames, sourceConfig == SourceConfig.COLUMNS);
    analyzeMetricDTable = createTable(metricNames, sourceConfig == SourceConfig.METRICS);

    configureTableColumns(analyzeColumnDTable);
    configureTableColumns(analyzeMetricDTable);
  }

  private JXTableCase createTable(String[] columnNames, boolean withColorRow) {
    return withColorRow ?
        GUIHelper.getJXTableCaseCheckBoxWithColorRow(columnNames.length, columnNames, 2) :
        GUIHelper.getJXTableCaseCheckBox(columnNames.length, columnNames, 2);
  }

  private void configureTableColumns(JXTableCase table) {
    table.getJxTable().getColumnExt(0).setVisible(false);
    TableColumn column = table.getJxTable().getColumnModel().getColumn(1);
    column.setMinWidth(30);
    column.setMaxWidth(35);
  }

  private void populateTables() {
    populateMetricTable();
    populateColumnTable();
  }

  private void populateMetricTable() {
    if (!queryInfo.getMetricList().isEmpty()) {
      if (sourceConfig == SourceConfig.METRICS) {
        Metric metricSelected = findSelectedMetric();
        if (metricSelected != null) {
          analyzeMetricDTable.getDefaultTableModel().addRow(getRowData(metricSelected, true));
        }
      }

      queryInfo.getMetricList().forEach(this::addMetricToTable);
    }
  }

  private Metric findSelectedMetric() {
    return queryInfo.getMetricList().stream()
        .filter(m -> m.getYAxis().getColName().equalsIgnoreCase(metric.getYAxis().getColName()))
        .findFirst()
        .orElse(null);
  }

  private void addMetricToTable(Metric m) {
    if (sourceConfig == SourceConfig.METRICS) {
      if (!m.getYAxis().getColName().equalsIgnoreCase(metric.getYAxis().getColName())) {
        analyzeMetricDTable.getDefaultTableModel().addRow(getRowData(m, false));
      }
    } else if (sourceConfig == SourceConfig.COLUMNS) {
      analyzeMetricDTable.getDefaultTableModel().addRow(getRowData(m, false));
    }
  }

  private void populateColumnTable() {
    boolean isSelected = sourceConfig == SourceConfig.COLUMNS;
    analyzeColumnDTable.getDefaultTableModel().addRow(getRowData(metric.getYAxis(), isSelected));

    tableInfo.getCProfiles().stream()
        .filter(profile -> !profile.getCsType().isTimeStamp())
        .filter(profile -> !profile.getColName().equalsIgnoreCase(metric.getYAxis().getColName()))
        .forEach(profile -> analyzeColumnDTable.getDefaultTableModel().addRow(getRowData(profile, false)));
  }

  private void setupLayout() {
    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    add(new JXTitledSeparator("Dimension"));

    if (analyzeMetricDTable.getJxTable().getRowCount() != 0) {
      gbl.row().cell(analyzeMetricDTable.getJScrollPane()).fillXY();
    }
    if (analyzeColumnDTable.getJxTable().getRowCount() != 0) {
      gbl.row().cell(analyzeColumnDTable.getJScrollPane()).fillXY();
    }

    gbl.done();
  }

  private void initMetricAndColumnSelector() {
    setupColumnEditor(analyzeColumnDTable.getJxTable(), this::getcProfileStackChartPanelHandler, tableInfo::getCProfiles);
    setupColumnEditor(analyzeMetricDTable.getJxTable(), this::getMetricStackChartPanelHandler, queryInfo::getMetricList);
  }

  private <T> void setupColumnEditor(JXTable dataTable,
                                     Supplier<StackChartPanelHandler<T>> handlerSupplier,
                                     Supplier<List<T>> itemsSupplier) {
    DefaultCellEditor editor = createCheckboxEditor();
    dataTable.getColumnModel().getColumn(1).setCellEditor(editor);

    StackChartPanelHandler<T> handler = handlerSupplier.get();
    List<T> items = itemsSupplier.get();
    editor.addCellEditorListener(new GenericCellEditorListener<>(items, dataTable, handler));
  }

  private DefaultCellEditor createCheckboxEditor() {
    return new DefaultCellEditor(new JCheckBox());
  }

  @NotNull
  private StackChartPanelHandler<CProfile> getcProfileStackChartPanelHandler() {

    if (filterMap.isEmpty()) {
      return (cProfile, add) -> router.sendMessage(Message.builder()
                                                       .destination(Destination.CHART_LIST)
                                                       .action(add ? Action.ADD_CHART : Action.REMOVE_CHART)
                                                       .parameter("cProfile", cProfile)
                                                       .parameter("seriesColorMap", add ? seriesColorMap : null)
                                                       .build());
    } else {
      return (cProfile, add) -> router.sendMessage(Message.builder()
                                                       .destination(Destination.CHART_LIST)
                                                       .action(add ? Action.ADD_CHART_FILTER : Action.REMOVE_CHART_FILTER)
                                                       .parameter("metric", getMetric(cProfile))
                                                       .parameter("cProfileFilter", filterMap.entrySet().stream().findFirst().orElseThrow().getKey())
                                                       .parameter("filter", filterMap.entrySet().stream().findFirst().orElseThrow().getValue())
                                                       .parameter("seriesColorMap", add ? seriesColorMap : null)
                                                       .build());
    }
  }

  private Metric getMetric(CProfile cProfile) {
    Metric metric = new Metric(tableInfo, cProfile);

    if (cProfile.equals(filterMap.entrySet().stream().findFirst().orElseThrow().getKey())) {
      metric = this.metric;
    }

    return metric;
  }

  @NotNull
  private StackChartPanelHandler<Metric> getMetricStackChartPanelHandler() {

    if (filterMap.isEmpty()) {
      return (metric, add) -> router.sendMessage(Message.builder()
                                                       .destination(Destination.CHART_LIST)
                                                       .action(add ? Action.ADD_CHART : Action.REMOVE_CHART)
                                                       .parameter("cProfile", metric.getYAxis())
                                                       .parameter("seriesColorMap", add ? seriesColorMap : null)
                                                       .build());
    } else {
      return (metric, add) -> router.sendMessage(Message.builder()
                                                       .destination(Destination.CHART_LIST)
                                                       .action(add ? Action.ADD_CHART_FILTER : Action.REMOVE_CHART_FILTER)
                                                       .parameter("metric", metric)
                                                       .parameter("cProfileFilter", filterMap.entrySet().stream().findFirst().orElseThrow().getKey())
                                                       .parameter("filter", filterMap.entrySet().stream().findFirst().orElseThrow().getValue())
                                                       .parameter("seriesColorMap", add ? seriesColorMap : null)
                                                       .build());
    }
  }

  private Object[] getRowData(CProfile cProfile, boolean pick) {
    return new Object[]{cProfile.getColId(), cProfile.getColName(), pick};
  }

  private Object[] getRowData(Metric metric, boolean pick) {
    return new Object[]{metric.getId(), metric.getName(), pick};
  }

  @Override
  public void receive(Message message) {
    switch (message.getAction()) {
      case CLEAR_ALL_CHECKBOX_CHART -> {
        setAllCheckboxes(false);
        log.info("Message action: " + message.getAction());
      }
      case SET_CHECKBOX_CHART -> {
        CProfile cProfile = message.getParameters().get("cProfile");
        setCheckboxForColumn(cProfile.getColName(), true);
        log.info("Checkbox set for column: " + cProfile.getColName());
      }
      case SET_FILTER -> {
        CProfile cProfile = message.getParameters().get("cProfile");
        List<String> filter = message.getParameters().get("filter");
        filterMap.clear();
        filterMap.put(cProfile, filter);

        initMetricAndColumnSelector();
        log.info("Set filter column: " + cProfile.getColName() + " filter " + filter);
      }
    }
  }

  private void setAllCheckboxes(boolean value) {
    setCheckboxForTable(analyzeColumnDTable, value);
    setCheckboxForTable(analyzeMetricDTable, value);
  }

  private void setCheckboxForTable(JXTableCase table, boolean value) {
    for (int i = 0; i < table.getDefaultTableModel().getRowCount(); i++) {
      table.getDefaultTableModel().setValueAt(value, i, 2);
    }
  }

  private void setCheckboxForColumn(String columnName, boolean value) {
    setCheckboxForColumnInTable(analyzeColumnDTable, columnName, value);
    setCheckboxForColumnInTable(analyzeMetricDTable, columnName, value);
  }

  private void setCheckboxForColumnInTable(JXTableCase table, String columnName, boolean value) {
    for (int i = 0; i < table.getDefaultTableModel().getRowCount(); i++) {
      String currentName = (String) table.getDefaultTableModel().getValueAt(i, 1);
      if (currentName.equals(columnName)) {
        table.getDefaultTableModel().setValueAt(value, i, 2);
        break;
      }
    }
  }

  @FunctionalInterface
  public interface StackChartPanelHandler<T> {
    void handle(T item, boolean add);
  }

  public static class GenericCellEditorListener<T> implements CellEditorListener {
    private final List<T> itemList;
    private final JXTable analyzeTable;
    private final StackChartPanelHandler<T> panelHandler;

    public GenericCellEditorListener(List<T> itemList,
                                     JXTable analyzeTable,
                                     StackChartPanelHandler<T> panelHandler) {
      this.itemList = itemList;
      this.analyzeTable = analyzeTable;
      this.panelHandler = panelHandler;
    }

    @Override
    public void editingStopped(ChangeEvent e) {
      TableCellEditor editor = (TableCellEditor) e.getSource();
      Boolean cValue = (Boolean) editor.getCellEditorValue();

      int selectedRow = analyzeTable.getSelectedRow();
      String selectedValue = analyzeTable.getValueAt(selectedRow, 0).toString();

      itemList.stream()
          .filter(item -> getItemColumnName(item).equals(selectedValue))
          .forEach(item -> panelHandler.handle(item, cValue));
    }

    private String getItemColumnName(T item) {
      return item instanceof CProfile ? ((CProfile) item).getColName() : ((Metric) item).getName();
    }

    @Override
    public void editingCanceled(ChangeEvent changeEvent) {
    }
  }
}