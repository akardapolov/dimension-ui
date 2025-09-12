package ru.dimension.ui.component.module.report.playground;

import java.awt.Component;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.column.QueryColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.table.JXTableCase;

@Log4j2
@Data
public class MetricColumnPanel extends JXTaskPane {

  @EqualsAndHashCode.Include
  private final ProfileTaskQueryKey key;
  private final MessageBroker.Component component;
  private final JXTableCase jtcMetric;
  private final JXTableCase jtcColumn;
  private final DefaultCellEditor mEditor;
  private final DefaultCellEditor cEditor;
  private final ProfileManager profileManager;
  private final JCheckBox collapseCard;
  private final JXTaskPaneContainer container;

  private boolean collapseAll = true;

  private final MessageBroker broker = MessageBroker.getInstance();

  public MetricColumnPanel(MessageBroker.Component component,
                           ProfileTaskQueryKey key,
                           ProfileManager profileManager,
                           JCheckBox collapseCard,
                           JXTaskPaneContainer container) {

    this.component = component;
    this.key = key;

    this.profileManager = profileManager;
    this.collapseCard = collapseCard;
    this.container = container;

    this.addPropertyChangeListener(propertyChangeEvent -> {
      if (!isCollapsed()) {
        collapseAll = true;
        collapseCard.setText("Collapse all");
        collapseCard.setSelected(false);
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
          collapseCard.setText("Expand all");
          collapseCard.setSelected(true);
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
        QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());
        List<Metric> metricList = queryInfo.getMetricList();

        TableCellEditor editor = (TableCellEditor) e.getSource();
        Boolean mValue = (Boolean) editor.getCellEditorValue();

        if (mValue) {
          Metric metric = metricList
              .stream().filter(f -> f.getName()
                  .equals(jtcMetric.getDefaultTableModel()
                              .getValueAt(jtcMetric.getJxTable().getSelectedRow(), 2)))
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found metric"));

          addCard(key, metric.getYAxis());

        } else {
          Metric metric = metricList
              .stream().filter(f -> f.getName()
                  .equals(jtcMetric.getDefaultTableModel()
                              .getValueAt(jtcMetric.getJxTable().getSelectedRow(), 2)))
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found metric"));

          removeCard(key, metric.getYAxis());
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
        QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());
        TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());
        List<CProfile> cProfileList = tableInfo.getCProfiles();

        TableCellEditor editor = (TableCellEditor) e.getSource();
        Boolean cValue = (Boolean) editor.getCellEditorValue();

        if (cValue) {
          CProfile cProfile = cProfileList
              .stream()
              .filter(f -> f.getColName().equals(jtcColumn.getDefaultTableModel()
                                                     .getValueAt(jtcColumn.getJxTable().getSelectedRow(), 2)))
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found column profile"));

          addCard(key, cProfile);
        } else {
          CProfile cProfile = cProfileList
              .stream()
              .filter(f -> f.getColName().equals(jtcColumn.getDefaultTableModel()
                                                     .getValueAt(jtcColumn.getJxTable().getSelectedRow(), 2)))
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found column profile"));

          removeCard(key, cProfile);
        }
      }

      @Override
      public void editingCanceled(ChangeEvent changeEvent) {
      }
    });
  }

  private void addCard(ProfileTaskQueryKey key,
                       CProfile cProfile) {
    log.info("Add card by key: " + key + " and profile: " + cProfile);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(component))
                           .action(Action.ADD_CHART)
                           .parameter("key", key)
                           .parameter("cProfile", cProfile)
                           .build());
  }

  private void removeCard(ProfileTaskQueryKey key,
                          CProfile cProfile) {
    log.info("Remove card by key: " + key + " and profile: " + cProfile);

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(component))
                           .action(Action.REMOVE_CHART)
                           .parameter("key", key)
                           .parameter("cProfile", cProfile)
                           .build());
  }
}
