package ru.dimension.ui.view.handler.query;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.exception.EmptyNameException;
import ru.dimension.ui.exception.NameAlreadyExistException;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.exception.NotSelectedRowException;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.chart.ChartType;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.function.NormFunction;
import ru.dimension.ui.model.function.TimeRangeFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.panel.config.query.MainQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetadataQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetricQueryPanel;
import ru.dimension.ui.view.tab.ConfigTab;

@Log4j2
@Singleton
public final class QueryMetricButtonPanelHandler implements java.awt.event.ActionListener {

  private final ProfileManager profileManager;
  private final MetricQueryPanel metricQueryPanel;
  private final MainQueryPanel mainQueryPanel;
  private final MetadataQueryPanel metadataQueryPanel;
  private final JTabbedPane mainQuery;
  private final ConfigTab configTab;

  private List<Metric> saveMetricList;
  private List<Metric> editMetricList;

  private String nameSelectedMetric;

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final JCheckBox checkboxConfig;

  private String mode;

  @Inject
  public QueryMetricButtonPanelHandler(@Named("profileManager") ProfileManager profileManager,
                                       @Named("metricQueryPanel") MetricQueryPanel metricQueryPanel,
                                       @Named("metadataQueryPanel") MetadataQueryPanel metadataQueryPanel,
                                       @Named("mainQueryPanel") MainQueryPanel mainQueryPanel,
                                       @Named("profileConfigCase") JXTableCase profileCase,
                                       @Named("taskConfigCase") JXTableCase taskCase,
                                       @Named("connectionConfigCase") JXTableCase connectionCase,
                                       @Named("queryConfigCase") JXTableCase queryCase,
                                       @Named("mainQueryTab") JTabbedPane mainQuery,
                                       @Named("configTab") ConfigTab configTab,
                                       @Named("checkboxConfig") JCheckBox checkboxConfig) {
    this.profileManager = profileManager;
    this.metricQueryPanel = metricQueryPanel;
    this.mainQueryPanel = mainQueryPanel;
    this.metadataQueryPanel = metadataQueryPanel;
    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.mainQuery = mainQuery;
    this.configTab = configTab;
    this.checkboxConfig = checkboxConfig;

    this.metricQueryPanel.getMetricQueryButtonPanel().getBtnNew().addActionListener(this);
    this.metricQueryPanel.getMetricQueryButtonPanel().getBtnCopy().addActionListener(this);
    this.metricQueryPanel.getMetricQueryButtonPanel().getBtnDel().addActionListener(this);
    this.metricQueryPanel.getMetricQueryButtonPanel().getBtnEdit().addActionListener(this);
    this.metricQueryPanel.getMetricQueryButtonPanel().getBtnSave().addActionListener(this);
    this.metricQueryPanel.getMetricQueryButtonPanel().getBtnCancel().addActionListener(this);

    this.metricQueryPanel.getMetricQueryButtonPanel().getBtnDel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
    this.metricQueryPanel.getMetricQueryButtonPanel().getBtnDel().getActionMap().put("delete", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        metricQueryPanel.getMetricQueryButtonPanel().getBtnDel().doClick();
      }
    });

    this.metricQueryPanel.getMetricQueryButtonPanel().getBtnCancel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    this.metricQueryPanel.getMetricQueryButtonPanel().getBtnCancel().getActionMap().put("cancel", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        metricQueryPanel.getMetricQueryButtonPanel().getBtnCancel().doClick();
      }
    });

    this.mode = "";
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == metricQueryPanel.getMetricQueryButtonPanel().getBtnNew()) {
      mode = "new";
      if (queryCase.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("The query is not selected. Please select and try again!");
      }
      setPanelView(false);
      newEmptyPanel();
      return;
    }

    if (e.getSource() == metricQueryPanel.getMetricQueryButtonPanel().getBtnCopy()) {
      mode = "copy";
      if (metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("The metric to copy is not selected. Please select and try again!");
      }
      String nameMetric = getSelectedMetricName();
      setPanelView(false);
      metricQueryPanel.getNameMetric().setText(nameMetric + "_copy");
      return;
    }

    if (e.getSource() == metricQueryPanel.getMetricQueryButtonPanel().getBtnDel()) {
      if (metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow() == -1) {
        JOptionPane.showMessageDialog(null, "Not selected metrics. Please select and try again!",
                                      "General Error", JOptionPane.ERROR_MESSAGE);
        return;
      }

      String metricName = getSelectedMetricName();
      int input = JOptionPane.showConfirmDialog(new JDialog(),
                                                "Do you want to delete configuration: " + metricName + "?");
      if (input != 0) {
        return;
      }

      QueryInfo query = getQueryByName(mainQueryPanel.getQueryName().getText());
      query.getMetricList().removeIf(f -> f.getName().equalsIgnoreCase(metricName));
      profileManager.updateQuery(query);

      fillMetricCase(query);
      if (metricQueryPanel.getConfigMetricCase().getJxTable().getRowCount() == 0) {
        metricQueryPanel.getNameMetric().setText("");
        metricQueryPanel.getNameMetric().setPrompt("Metric name...");
      }
      return;
    }

    if (e.getSource() == metricQueryPanel.getMetricQueryButtonPanel().getBtnEdit()) {
      if (metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("Not selected metric. Please select and try again!");
      }
      mode = "edit";
      setPanelView(false);
      this.nameSelectedMetric = metricQueryPanel.getNameMetric().getText();
      return;
    }

    if (e.getSource() == metricQueryPanel.getMetricQueryButtonPanel().getBtnSave()) {
      if (mode.equals("new") || mode.equals("copy")) {
        saveNewOrCopy();
        return;
      }
      if (mode.equals("edit")) {
        saveEdit();
        return;
      }
    }

    if (e.getSource() == metricQueryPanel.getMetricQueryButtonPanel().getBtnCancel()) {
      cancelEdit();
    }
  }

  private void saveNewOrCopy() {
    QueryInfo saveQuery = getQueryByName(mainQueryPanel.getQueryName().getText());

    AtomicInteger metricIdNext = new AtomicInteger();
    saveQuery.getMetricList().stream()
        .max(Comparator.comparing(Metric::getId))
        .ifPresentOrElse(metric -> metricIdNext.set(metric.getId()), () -> {});

    String tableName = metadataQueryPanel.getTableName().getText();
    String nameMetrics = metricQueryPanel.getNameMetric().getText();

    if (nameMetrics.trim().isEmpty()) {
      throw new EmptyNameException("The name field is empty");
    }

    int newId = metricIdNext.incrementAndGet();
    if (!isBusyName(nameMetrics, newId, saveQuery)) {
      return;
    }

    TableInfo selectedTable = profileManager.getTableInfoList().stream()
        .filter(f -> f.getTableName().equals(tableName))
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found table: " + tableName));

    Metric metric = buildMetric(selectedTable, newId, nameMetrics, saveQuery);

    this.saveMetricList = saveQuery.getMetricList();
    saveMetricList.add(metric);
    saveQuery.setMetricList(saveMetricList);

    profileManager.updateQuery(saveQuery);
    fillMetricCase(saveQuery);
    setPanelView(true);
  }

  private void saveEdit() {
    QueryInfo editQuery = getQueryByName(mainQueryPanel.getQueryName().getText());

    Metric selectedMetric = editQuery.getMetricList().stream()
        .filter(f -> f.getName().equals(nameSelectedMetric))
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found metric: " + nameSelectedMetric));

    String tableName = metadataQueryPanel.getTableName().getText();
    String newMetricName = metricQueryPanel.getNameMetric().getText();

    if (newMetricName.trim().isEmpty()) {
      throw new EmptyNameException("The name field is empty");
    }

    if (!isBusyName(newMetricName, selectedMetric.getId(), editQuery)) {
      return;
    }

    TableInfo selectedTable = profileManager.getTableInfoByTableName(tableName);
    if (selectedTable == null) {
      throw new NotFoundException("Not found table: " + tableName);
    }

    Metric editMetric = buildMetric(selectedTable, selectedMetric.getId(), newMetricName, editQuery);

    this.editMetricList = editQuery.getMetricList();
    int index = editMetricList.indexOf(selectedMetric);
    editMetricList.set(index, editMetric);
    editQuery.setMetricList(editMetricList);

    profileManager.updateQuery(editQuery);
    fillMetricCase(editQuery);
    setPanelView(true);
  }

  private Metric buildMetric(TableInfo selectedTable, int id, String name, QueryInfo query) {
    String selectedXAxis = metricQueryPanel.getXTextFile().getText();
    CProfile xAxisProfile = getCProfile(selectedTable, selectedXAxis);

    String selectedYAxis = Objects.requireNonNull(metricQueryPanel.getYComboBox().getSelectedItem()).toString();
    CProfile yAxisProfile = getCProfile(selectedTable, selectedYAxis);

    String selectedDimension = Objects.requireNonNull(metricQueryPanel.getDimensionComboBox().getSelectedItem()).toString();
    CProfile dimensionProfile = getCProfile(selectedTable, selectedDimension);

    String selectedYAxisFunction = Objects.requireNonNull(metricQueryPanel.getGroupFunction().getSelectedItem()).toString();
    String selectedChartType = Objects.requireNonNull(metricQueryPanel.getChartType().getSelectedItem()).toString();

    boolean isChanged = false;
    boolean isDefault = metricQueryPanel.getDefaultCheckBox().isSelected();

    if (isDefault && idBusyDefault(query) == -1) {
      JOptionPane.showMessageDialog(null, " The metric " + name + " will be used by default",
                                    "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    if (isDefault && idBusyDefault(query) > -1) {
      String busyMetric = getSelectedMetricNameById(idBusyDefault(query), query);
      int input = JOptionPane.showOptionDialog(null,
                                               "Will the metric " + name
                                                   + " be the default? There is already a metric " + busyMetric
                                                   + " with a value of true",
                                               "Information",
                                               JOptionPane.YES_NO_OPTION,
                                               JOptionPane.QUESTION_MESSAGE,
                                               null,
                                               new String[]{"Yes", "No"},
                                               "No");
      if (input == 0) {
        int oldDefaultMetric = idBusyDefault(query);
        for (int i = 0; i < query.getMetricList().size(); i++) {
          if (query.getMetricList().get(i).getId() == oldDefaultMetric) {
            query.getMetricList().get(i).setIsDefault(false);
          }
        }
        isChanged = true;
      }
      if (input == 1) {
        metricQueryPanel.getDefaultCheckBox().setSelected(false);
        isDefault = metricQueryPanel.getDefaultCheckBox().isSelected();
      }
    }

    Metric metric = new Metric(id,
                               name,
                               isDefault,
                               xAxisProfile,
                               yAxisProfile,
                               dimensionProfile,
                               GroupFunction.valueOf(selectedYAxisFunction),
                               TimeRangeFunction.AUTO,
                               NormFunction.SECOND,
                               ChartType.valueOf(selectedChartType),
                               Collections.emptyList());

    if (isChanged) {
      return metric;
    }
    return metric;
  }

  private void cancelEdit() {
    int selectedIndex = metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow();
    if (metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRowCount() > 0) {
      metricQueryPanel.getConfigMetricCase().getJxTable().setRowSelectionInterval(0, 0);
      setPanelView(true);
      metricQueryPanel.getConfigMetricCase().getJxTable().setRowSelectionInterval(selectedIndex, selectedIndex);

      String selectedMetricName = metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
          .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 1).toString();
      boolean selectedMetricDefault = (Boolean) metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
          .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 2);
      String selectedMetricX = metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
          .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 3).toString();
      String selectedMetricY = metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
          .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 4).toString();
      String selectedMetricGroup = metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
          .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 5).toString();
      String selectedGroupFunction = metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
          .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 6).toString();
      String selectedMetricType = metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
          .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 7).toString();

      metricQueryPanel.getNameMetric().setText(selectedMetricName);
      metricQueryPanel.getDefaultCheckBox().setSelected(selectedMetricDefault);
      metricQueryPanel.getXTextFile().setText(selectedMetricX);
      metricQueryPanel.getYComboBox().setSelectedItem(selectedMetricY);
      metricQueryPanel.getDimensionComboBox().setSelectedItem(selectedMetricGroup);
      metricQueryPanel.getGroupFunction().setSelectedItem(selectedGroupFunction);
      metricQueryPanel.getChartType().setSelectedItem(selectedMetricType);
    } else {
      setPanelView(true);
      newEmptyPanel();
    }
  }

  private void newEmptyPanel() {
    metricQueryPanel.getNameMetric().setText("");
    metricQueryPanel.getNameMetric().setPrompt("New metric...");
    metricQueryPanel.getDefaultCheckBox().setSelected(false);
    metricQueryPanel.getYComboBox().setSelectedIndex(0);
    metricQueryPanel.getDimensionComboBox().setSelectedIndex(0);
    metricQueryPanel.getGroupFunction().setSelectedIndex(0);
    metricQueryPanel.getChartType().setSelectedIndex(0);
  }

  private void setPanelView(Boolean isSelected) {
    metricQueryPanel.getMetricQueryButtonPanel().setButtonView(isSelected);
    metricQueryPanel.getDefaultCheckBox().setEnabled(!isSelected);
    metricQueryPanel.getNameMetric().setEditable(!isSelected);
    metricQueryPanel.getXTextFile().setEditable(!isSelected);
    metricQueryPanel.getYComboBox().setEnabled(!isSelected);
    metricQueryPanel.getDimensionComboBox().setEnabled(!isSelected);
    metricQueryPanel.getGroupFunction().setEnabled(!isSelected);
    metricQueryPanel.getChartType().setEnabled(!isSelected);

    taskCase.getJxTable().setEnabled(isSelected);
    connectionCase.getJxTable().setEnabled(isSelected);
    profileCase.getJxTable().setEnabled(isSelected);
    queryCase.getJxTable().setEnabled(isSelected);

    mainQuery.setEnabledAt(0, isSelected);
    mainQuery.setEnabledAt(1, isSelected);

    configTab.setEnabledAt(1, isSelected);
    configTab.setEnabledAt(2, isSelected);
    configTab.setEnabledAt(0, isSelected);

    checkboxConfig.setEnabled(isSelected);
  }

  public boolean isBusyName(String newName, int id, QueryInfo query) {
    for (Metric metric : query.getMetricList()) {
      if (metric.getName().equals(newName) && metric.getId() != id) {
        throw new NameAlreadyExistException("Name " + newName + " already exists, please enter another one");
      }
    }
    return true;
  }

  private CProfile getCProfile(TableInfo selectedTable, String selectedNameCProfile) {
    return selectedTable.getCProfiles().stream()
        .filter(f -> f.getColName().equals(selectedNameCProfile))
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found CProfile: " + selectedNameCProfile));
  }

  private String getSelectedMetricName() {
    return metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
        .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 1).toString();
  }

  private String getSelectedMetricNameById(int id, QueryInfo query) {
    for (Metric metric : query.getMetricList()) {
      if (metric.getId() == id) {
        return metric.getName();
      }
    }
    return "";
  }

  private QueryInfo getQueryByName(String queryName) {
    return profileManager.getQueryInfoList()
        .stream()
        .filter(f -> f.getName().equals(queryName))
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found query: " + queryName));
  }

  private int idBusyDefault(QueryInfo query) {
    int id = -1;
    for (Metric metric : query.getMetricList()) {
      if (Boolean.TRUE.equals(metric.getIsDefault())) {
        id = metric.getId();
      }
    }
    return id;
  }

  private void fillMetricCase(QueryInfo query) {
    metricQueryPanel.getConfigMetricCase().getDefaultTableModel().getDataVector().removeAllElements();
    metricQueryPanel.getConfigMetricCase().getDefaultTableModel().fireTableDataChanged();

    for (Metric m : query.getMetricList()) {
      metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
          .addRow(new Object[]{
              m.getId(),
              m.getName(),
              m.getIsDefault(),
              m.getXAxis().getColName(),
              m.getYAxis().getColName(),
              m.getGroup().getColName(),
              m.getGroupFunction().toString(),
              m.getChartType().toString()
          });
    }

    if (metricQueryPanel.getConfigMetricCase().getJxTable().getRowCount() != 0) {
      metricQueryPanel.getConfigMetricCase().getJxTable().setRowSelectionInterval(0, 0);
    } else {
      metricQueryPanel.getNameMetric().setText("");
      metricQueryPanel.getNameMetric().setPrompt("Metric name");
    }
  }
}