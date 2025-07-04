package ru.dimension.ui.view.structure.report;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.ReportHelper;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.column.ProfileColumnNames;
import ru.dimension.ui.model.column.QueryColumnNames;
import ru.dimension.ui.model.column.TaskColumnNames;
import ru.dimension.ui.model.config.ConfigClasses;
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.report.QueryReportData;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.ReportState;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.router.listener.ReportListener;
import ru.dimension.ui.view.panel.report.ReportTabsPane;
import ru.dimension.ui.view.structure.ReportView;

@Log4j2
@Singleton
public class ReportPresenter implements ReportListener, ListSelectionListener {

  private final ProfileManager profileManager;
  private final ReportView reportView;
  private final JXTableCase profileReportCase;
  private final JXTableCase taskReportCase;
  private final JXTableCase queryReportCase;
  private final ConfigurationManager configurationManager;
  private final EventListener eventListener;
  private final Map<ProfileTaskQueryKey, QueryReportData> mapReportData;
  private final List<File> designSaveDirs;
  private final List<File> designReportDirs;
  private final ReportTabsPane reportTabsPane;
  private final FilesHelper filesHelper;
  private final ReportHelper reportHelper;

  @Inject
  public ReportPresenter(@Named("profileManager") ProfileManager profileManager,
                         @Named("reportView") ReportView reportView,
                         @Named("profileReportCase") JXTableCase profileReportCase,
                         @Named("taskReportCase") JXTableCase taskReportCase,
                         @Named("queryReportCase") JXTableCase queryReportCase,
                         @Named("configurationManager") ConfigurationManager configurationManager,
                         @Named("eventListener") EventListener eventListener,
                         @Named("mapReportData") Map<ProfileTaskQueryKey, QueryReportData> mapReportData,
                         @Named("reportTaskPanel") ReportTabsPane reportTabsPane,
                         FilesHelper filesHelper,
                         ReportHelper reportHelper) {
    this.profileManager = profileManager;
    this.reportView = reportView;
    this.profileReportCase = profileReportCase;
    this.taskReportCase = taskReportCase;
    this.queryReportCase = queryReportCase;
    this.configurationManager = configurationManager;
    this.eventListener = eventListener;
    this.eventListener.addReportStateListener(this);
    this.profileReportCase.getJxTable().getSelectionModel().addListSelectionListener(this);
    this.taskReportCase.getJxTable().getSelectionModel().addListSelectionListener(this);
    this.queryReportCase.getJxTable().getSelectionModel().addListSelectionListener(this);
    this.mapReportData = mapReportData;

    this.reportTabsPane = reportTabsPane;
    this.designSaveDirs = new ArrayList<>();
    this.designReportDirs = new ArrayList<>();
    this.filesHelper = filesHelper;
    this.reportHelper = reportHelper;
  }

  @Override
  public void fireShowReport(ReportState reportState) {
    this.reportView.showReport();
  }

