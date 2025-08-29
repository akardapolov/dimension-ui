package ru.dimension.ui.view.handler.report.design;

import static ru.dimension.ui.model.chart.ChartType.LINEAR;
import static ru.dimension.ui.model.chart.ChartType.STACKED;

import com.itextpdf.text.Document;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerFontProvider;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.AbstractButton;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
import org.jdesktop.swingx.JXTaskPaneContainer;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.ReportHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.ReportManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.SourceConfig;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.column.ProfileColumnNames;
import ru.dimension.ui.model.column.QueryColumnNames;
import ru.dimension.ui.model.column.TaskColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.db.TimestampType;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.report.CProfileReport;
import ru.dimension.ui.model.report.MetricReport;
import ru.dimension.ui.model.report.QueryReportData;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.tab.ReportTabPane;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.component.chart.history.HistorySCP;
import ru.dimension.ui.view.panel.report.ChartCardPanel;
import ru.dimension.ui.view.panel.report.MetricColumnPanel;
import ru.dimension.ui.view.panel.report.ReportTabsPane;
import ru.dimension.ui.view.panel.report.pdf.PathPdfInfo;
import ru.dimension.ui.view.panel.report.pdf.PdfViewer;

@Log4j2
@Singleton
public class DesignPanelHandler extends ChartReportHandler implements ActionListener, ListSelectionListener {

  private final Map<ProfileTaskQueryKey, QueryReportData> mapReportData;
  private final ReportTabsPane reportTabsPane;
  private final JXTableCase profileReportCase;
  private final JXTableCase taskReportCase;
  private final JXTableCase queryReportCase;
  private final DefaultCellEditor queryEditor;
  private final JXTaskPaneContainer containerCardDesign;
  private final FilesHelper filesHelper;
  private final ReportManager reportManager;
  private List<File> designSaveDirs;
  private List<File> designReportDirs;
  private final ReportHelper reportHelper;
  private final PathPdfInfo reportPdfPath;
  private JPanel containerChartCardDesign;
  private int savedRow;
  private int previousSelectedRow = -1;
  private int selectedRow;
  private final Object[] options = {"Overwrite", "Save", "Cancel"};

