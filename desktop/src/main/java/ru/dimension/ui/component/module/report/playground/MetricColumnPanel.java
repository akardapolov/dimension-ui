package ru.dimension.ui.component.module.report.playground;

import jakarta.inject.Inject;
import java.awt.Component;
import java.util.Arrays;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;
import javax.swing.event.TableModelEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.di.Assisted;
import ru.dimension.tt.api.TT;
import ru.dimension.tt.api.TTRegistry;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.tt.swing.TableUi;
import ru.dimension.tt.swingx.JXTableTables;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.report.event.AddChartEvent;
import ru.dimension.ui.component.module.report.event.RemoveChartEvent;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.view.table.icon.ModelIconProviders;
import ru.dimension.ui.view.table.row.Rows.ColumnRow;
import ru.dimension.ui.view.table.row.Rows.MetricRow;

@Log4j2
@Data
public class MetricColumnPanel extends JXTaskPane {

  @EqualsAndHashCode.Include
  private final ProfileTaskQueryKey key;
  private final MessageBroker.Component component;

  private final TTRegistry registry;
  private final TTTable<MetricRow, JXTable> metricTable;
  private final TTTable<ColumnRow, JXTable> columnTable;

  private int metricPickColumnIndex = -1;
  private int columnPickColumnIndex = -1;
  private boolean ignoreCheckboxEvents = false;

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

    this.registry = TT.builder()
        .scanPackages("ru.dimension.ui.view.table.row")
        .build();

    this.metricTable = createMetricTable();
    this.columnTable = createColumnTable();

    setupMetricTableListener();
    setupColumnTableListener();

    this.addPropertyChangeListener(propertyChangeEvent -> {
      if (!isCollapsed()) {
        collapseAll = true;
        collapseCard.setText("Collapse all");
        collapseCard.setSelected(false);
      } else {
        List<Component> components = Arrays.asList(container.getComponents());
        for (Component c : components) {
          if (c instanceof JXTaskPane jxTaskPane) {
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

    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Columns", columnTable.scrollPane());
    tabbedPane.addTab("Metrics", metricTable.scrollPane());
    this.add(tabbedPane);
  }

  private TTTable<MetricRow, JXTable> createMetricTable() {
    TTTable<MetricRow, JXTable> tt = JXTableTables.create(
        registry,
        MetricRow.class,
        TableUi.<MetricRow>builder()
            .rowIcon(ModelIconProviders.forMetricRow())
            .rowIconInColumn("name")
            .build()
    );

    JXTable table = tt.table();
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(java.awt.Color.GRAY);
    table.setIntercellSpacing(new java.awt.Dimension(1, 1));
    table.setEditable(true);

    if (table.getColumnExt("ID") != null) {
      table.getColumnExt("ID").setVisible(false);
    }

    if (table.getColumnExt("Name") != null) {
      table.getColumnExt("Name").setEditable(false);
    }

    metricPickColumnIndex = tt.model().schema().modelIndexOf("pick");

    return tt;
  }

  private TTTable<ColumnRow, JXTable> createColumnTable() {
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
    table.setEditable(true);

    if (table.getColumnExt("ID") != null) {
      table.getColumnExt("ID").setVisible(false);
    }

    if (table.getColumnExt("Name") != null) {
      table.getColumnExt("Name").setEditable(false);
    }

    columnPickColumnIndex = tt.model().schema().modelIndexOf("pick");

    return tt;
  }

  private void setupMetricTableListener() {
    if (metricPickColumnIndex < 0) {
      return;
    }

    metricTable.model().addTableModelListener(e -> {
      if (ignoreCheckboxEvents || e.getType() != TableModelEvent.UPDATE) {
        return;
      }

      if (e.getColumn() == metricPickColumnIndex) {
        int row = e.getFirstRow();
        if (row >= 0 && row < metricTable.model().getRowCount()) {
          MetricRow metricRow = metricTable.model().itemAt(row);
          if (metricRow != null && metricRow.hasOrigin()) {
            Metric metric = metricRow.getOrigin();
            if (metricRow.isPick()) {
              addCard(key, metric.getYAxis());
            } else {
              removeCard(key, metric.getYAxis());
            }
          }
        }
      }
    });
  }

  private void setupColumnTableListener() {
    if (columnPickColumnIndex < 0) {
      return;
    }

    columnTable.model().addTableModelListener(e -> {
      if (ignoreCheckboxEvents || e.getType() != TableModelEvent.UPDATE) {
        return;
      }

      if (e.getColumn() == columnPickColumnIndex) {
        int row = e.getFirstRow();
        if (row >= 0 && row < columnTable.model().getRowCount()) {
          ColumnRow columnRow = columnTable.model().itemAt(row);
          if (columnRow != null && columnRow.hasOrigin()) {
            CProfile cProfile = columnRow.getOrigin();
            if (columnRow.isPick()) {
              addCard(key, cProfile);
            } else {
              removeCard(key, cProfile);
            }
          }
        }
      }
    });
  }

  public void setMetricItems(List<MetricRow> items) {
    ignoreCheckboxEvents = true;
    try {
      metricTable.setItems(items);
    } finally {
      ignoreCheckboxEvents = false;
    }
  }

  public void setColumnItems(List<ColumnRow> items) {
    ignoreCheckboxEvents = true;
    try {
      columnTable.setItems(items);
    } finally {
      ignoreCheckboxEvents = false;
    }
  }

  public void setMetricSelected(int metricId, boolean selected) {
    ignoreCheckboxEvents = true;
    try {
      for (int i = 0; i < metricTable.model().getRowCount(); i++) {
        MetricRow row = metricTable.model().itemAt(i);
        if (row != null && row.getId() == metricId) {
          metricTable.model().setValueAt(selected, i, metricPickColumnIndex);
          break;
        }
      }
    } finally {
      ignoreCheckboxEvents = false;
    }
  }

  public void setColumnSelected(int colId, boolean selected) {
    ignoreCheckboxEvents = true;
    try {
      for (int i = 0; i < columnTable.model().getRowCount(); i++) {
        ColumnRow row = columnTable.model().itemAt(i);
        if (row != null && row.getId() == colId) {
          columnTable.model().setValueAt(selected, i, columnPickColumnIndex);
          break;
        }
      }
    } finally {
      ignoreCheckboxEvents = false;
    }
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