package ru.dimension.ui.view.panel.report;

import java.awt.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.dimension.db.model.profile.CProfile;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.ReportHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.column.QueryColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.report.CProfileReport;
import ru.dimension.ui.model.report.MetricReport;
import ru.dimension.ui.model.report.QueryReportData;
import ru.dimension.ui.model.table.JXTableCase;

@Data
public class MetricColumnPanel extends JXTaskPane {

  @EqualsAndHashCode.Include
  private final ProfileTaskQueryKey key;
  private final JXTableCase jtcMetric;
  private final JXTableCase jtcColumn;
  private final DefaultCellEditor mEditor;
  private final DefaultCellEditor cEditor;
  private final ProfileManager profileManager;
  private ReportTabsPane reportTabsPane;
  private final ReportHelper reportHelper;
  private boolean collapseAll = true;
  private final JXTaskPaneContainer container;

  public MetricColumnPanel(ProfileTaskQueryKey key,
                           Map<ProfileTaskQueryKey, QueryReportData> mapReportData,
                           ProfileManager profileManager,
                           ReportTabsPane reportTabsPane,
                           ReportHelper reportHelper,
                           JXTaskPaneContainer container) {

    this.profileManager = profileManager;
    this.reportTabsPane = reportTabsPane;
    this.reportHelper = reportHelper;
    this.container = container;

    this.key = key;

    this.addPropertyChangeListener(propertyChangeEvent -> {
      if (!isCollapsed()) {
        collapseAll = true;
        reportTabsPane.getCollapseBtnDesign().setText("Collapse all");
        reportTabsPane.getCollapseBtnDesign().setSelected(false);
      } else {
        List<Component> components = Arrays.asList(container.getComponents());
        for (Component c : components) {
          if (c instanceof JXTaskPane) {
            JXTaskPane jxTaskPane = (JXTaskPane) c;
            if (!jxTaskPane.isCollapsed()) {
              collapseAll = false;
            }
          }
        }
        if (collapseAll) {
          reportTabsPane.getCollapseBtnDesign().setText("Expand all");
          reportTabsPane.getCollapseBtnDesign().setSelected(true);
        }
      }
    });

    jtcMetric = GUIHelper.getJXTableCaseCheckBox(3,
                                                 new String[]{MetricsColumnNames.ID.getColName(),
                                                     MetricsColumnNames.PICK.getColName(),
                                                     MetricsColumnNames.METRIC_NAME.getColName()}, 1);
    jtcMetric.getJxTable().getColumnExt(0).setVisible(false);

    TableColumn colM = jtcMetric.getJxTable().getColumnModel().getColumn(0);
    colM.setMinWidth(30);
    colM.setMaxWidth(35);

    jtcColumn = GUIHelper.getJXTableCaseCheckBox(3,
                                                 new String[]{QueryColumnNames.ID.getColName(),
                                                     MetricsColumnNames.PICK.getColName(),
                                                     MetricsColumnNames.COLUMN_NAME.getColName()}, 1);
    jtcColumn.getJxTable().getColumnExt(0).setVisible(false);

    TableColumn colC = jtcColumn.getJxTable().getColumnModel().getColumn(0);
    colC.setMinWidth(30);
    colC.setMaxWidth(35);

    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Columns", jtcColumn.getJScrollPane());
    tabbedPane.addTab("Metrics", jtcMetric.getJScrollPane());

    this.add(tabbedPane);

    this.mEditor = new DefaultCellEditor(new JCheckBox());
    this.jtcMetric.getJxTable().getColumnModel().getColumn(0).setCellEditor(mEditor);

    mEditor.addCellEditorListener(new CellEditorListener() {
      @Override
      public void editingStopped(ChangeEvent e) {

        mapReportData.putIfAbsent(key, new QueryReportData());

        QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());
        List<Metric> metricList = queryInfo.getMetricList();

        TableCellEditor editor = (TableCellEditor) e.getSource();
        int row = jtcMetric.getJxTable().getSelectedRow();
        Boolean mValue = (Boolean) editor.getCellEditorValue();

        if (mValue) {
          Metric metric = metricList
              .stream().filter(f -> f.getName()
                  .equals(jtcMetric.getDefaultTableModel()
                              .getValueAt(jtcMetric.getJxTable().getSelectedRow(), 2)))
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found metric by : "));

          MetricReport metricReport = new MetricReport();
          metricReport.setComment("");
          metricReport.setId(metric.getId());
          metricReport.setName(metric.getName());
          metricReport.setIsDefault(metric.getIsDefault());
          metricReport.setXAxis(metric.getXAxis());
          metricReport.setYAxis(metric.getYAxis());
          metricReport.setGroup(metric.getGroup());
          metricReport.setMetricFunction(metric.getMetricFunction());
          metricReport.setChartType(metric.getChartType());
          metricReport.setColumnGanttList(metric.getColumnGanttList());

          if (!mapReportData.get(key)
              .getMetricReportList()
              .contains(metricReport)) {
            mapReportData.get(key)
                .getMetricReportList()
                .add(metricReport);
          }

        } else {
          int metricId = (int) jtcMetric.getDefaultTableModel()
              .getValueAt(row, jtcMetric.getDefaultTableModel()
                  .findColumn(MetricsColumnNames.ID.getColName()));
          String metricName = (String) jtcMetric.getDefaultTableModel()
              .getValueAt(row, jtcMetric.getDefaultTableModel()
                  .findColumn(MetricsColumnNames.METRIC_NAME.getColName()));

          mapReportData.get(key)
              .getMetricReportList()
              .removeIf(c -> c.getId() == metricId);

          for (int index = 0; index < jtcMetric.getJxTable().getRowCount(); index++) {
            int mId = (int) jtcMetric.getDefaultTableModel()
                .getValueAt(index, jtcMetric.getDefaultTableModel()
                    .findColumn(MetricsColumnNames.ID.getColName()));
            if (metricId == mId) {
              removeCard(key, metricName);
              reportHelper.setMadeChanges(true);
            }
          }
        }
      }

      @Override
      public void editingCanceled(ChangeEvent changeEvent) {

      }
    });

