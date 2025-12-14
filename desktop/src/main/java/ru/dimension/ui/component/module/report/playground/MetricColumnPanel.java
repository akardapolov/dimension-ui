package ru.dimension.ui.component.module.report.playground;

import jakarta.inject.Inject;
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
import ru.dimension.di.Assisted;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.report.event.AddChartEvent;
import ru.dimension.ui.component.module.report.event.RemoveChartEvent;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.column.ColumnNames;
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

  private final EventBus eventBus;

  @Inject
  public MetricColumnPanel(@Assisted MessageBroker.Component component,
                           @Assisted ProfileTaskQueryKey key,
                           @Assisted JCheckBox collapseCard,
                           @Assisted JXTaskPaneContainer container,
                           ProfileManager profileManager,
                           EventBus eventBus) {

    this.component = component;
    this.key = key;

    this.profileManager = profileManager;
    this.collapseCard = collapseCard;
    this.container = container;
    this.eventBus = eventBus;

    // ... Rest of the constructor code remains exactly the same ...

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

    String[] headers = new String[]{
        ColumnNames.ID.getColName(),
        ColumnNames.NAME.getColName(),
        ColumnNames.PICK.getColName()
    };

    jtcMetric = GUIHelper.getJXTableCaseCheckBox(
        ColumnNames.values().length,
        headers,
        ColumnNames.PICK.ordinal()
    );

    jtcColumn = GUIHelper.getJXTableCaseCheckBox(
        ColumnNames.values().length,
        headers,
        ColumnNames.PICK.ordinal()
    );

    jtcMetric.getJxTable().getColumnExt(ColumnNames.ID.ordinal()).setVisible(false);
    jtcColumn.getJxTable().getColumnExt(ColumnNames.ID.ordinal()).setVisible(false);

    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Columns", jtcColumn.getJScrollPane());
    tabbedPane.addTab("Metrics", jtcMetric.getJScrollPane());
    this.add(tabbedPane);

    this.mEditor = new DefaultCellEditor(new JCheckBox());
    configurePickColumn(jtcMetric, mEditor);

    mEditor.addCellEditorListener(new CellEditorListener() {
      @Override
      public void editingStopped(ChangeEvent e) {
        QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());
        List<Metric> metricList = queryInfo.getMetricList();

        TableCellEditor editor = (TableCellEditor) e.getSource();
        Boolean mValue = (Boolean) editor.getCellEditorValue();

        int viewRow = jtcMetric.getJxTable().getSelectedRow();
        if (viewRow < 0) {
          return;
        }
        int modelRow = jtcMetric.getJxTable().convertRowIndexToModel(viewRow);

        Object nameObj = jtcMetric.getDefaultTableModel()
            .getValueAt(modelRow, ColumnNames.NAME.ordinal());

        final String metricName = (String) nameObj;

        Metric metric = metricList.stream()
            .filter(f -> f.getName().equals(metricName))
            .findAny()
            .orElseThrow(() -> new NotFoundException("Not found metric"));

        if (Boolean.TRUE.equals(mValue)) {
          addCard(key, metric.getYAxis());
        } else {
          removeCard(key, metric.getYAxis());
        }
      }

      @Override
      public void editingCanceled(ChangeEvent changeEvent) {
        // no-op
      }
    });

    this.cEditor = new DefaultCellEditor(new JCheckBox());
    configurePickColumn(jtcColumn, cEditor);

    cEditor.addCellEditorListener(new CellEditorListener() {
      @Override
      public void editingStopped(ChangeEvent e) {
        QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());
        TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());
        List<CProfile> cProfileList = tableInfo.getCProfiles();

        TableCellEditor editor = (TableCellEditor) e.getSource();
        Boolean cValue = (Boolean) editor.getCellEditorValue();

        int viewRow = jtcColumn.getJxTable().getSelectedRow();
        if (viewRow < 0) {
          return;
        }
        int modelRow = jtcColumn.getJxTable().convertRowIndexToModel(viewRow);

        Object nameObj = jtcColumn.getDefaultTableModel()
            .getValueAt(modelRow, ColumnNames.NAME.ordinal());

        final String colName = (String) nameObj;

        CProfile cProfile = cProfileList.stream()
            .filter(f -> f.getColName().equals(colName))
            .findAny()
            .orElseThrow(() -> new NotFoundException("Not found column profile"));

        if (Boolean.TRUE.equals(cValue)) {
          addCard(key, cProfile);
        } else {
          removeCard(key, cProfile);
        }
      }

      @Override
      public void editingCanceled(ChangeEvent changeEvent) {
        // no-op
      }
    });
  }

  // ... Rest of the methods (configurePickColumn, addCard, removeCard) remain the same ...
  private void configurePickColumn(JXTableCase tableCase, DefaultCellEditor editor) {
    int pickViewIndex = tableCase.getJxTable().convertColumnIndexToView(ColumnNames.PICK.ordinal());
    if (pickViewIndex < 0) {
      throw new IllegalStateException("PICK column is not visible");
    }

    TableColumn pickCol = tableCase.getJxTable().getColumnModel().getColumn(pickViewIndex);
    pickCol.setMinWidth(30);
    pickCol.setMaxWidth(35);
    pickCol.setCellEditor(editor);
  }

  private void addCard(ProfileTaskQueryKey key, CProfile cProfile) {
    log.info("Add card by key: {} and profile: {}", key, cProfile);

    eventBus.publish(new AddChartEvent(component, key, cProfile));
  }

  private void removeCard(ProfileTaskQueryKey key, CProfile cProfile) {
    log.info("Remove card by key: {} and profile: {}", key, cProfile);

    eventBus.publish(new RemoveChartEvent(component, key, cProfile));
  }
}