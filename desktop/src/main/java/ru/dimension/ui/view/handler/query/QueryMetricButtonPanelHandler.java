package ru.dimension.ui.view.handler.query;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
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
import ru.dimension.ui.model.chart.ChartType;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.function.NormFunction;
import ru.dimension.ui.model.function.TimeRangeFunction;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.panel.config.query.MainQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetadataQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetricQueryPanel;

@Log4j2
@Singleton
public class QueryMetricButtonPanelHandler implements ActionListener {

  private final ProfileManager profileManager;
  private final MetricQueryPanel metricQueryPanel;
  private final MainQueryPanel mainQueryPanel;
  private final MetadataQueryPanel metadataQueryPanel;
  private final JTabbedPane mainQuery;
  private final ConfigTab configTab;
  private List<Metric> saveMetricList;
  private List<Metric> editMetricList;
  private final EventListener eventListener;
  private String nameSelectedMetric;
  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final JCheckBox checkboxConfig;
  private String description;

  @Inject
  public QueryMetricButtonPanelHandler(@Named("profileManager") ProfileManager profileManager,
                                       @Named("metricQueryPanel") MetricQueryPanel metricQueryPanel,
                                       @Named("metadataQueryPanel") MetadataQueryPanel metadataQueryPanel,
                                       @Named("mainQueryPanel") MainQueryPanel mainQueryPanel,
                                       @Named("eventListener") EventListener eventListener,
                                       @Named("profileConfigCase") JXTableCase profileCase,
                                       @Named("taskConfigCase") JXTableCase taskCase,
                                       @Named("connectionConfigCase") JXTableCase connectionCase,
                                       @Named("queryConfigCase") JXTableCase queryCase,
                                       @Named("mainQueryTab") JTabbedPane mainQuery,
                                       @Named("jTabbedPaneConfig") ConfigTab configTab,
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

    this.description = "";

    this.eventListener = eventListener;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == metricQueryPanel.getMetricQueryButtonPanel().getBtnNew()) {

      description = "new";
      if (queryCase.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("The query is not selected. Please select and try again!");
      } else {
        setPanelView(false);
        newEmptyPanel();
      }

    } else if (e.getSource() == metricQueryPanel.getMetricQueryButtonPanel().getBtnCopy()) {

      description = "copy";
      if (metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("The metric to copy is not selected. Please select and try again!");
      } else {
        String nameMetric = getSelectedMetricName();
        setPanelView(false);
        metricQueryPanel.getNameMetric().setText(nameMetric + "_copy");
      }

    } else if (e.getSource() == metricQueryPanel.getMetricQueryButtonPanel().getBtnDel()) {

      if (metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow() == -1) {
        JOptionPane.showMessageDialog(null, "Not selected metrics. Please select and try again!",
                                      "General Error", JOptionPane.ERROR_MESSAGE);
      } else {
        String metricName = getSelectedMetricName();
        int input = JOptionPane.showConfirmDialog(new JDialog(),// 0=yes, 1=no, 2=cancel
                                                  "Do you want to delete configuration: " + metricName + "?");
        if (input == 0) {

          QueryInfo query = getQueryByName(mainQueryPanel.getQueryName().getText());

          query.getMetricList().removeIf(f -> f.getName().equalsIgnoreCase(metricName));
          profileManager.updateQuery(query);

          metricQueryPanel.getConfigMetricCase().getDefaultTableModel().getDataVector().removeAllElements();
          metricQueryPanel.getConfigMetricCase().getDefaultTableModel().fireTableDataChanged();
          for (Metric m : query.getMetricList()) {
            metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
                .addRow(new Object[]{m.getId(),
                    m.getName(),
                    m.getIsDefault(),
                    m.getXAxis().getColName(),
                    m.getYAxis().getColName(),
                    m.getGroup().getColName(),
                    m.getGroupFunction().toString(),
                    m.getChartType().toString()});
          }
          if (metricQueryPanel.getConfigMetricCase().getJxTable().getRowCount() != 0) {
            metricQueryPanel.getConfigMetricCase().getJxTable().setRowSelectionInterval(0, 0);
          } else {
            metricQueryPanel.getNameMetric().setText("");
            metricQueryPanel.getNameMetric().setPrompt("Metric name...");
          }

        }
      }
    } else if (e.getSource() == metricQueryPanel.getMetricQueryButtonPanel().getBtnEdit()) {

      if (metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("Not selected metric. Please select and try again!");
      }
      description = "edit";
      setPanelView(false);
      this.nameSelectedMetric = metricQueryPanel.getNameMetric().getText();

    } else if (e.getSource() == metricQueryPanel.getMetricQueryButtonPanel().getBtnSave()) {
      if (description.equals("new") || description.equals("copy")) {

        QueryInfo saveQuery = profileManager.getQueryInfoList().stream()
            .filter(f -> f.getName().equalsIgnoreCase(mainQueryPanel.getQueryName().getText()))
            .findAny()
            .orElseThrow(() ->
                             new NotFoundException(
                                 "Not found query by name: " + mainQueryPanel.getQueryName().getText()));

        AtomicInteger metricIdNext = new AtomicInteger();

        saveQuery.getMetricList().stream()
            .max(Comparator.comparing(Metric::getId))
            .ifPresentOrElse(metric -> metricIdNext.set(metric.getId()),
                             () -> log.info("Not found Metric"));

        String tableName = metadataQueryPanel.getTableName().getText();
        String nameMetrics = metricQueryPanel.getNameMetric().getText();
        if (!metricQueryPanel.getNameMetric().getText().trim().isEmpty()) {
          if (isBusyName(nameMetrics, metricIdNext.incrementAndGet(), saveQuery)) {

            TableInfo selectedTable = profileManager.getTableInfoList()
                .stream()
                .filter(f -> f.getTableName().equals(tableName))
                .findAny()
                .orElseThrow(() -> new NotFoundException("Not found table: " + tableName));

            String selectedXAxis = metricQueryPanel.getXTextFile().getText();
            CProfile xAxisProfile = getCProfile(selectedTable, selectedXAxis);

            String selectedYAxis = Objects.requireNonNull(metricQueryPanel.getYComboBox()
                                                              .getSelectedItem()).toString();
            CProfile yAxisProfile = getCProfile(selectedTable, selectedYAxis);

            String selectedDimension = Objects.requireNonNull(metricQueryPanel.getDimensionComboBox()
                                                                  .getSelectedItem()).toString();
            CProfile dimensionProfile = getCProfile(selectedTable, selectedDimension);

            String selectedYAxisFunction = Objects.requireNonNull(metricQueryPanel.getGroupFunction()
                                                                      .getSelectedItem()).toString();

            String selectedChartType = Objects.requireNonNull(metricQueryPanel.getChartType()
                                                                  .getSelectedItem()).toString();

            boolean isChanged = false;
            boolean isDefault = metricQueryPanel.getDefaultCheckBox().isSelected();
            if (isDefault && idBusyDefault(saveQuery) == -1) {
              JOptionPane.showMessageDialog(null, " The metric " + nameMetrics
                  + " will be used by default", "Information", JOptionPane.INFORMATION_MESSAGE);
            }
            if (isDefault && idBusyDefault(saveQuery) > -1) {
              String busyMetric = getSelectedMetricNameById(idBusyDefault(saveQuery), saveQuery);
              int input = JOptionPane.showOptionDialog(null,// 0=yes, 1=no
                                                       "Will the metric " + nameMetrics
                                                           + " be the default? There is already a metric " +
                                                           busyMetric
                                                           + " with a value of true", "Information", JOptionPane.YES_NO_OPTION,
                                                       JOptionPane.QUESTION_MESSAGE, null, new String[]{"Yes",
                      "No"}, "No");
              if (input == 0) {
                int oldDefaultMetric = idBusyDefault(saveQuery);
                for (int i = 0; i < saveQuery.getMetricList().size(); i++) {
                  if (saveQuery.getMetricList().get(i).getId() == oldDefaultMetric) {
                    saveQuery.getMetricList().get(i).setIsDefault(false);
                  }
                }
                isChanged = true;
              }
              if (input == 1) {
                metricQueryPanel.getDefaultCheckBox().setSelected(false);
                isDefault = metricQueryPanel.getDefaultCheckBox().isSelected();
              }
            }

            Metric saveMetric = new Metric(metricIdNext.incrementAndGet(),
                                           nameMetrics,
                                           isDefault,
                                           xAxisProfile,
                                           yAxisProfile,
                                           dimensionProfile,
                                           GroupFunction.valueOf(selectedYAxisFunction),
                                           TimeRangeFunction.AUTO,
                                           NormFunction.SECOND,
                                           ChartType.valueOf(selectedChartType),
                                           Collections.emptyList());

            this.saveMetricList = saveQuery.getMetricList();

            saveMetricList.add(saveMetric);
            saveQuery.setMetricList(saveMetricList);

            profileManager.updateQuery(saveQuery);

            if (isChanged) {
              fillMetricCase(saveQuery);
            } else {
              metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
                  .addRow(new Object[]{saveMetric.getId(),
                      saveMetric.getName(),
                      saveMetric.getIsDefault(),
                      saveMetric.getXAxis().getColName(),
                      saveMetric.getYAxis().getColName(),
                      saveMetric.getGroup().getColName(),
                      saveMetric.getGroupFunction().toString(),
                      saveMetric.getChartType().toString()});
            }
            setPanelView(true);

          }
        } else {
          throw new EmptyNameException("The name field is empty");
        }
      } else if (description.equals("edit")) {
        QueryInfo editQuery = profileManager.getQueryInfoList().stream()
            .filter(f -> f.getName().equalsIgnoreCase(mainQueryPanel.getQueryName().getText()))
            .findAny()
            .orElseThrow(
                () -> new NotFoundException("Not found query: " + mainQueryPanel.getQueryName().getText()));

        Metric selectedMetric = editQuery.getMetricList().stream()
            .filter(f -> f.getName().equals(nameSelectedMetric))
            .findAny()
            .orElseThrow(() -> new NotFoundException("Not found metric: " + nameSelectedMetric));
        String tableName = metadataQueryPanel.getTableName().getText();
        String newMetricName = metricQueryPanel.getNameMetric().getText();
        if (!newMetricName.trim().isEmpty()) {
          if (isBusyName(newMetricName, selectedMetric.getId(), editQuery)) {
            String nameMetric = metricQueryPanel.getNameMetric().getText();

            TableInfo selectedTable = profileManager.getTableInfoByTableName(tableName);
            if (Objects.isNull(selectedTable)) {
              throw new NotFoundException("Not found table: " + tableName);
            }

            String selectedXAxis = metricQueryPanel.getXTextFile().getText();
            CProfile xAxisProfile = getCProfile(selectedTable, selectedXAxis);

            String selectedYAxis = Objects.requireNonNull(metricQueryPanel.getYComboBox()
                                                              .getSelectedItem()).toString();
            CProfile yAxisProfile = getCProfile(selectedTable, selectedYAxis);

            String selectedDimension = Objects.requireNonNull(metricQueryPanel.getDimensionComboBox()
                                                                  .getSelectedItem()).toString();
            CProfile dimensionProfile = getCProfile(selectedTable, selectedDimension);

            String selectedYAxisFunction = Objects.requireNonNull(metricQueryPanel.getGroupFunction()
                                                                      .getSelectedItem()).toString();

            String selectedChartType = Objects.requireNonNull(metricQueryPanel.getChartType()
                                                                  .getSelectedItem()).toString();

            boolean isChanged = false;
            boolean isDefault = metricQueryPanel.getDefaultCheckBox().isSelected();
            if (isDefault && idBusyDefault(editQuery) == -1) {
              JOptionPane.showMessageDialog(null, " The metric " + nameMetric
                  + " will be used by default", "Information", JOptionPane.INFORMATION_MESSAGE);
            }
            if (isDefault && idBusyDefault(editQuery) > -1) {
              String busyMetric = getSelectedMetricNameById(idBusyDefault(editQuery), editQuery);
              int input = JOptionPane.showOptionDialog(null,// 0=yes, 1=no
                                                       "Will the metric " + nameMetric
                                                           + " be the default? There is already a metric " +
                                                           busyMetric
                                                           + " with a value of true", "Information", JOptionPane.YES_NO_OPTION,
                                                       JOptionPane.QUESTION_MESSAGE, null, new String[]{"Yes",
                      "No"}, "No");
              if (input == 0) {
                int oldDefaultMetric = idBusyDefault(editQuery);
                for (int i = 0; i < editQuery.getMetricList().size(); i++) {
                  if (editQuery.getMetricList().get(i).getId() == oldDefaultMetric) {
                    editQuery.getMetricList().get(i).setIsDefault(false);
                  }
                }
                isChanged = true;
              }
              if (input == 1) {
                metricQueryPanel.getDefaultCheckBox().setSelected(false);
                isDefault = metricQueryPanel.getDefaultCheckBox().isSelected();
              }

            }

            Metric editMetric = new Metric(selectedMetric.getId(),
                                           nameMetric,
                                           isDefault,
                                           xAxisProfile,
                                           yAxisProfile,
                                           dimensionProfile,
                                           GroupFunction.valueOf(selectedYAxisFunction),
                                           TimeRangeFunction.AUTO,
                                           NormFunction.SECOND,
                                           ChartType.valueOf(selectedChartType),
                                           Collections.emptyList());

            this.editMetricList = editQuery.getMetricList();
            int index = editMetricList.indexOf(selectedMetric);

            editMetricList.set(index, editMetric);
            editQuery.setMetricList(editMetricList);

            profileManager.updateQuery(editQuery);

            fillMetricCase(editQuery);

            setPanelView(true);
          }
        } else {
          throw new EmptyNameException("The name field is empty");
        }
      }
    } else if (e.getSource() == metricQueryPanel.getMetricQueryButtonPanel().getBtnCancel()) {
      int selectedIndex = metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow();
      if (metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRowCount() > 0) {
        metricQueryPanel.getConfigMetricCase().getJxTable().setRowSelectionInterval(0, 0);
        setPanelView(true);
        metricQueryPanel.getConfigMetricCase().getJxTable()
            .setRowSelectionInterval(selectedIndex, selectedIndex);
        String selectedMetricName = (String) metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
            .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 1);
        boolean selectedMetricDefault = (Boolean) metricQueryPanel.getConfigMetricCase()
            .getDefaultTableModel()
            .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 2);
        String selectedMetricX = (String) metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
            .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 3);
        String selectedMetricY = (String) metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
            .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 4);
        String selectedMetricGroup = (String) metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
            .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 5);
        String selectedGroupFunction = (String) metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
            .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 6);
        String selectedMetricType = (String) metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
            .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 7);

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

  public boolean isBusyName(String newName,
                            int id,
                            QueryInfo query) {
    boolean isBusy = true;
    List<Metric> metricList = query.getMetricList();
    for (Metric metric : metricList) {
      if (metric.getName().equals(newName) && metric.getId() != id) {
        isBusy = false;
        throw new NameAlreadyExistException("Name " + newName + " already exists, please enter another one");
      }
      if (!metric.getName().equals(newName) && metric.getId() == id) {
        isBusy = true;
      }
    }
    return isBusy;
  }

  private CProfile getCProfile(TableInfo selectedTable,
                               String selectedNameCProfile) {
    return selectedTable.getCProfiles().stream()
        .filter(f -> f.getColName().equals(selectedNameCProfile))
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found CProfile: " + selectedNameCProfile));
  }


  private String getSelectedMetricName() {
    return metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
        .getValueAt(metricQueryPanel.getConfigMetricCase().getJxTable().getSelectedRow(), 1).toString();
  }

  private String getSelectedMetricNameById(int id,
                                           QueryInfo query) {
    String metricName = "";
    List<Metric> metricList = query.getMetricList();
    for (Metric metric : metricList) {
      if (metric.getId() == id) {
        metricName = metric.getName();
      }
    }
    return metricName;
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
    List<Metric> metricList = query.getMetricList();
    for (Metric metric : metricList) {
      if (metric.getIsDefault()) {
        id = metric.getId();
      }
    }
    return id;
  }

  private void fillMetricCase(QueryInfo query) {
    metricQueryPanel.getConfigMetricCase().getDefaultTableModel().getDataVector().removeAllElements();
    metricQueryPanel.getConfigMetricCase().getDefaultTableModel().fireTableDataChanged();
    List<Metric> metrics = query.getMetricList();
    for (Metric m : metrics) {
      metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
          .addRow(new Object[]{m.getId(),
              m.getName(),
              m.getIsDefault(),
              m.getXAxis().getColName(),
              m.getYAxis().getColName(),
              m.getGroup().getColName(),
              m.getGroupFunction().toString(),
              m.getChartType().toString()});
    }
    if (metricQueryPanel.getConfigMetricCase().getJxTable().getRowCount() != 0) {
      metricQueryPanel.getConfigMetricCase().getJxTable().setRowSelectionInterval(0, 0);
    } else {
      metricQueryPanel.getNameMetric().setText("");
      metricQueryPanel.getNameMetric().setPrompt("Metric name");
    }
  }

}