  @Inject
  public DesignPanelHandler(
      @Named("profileReportCase") JXTableCase profileReportCase,
      @Named("taskReportCase") JXTableCase taskReportCase,
      @Named("queryReportCase") JXTableCase queryReportCase,
      @Named("reportTaskPanel") ReportTabsPane reportTabsPane,
      @Named("profileManager") ProfileManager profileManager,
      @Named("eventListener") EventListener eventListener,
      @Named("mapReportData") Map<ProfileTaskQueryKey, QueryReportData> mapReportData,
      @Named("containerCardDesign") JXTaskPaneContainer containerCardDesign,
      @Named("reportPdfPath") PathPdfInfo reportPdfPath,
      @Named("localDB") DStore fStore,
      @Named("reportManager") ReportManager reportManager,
      FilesHelper filesHelper,
      ReportHelper reportHelper) {
    super(profileManager, eventListener, fStore);

    this.profileReportCase = profileReportCase;
    this.taskReportCase = taskReportCase;
    this.queryReportCase = queryReportCase;
    this.mapReportData = mapReportData;
    this.containerCardDesign = containerCardDesign;
    this.reportTabsPane = reportTabsPane;
    this.filesHelper = filesHelper;
    this.reportHelper = reportHelper;
    this.reportManager = reportManager;
    this.reportPdfPath = reportPdfPath;
    this.containerChartCardDesign = new JPanel();

    this.savedRow = -1;
    this.selectedRow = -1;

    updateDesignReportCase();
    updateSavedReportCase();

    this.reportTabsPane.getShowBtnDesign().addActionListener(this);
    this.reportTabsPane.getCollapseBtnDesign().addActionListener(this);
    this.reportTabsPane.getSaveBtnDesign().addActionListener(this);
    this.reportTabsPane.getDelBtnDesign().addActionListener(this);
    this.reportTabsPane.getClearBtnDesign().addActionListener(this);
    this.reportTabsPane.getGenerateReportBtnDesign().addActionListener(this);
    this.reportTabsPane.getDesignReportCase().getJxTable().getSelectionModel().addListSelectionListener(this);

    this.queryEditor = new DefaultCellEditor(new JCheckBox());
    this.queryReportCase.getJxTable().getColumnModel().getColumn(0).setCellEditor(queryEditor);

    queryEditor.addCellEditorListener(new CellEditorListener() {
      @Override
      public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = (TableCellEditor) e.getSource();
        Boolean qValue = (Boolean) editor.getCellEditorValue();

        ProfileTaskQueryKey key = getProfileTaskQueryKey();

        String title = setTitleCard(profileManager.getProfileInfoById(key.getProfileId()).getName(),
                                    profileManager.getTaskInfoById(key.getTaskId()).getName(),
                                    profileManager.getQueryInfoById(key.getQueryId()).getName());

        if (qValue) {
          viewCardMetricColumns(key);
          mapReportData.put(key, new QueryReportData());

          reportTabsPane.getCollapseBtnDesign().setVisible(true);
          reportTabsPane.getClearBtnDesign().setEnabled(true);
          reportTabsPane.getJTabbedPaneChart().add(title, new JPanel());
        } else {
          if (isVisibleCard(key)) {
            containerCardDesign.remove(getCardComponent(key));
            int index = reportTabsPane.getJTabbedPaneChart().indexOfTab(title);
            reportTabsPane.getJTabbedPaneChart().remove(index);
            mapReportData.remove(key);
            if (mapReportData.isEmpty()) {
              reportTabsPane.getCollapseBtnDesign().setVisible(false);
              reportTabsPane.getClearBtnDesign().setEnabled(false);
            }
          }
        }
      }

      @Override
      public void editingCanceled(ChangeEvent e) {

      }
    });

  }

  private ProfileTaskQueryKey getProfileTaskQueryKey() {
    int profileId = (int) profileReportCase.getDefaultTableModel()
        .getValueAt(profileReportCase.getJxTable()
                        .getSelectedRow(), profileReportCase.getDefaultTableModel()
                        .findColumn(ProfileColumnNames.ID.getColName()));
    int taskId = (int) taskReportCase.getDefaultTableModel()
        .getValueAt(taskReportCase.getJxTable()
                        .getSelectedRow(), taskReportCase.getDefaultTableModel()
                        .findColumn(TaskColumnNames.ID.getColName()));
    int queryId = (int) queryReportCase.getDefaultTableModel()
        .getValueAt(queryReportCase.getJxTable()
                        .getSelectedRow(), queryReportCase.getDefaultTableModel()
                        .findColumn(QueryColumnNames.ID.getColName()));

    return new ProfileTaskQueryKey(profileId, taskId, queryId);
  }


  private void viewCardMetricColumns(ProfileTaskQueryKey key) {

    QueryInfo query = profileManager.getQueryInfoById(key.getQueryId());
    ProfileInfo profile = profileManager.getProfileInfoById(key.getProfileId());
    TaskInfo task = profileManager.getTaskInfoById(key.getTaskId());
    List<Metric> metricList = query.getMetricList();
    TableInfo tableInfo = profileManager.getTableInfoByTableName(query.getName());

    List<CProfile> cProfileList = tableInfo.getCProfiles();

    MetricColumnPanel cardMetricCol = getCardComponent(key);

    cardMetricCol.setTitle(query.getName());
    cardMetricCol.setToolTipText(setTitleCard(profile.getName(), task.getName(), ""));

    cardMetricCol.getJtcMetric().getDefaultTableModel().getDataVector().removeAllElements();
    cardMetricCol.getJtcMetric().getDefaultTableModel().fireTableDataChanged();

    if (metricList.size() != 0) {
      for (Metric m : metricList) {
        cardMetricCol.getJtcMetric().getDefaultTableModel().addRow(new Object[]{m.getId(), false, m.getName()});
      }
    }

    cardMetricCol.getJtcColumn().getDefaultTableModel().getDataVector().removeAllElements();
    cardMetricCol.getJtcColumn().getDefaultTableModel().fireTableDataChanged();

    if (cProfileList != null) {

      cProfileList.stream()
          .filter(f -> !f.getCsType().isTimeStamp())
          .forEach(c -> cardMetricCol.getJtcColumn().getDefaultTableModel()
              .addRow(new Object[]{c.getColId(), false, c.getColName()}));
    }
    containerCardDesign.add(cardMetricCol);
    containerCardDesign.revalidate();
    containerCardDesign.repaint();
  }

  private boolean isVisibleCard(ProfileTaskQueryKey key) {
    List<Component> components = Arrays.asList(containerCardDesign.getComponents());
    return components.stream()
        .filter(card -> card instanceof MetricColumnPanel)
        .map(card -> (MetricColumnPanel) card)
        .anyMatch(cardI -> key.equals(cardI.getKey()));
  }

  private MetricColumnPanel getCardComponent(ProfileTaskQueryKey key) {
    List<Component> components = Arrays.asList(containerCardDesign.getComponents());
    return components.stream()
        .filter(card -> card instanceof MetricColumnPanel)
        .map(card -> (MetricColumnPanel) card)
        .filter(card -> key.equals(card.getKey()))
        .findFirst()
        .orElse(new MetricColumnPanel(key, mapReportData, profileManager, reportTabsPane, reportHelper, containerCardDesign));
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == reportTabsPane.getShowBtnDesign()) {

      List<String> titlesList = new ArrayList<>();
      int tabCount = reportTabsPane.getJTabbedPaneChart().getTabCount();
      for (int i = 0; i < tabCount; i++) {
        String title = reportTabsPane.getJTabbedPaneChart().getTitleAt(i);
        titlesList.add(title);
      }

      Component[] componentsList = containerChartCardDesign.getComponents();
      if (componentsList != null) {
        for (Component c : componentsList) {
          if (c instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) c;
            JViewport viewport = scrollPane.getViewport();
            if (viewport != null) {
              Component[] components = viewport.getComponents();
              for (Component component : components) {
                viewport.remove(component);
              }
              viewport.revalidate();
              viewport.repaint();
            }
          }
        }
      }

      Date beginDate = reportTabsPane.getDateTimePickerFrom().getDate();
      Date endDate = reportTabsPane.getDateTimePickerTo().getDate();

      LocalDateTime begin = beginDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
      LocalDateTime end = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

      if (!begin.isBefore(end)) {
        JOptionPane.showMessageDialog(new JDialog(), "Begin value must be less the end one",
                                      "General Error", JOptionPane.ERROR_MESSAGE);
      } else {

        viewButtons(true);

        reportTabsPane.getDesignSaveInfo().setText("This design has not been saved");
        reportTabsPane.getDesignSaveInfo().setForeground(new Color(255, 93, 93));
        savedRow = -1;

        reportTabsPane.getDesignReportCase().getJxTable().clearSelection();

        for (Map.Entry<ProfileTaskQueryKey, QueryReportData> entry : mapReportData.entrySet()) {

          ProfileTaskQueryKey key = entry.getKey();

          QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());
          ProfileInfo profileInfo = profileManager.getProfileInfoById(key.getProfileId());
          TaskInfo taskInfo = profileManager.getTaskInfoById(key.getTaskId());
          TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());

          List<MetricReport> metricReportList = entry.getValue().getMetricReportList();
          List<CProfileReport> cProfileReportList = entry.getValue().getCProfileReportList();

          ChartInfo chartInfo = profileManager.getChartInfoById(queryInfo.getId());
          if (Objects.isNull(chartInfo)) {
            throw new NotFoundException(String.format("Chart info with id=%s not found",
                                                      queryInfo.getId()));
          }

          chartInfo.setRangeHistory(RangeHistory.CUSTOM);

          containerChartCardDesign = new JPanel(new GridLayout(
              metricReportList.size() + cProfileReportList.size(), 1, 5, 5));

          if (metricReportList.size() != 0 || cProfileReportList.size() != 0) {
            String title = setTitleCard(profileInfo.getName(), taskInfo.getName(), queryInfo.getName());
            int index = reportTabsPane.getJTabbedPaneChart().indexOfTab(title);

            JScrollPane jScrollPane = new JScrollPane();
            GUIHelper.setScrolling(jScrollPane);

            Component tabComponent = reportTabsPane.getJTabbedPaneChart().getComponentAt(index);
            if (tabComponent instanceof JScrollPane container) {
              container.removeAll();
              container.revalidate();
              container.repaint();
            }

            containerChartCardDesign.setToolTipText(setTitleCard(profileInfo.getName(), taskInfo.getName(), queryInfo.getName()));

            chartInfo.setCustomBegin(begin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            chartInfo.setCustomEnd(end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

            if (metricReportList.size() != 0) {
              for (MetricReport m : metricReportList) {

                Metric metric = getMetricByMetricReport(m);

                ChartCardPanel cardChart = new ChartCardPanel(m.getId(), chartInfo, key,
                                                              SourceConfig.METRICS, metric, profileManager, eventListener, fStore, reportHelper, mapReportData);

                cardChart.getFunctionPanel().getCount().setEnabled(false);
                cardChart.getFunctionPanel().getSum().setEnabled(false);
                cardChart.getFunctionPanel().getAvg().setEnabled(false);

                cardChart.setBorder(new EtchedBorder());
                cardChart.setSelectedRadioButton(m.getGroupFunction());
                cardChart.getJtaDescription().setText(m.getComment());

                cardChart.loadChart(m.getId(), chartInfo, key, cardChart, SourceConfig.METRICS);
                containerChartCardDesign.add(cardChart);
                containerChartCardDesign.repaint();
                containerChartCardDesign.revalidate();
              }

            }

            if (cProfileReportList.size() != 0) {
              for (CProfileReport cProfileReport : cProfileReportList) {

                Metric metric = getMetricByCProfileReport(key, cProfileReport, tableInfo);

                ChartCardPanel cardChart = new ChartCardPanel(cProfileReport.getColId(), chartInfo, key,
                                                              SourceConfig.COLUMNS, metric, profileManager, eventListener, fStore, reportHelper, mapReportData);

                cardChart.setBorder(new EtchedBorder());
                cardChart.setSelectedRadioButton(metric.getGroupFunction());
                cardChart.getJtaDescription().setText(cProfileReport.getComment());

                cardChart.loadChart(cProfileReport.getColId(), chartInfo, key, cardChart, SourceConfig.COLUMNS);
                containerChartCardDesign.add(cardChart);
                containerChartCardDesign.repaint();
                containerChartCardDesign.revalidate();

              }
            }
            jScrollPane.setViewportView(containerChartCardDesign);
            reportTabsPane.getJTabbedPaneChart().setComponentAt(index, jScrollPane);
            reportTabsPane.getJTabbedPaneChart().setSelectedIndex(index);
          }
        }

        for (int i = 0; i < tabCount; i++) {
          reportTabsPane.getJTabbedPaneChart().setTitleAt(i, " ");
          reportTabsPane.getJTabbedPaneChart().setTitleAt(i, titlesList.get(i));
        }
        reportTabsPane.repaint();
        reportTabsPane.revalidate();
      }
    }
    if (e.getSource() == reportTabsPane.getCollapseBtnDesign()) {
      ArrayList<Component> containerCards = new ArrayList<>(List.of(containerCardDesign.getComponents()));

      boolean collapseAll = reportTabsPane.getCollapseBtnDesign().getText().equals("Collapse all");

      containerCards.stream()
          .filter(c -> c instanceof MetricColumnPanel)
          .map(c -> (MetricColumnPanel) c)
          .forEach(cardChart -> cardChart.setCollapsed(collapseAll));

      reportTabsPane.getCollapseBtnDesign().setText(collapseAll ? "Expand all" : "Collapse all");
    }

    if (e.getSource() == reportTabsPane.getClearBtnDesign()) {
      int input = JOptionPane.showConfirmDialog(new JDialog(),// 0=yes, 1=no, 2=cancel
                                                "Do you want to clear the design panel ?");
      if (input == 0) {
        reportTabsPane.getDesignSaveInfo().setText(" ");
        reportTabsPane.getDesignSaveInfo().setForeground(new Color(255, 93, 93));
        savedRow = -1;
        reportTabsPane.getDesignReportCase().getJxTable().clearSelection();

        containerCardDesign.removeAll();
        reportTabsPane.getJTabbedPaneChart().removeAll();
        mapReportData.clear();
        reportTabsPane.getCollapseBtnDesign().setVisible(false);
        for (int row = 0; row < queryReportCase.getJxTable().getRowCount(); row++) {
          queryReportCase.getJxTable().setValueAt(false, row, 0);
        }

        Map.Entry<Date, Date> range = DateHelper.getRangeDate();
        reportTabsPane.getDateTimePickerFrom().setDate(range.getKey());
        reportTabsPane.getDateTimePickerTo().setDate(range.getValue());

        reportTabsPane.getCollapseBtnDesign().setText("Collapse all");
        viewButtons(false);
        reportTabsPane.getDelBtnDesign().setEnabled(false);
      }
    }
    if (e.getSource() == reportTabsPane.getSaveBtnDesign()) {
      if (savedRow == -1) {
        savedDesign();
        reportHelper.setMadeChanges(false);
        savedRow = 0;
        reportTabsPane.getDesignReportCase().getJxTable().setRowSelectionInterval(0, 0);
        reportTabsPane.getDelBtnDesign().setEnabled(true);
      } else {
        int input = JOptionPane.showOptionDialog(new JDialog(),
                                                 "This design has already been saved, should overwrite it or save it as a new one?",
                                                 "Information", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (input == 0) {
          selectedRow = reportTabsPane.getDesignReportCase().getJxTable().getSelectedRow();
          savedRow = selectedRow;
          updatedDesign();
          reportHelper.setMadeChanges(false);
          reportTabsPane.getDesignReportCase().getJxTable().setRowSelectionInterval(selectedRow, selectedRow);
          reportTabsPane.getDelBtnDesign().setEnabled(true);
        } else if (input == 1) {
          savedDesign();
          reportHelper.setMadeChanges(false);
          savedRow = 0;
          reportTabsPane.getDesignReportCase().getJxTable().setRowSelectionInterval(0, 0);
          reportTabsPane.getDelBtnDesign().setEnabled(true);
        }
      }
    }
    if (e.getSource() == reportTabsPane.getGenerateReportBtnDesign()) {

      ListSelectionModel selectionModel = reportTabsPane.getDesignReportCase().getJxTable().getSelectionModel();
      if (!selectionModel.isSelectionEmpty()) {
        savedRow = reportTabsPane.getDesignReportCase().getJxTable().getSelectedRow();
      }

      String folderPath = filesHelper.getTemplateDir();
      boolean isEmpty = isFolderEmpty(folderPath);
      File folder = new File(folderPath);
      File[] files = folder.listFiles();
      boolean isTTFFile = false;
      boolean isFTLFile = false;

      if (isEmpty) {
        try {
          filesHelper.loadFileToFolder("default.ftl", folderPath);
          filesHelper.loadFileToFolder("arialuni.ttf", folderPath);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      } else {
        for (File file : files) {
          if (file.getName().endsWith(".ftl")) {
            isFTLFile = true;
          }
          if (file.getName().endsWith(".ttf")) {
            isTTFFile = true;
          }
        }
        try {
          if (!isFTLFile) {
            filesHelper.loadFileToFolder("default.ftl", folderPath);
          }
          if (!isTTFFile) {
            filesHelper.loadFileToFolder("arialuni.ttf", folderPath);
          }
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }

      if (savedRow == -1) {
        savedDesign();
        reportHelper.setMadeChanges(false);
        savedRow = 0;
        reportTabsPane.getDesignReportCase().getJxTable().setRowSelectionInterval(0, 0);
        createdReport();
        reportTabsPane.getSavedReportCase().getJxTable().setRowSelectionInterval(0, 0);
      } else {
        int inputDesign = JOptionPane.showOptionDialog(new JDialog(),
                                                       "This design has already been saved, should overwrite it or save it as a new one?",
                                                       "Information", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (inputDesign == 0) {

          selectedRow = reportTabsPane.getDesignReportCase().getJxTable().getSelectedRow();
          savedRow = selectedRow;
          updatedDesign();
          reportHelper.setMadeChanges(false);
          createdReport();

          String formattedDateForDir = reportPdfPath.getDateTimeFolder();
          LocalDateTime dateTime = LocalDateTime.parse(formattedDateForDir, reportHelper.getDateTimeFormatterFused());
          reportTabsPane.getDesignReportCase().getJxTable().setRowSelectionInterval(selectedRow, selectedRow);
          int rowCount = reportTabsPane.getSavedReportCase().getJxTable().getRowCount();
          for (int row = 0; row < rowCount; row++) {
            String value = (String) reportTabsPane.getSavedReportCase().getJxTable().getValueAt(row, 0);
            if (value.contains(dateTime.format(reportHelper.getDateTimeFormatter()))) {
              reportTabsPane.getSavedReportCase().getJxTable().setRowSelectionInterval(row, row);
            }
          }
          reportTabsPane.getDelBtnReport().setEnabled(true);

        } else if (inputDesign == 1) {
          savedDesign();
          reportHelper.setMadeChanges(false);
          savedRow = 0;
          reportTabsPane.getDesignReportCase().getJxTable().setRowSelectionInterval(0, 0);
          createdReport();
          reportTabsPane.getSavedReportCase().getJxTable().setRowSelectionInterval(0, 0);
          reportTabsPane.getDelBtnReport().setEnabled(true);
        }
      }

      if (reportTabsPane.isEnabledAt(1)) {
        reportTabsPane.setSelectedTab(ReportTabPane.REPORT);
        reportTabsPane.getSaveBtnPDFReport().setEnabled(true);
      }
    }

    if (e.getSource() == reportTabsPane.getDelBtnDesign()) {

      int selectedRow = reportTabsPane.getDesignReportCase().getJxTable().getSelectedRow();

      if (selectedRow == -1) {
        JOptionPane.showMessageDialog(null, "Not selected design. Please select and try again!",
                                      "General Error", JOptionPane.ERROR_MESSAGE);
      } else {
        JCheckBox checkbox = new JCheckBox("Delete with the report data");
        String message =
            "Do you want to delete configuration: " + reportTabsPane.getDesignReportCase().getDefaultTableModel()
                .getValueAt(selectedRow, 0) + "?";
        Object[] params = {message, checkbox};
        int input = JOptionPane.showConfirmDialog(new JDialog(),// 0=yes, 1=no, 2=cancel
                                                  params, "Information", JOptionPane.YES_NO_OPTION);
        boolean delWithReport = checkbox.isSelected();
        if (input == 0) {

          String dirName = reportPdfPath.getDirDesignName();
          String folderPath = filesHelper.getDesignDir() + filesHelper.getFileSeparator() + dirName;

          savedRow = -1;
          reportTabsPane.getDesignReportCase().getDefaultTableModel().removeRow(selectedRow);
          reportTabsPane.getDesignReportCase().getJxTable().clearSelection();

          reportTabsPane.getDesignSaveInfo().setText(" ");

          containerCardDesign.removeAll();
          reportTabsPane.getJTabbedPaneChart().removeAll();
          mapReportData.clear();

          reportTabsPane.getCollapseBtnDesign().setVisible(false);

          for (int row = 0; row < queryReportCase.getJxTable().getRowCount(); row++) {
            queryReportCase.getJxTable().setValueAt(false, row, 0);
          }
          reportTabsPane.getDateTimePickerFrom().setDate(new Date());
          reportTabsPane.getDateTimePickerTo().setDate(reportTabsPane.getDateTimePickerFrom().getDate());

          reportTabsPane.getCollapseBtnDesign().setText("Collapse all");
          viewButtons(false);
          reportTabsPane.getDelBtnDesign().setEnabled(false);

          File folder = new File(folderPath);
          if (!delWithReport) {
            if (folder.isDirectory()) {
              File[] files = folder.listFiles();
              if (files != null) {
                for (File file : files) {
                  if (!file.getName().endsWith(".pdf")) {
                    if (!file.isDirectory()) {
                      file.delete();
                    } else {
                      deleteDirectory(file);
                      file.delete();
                    }
                  }
                }
              }
            }
          } else {
            deleteDirectory(folder);
            folder.delete();
            int rowCount = reportTabsPane.getSavedReportCase().getJxTable().getRowCount();
            String formattedDateForDir = reportPdfPath.getDateTimeFolder();
            LocalDateTime dateTime = LocalDateTime.parse(formattedDateForDir, reportHelper.getDateTimeFormatterFused());
            int delRow = -1;
            int row = 0;
            while (delRow == -1 && row < rowCount) {
              String value = (String) reportTabsPane.getSavedReportCase().getJxTable().getValueAt(row, 0);
              if (value.contains(dateTime.format(reportHelper.getDateTimeFormatter()))) {
                delRow = row;
              }
              row++;
            }
            if (delRow != -1) {
              reportTabsPane.getSavedReportCase().getDefaultTableModel().removeRow(delRow);
              reportTabsPane.getSavedReportCase().getJxTable().clearSelection();
              reportTabsPane.getScrollPanePDF().getViewport().removeAll();
              reportTabsPane.getScrollPanePDF().getViewport().revalidate();

            }
          }
        }
      }
    }
  }

  private void deleteDirectory(File f) {
    for (File file : f.listFiles()) {
      if (file.isDirectory()) {
        deleteDirectory(file);
      }
      file.delete();
    }
  }

  private void createdReport() {

    String folderName = reportPdfPath.getDirDesignName();
    String formattedDateForDir = reportPdfPath.getDateTimeFolder();

    int profileId;
    int taskId;
    int queryId;
    String profileName = "";
    String taskName = "";
    String queryName = "";
    String dateFrom = "";
    String dateTo = "";

    Map<String, Object> dataReport = new HashMap<>();
    int pageCount = 0;
    for (int tabIndex = 0; tabIndex < reportTabsPane.getJTabbedPaneChart().getTabCount(); tabIndex++) {
      Component component = reportTabsPane.getJTabbedPaneChart().getComponentAt(tabIndex);
      if (component instanceof JScrollPane scrollPane) {
        Component viewComponent = scrollPane.getViewport().getView();
        Container container = (Container) viewComponent;
        Component[] cards = container.getComponents();
        for (Component c : cards) {
          if (c instanceof ChartCardPanel cardChart) {
            ProfileTaskQueryKey profileTaskQueryKey = cardChart.getKey();
            profileId = profileTaskQueryKey.getProfileId();
            profileName = profileManager.getProfileInfoById(profileId).getName();

            taskId = profileTaskQueryKey.getTaskId();
            taskName = profileManager.getTaskInfoById(taskId).getName();

            queryId = profileTaskQueryKey.getQueryId();
            queryName = profileManager.getQueryInfoById(queryId).getName();

            ChartInfo chartInfo = profileManager.getChartInfoById(queryId);

            dateFrom = reportHelper.getDateFormat(chartInfo.getCustomBegin());
            dateTo = reportHelper.getDateFormat(chartInfo.getCustomEnd());

            String fileName = cardChart.getMetric().getName().trim().replace(" ", "_").toLowerCase();
            String description = cardChart.getJtaDescription().getText();
            String nameFunction = "";
            for (AbstractButton button : Collections.list(cardChart.getFunctionPanel().getButtonGroup().getElements())) {
              if (button.isSelected()) {
                nameFunction = " FUNCTION: " + button.getText();
              }
            }

            if (cardChart.getJSplitPane().getTopComponent() instanceof HistorySCP chartPanel) {
              String designDir = filesHelper.getDesignDir() + filesHelper.getFileSeparator() + folderName
                  + filesHelper.getFileSeparator() + "profileId_" + profileId
                  + "_taskId_" + taskId
                  + "_queryId_" + queryId;

              /**
               *     todo implement relative path
               *    String designDir2 = "profileId_" + profileId
               *                               + "_taskId_" + taskId
               *                                + "_queryId_" + queryId;
               */

              String filePath = designDir + filesHelper.getFileSeparator() + fileName + ".png";

              BufferedImage image = new BufferedImage(cardChart.getJSplitPane().getWidth(),
                                                      cardChart.getJSplitPane()
                                                          .getHeight(), BufferedImage.TYPE_INT_ARGB);
              Graphics2D g2d = image.createGraphics();
              cardChart.getJSplitPane().printAll(g2d);
              g2d.dispose();
              try {
                Files.createDirectories(Paths.get(designDir));
                File outputFile = new File(filePath);
                ImageIO.write(image, "png", outputFile);
                String pathPNG = designDir + filesHelper.getFileSeparator() + fileName + ".png";
                dataReport.put("profileName", profileName);
                dataReport.put("taskName", taskName);
                dataReport.put("queryName", queryName);
                dataReport.put("dateFrom", dateFrom);
                dataReport.put("dateTo", dateTo);
                dataReport.put("nameCard", cardChart.getJlTitle().getText().replace("html", "p"));
                dataReport.put("nameFunction", nameFunction);
                dataReport.put("description", description);
                dataReport.put("pathChart", pathPNG);

              } catch (IOException ex) {
                throw new RuntimeException(ex);
              }
            }
          }

          try {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
            String fileNameTemplate = "default.ftl";
            Path path = filesHelper.getFilePathTemplate("default.ftl");
            cfg.setDirectoryForTemplateLoading(path.toFile());
            cfg.setDefaultEncoding("UTF-8");

            StringWriter stringWriter = new StringWriter();
            Template template = cfg.getTemplate(fileNameTemplate);
            template.process(dataReport, stringWriter);
            String html = stringWriter.toString();

            String designDir = filesHelper.getDesignDir() + filesHelper.getFileSeparator() + folderName;
            String templateFileName = String.format("template_%d_%d_%s.html", tabIndex, pageCount++, formattedDateForDir);
            String filePath = designDir + filesHelper.getFileSeparator() + templateFileName;

            File file = new File(filePath);
            FileWriter fw = new FileWriter(file);
            BufferedWriter bf = new BufferedWriter(fw);
            bf.write(html);
            bf.close();

          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        }
      }
    }
    String designDir = filesHelper.getDesignDir() + filesHelper.getFileSeparator() + folderName;
    String reportFileName = String.format("report_%s.pdf", formattedDateForDir);
    String fileReportPath = designDir + filesHelper.getFileSeparator() + reportFileName;

    try {
      String font = "arialuni.ttf";
      String pathDir = filesHelper.getFilePathFont(font);

      Document document = new Document(PageSize.A4);

      PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fileReportPath));

      document.open();

      File folder = new File(filesHelper.getDesignDir() + filesHelper.getFileSeparator() + folderName);
      if (folder.isDirectory()) {
        File[] files = folder.listFiles();
        if (files != null) {
          for (File htmlFile : files) {
            if (htmlFile.isFile() && htmlFile.getName().endsWith(".html")) {
              String filePathHtml = htmlFile.getAbsolutePath();
              String contentHtml = new String(Files.readAllBytes(Paths.get(filePathHtml)));

              InputStream inf = new ByteArrayInputStream(contentHtml.getBytes("UTF-8"));

              XMLWorkerFontProvider fontProvider = new XMLWorkerFontProvider(XMLWorkerFontProvider.DONTLOOKFORFONTS);
              fontProvider.register(pathDir);

              FontFactory.setFontImp(fontProvider);
              XMLWorkerHelper.getInstance().parseXHtml(writer, document, inf, null, null, fontProvider);
              document.newPage();
            }
          }
        }
      }
      document.close();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    try {
      PdfViewer pdfViewer = new PdfViewer(new File(fileReportPath));
      reportTabsPane.getScrollPanePDF().setViewportView(pdfViewer);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    this.reportTabsPane.getSavedReportCase().getDefaultTableModel().getDataVector().removeAllElements();
    this.reportTabsPane.getSavedReportCase().getDefaultTableModel().fireTableDataChanged();

    updateSavedReportCase();

  }

  private void savedDesign() {
    Map<String, QueryReportData> mapKeyString = createMapKeyString();

    LocalDateTime nowDateTime = LocalDateTime.now();
    String formattedDateForDir = nowDateTime.format(reportHelper.getDateTimeFormatterFused());

    reportManager.addConfig(mapKeyString, formattedDateForDir);
    reportPdfPath.setDirDesignName("design_" + formattedDateForDir);

    updateDesignReportCase();

    reportTabsPane.getDesignSaveInfo()
        .setText("Saved design: Design- " + nowDateTime.format(reportHelper.getDateTimeFormatter()));
    reportTabsPane.getDesignSaveInfo().setForeground(new Color(0x0F850F));

    JOptionPane.showMessageDialog(new JDialog(),
                                  "Design - " + nowDateTime.format(reportHelper.getDateTimeFormatter())
                                      + " has been saved",
                                  "Information", JOptionPane.INFORMATION_MESSAGE);

    reportTabsPane.repaint();
    reportTabsPane.revalidate();
  }

  private void updatedDesign() {
    Map<String, QueryReportData> mapKeyString = createMapKeyString();

    String dirName = reportPdfPath.getDirDesignName();
    String fileJson = dirName + filesHelper.getFileSeparator() + "design.json";
    LocalDateTime dateTime = LocalDateTime.parse(reportPdfPath.getDateTimeFolder(), reportHelper.getDateTimeFormatterFused());
    String folderDate = dateTime.format(reportHelper.getDateTimeFormatter());

    try {
      reportManager.deleteDesign(fileJson);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    reportManager.addConfig(mapKeyString, reportPdfPath.getDateTimeFolder());

    JOptionPane.showMessageDialog(new JDialog(),
                                  "Design - " + folderDate + " has been updated",
                                  "Information", JOptionPane.INFORMATION_MESSAGE);

    reportTabsPane.repaint();
    reportTabsPane.revalidate();
  }

  private Map<String, QueryReportData> createMapKeyString() {
    Map<String, QueryReportData> mapKeyString = new HashMap<>();

    saveDescriptions();

    mapReportData.forEach((key, value) -> {
      {
        ChartInfo chartInfo = profileManager.getChartInfoById(key.getQueryId());
        mapKeyString
            .put(key.getProfileId()
                     + "_" + key.getTaskId()
                     + "_" + key.getQueryId()
                     + "_" + reportHelper.getDateFormatFused(chartInfo.getCustomBegin())
                     + "_" + reportHelper.getDateFormatFused(chartInfo.getCustomEnd()),
                 value);
      }
    });
    return mapKeyString;
  }

  private void updateSavedReportCase() {
    designReportDirs = new ArrayList<>();

    File designFolder = new File(filesHelper.getDesignDir());
    if (designFolder.exists() && designFolder.isDirectory()) {
      File[] folders = designFolder.listFiles(File::isDirectory);
      for (File folder : folders) {
        File[] files = folder.listFiles();
        for (File file : files) {
          if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
            designReportDirs.add(folder);
          }
        }
      }

      Collections.reverse(designReportDirs);

      this.reportTabsPane.getSavedReportCase().getDefaultTableModel().getDataVector().removeAllElements();
      this.reportTabsPane.getSavedReportCase().getDefaultTableModel().fireTableDataChanged();
      if (designReportDirs != null) {
        for (File folder : designReportDirs) {
          if (folder.listFiles() != null && folder.listFiles().length != 0) {
            String dateStr = folder.getName().substring(folder.getName().indexOf("_") + 1);
            LocalDateTime dateTime = LocalDateTime.parse(dateStr, reportHelper.getDateTimeFormatterFused());
            String reportName = "Report - " + dateTime.format(reportHelper.getDateTimeFormatter());
            reportTabsPane.getSavedReportCase().getDefaultTableModel()
                .addRow(new Object[]{reportName});

          }
        }
      }
    }
  }

  private void updateDesignReportCase() {
    designSaveDirs = new ArrayList<>();

    String designName = "";
    File designFolder = new File(filesHelper.getDesignDir());
    if (designFolder.exists() && designFolder.isDirectory()) {
      File[] folders = designFolder.listFiles(File::isDirectory);
      for (File folder : folders) {
        File[] files = folder.listFiles();
        if (folder.listFiles().length != 0) {
          for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".json")) {
              designSaveDirs.add(folder);
            }
          }
        } else {
          folder.delete();
        }
      }

      Collections.reverse(designSaveDirs);

      this.reportTabsPane.getDesignReportCase().getDefaultTableModel().getDataVector().removeAllElements();
      this.reportTabsPane.getDesignReportCase().getDefaultTableModel().fireTableDataChanged();
      if (designSaveDirs != null) {
        for (File folder : designSaveDirs) {
          if (folder.listFiles() != null && folder.listFiles().length != 0) {
            String dateStr = folder.getName().substring(folder.getName().indexOf("_") + 1);
            LocalDateTime dateTime = LocalDateTime.parse(dateStr, reportHelper.getDateTimeFormatterFused());
            designName = "Design - " + dateTime.format(reportHelper.getDateTimeFormatter());
            this.reportTabsPane.getDesignReportCase().getDefaultTableModel()
                .addRow(new Object[]{designName});
          }
        }
      }
    }
  }

  private void viewButtons(boolean isEnabled) {
    reportTabsPane.getSaveBtnDesign().setEnabled(isEnabled);
    reportTabsPane.getClearBtnDesign().setEnabled(isEnabled);
    reportTabsPane.getGenerateReportBtnDesign().setEnabled(isEnabled);
  }

  private String setTitleCard(String profileName,
                              String taskName,
                              String queryName) {
    String title = "";
    if (!queryName.equals("")) {
      title = "<html><b>Profile:</b> " + profileName + " <br>"
          + "  <b>Task:</b> " + taskName + " <br>"
          + "  <b>Query:</b> " + queryName + " </html>";
    } else {
      title = "<html><b>Profile:</b> " + profileName + "<br>"
          + "  <b>Task:</b> " + taskName + "</html>";
    }

    return title;
  }

  private void highlightRowsTables(ProfileTaskQueryKey profileTaskQueryKey) {
    int profileId = profileTaskQueryKey.getProfileId();
    int taskId = profileTaskQueryKey.getTaskId();
    int queryId = profileTaskQueryKey.getQueryId();

    QueryReportData queryReportData = mapReportData.get(profileTaskQueryKey);

    int profileRow = 0;
    boolean flag = true;
    while (profileRow < profileReportCase.getJxTable().getRowCount() && flag) {
      int profileIdRow = (int) profileReportCase.getDefaultTableModel()
          .getValueAt(profileRow, profileReportCase.getDefaultTableModel()
              .findColumn(ProfileColumnNames.ID.getColName()));
      if (profileIdRow == profileId) {
        profileReportCase.getJxTable().setRowSelectionInterval(profileRow, profileRow);
        flag = false;
      } else {
        profileRow++;
      }
    }

    int taskRow = 0;
    flag = true;
    while (taskRow < taskReportCase.getJxTable().getRowCount() && flag) {
      int taskIdRow = (int) taskReportCase.getDefaultTableModel()
          .getValueAt(taskRow, taskReportCase.getDefaultTableModel()
              .findColumn(TaskColumnNames.ID.getColName()));
      if (taskIdRow == taskId) {
        taskReportCase.getJxTable().setRowSelectionInterval(taskRow, taskRow);
        flag = false;
      } else {
        taskRow++;
      }
    }
    int queryRow = 0;
    flag = true;
    while (queryRow < queryReportCase.getJxTable().getRowCount() && flag) {
      int queryIdRow = (int) queryReportCase.getDefaultTableModel()
          .getValueAt(queryRow, queryReportCase.getDefaultTableModel()
              .findColumn(QueryColumnNames.ID.getColName()));
      if (queryIdRow == queryId) {
        queryReportCase.getJxTable().setRowSelectionInterval(queryRow, queryRow);
        if (queryReportData.getMetricReportList().size() != 0 || queryReportData.getCProfileReportList().size() != 0) {
          queryReportCase.getJxTable().setValueAt(true, queryRow, 0);
          fillMetricColumnCard(profileTaskQueryKey);
        }
        flag = false;
      } else {
        queryRow++;
      }
    }

    containerChartCardDesign.repaint();
    containerChartCardDesign.revalidate();
  }

  private void fillMetricColumnCard(ProfileTaskQueryKey profileTaskQueryKey) {
    Component[] components = containerCardDesign.getComponents();

    for (Component component : components) {
      if (component instanceof MetricColumnPanel cardInfo) {

        int queryId = profileTaskQueryKey.getQueryId();
        String queryName = profileManager.getQueryInfoById(queryId).getName();

        if (cardInfo.getTitle().equals(queryName)) {
          List<MetricReport> metricReportList = mapReportData.get(profileTaskQueryKey).getMetricReportList();
          List<CProfileReport> cProfileReportList = mapReportData.get(profileTaskQueryKey).getCProfileReportList();

          if (metricReportList.size() != 0) {
            for (MetricReport m : metricReportList) {
              for (int row = 0; row < cardInfo.getJtcMetric().getJxTable().getRowCount(); row++) {
                int metricId = (int) cardInfo.getJtcMetric().getDefaultTableModel()
                    .getValueAt(row, cardInfo.getJtcMetric().getDefaultTableModel()
                        .findColumn(MetricsColumnNames.ID.getColName()));
                if (m.getId() == metricId) {
                  cardInfo.getJtcMetric().getJxTable().setValueAt(true, row, 0);
                }
              }
            }
          }

          if (cProfileReportList.size() != 0) {
            for (CProfileReport c : cProfileReportList) {
              for (int row = 0; row < cardInfo.getJtcColumn().getJxTable().getRowCount(); row++) {
                int columId = (int) cardInfo.getJtcColumn().getDefaultTableModel()
                    .getValueAt(row, cardInfo.getJtcMetric().getDefaultTableModel()
                        .findColumn(MetricsColumnNames.ID.getColName()));
                if (c.getColId() == columId) {
                  cardInfo.getJtcColumn().getJxTable().setValueAt(true, row, 0);
                }
              }
            }
          }

          if (mapReportData.get(profileTaskQueryKey).getCProfileReportList().isEmpty()
              && mapReportData.get(profileTaskQueryKey).getMetricReportList().isEmpty()) {
            containerCardDesign.remove(cardInfo);
            containerCardDesign.revalidate();
            containerCardDesign.repaint();
          }
        }
      }
    }

  }

  public boolean isFolderEmpty(String folderPath) {
    File folder = new File(folderPath);

    if (folder.isDirectory()) {
      String[] files = folder.list();
      return (files == null || files.length == 0);
    }
    return true;
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();

    if (!e.getValueIsAdjusting()) {
      if (listSelectionModel.isSelectionEmpty()) {
        log.info("Clearing profile fields");
      } else {

        if (e.getSource() == reportTabsPane.getDesignReportCase().getJxTable().getSelectionModel()
            && reportTabsPane.getDesignReportCase().getJxTable().getSelectedRow() != savedRow) {
          selectedRow = reportTabsPane.getDesignReportCase().getJxTable().getSelectedRow();

          if (!reportHelper.isMadeChanges()) {

            String designDir = GUIHelper.getNameByColumnName(reportTabsPane.getDesignReportCase().getJxTable(),
                                                             reportTabsPane.getDesignReportCase()
                                                                 .getDefaultTableModel(),
                                                             listSelectionModel, "Design name");
            String dateStr = designDir.substring(designDir.indexOf("-") + 1).trim();
            LocalDateTime dateTime = LocalDateTime.parse(dateStr, reportHelper.getDateTimeFormatter());
            String folderDate = dateTime.format(reportHelper.getDateTimeFormatterFused());
            String folderName = "design_" + folderDate;

            reportPdfPath.setDirDesignName(folderName);
            reportTabsPane.getDesignSaveInfo().setText("Saved design: " + designDir);
            reportTabsPane.getDesignSaveInfo().setForeground(new Color(0x0A8D0A));
            savedRow = reportTabsPane.getDesignReportCase().getJxTable().getSelectedRow();

            Map<String, QueryReportData> mapDesign = reportManager.getConfig(folderName, "design");
            if (!mapDesign.isEmpty()) {
              containerCardDesign.removeAll();
              reportTabsPane.getJTabbedPaneChart().removeAll();

              for (Map.Entry<String, QueryReportData> entry : mapDesign.entrySet()) {
                String[] stringKey = entry.getKey().split("_");

                int profileId = Integer.parseInt(stringKey[0]);
                String profileName = profileManager.getProfileInfoById(profileId).getName();
                int taskId = Integer.parseInt(stringKey[1]);
                String taskName = profileManager.getTaskInfoById(taskId).getName();
                int queryId = Integer.parseInt(stringKey[2]);
                String queryName = profileManager.getQueryInfoById(queryId).getName();
                QueryInfo queryInfo = profileManager.getQueryInfoById(queryId);

                String dateFrom = stringKey[3];
                LocalDateTime begin = LocalDateTime.parse(dateFrom, reportHelper.getDateTimeFormatterFused());
                Date dateBegin = Date.from(begin.atZone(ZoneId.systemDefault()).toInstant());

                String dateTo = stringKey[4];
                LocalDateTime end = LocalDateTime.parse(dateTo, reportHelper.getDateTimeFormatterFused());
                Date dateEnd = Date.from(end.atZone(ZoneId.systemDefault()).toInstant());

                ProfileTaskQueryKey key = new ProfileTaskQueryKey(profileId, taskId, queryId);

                List<MetricReport> metricReportList = entry.getValue().getMetricReportList();
                List<CProfileReport> cProfileReportList = entry.getValue().getCProfileReportList();
                mapReportData.put(key, new QueryReportData(cProfileReportList, metricReportList));

                ChartInfo chartInfo = profileManager.getChartInfoById(queryInfo.getId());
                if (Objects.isNull(chartInfo)) {
                  throw new NotFoundException(String.format("Chart info with id=%s not found",
                                                            queryInfo.getId()));
                }

                chartInfo.setRangeHistory(RangeHistory.CUSTOM);

                reportTabsPane.getDateTimePickerFrom().setDate(dateBegin);
                reportTabsPane.getDateTimePickerTo().setDate(dateEnd);

                JPanel containerChartCardDesign = new JPanel(new GridLayout(
                    metricReportList.size() + cProfileReportList.size(), 1, 5, 5));
                if (metricReportList.size() != 0 || cProfileReportList.size() != 0) {
                  String title = setTitleCard(profileName, taskName, queryName);

                  JScrollPane jScrollPane = new JScrollPane();
                  GUIHelper.setScrolling(jScrollPane);

                  reportTabsPane.getJTabbedPaneChart().add(title, jScrollPane);
                  int index = reportTabsPane.getJTabbedPaneChart().indexOfTab(title);
                  reportTabsPane.getJTabbedPaneChart().setSelectedIndex(index);

                  containerChartCardDesign.setToolTipText(setTitleCard(profileName, taskName, queryName));
                  jScrollPane.setViewportView(containerChartCardDesign);

                  chartInfo.setCustomBegin(begin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                  chartInfo.setCustomEnd(end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

                  if (metricReportList.size() != 0) {
                    for (MetricReport m : metricReportList) {

                      Metric metric = getMetricByMetricReport(m);

                      ChartCardPanel cardChart =
                          new ChartCardPanel(m.getId(), chartInfo, key, SourceConfig.METRICS,
                                             metric, profileManager, eventListener, fStore, reportHelper, mapReportData);

                      cardChart.getFunctionPanel().getCount().setEnabled(false);
                      cardChart.getFunctionPanel().getSum().setEnabled(false);
                      cardChart.getFunctionPanel().getAvg().setEnabled(false);

                      cardChart.setBorder(new EtchedBorder());
                      cardChart.setSelectedRadioButton(m.getGroupFunction());
                      cardChart.getJtaDescription().setText(m.getComment());

                      cardChart.loadChart(m.getId(), chartInfo, key, cardChart, SourceConfig.METRICS);
                      containerChartCardDesign.add(cardChart);
                      containerChartCardDesign.repaint();
                      containerChartCardDesign.revalidate();
                    }
                  }
                  if (cProfileReportList.size() != 0) {
                    for (CProfileReport cProfileReport : cProfileReportList) {

                      TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());

                      Metric metric = getMetricByCProfileReport(key, cProfileReport, tableInfo);

                      ChartCardPanel cardChart = new ChartCardPanel(cProfileReport.getColId(), chartInfo, key,
                                                                    SourceConfig.COLUMNS, metric, profileManager, eventListener, fStore, reportHelper, mapReportData);

                      cardChart.setBorder(new EtchedBorder());
                      cardChart.setSelectedRadioButton(metric.getGroupFunction());
                      cardChart.getJtaDescription().setText(cProfileReport.getComment());

                      cardChart.loadChart(cProfileReport.getColId(), chartInfo, key, cardChart, SourceConfig.COLUMNS);
                      containerChartCardDesign.add(cardChart);
                      containerChartCardDesign.repaint();
                      containerChartCardDesign.revalidate();
                    }
                  }
                  viewButtons(true);

                  reportTabsPane.getCollapseBtnDesign().setVisible(true);
                  viewCardMetricColumns(key);
                  queryReportCase.getJxTable().clearSelection();
                  highlightRowsTables(key);
                  reportPdfPath.setDirDesignName(folderName);
                }
              }
            }
          } else {
            int input = JOptionPane.showOptionDialog(new JDialog(),
                                                     "Changes have been made to the current design , re - update it or cancel it?",
                                                     "Information", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            reportTabsPane.getDesignReportCase().getJxTable().clearSelection();

            if (input == 0) {
              updatedDesign();
              savedRow = previousSelectedRow;
              reportHelper.setMadeChanges(false);
              reportTabsPane.getDesignReportCase().getJxTable()
                  .setRowSelectionInterval(previousSelectedRow, previousSelectedRow);
            } else if (input == 1) {
              savedDesign();
              savedRow = 0;
              reportHelper.setMadeChanges(false);
              reportTabsPane.getDesignReportCase().getJxTable().setRowSelectionInterval(0, 0);
            } else if (input == 2) {
              savedRow = selectedRow;
              reportHelper.setMadeChanges(false);
              reportTabsPane.getDesignReportCase().getJxTable()
                  .setRowSelectionInterval(previousSelectedRow, previousSelectedRow);
              containerChartCardDesign.repaint();
              containerChartCardDesign.revalidate();
            }
          }
        }
        previousSelectedRow = selectedRow;
        reportTabsPane.getDelBtnDesign().setEnabled(true);
      }
    }
  }

  protected Metric getMetricByMetricReport(MetricReport mr) {
    Metric metric = new Metric();
    metric.setName(mr.getName());
    metric.setXAxis(mr.getXAxis());
    metric.setYAxis(mr.getYAxis());
    metric.setGroup(mr.getGroup());
    metric.setGroupFunction(mr.getGroupFunction());
    metric.setChartType(mr.getChartType());

    return metric;
  }

  protected Metric getMetricByCProfileReport(ProfileTaskQueryKey key,
                                             CProfileReport cProfile,
                                             TableInfo tableInfo) {
    Metric metric = new Metric();
    metric.setName(cProfile.getColName());
    metric.setXAxis(tableInfo.getCProfiles().stream().filter(f -> f.getCsType().isTimeStamp()).findAny().orElseThrow());
    metric.setYAxis(cProfile);
    metric.setGroup(cProfile);

    setGroupFunction(cProfile, metric);

    if (mapReportData != null && mapReportData.get(key) != null) {
      Optional<CProfileReport> value = mapReportData.get(key)
          .getCProfileReportList()
          .stream()
          .filter(f -> f.getColId() == cProfile.getColId())
          .findAny();

      if (value.isPresent()) {
        if (value.get().getGroupFunction() != null && value.get().getChartType() != null) {
          metric.setGroupFunction(value.get().getGroupFunction());
          metric.setChartType(value.get().getChartType());
        } else {
          setGroupFunction(cProfile, metric);
        }
      } else {
        setGroupFunction(cProfile, metric);
      }
    }

    return metric;
  }

  private void setGroupFunction(CProfileReport cProfile,
                                 Metric metric) {
    if (CType.STRING.equals(cProfile.getCsType().getCType())) {
      metric.setGroupFunction(GroupFunction.COUNT);
      metric.setChartType(STACKED);
    } else {
      if (Arrays.stream(TimestampType.values()).anyMatch((t) -> t.name().equals(cProfile.getColDbTypeName()))) {
        metric.setGroupFunction(GroupFunction.COUNT);
        metric.setChartType(STACKED);
      } else {
        metric.setGroupFunction(GroupFunction.AVG);
        metric.setChartType(LINEAR);
      }
    }
  }

  private void saveDescriptions() {
    for (int tabIndex = 0; tabIndex < reportTabsPane.getJTabbedPaneChart().getTabCount(); tabIndex++) {
      Component component = reportTabsPane.getJTabbedPaneChart().getComponentAt(tabIndex);
      if (component instanceof JScrollPane scrollPane) {
        Component viewComponent = scrollPane.getViewport().getView();
        Container container = (Container) viewComponent;
        Component[] cards = container.getComponents();
        for (Component card : cards) {
          if (card instanceof ChartCardPanel cardChart) {
            ProfileTaskQueryKey profileTaskQueryKey = cardChart.getKey();
            QueryReportData value = mapReportData.get(profileTaskQueryKey);
            String nameMetricOrCol = cardChart.getJlTitle().getText().split("</b>")[1].replace("</html>", "");
            List<CProfileReport> cList = value.getCProfileReportList();
            List<MetricReport> mList = value.getMetricReportList();
            if (cardChart.getJlTitle().getText().contains("<b>Column: </b>")) {
              for (CProfileReport c : cList) {
                if (c.getColName().equalsIgnoreCase(nameMetricOrCol)) {
                  c.setComment(cardChart.getJtaDescription().getText());
                }
              }
            }
            if (cardChart.getJlTitle().getText().contains("<b>Metric: </b>")) {
              for (MetricReport m : mList) {
                if (m.getName().equalsIgnoreCase(nameMetricOrCol)) {
                  m.setComment(cardChart.getJtaDescription().getText());
                }
              }
            }
            mapReportData.putIfAbsent(profileTaskQueryKey, new QueryReportData(cList, mList));
          }
        }
      }
    }
  }
}


