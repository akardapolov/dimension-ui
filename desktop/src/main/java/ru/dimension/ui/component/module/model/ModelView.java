package ru.dimension.ui.component.module.model;

import static ru.dimension.ui.laf.LafColorGroup.CONFIG_PANEL;

import java.util.List;
import java.util.function.Supplier;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import javax.swing.table.TableColumn;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;

@Log4j2
public class ModelView extends JPanel {

  private final JXTableCase profileTableCase;
  private final JXTableCase taskTableCase;
  private final JXTableCase queryTableCase;
  private final JXTableCase columnTableCase;
  private final JXTableCase metricTableCase;

  public ModelView() {
    this.profileTableCase = createBasicTableCase();
    this.taskTableCase = createBasicTableCase();
    this.queryTableCase = createBasicTableCase();
    this.columnTableCase = createCheckboxTableCase();
    this.metricTableCase = createCheckboxTableCase();

    setupLayout();
  }

  private void setupLayout() {
    LaF.setBackgroundConfigPanel(CONFIG_PANEL, this);
    this.setBorder(new EtchedBorder());

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    addSection(gbl, "Profile", profileTableCase.getJScrollPane());
    addSection(gbl, "Task", taskTableCase.getJScrollPane());
    addSection(gbl, "Query", queryTableCase.getJScrollPane());

    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Columns", columnTableCase.getJScrollPane());
    tabbedPane.addTab("Metrics", metricTableCase.getJScrollPane());

    gbl.row().cellXYRemainder(tabbedPane).fillXY();

    gbl.done();
  }

  private JXTableCase createBasicTableCase() {
    JXTableCase jxTableCase = GUIHelper.getJXTableCase(7,
                                                       new String[]{ColumnNames.ID.getColName(),
                                                           ColumnNames.NAME.getColName()});
    jxTableCase.getJxTable().getColumnExt(ColumnNames.ID.ordinal()).setVisible(false);
    return jxTableCase;
  }

  private JXTableCase createCheckboxTableCase() {
    JXTableCase jxTableCase = GUIHelper.getJXTableCaseCheckBoxAdHoc(10,
                                                               new String[]{
                                                                   ColumnNames.ID.getColName(),
                                                                   ColumnNames.NAME.getColName(),
                                                                   ColumnNames.PICK.getColName()
                                                               }, ColumnNames.PICK.ordinal());

    jxTableCase.getJxTable().getColumnExt(ColumnNames.ID.ordinal()).setVisible(false);

    TableColumn nameColumn = jxTableCase.getJxTable().getColumnModel().getColumn(ColumnNames.NAME.ordinal());
    nameColumn.setMinWidth(30);
    nameColumn.setMaxWidth(40);

    return jxTableCase;
  }

  public void initializeProfileTable(List<ProfileInfo> profileInfoList) {
    profileTableCase.clearTable();
    profileInfoList.forEach(profile -> profileTableCase.getDefaultTableModel()
        .addRow(new Object[]{profile.getId(), profile.getName()}));
  }

  public void selectFirstProfileRow() {
    if (profileTableCase.getDefaultTableModel().getRowCount() > 0) {
      profileTableCase.getJxTable().setRowSelectionInterval(0, 0);
    }
  }

  private void addSection(PainlessGridBag gbl,
                          String title,
                          JComponent component) {
    gbl.row().cell(new JXTitledSeparator(title)).fillX();
    gbl.row().cell(component).fillX();
  }

  public void updateTaskTable(List<TaskInfo> taskInfoList) {
    taskTableCase.clearTable();
    taskInfoList.forEach(taskInfo -> taskTableCase.getDefaultTableModel()
        .addRow(new Object[]{taskInfo.getId(), taskInfo.getName()}));
  }

  public void selectFirstTaskRow() {
    if (taskTableCase.getDefaultTableModel().getRowCount() > 0) {
      taskTableCase.getJxTable().setRowSelectionInterval(0, 0);
    } else {
      clearQueryAndDetailsTables();
    }
  }

  public void updateQueryTable(List<QueryInfo> queryInfoList) {
    queryTableCase.clearTable();
    queryInfoList.forEach(queryInfo -> queryTableCase.getDefaultTableModel()
        .addRow(new Object[]{queryInfo.getId(), queryInfo.getName()}));
  }

  public void selectFirstQueryRow() {
    if (queryTableCase.getDefaultTableModel().getRowCount() > 0) {
      queryTableCase.getJxTable().setRowSelectionInterval(0, 0);
    } else {
      clearQueryAndDetailsTables();
    }
  }

  private void clearQueryAndDetailsTables() {
    queryTableCase.clearTable();
    columnTableCase.clearTable();
    metricTableCase.clearTable();
  }