  public <T> void fillModel(Class<T> clazz) {
    if (ConfigClasses.Profile.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Profile..");
      configurationManager.getConfigList(Profile.class)
          .forEach(e -> profileReportCase.getDefaultTableModel().addRow(new Object[]{e.getId(), e.getName()}));

      if (profileReportCase.getDefaultTableModel().getRowCount() != 0) {
        profileReportCase.getJxTable().setRowSelectionInterval(0, 0);
      }
    }
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();

    if (!e.getValueIsAdjusting()) {
      if (listSelectionModel.isSelectionEmpty()) {
        log.info("Clearing profile fields");
      } else {
        if (e.getSource() == profileReportCase.getJxTable().getSelectionModel()) {

          taskReportCase.getDefaultTableModel().getDataVector().removeAllElements();
          taskReportCase.getDefaultTableModel().fireTableDataChanged();
          int profileId = GUIHelper.getIdByColumnName(profileReportCase.getJxTable(),
                                                      profileReportCase.getDefaultTableModel(),
                                                      listSelectionModel, ProfileColumnNames.ID.getColName());
          if (profileId >= 0) {
            profileManager.getProfileInfoById(profileId).getTaskInfoList()
                .forEach(taskId -> {
                  TaskInfo taskIn = profileManager.getTaskInfoById(taskId);
                  if (Objects.isNull(taskIn)) {
                    throw new NotFoundException("Not found profile: " + taskId);
                  }
                  taskReportCase.getDefaultTableModel().addRow(new Object[]{taskIn.getId(), taskIn.getName()});
                });

            if (taskReportCase.getDefaultTableModel().getRowCount() > 0) {
              taskReportCase.getJxTable().setRowSelectionInterval(0, 0);
            } else {
              queryReportCase.getDefaultTableModel().getDataVector().removeAllElements();
              queryReportCase.getDefaultTableModel().fireTableDataChanged();
            }
          } else {
            JOptionPane.showMessageDialog(null, "Profile is not selected", "General Error",
                                          JOptionPane.ERROR_MESSAGE);
          }
        }
        if (e.getSource() == taskReportCase.getJxTable().getSelectionModel()) {
          queryReportCase.getDefaultTableModel().getDataVector().removeAllElements();
          queryReportCase.getDefaultTableModel().fireTableDataChanged();

          int taskId = GUIHelper.getIdByColumnName(taskReportCase.getJxTable(),
                                                   taskReportCase.getDefaultTableModel(), taskReportCase.getJxTable()
                                                       .getSelectionModel(),
                                                   TaskColumnNames.ID.getColName());

          if (taskReportCase.getDefaultTableModel().getRowCount() > 0) {
            if (Objects.isNull(profileManager.getTaskInfoById(taskId))) {
              throw new NotFoundException("Not found task: " + taskId);
            } else {
              AtomicInteger row = new AtomicInteger();
              profileManager.getTaskInfoById(taskId)
                  .getQueryInfoList()
                  .forEach(queryId -> {
                    QueryInfo queryIn = profileManager.getQueryInfoById(queryId);
                    if (Objects.isNull(queryIn)) {
                      throw new NotFoundException("Not found query: " + queryId);
                    }
                    queryReportCase.getDefaultTableModel().addRow(new Object[0]);
                    queryReportCase.getJxTable().setValueAt(false, row.get(), 0);
                    queryReportCase.getDefaultTableModel().setValueAt(queryIn.getId(), row.get(), 0);
                    queryReportCase.getDefaultTableModel().setValueAt(queryIn.getName(), row.get(), 2);
                    row.getAndIncrement();
                  });
              queryReportCase.getJxTable().repaint();
              queryReportCase.getJxTable().revalidate();
            }
            if (queryReportCase.getDefaultTableModel().getRowCount() > 0) {
              queryReportCase.getJxTable().setRowSelectionInterval(0, 0);
            } else {
              queryReportCase.getDefaultTableModel().getDataVector().removeAllElements();
              queryReportCase.getDefaultTableModel().fireTableDataChanged();
            }
          } else {
            List<QueryInfo> queyrList = profileManager.getQueryInfoList();
            for (int i = 0; i < queyrList.size(); i++) {
              queryReportCase.getDefaultTableModel().addRow(new Object[0]);
              queryReportCase.getDefaultTableModel().setValueAt(false, i, 1);
              queryReportCase.getDefaultTableModel().setValueAt(queyrList.get(i).getId(), i, 0);
              queryReportCase.getDefaultTableModel().setValueAt(queyrList.get(i).getName(), i, 2);
            }
          }
          if (!mapReportData.isEmpty()) {
            for (ProfileTaskQueryKey key : mapReportData.keySet()) {
              int profileId = (int) profileReportCase.getDefaultTableModel()
                  .getValueAt(profileReportCase.getJxTable().getSelectedRow()
                      , profileReportCase.getDefaultTableModel()
                                  .findColumn(ProfileColumnNames.ID.getColName()));
              if (profileId == key.getProfileId() && taskId == key.getTaskId()) {
                for (int row = 0; row < queryReportCase.getJxTable().getRowCount(); row++) {
                  int queryId = (int) queryReportCase.getDefaultTableModel()
                      .getValueAt(row, queryReportCase.getDefaultTableModel()
                          .findColumn(QueryColumnNames.ID.getColName()));
                  if (queryId == key.getQueryId()) {
                    queryReportCase.getJxTable().setValueAt(true, row, 0);
                  }
                }
              }
            }
          }
        }
      }
    }

  }
}

