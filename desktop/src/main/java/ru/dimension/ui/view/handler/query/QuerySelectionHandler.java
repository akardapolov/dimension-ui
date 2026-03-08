package ru.dimension.ui.view.handler.query;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.table.IType;
import ru.dimension.db.model.profile.table.TType;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.view.handler.CommonViewHandler;
import ru.dimension.ui.view.handler.core.AbstractTableSelectionHandler;
import ru.dimension.ui.view.handler.core.ConfigSelectionContext;
import ru.dimension.ui.view.handler.core.RelatedHighlightService;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.query.MainQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetadataQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetricQueryPanel;
import ru.dimension.ui.view.panel.config.query.QueryPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.table.row.Rows.MetadataRow;
import ru.dimension.ui.view.table.row.Rows.QueryRow;

@Log4j2
@Singleton
public final class QuerySelectionHandler extends AbstractTableSelectionHandler<QueryRow>
    implements ItemListener, CommonViewHandler {

  private final ProfileManager profileManager;

  private final JXTableCase queryCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase profileCase;

  private final MainQueryPanel mainQueryPanel;
  private final MetadataQueryPanel metadataQueryPanel;
  private final MetricQueryPanel metricQueryPanel;
  private final ConfigTab configTab;
  private final QueryPanel queryPanel;
  private final JXTableCase configMetadataCase;
  private final ButtonPanel queryButtonPanel;
  private final JCheckBox checkboxConfig;

  private final ConfigSelectionContext selectionContext;
  private final RelatedHighlightService highlightService;

  private boolean isSelected;
  private final ResourceBundle bundleDefault;

  @Inject
  public QuerySelectionHandler(@Named("profileManager") ProfileManager profileManager,
                               @Named("configTab") ConfigTab configTab,
                               @Named("queryConfigPanel") QueryPanel queryPanel,
                               @Named("queryConfigCase") JXTableCase queryCase,
                               @Named("taskConfigCase") JXTableCase taskCase,
                               @Named("connectionConfigCase") JXTableCase connectionCase,
                               @Named("profileConfigCase") JXTableCase profileCase,
                               @Named("configMetadataCase") JXTableCase configMetadataCase,
                               @Named("queryButtonPanel") ButtonPanel queryButtonPanel,
                               @Named("checkboxConfig") JCheckBox checkboxConfig,
                               @Named("mainQueryPanel") MainQueryPanel mainQueryPanel,
                               @Named("metadataQueryPanel") MetadataQueryPanel metadataQueryPanel,
                               @Named("metricQueryPanel") MetricQueryPanel metricQueryPanel,
                               @Named("configSelectionContext") ConfigSelectionContext selectionContext,
                               @Named("relatedHighlightService") RelatedHighlightService highlightService) {
    super(queryCase);
    this.profileManager = profileManager;
    this.queryCase = queryCase;
    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;

    this.mainQueryPanel = mainQueryPanel;
    this.metadataQueryPanel = metadataQueryPanel;
    this.metricQueryPanel = metricQueryPanel;

    this.configTab = configTab;
    this.queryPanel = queryPanel;
    this.configMetadataCase = configMetadataCase;

    this.queryButtonPanel = queryButtonPanel;
    this.checkboxConfig = checkboxConfig;
    this.selectionContext = selectionContext;
    this.highlightService = highlightService;

    this.checkboxConfig.addItemListener(this);
    this.isSelected = false;

    this.bundleDefault = Internationalization.getInternationalizationBundle();

    bind();
  }

  @Override
  protected void onSelection(Optional<QueryRow> item) {
    Integer queryId = item.map(QueryRow::getId).orElse(null);
    selectionContext.setSelectedQueryId(queryId);

    if (queryId == null) {
      clearAll();
      applyCheckboxState();
      if (!checkboxConfig.isSelected()) {
        highlightService.highlightFromQuery(null);
      }
      return;
    }

    QueryInfo queryInfo = profileManager.getQueryInfoById(queryId);
    if (queryInfo == null) {
      throw new NotFoundException("Not found query: " + queryId);
    }

    mainQueryPanel.getQueryName().setText(queryInfo.getName());
    mainQueryPanel.getQueryDescription().setText(queryInfo.getDescription());
    mainQueryPanel.getQueryGatherDataComboBox().setSelectedItem(queryInfo.getGatherDataMode());
    mainQueryPanel.getQuerySqlText().setText(queryInfo.getText());

    TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());
    if (tableInfo == null) {
      throw new NotFoundException("Not found table: " + queryInfo.getName());
    }

    metadataQueryPanel.getTableName().setText(queryInfo.getName());

    fillMetadataAndMetrics(queryId, queryInfo, tableInfo);
    applyCheckboxState();
    applyMetadataAvailability(queryInfo.getId());

    if (!checkboxConfig.isSelected()) {
      highlightService.highlightFromQuery(queryId);
    }
  }

  private void clearAll() {
    mainQueryPanel.getQueryName().setEditable(false);
    mainQueryPanel.getQueryDescription().setEditable(false);
    mainQueryPanel.getQuerySqlText().setEditable(false);
    mainQueryPanel.getQueryGatherDataComboBox().setEnabled(false);

    mainQueryPanel.getQueryName().setText("");
    mainQueryPanel.getQueryName().setPrompt(bundleDefault.getString("qName"));
    mainQueryPanel.getQueryDescription().setText("");
    mainQueryPanel.getQueryDescription().setPrompt(bundleDefault.getString("qDesc"));
    mainQueryPanel.getQuerySqlText().setText("");

    metadataQueryPanel.getTableName().setText("");
    metadataQueryPanel.getTableType().setSelectedItem(TType.TIME_SERIES);
    metadataQueryPanel.getTableIndex().setSelectedItem(IType.LOCAL);
    metadataQueryPanel.getCompression().setSelected(true);

    List<List<?>> timestampList = new LinkedList<>();
    timestampList.add(new ArrayList<>(Arrays.asList(" ", " ")));
    metadataQueryPanel.getTimestampComboBox().setTableData(timestampList);
    metadataQueryPanel.getQueryConnectionMetadataComboBox().setTableData(new LinkedList<>());

    metadataQueryPanel.getEditMetadata().setEnabled(false);
    metadataQueryPanel.getSaveMetadata().setEnabled(false);
    metadataQueryPanel.getConfigMetadataCase().getJxTable().setEditable(false);

    configMetadataCase.clearTable();

    metricQueryPanel.getConfigMetricCase().getDefaultTableModel().getDataVector().removeAllElements();
    metricQueryPanel.getConfigMetricCase().getDefaultTableModel().fireTableDataChanged();
    metricQueryPanel.getNameMetric().setText("");
    metricQueryPanel.getNameMetric().setPrompt(bundleDefault.getString("metricName"));
    metricQueryPanel.getDefaultCheckBox().setSelected(false);
  }

  private void fillMetadataAndMetrics(int queryId, QueryInfo queryInfo, TableInfo tableInfo) {
    List<CProfile> cProfiles = tableInfo.getCProfiles();

    TTTable<MetadataRow, JXTable> ttMeta = configMetadataCase.getTypedTable();
    ttMeta.setItems(Collections.emptyList());

    List<String> yAxisList = new ArrayList<>();

    if (cProfiles != null) {
      List<MetadataRow> metadataRows = cProfiles.stream()
          .filter(f -> !f.getCsType().isTimeStamp())
          .map(cProfile -> {
            boolean isDimension = tableInfo.getDimensionColumnList() != null &&
                tableInfo.getDimensionColumnList().contains(cProfile.getColName());
            return new MetadataRow(cProfile, isDimension);
          })
          .collect(Collectors.toList());

      ttMeta.setItems(metadataRows);

      fillConfigMetadata(tableInfo, configMetadataCase);

      cProfiles.stream()
          .filter(f -> !f.getCsType().isTimeStamp())
          .forEach(cProfile -> yAxisList.add(cProfile.getColName()));
    }

    DefaultComboBoxModel<String> cbModelY = new DefaultComboBoxModel<>();
    for (String s : yAxisList) {
      cbModelY.addElement(s);
    }
    metricQueryPanel.getYComboBox().setModel(cbModelY);

    DefaultComboBoxModel<String> cbModelDimension = new DefaultComboBoxModel<>();
    for (String s : yAxisList) {
      cbModelDimension.addElement(s);
    }
    metricQueryPanel.getDimensionComboBox().setModel(cbModelDimension);

    List<List<?>> connectionData = new LinkedList<>();
    profileManager.getTaskInfoList().stream()
        .filter(t -> t.getQueryInfoList().stream().anyMatch(qId -> qId == queryId))
        .findAny()
        .ifPresentOrElse(t -> {
          ConnectionInfo ci = profileManager.getConnectionInfoById(t.getConnectionId());
          if (ci == null) {
            throw new NotFoundException("Not found task: " + t.getConnectionId());
          }
          connectionData.add(new ArrayList<>(Arrays.asList(
              ci.getName(), ci.getUserName(), ci.getUrl(), ci.getJar(), ci.getDriver(), ci.getType()
          )));
        }, () -> log.info("Not found query by query id: {}", queryId));

    metadataQueryPanel.getQueryConnectionMetadataComboBox().setTableData(connectionData);

    metadataQueryPanel.getTableType().setSelectedItem(tableInfo.getTableType() != null ? tableInfo.getTableType() : TType.TIME_SERIES);
    metadataQueryPanel.getTableIndex().setSelectedItem(tableInfo.getIndexType() != null ? tableInfo.getIndexType() : IType.LOCAL);

    metadataQueryPanel.getCompression().setEnabled(Boolean.FALSE);
    metadataQueryPanel.getCompression().setSelected(Boolean.TRUE.equals(tableInfo.getCompression()));

    List<List<?>> timestampListAll = new LinkedList<>();
    List<List<?>> timestampList = new LinkedList<>();
    timestampList.add(new ArrayList<>(Arrays.asList(" ", " ")));

    if (cProfiles != null) {
      cProfiles.forEach(cProfile -> timestampListAll.add(
          new ArrayList<>(Arrays.asList(cProfile.getColName(), cProfile.getCsType().getSType(), cProfile.getCsType().isTimeStamp()))
      ));
    }

    timestampListAll.stream().filter(f -> f.get(2).equals(true))
        .forEach(t -> timestampList.set(0, new ArrayList<>(Arrays.asList(t.get(0), t.get(1)))));
    timestampListAll.stream().filter(f -> f.get(2).equals(false))
        .forEach(t -> timestampList.add(new ArrayList<>(Arrays.asList(t.get(0), t.get(1)))));

    metadataQueryPanel.getTimestampComboBox().setTableData(timestampList);
    metricQueryPanel.getXTextFile().setText((String) timestampList.get(0).get(0));

    metricQueryPanel.getConfigMetricCase().getDefaultTableModel().getDataVector().removeAllElements();
    metricQueryPanel.getConfigMetricCase().getDefaultTableModel().fireTableDataChanged();

    for (Metric m : queryInfo.getMetricList()) {
      metricQueryPanel.getConfigMetricCase().getDefaultTableModel()
          .addRow(new Object[]{
              m.getId(),
              m.getName(),
              m.getIsDefault(),
              m.getXAxis().getColName(),
              m.getYAxis().getColName(),
              m.getGroup().getColName(),
              m.getSafeGroupFunction().toString(),
              m.getSafeChartType().toString()
          });
    }

    if (metricQueryPanel.getConfigMetricCase().getJxTable().getRowCount() != 0) {
      metricQueryPanel.getConfigMetricCase().getJxTable().setRowSelectionInterval(0, 0);
    } else {
      metricQueryPanel.getNameMetric().setText("");
      metricQueryPanel.getNameMetric().setPrompt(bundleDefault.getString("metricName"));
    }

    metadataQueryPanel.getEditMetadata().setEnabled(true);
  }

  private void applyMetadataAvailability(int queryId) {
    if (queryCase.getJxTable().getRowCount() <= 0) {
      return;
    }

    boolean usedInAnyTask = profileManager.getTaskInfoList().stream()
        .flatMap(t -> t.getQueryInfoList().stream())
        .distinct()
        .anyMatch(id -> Objects.equals(id, queryId));

    if (usedInAnyTask) {
      metadataQueryPanel.getEditMetadata().setEnabled(!isSelected);
      metadataQueryPanel.getLoadMetadata().setEnabled(!isSelected);
    } else {
      metadataQueryPanel.getEditMetadata().setEnabled(false);
      metadataQueryPanel.getLoadMetadata().setEnabled(false);
    }
  }

  private void applyCheckboxState() {
    GUIHelper.disableButton(queryButtonPanel, !isSelected);

    metadataQueryPanel.getEditMetadata().setEnabled(!isSelected && metadataQueryPanel.getEditMetadata().isEnabled());
    metadataQueryPanel.getLoadMetadata().setEnabled(!isSelected && metadataQueryPanel.getLoadMetadata().isEnabled());

    GUIHelper.disableButton(metricQueryPanel.getMetricQueryButtonPanel(), !isSelected);
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      isSelected = true;
      highlightService.clearAllHighlights();
      GUIHelper.disableButton(queryButtonPanel, false);
      metadataQueryPanel.getEditMetadata().setEnabled(false);
      metadataQueryPanel.getLoadMetadata().setEnabled(false);
      GUIHelper.disableButton(metricQueryPanel.getMetricQueryButtonPanel(), false);
    } else {
      isSelected = false;
      GUIHelper.disableButton(queryButtonPanel, true);
      metadataQueryPanel.getEditMetadata().setEnabled(true);
      metadataQueryPanel.getLoadMetadata().setEnabled(true);
      GUIHelper.disableButton(metricQueryPanel.getMetricQueryButtonPanel(), true);
      highlightService.highlightFromQuery(selectionContext.getSelectedQueryId());
    }
  }
}