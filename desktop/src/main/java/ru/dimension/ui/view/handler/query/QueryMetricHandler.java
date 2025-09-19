package ru.dimension.ui.view.handler.query;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.column.TaskColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.view.panel.config.query.MainQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetricQueryPanel;

@Log4j2
@Singleton
public class QueryMetricHandler implements ListSelectionListener {

  private final ProfileManager profileManager;
  private final MetricQueryPanel metricQueryPanel;
  private final MainQueryPanel mainQueryPanel;

  @Inject
  public QueryMetricHandler(@Named("profileManager") ProfileManager profileManager,
                            @Named("metricQueryPanel") MetricQueryPanel metricQueryPanel,
                            @Named("mainQueryPanel") MainQueryPanel mainQueryPanel) {
    this.profileManager = profileManager;
    this.metricQueryPanel = metricQueryPanel;
    this.mainQueryPanel = mainQueryPanel;
    this.metricQueryPanel.getConfigMetricCase().getJxTable().getSelectionModel().addListSelectionListener(this);
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();
    if (!e.getValueIsAdjusting()) {
      if (listSelectionModel.isSelectionEmpty()) {
        log.info("Clearing profile fields");
      } else {
        int metricId = getSelectedMetricId(listSelectionModel);
        String queryName = mainQueryPanel.getQueryName().getText();
        Metric metric = getMetricById(metricId, queryName);

        metricQueryPanel.getDefaultCheckBox().setEnabled(false);
        metricQueryPanel.getNameMetric().setEditable(false);
        metricQueryPanel.getXTextFile().setEnabled(false);
        metricQueryPanel.getYComboBox().setEnabled(false);
        metricQueryPanel.getDimensionComboBox().setEnabled(false);
        metricQueryPanel.getGroupFunction().setEnabled(false);
        metricQueryPanel.getChartType().setEnabled(false);

        metricQueryPanel.getNameMetric().setText(metric.getName());
        metricQueryPanel.getDefaultCheckBox().setSelected(metric.getIsDefault());
        metricQueryPanel.getXTextFile().setText(metric.getXAxis().getColName());
        metricQueryPanel.getYComboBox().setSelectedItem(metric.getYAxis().getColName());
        metricQueryPanel.getDimensionComboBox().setSelectedItem(metric.getGroup().getColName());
        metricQueryPanel.getGroupFunction().setSelectedItem(metric.getSafeGroupFunction().toString());
        metricQueryPanel.getChartType().setSelectedItem(metric.getSafeChartType().toString());

        GUIHelper.disableButton(metricQueryPanel.getMetricQueryButtonPanel(), true);
      }
    }
  }

  private int getSelectedMetricId(ListSelectionModel listSelectionModel) {
    return GUIHelper.getIdByColumnName(metricQueryPanel.getConfigMetricCase().getJxTable(),
                                       metricQueryPanel.getConfigMetricCase().getDefaultTableModel(),
                                       listSelectionModel, TaskColumnNames.ID.getColName());
  }

  private Metric getMetricById(int metricId,
                               String queryName) {
    List<Metric> metricslist = profileManager.getQueryInfoList()
        .stream()
        .filter(f -> f.getName().equals(queryName))
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found query: " + queryName))
        .getMetricList();

    return metricslist.stream()
        .filter(f -> f.getId() == metricId)
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found query: " + queryName));
  }
}
