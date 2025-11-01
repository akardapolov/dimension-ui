package ru.dimension.ui.view.handler.query;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
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
import ru.dimension.ui.exception.EmptyNameException;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.exception.NotSelectedRowException;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.handler.LifeCycleStatus;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.panel.config.query.MainQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetadataQueryPanel;
import ru.dimension.ui.view.panel.config.query.QueryPanel;

@Log4j2
@Singleton
public class QueryButtonPanelHandler implements ActionListener {

  private final ProfileManager profileManager;
  private final EventListener eventListener;

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final QueryPanel queryPanel;
  private final ButtonPanel queryButtonPanel;
  private final ConfigTab configTab;
  private final JXTableCase configMetadataCase;
  private final JTabbedPane mainQuery;
  private final JCheckBox checkboxConfig;

  private final MainQueryPanel mainQueryPanel;
  private final MetadataQueryPanel metadataQueryPanel;
  private LifeCycleStatus status;
  private final ResourceBundle bundleDefault;

  @Inject
  public QueryButtonPanelHandler(@Named("profileManager") ProfileManager profileManager,
                                 @Named("eventListener") EventListener eventListener,
                                 @Named("profileConfigCase") JXTableCase profileCase,
                                 @Named("taskConfigCase") JXTableCase taskCase,
                                 @Named("connectionConfigCase") JXTableCase connectionCase,
                                 @Named("queryConfigCase") JXTableCase queryCase,
                                 @Named("configMetadataCase") JXTableCase configMetadataCase,
                                 @Named("queryConfigPanel") QueryPanel queryPanel,
                                 @Named("queryButtonPanel") ButtonPanel queryButtonPanel,
                                 @Named("mainQueryPanel") MainQueryPanel mainQueryPanel,
                                 @Named("metadataQueryPanel") MetadataQueryPanel metadataQueryPanel,
                                 @Named("jTabbedPaneConfig") ConfigTab configTab,
                                 @Named("mainQueryTab") JTabbedPane mainQuery,
                                 @Named("checkboxConfig") JCheckBox checkboxConfig) {
    this.profileManager = profileManager;
    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.queryPanel = queryPanel;
    this.queryButtonPanel = queryButtonPanel;
    this.configMetadataCase = configMetadataCase;
    this.mainQuery = mainQuery;

    this.mainQueryPanel = mainQueryPanel;
    this.metadataQueryPanel = metadataQueryPanel;

    this.configTab = configTab;
    this.checkboxConfig = checkboxConfig;

    this.bundleDefault = Internationalization.getInternationalizationBundle();

    this.queryButtonPanel.getBtnNew().addActionListener(this);
    this.queryButtonPanel.getBtnCopy().addActionListener(this);
    this.queryButtonPanel.getBtnDel().addActionListener(this);
    this.queryButtonPanel.getBtnEdit().addActionListener(this);
    this.queryButtonPanel.getBtnSave().addActionListener(this);
    this.queryButtonPanel.getBtnCancel().addActionListener(this);

    this.queryButtonPanel.getBtnDel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
    this.queryButtonPanel.getBtnDel().getActionMap().put("delete", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        queryButtonPanel.getBtnDel().doClick();
      }
    });

    this.queryButtonPanel.getBtnCancel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    this.queryButtonPanel.getBtnCancel().getActionMap().put("cancel", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        queryButtonPanel.getBtnCancel().doClick();
      }
    });

    this.status = LifeCycleStatus.NONE;

    this.eventListener = eventListener;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == queryButtonPanel.getBtnNew()) {

      status = LifeCycleStatus.NEW;
      setPanelView(false);
      newEmptyPanel();
      clearProfileMetadataCase();

    } else if (e.getSource() == queryButtonPanel.getBtnCopy()) {

      status = LifeCycleStatus.COPY;
      if (queryCase.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("The query to copy is not selected. Please select and try again!");
      } else {
        setPanelView(false);

        int queryId = getSelectedQueryId();
        QueryInfo query = profileManager.getQueryInfoById(queryId);
        if (Objects.isNull(query)) {
          throw new NotFoundException("Not found query: " + queryId);
        }

        mainQueryPanel.getQueryName().setText(query.getName() + "_copy");
        mainQueryPanel.getQueryDescription().setText(query.getDescription() + "_copy");
        mainQueryPanel.getQueryGatherDataComboBox().setSelectedItem(query.getGatherDataMode());
        mainQueryPanel.getQuerySqlText().setText(query.getText());
      }

    } else if (e.getSource() == queryButtonPanel.getBtnDel()) {
      if (queryCase.getJxTable().getSelectedRow() == -1) {
        JOptionPane.showMessageDialog(null, "Not selected query. Please select and try again!",
                                      "General Error", JOptionPane.ERROR_MESSAGE);
      } else {
        int queryId = getSelectedQueryId();
        int input = JOptionPane.showConfirmDialog(new JDialog(),// 0=yes, 1=no, 2=cancel
                                                  "Do you want to delete configuration: "
                                                      + queryCase.getDefaultTableModel()
                                                      .getValueAt(queryCase.getJxTable().getSelectedRow(), 1) + "?");
        if (isUsedOnTask(queryId)) {
          if (input == 0) {
            QueryInfo query = profileManager.getQueryInfoById(queryId);
            if (Objects.isNull(query)) {
              throw new NotFoundException("Not found query by id: " + queryId);
            }

            profileManager.deleteQuery(query.getId(), query.getName());
            profileManager.deleteTable(query.getName());

            clearQueryCase();

            profileManager.getQueryInfoList().forEach(queryInfo -> queryCase.getDefaultTableModel()
                .addRow(new Object[]{queryInfo.getId(), queryInfo.getName()}));

            if (queryCase.getJxTable().getSelectedRow() > 0) {
              queryCase.getJxTable().setRowSelectionInterval(0, 0);
            }
          }
        } else {
          JOptionPane.showMessageDialog(null, "Cannot delete this query it is used in the task",
                                        "General Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    } else if (e.getSource() == queryButtonPanel.getBtnEdit()) {
      if (queryCase.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("Not selected task. Please select and try again!");
      }
      status = LifeCycleStatus.EDIT;
      setPanelView(false);

    } else if (e.getSource() == queryButtonPanel.getBtnSave()) {
      if (LifeCycleStatus.NEW.equals(status) || LifeCycleStatus.COPY.equals(status)) {

        AtomicInteger queryIdNext = new AtomicInteger();

        profileManager.getQueryInfoList().stream()
            .max(Comparator.comparing(QueryInfo::getId))
            .ifPresentOrElse(query -> queryIdNext.set(query.getId()),
                             () -> {
                               log.info("Not found Query");
                               queryIdNext.set(0);
                             });

        if (!queryPanel.getMainQueryPanel().getQueryName().getText().trim().isEmpty()) {
          int queryId = queryIdNext.incrementAndGet();
          String newQueryName = queryPanel.getMainQueryPanel().getQueryName().getText();
          checkQueryNameIsBusy(queryId, newQueryName);

          QueryInfo queryInfo = new QueryInfo();
          queryInfo.setId(queryId);
          queryInfo.setName(mainQueryPanel.getQueryName().getText());
          queryInfo.setDescription(mainQueryPanel.getQueryDescription().getText());

          String selectedEnumGatherData = Objects.requireNonNull(mainQueryPanel.getQueryGatherDataComboBox()
                                                                     .getSelectedItem()).toString();

          queryInfo.setGatherDataMode(GatherDataMode.valueOf(selectedEnumGatherData));
          queryInfo.setText(mainQueryPanel.getQuerySqlText().getText());

          TableInfo tableInfo = new TableInfo();
          tableInfo.setTableName(queryInfo.getName());

          profileManager.addQuery(queryInfo);
          profileManager.addTable(tableInfo);

          clearQueryCase();

          int selection = 0;
          int index = 0;
          for (QueryInfo query : profileManager.getQueryInfoList()) {
            queryCase.getDefaultTableModel().addRow(new Object[]{query.getId(), query.getName()});

            if (query.getId() == queryInfo.getId()) {
              index++;
              selection = index;
            }
            index++;
          }

          setPanelView(true);
          queryCase.getJxTable().setRowSelectionInterval(selection - 1, selection - 1);
        } else {
          throw new EmptyNameException("The name field is empty");
        }
      } else if (LifeCycleStatus.EDIT.equals(status)) {
        int queryId = getSelectedQueryId();

        if (!queryPanel.getMainQueryPanel().getQueryName().getText().trim().isEmpty()) {

          int selectedIndex = queryCase.getJxTable().getSelectedRow();
          String newQueryName = queryPanel.getMainQueryPanel().getQueryName().getText();
          checkQueryNameIsBusy(queryId, newQueryName);

          QueryInfo oldQuery = profileManager.getQueryInfoById(queryId);

          QueryInfo editQuery = new QueryInfo();
          editQuery.setId(queryId);
          editQuery.setName(mainQueryPanel.getQueryName().getText());
          editQuery.setDescription(mainQueryPanel.getQueryDescription().getText());
          String selectedEnumGatherData = Objects.requireNonNull(mainQueryPanel.getQueryGatherDataComboBox()
                                                                     .getSelectedItem())
              .toString();
          editQuery.setGatherDataMode(GatherDataMode.valueOf(selectedEnumGatherData));
          editQuery.setText(mainQueryPanel.getQuerySqlText().getText());

          clearProfileMetadataCase();

          TableInfo tableInfo = new TableInfo();
          tableInfo.setTableName(editQuery.getName());

          if (!oldQuery.getName().equals(newQueryName)) {
            deleteQueryById(queryId);
            profileManager.addQuery(editQuery);
            profileManager.addTable(tableInfo);
          } else {
            profileManager.updateQuery(editQuery);
          }

          clearQueryCase();

          for (QueryInfo q : profileManager.getQueryInfoList()) {
            queryCase.getDefaultTableModel()
                .addRow(new Object[]{q.getId(), q.getName()});
          }
          setPanelView(true);

          mainQueryPanel.getQueryGatherDataComboBox().setSelectedItem(GatherDataMode.valueOf(selectedEnumGatherData));
          queryCase.getJxTable().setRowSelectionInterval(selectedIndex, selectedIndex);

        } else {
          throw new EmptyNameException("The name field is empty");
        }

      }
    } else if (e.getSource() == queryButtonPanel.getBtnCancel()) {
      if (queryCase.getJxTable().getSelectedRowCount() > 0) {
        int selectedIndex = queryCase.getJxTable().getSelectedRow();
        queryCase.getJxTable().setRowSelectionInterval(0, 0);
        setPanelView(true);
        queryCase.getJxTable().setRowSelectionInterval(selectedIndex, selectedIndex);
        int queryId = getSelectedQueryId();
        QueryInfo queryInfo = profileManager.getQueryInfoById(queryId);
        if (Objects.isNull(queryInfo)) {
          throw new NotFoundException("Not found query: " + queryId);
        }
        mainQueryPanel.getQueryName().setText(queryInfo.getName());
        mainQueryPanel.getQueryDescription().setText(queryInfo.getDescription());
        mainQueryPanel.getQueryGatherDataComboBox().setSelectedItem(queryInfo.getGatherDataMode());
        mainQueryPanel.getQuerySqlText().setText(queryInfo.getText());

      } else {
        setPanelView(true);
        newEmptyPanel();
        clearProfileMetadataCase();
      }
    }
  }

  private void newEmptyPanel() {
    mainQueryPanel.getQueryName().setText("");
    mainQueryPanel.getQueryName().setPrompt(bundleDefault.getString("qName"));
    mainQueryPanel.getQueryDescription().setText("");
    mainQueryPanel.getQueryDescription().setPrompt(bundleDefault.getString("qDesc"));
    mainQueryPanel.getQuerySqlText().setText("");
  }

  public void checkQueryNameIsBusy(int id,
                                   String newQueryName) {
    List<QueryInfo> queryList = profileManager.getQueryInfoList();
    for (QueryInfo query : queryList) {
      if (query.getName().equals(newQueryName) && query.getId() != id) {
        throw new NotFoundException("Name " + newQueryName
                                        + " already exists, please enter another one.");
      }
    }
  }

  public void deleteQueryById(int id) {
    QueryInfo queryDel = profileManager.getQueryInfoById(id);
    if (Objects.isNull(queryDel)) {
      throw new NotFoundException("Not found query by id: " + id);
    }
    profileManager.deleteQuery(queryDel.getId(), queryDel.getName());
  }

  private void clearProfileMetadataCase() {
    metadataQueryPanel.getConfigMetadataCase().getDefaultTableModel().getDataVector().removeAllElements();
    metadataQueryPanel.getConfigMetadataCase().getDefaultTableModel().fireTableDataChanged();
  }

  private void clearQueryCase() {
    queryCase.getDefaultTableModel().getDataVector().removeAllElements();
    queryCase.getDefaultTableModel().fireTableDataChanged();
  }

  private int getSelectedQueryId() {
    return (Integer) queryCase.getDefaultTableModel()
        .getValueAt(queryCase.getJxTable().getSelectedRow(), 0);
  }

  private void setPanelView(Boolean isSelected) {
    queryButtonPanel.setButtonView(isSelected);
    mainQueryPanel.getQueryName().setEditable(!isSelected);
    mainQueryPanel.getQueryDescription().setEditable(!isSelected);
    mainQueryPanel.getQueryGatherDataComboBox().setEnabled(!isSelected);
    mainQueryPanel.getQuerySqlText().setEditable(!isSelected);
    configTab.setEnabledAt(1, isSelected);
    configTab.setEnabledAt(2, isSelected);
    configTab.setEnabledAt(0, isSelected);
    taskCase.getJxTable().setEnabled(isSelected);
    connectionCase.getJxTable().setEnabled(isSelected);
    profileCase.getJxTable().setEnabled(isSelected);
    queryCase.getJxTable().setEnabled(isSelected);
    mainQuery.setEnabledAt(1, isSelected);
    mainQuery.setEnabledAt(2, isSelected);
    checkboxConfig.setEnabled(isSelected);
  }

  private boolean isUsedOnTask(int queryId) {
    return !profileManager.getTaskInfoList().stream()
        .anyMatch(task -> task.getQueryInfoList().contains(queryId));
  }
}