  public void updateColumnAndMetricTables(TableInfo tableInfo,
                                          List<Metric> metricList) {
    columnTableCase.clearTable();
    metricTableCase.clearTable();

    populateColumnTable(tableInfo);
    populateMetricTable(metricList);
  }

  private void populateColumnTable(TableInfo tableInfo) {
    if (tableInfo.getCProfiles() != null) {
      tableInfo.getCProfiles().stream()
          .filter(cProfile -> !cProfile.getCsType().isTimeStamp())
          .forEach(cProfile -> columnTableCase.getDefaultTableModel()
              .addRow(new Object[]{cProfile.getColId(), cProfile.getColName(), false}));
    }
  }

  private void populateMetricTable(List<Metric> metricList) {
    if (metricList != null) {
      metricList.forEach(metric -> metricTableCase.getDefaultTableModel()
          .addRow(new Object[]{metric.getId(), metric.getName(), false}));
    }
  }

  public void restoreSelections(List<CProfile> selectedColumns,
                                List<Metric> selectedMetrics) {
    restoreColumnSelections(selectedColumns);
    restoreMetricSelections(selectedMetrics);
  }

  private void restoreColumnSelections(List<CProfile> selectedColumns) {
    for (int row = 0; row < columnTableCase.getDefaultTableModel().getRowCount(); row++) {
      int id = (int) columnTableCase.getDefaultTableModel().getValueAt(row, ColumnNames.ID.ordinal());
      boolean exists = selectedColumns.stream().anyMatch(cProfile -> cProfile.getColId() == id);
      columnTableCase.getDefaultTableModel().setValueAt(exists, row, ColumnNames.PICK.ordinal());
    }
  }

  private void restoreMetricSelections(List<Metric> selectedMetrics) {
    for (int row = 0; row < metricTableCase.getDefaultTableModel().getRowCount(); row++) {
      int id = (int) metricTableCase.getDefaultTableModel().getValueAt(row, ColumnNames.ID.ordinal());
      boolean exists = selectedMetrics.stream().anyMatch(metric -> metric.getId() == id);
      metricTableCase.getDefaultTableModel().setValueAt(exists, row, ColumnNames.PICK.ordinal());
    }
  }

  public void setupColumnEditors(Supplier<ModelHandler<CProfile>> columnHandlerSupplier,
                                 Supplier<ModelHandler<Metric>> metricHandlerSupplier,
                                 Supplier<List<CProfile>> cProfilesSupplier,
                                 Supplier<List<Metric>> metricsSupplier) {
    setupColumnEditor(columnTableCase.getJxTable(), columnHandlerSupplier, cProfilesSupplier);
    setupColumnEditor(metricTableCase.getJxTable(), metricHandlerSupplier, metricsSupplier);
  }

  private <T> void setupColumnEditor(JXTable dataTable,
                                     Supplier<ModelHandler<T>> handlerSupplier,
                                     Supplier<List<T>> itemsSupplier) {
    DefaultCellEditor editor = createCheckboxEditor();
    dataTable.getColumnModel().getColumn(ColumnNames.NAME.ordinal()).setCellEditor(editor);

    ModelHandler<T> handler = handlerSupplier.get();
    List<T> items = itemsSupplier.get();
    ModelCellEditorListener<T> editorListener = new ModelCellEditorListener<>(items, dataTable, handler);
    editor.addCellEditorListener(editorListener);
  }

  private DefaultCellEditor createCheckboxEditor() {
    return new DefaultCellEditor(new JCheckBox());
  }

  public void selectFirstRows() {
    if (columnTableCase.getDefaultTableModel().getRowCount() > 0) {
      columnTableCase.getJxTable().setRowSelectionInterval(0, 0);
    }
    if (metricTableCase.getDefaultTableModel().getRowCount() > 0) {
      metricTableCase.getJxTable().setRowSelectionInterval(0, 0);
    }
  }

  public void resetColumnSelections() {
    for (int r = 0; r < columnTableCase.getJxTable().getRowCount(); r++) {
      columnTableCase.getDefaultTableModel().setValueAt(false, r, ColumnNames.PICK.ordinal());
    }
  }

  public void resetMetricSelections() {
    for (int r = 0; r < metricTableCase.getJxTable().getRowCount(); r++) {
      metricTableCase.getDefaultTableModel().setValueAt(false, r, ColumnNames.PICK.ordinal());
    }
  }

  public void showNotRunningMessage(String profileName) {
    log.warn("Profile: {} not started", profileName);
  }

  public JXTableCase getProfileTableCase() {
    return profileTableCase;
  }

  public JXTableCase getTaskTableCase() {
    return taskTableCase;
  }

  public JXTableCase getQueryTableCase() {
    return queryTableCase;
  }
}