    this.cEditor = new DefaultCellEditor(new JCheckBox());
    this.jtcColumn.getJxTable().getColumnModel().getColumn(0).setCellEditor(cEditor);

    cEditor.addCellEditorListener(new CellEditorListener() {
      @Override
      public void editingStopped(ChangeEvent e) {
        mapReportData.putIfAbsent(key, new QueryReportData());

        QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());
        TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());
        List<CProfile> cProfileList = tableInfo.getCProfiles();

        TableCellEditor editor = (TableCellEditor) e.getSource();
        int row = jtcColumn.getJxTable().getSelectedRow();
        Boolean cValue = (Boolean) editor.getCellEditorValue();

        if (cValue) {
          CProfile cProfile = cProfileList
              .stream()
              .filter(f -> f.getColName().equals(jtcColumn.getDefaultTableModel()
                                                     .getValueAt(jtcColumn.getJxTable().getSelectedRow(), 2)))
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found metric by : "));

          CProfileReport cProfileReport = new CProfileReport();
          cProfileReport.setComment("");
          cProfileReport.setColId(cProfile.getColId());
          cProfileReport.setColIdSql(cProfile.getColIdSql());
          cProfileReport.setColName(cProfile.getColName());
          cProfileReport.setColDbTypeName(cProfile.getColDbTypeName());
          cProfileReport.setColSizeDisplay(cProfile.getColSizeDisplay());
          cProfileReport.setCsType(cProfile.getCsType());

          if (!mapReportData.get(key)
              .getCProfileReportList()
              .contains(cProfileReport)) {
            mapReportData.get(key)
                .getCProfileReportList()
                .add(cProfileReport);
          }

        } else {
          int colId = (int) jtcColumn.getDefaultTableModel()
              .getValueAt(row, jtcColumn.getDefaultTableModel()
                  .findColumn(MetricsColumnNames.ID.getColName()));
          String columName = (String) jtcColumn.getDefaultTableModel()
              .getValueAt(row, jtcColumn.getDefaultTableModel()
                  .findColumn(MetricsColumnNames.COLUMN_NAME.getColName()));

          mapReportData.get(key)
              .getCProfileReportList()
              .removeIf(c -> c.getColId() == colId);

          for (int index = 0; index < jtcColumn.getJxTable().getRowCount(); index++) {
            int columnId = (int) jtcColumn.getDefaultTableModel()
                .getValueAt(index, jtcColumn.getDefaultTableModel()
                    .findColumn(MetricsColumnNames.ID.getColName()));
            if (colId == columnId) {
              removeCard(key, columName);
              reportHelper.setMadeChanges(true);
            }
          }
        }
      }

      @Override
      public void editingCanceled(ChangeEvent changeEvent) {
      }
    });
  }

  private void removeCard(ProfileTaskQueryKey key,
                          String name) {

    ProfileInfo profileInfo = profileManager.getProfileInfoById(key.getProfileId());
    TaskInfo taskInfo = profileManager.getTaskInfoById(key.getTaskId());
    QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());

    String title = "<html><b>Profile:</b> " + profileInfo.getName() + " <br>"
        + "  <b>Task:</b> " + taskInfo.getName() + " <br>"
        + "  <b>Query:</b> " + queryInfo.getName() + " </html>";

    int index = reportTabsPane.getJTabbedPaneChart().indexOfTab(title);
    Component component = reportTabsPane.getJTabbedPaneChart().getComponentAt(index);

    if (component instanceof JScrollPane jScrollPane) {
      JViewport viewport = jScrollPane.getViewport();
      if (viewport != null) {
        Component[] components = viewport.getComponents();
        for (Component cardContainer : components) {
          if (cardContainer instanceof JPanel containerChartCardDesign) {
            Component[] cards = containerChartCardDesign.getComponents();
            for (Component card : cards) {
              if (card instanceof ChartCardPanel cardInfo) {
                String nameFromTitle = cardInfo.getJlTitle().getText()
                    .substring(cardInfo.getJlTitle().getText().indexOf(":") + 1).trim();
                if (name.equals(nameFromTitle)) {
                  containerChartCardDesign.remove(card);
                }
              }
            }
          }
        }
        viewport.revalidate();
        viewport.repaint();
      }
    }
  }
}
