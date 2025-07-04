package ru.dimension.ui.view.structure.workspace.handler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.view.handler.LifeCycleStatus;
import ru.dimension.ui.view.structure.workspace.query.DetailsControlPanel;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.db.TimestampType;
import ru.dimension.ui.model.textfield.JTextFieldCase;
import ru.dimension.ui.view.action.RadioButtonActionExecutor;

@Log4j2
public class DetailsControlPanelHandler implements ActionListener {

  private final DetailsControlPanel detailsControlPanel;

  @Getter
  private LifeCycleStatus status;

  private Metric metric;

  public DetailsControlPanelHandler(DetailsControlPanel detailsControlPanel) {
    this.detailsControlPanel = detailsControlPanel;

    this.detailsControlPanel.getSaveButton().addActionListener(this);
    this.detailsControlPanel.getEditButton().addActionListener(this);
    this.detailsControlPanel.getCancelButton().addActionListener(this);

    this.detailsControlPanel.getButtonGroupFunction().getCount().addActionListener(new RadioListenerDetailsUI());
    this.detailsControlPanel.getButtonGroupFunction().getSum().addActionListener(new RadioListenerDetailsUI());
    this.detailsControlPanel.getButtonGroupFunction().getAverage().addActionListener(new RadioListenerDetailsUI());

    this.status = LifeCycleStatus.NONE;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == detailsControlPanel.getSaveButton()) {
      log.info("Save metric button clicked");

      JTextFieldCase jTextFieldCase = GUIHelper.getJTextFieldCase(MetricsColumnNames.METRIC_NAME.getColName());

      int input = JOptionPane.showOptionDialog(null,
                                               jTextFieldCase.getJPanel(), "Create new metric?",
                                               JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                                               new String[]{"Yes", "No"}, "No");

      if (input == 0) {
        saveNewMetric(jTextFieldCase.getJTextField().getText());
      } else if (input == 1) {
        cancelToSaveNewMetric();

        setButtonEnabled(false, true, false);
        setRadioButtonEnabled(false, false, false);
        setComboBoxEnabled(false, false, false);
      }

      setButtonEnabled(false, true, false);
      setRadioButtonEnabled(false, false, false);
      setComboBoxEnabled(false, false, false);

    } else if (e.getSource() == detailsControlPanel.getEditButton()) {
      log.info("Edit button clicked");

      this.status = LifeCycleStatus.EDIT;

      setButtonEnabled(false, false, true);

      if (Arrays.stream(TimestampType.values())
          .anyMatch((t) -> t.name().equals(metric.getYAxis().getColDbTypeName()))) {
        setRadioButtonEnabled(true, true, true);
      } else if (CType.STRING.equals(metric.getYAxis().getCsType().getCType())) {
        setRadioButtonEnabled(true, false, false);
      } else {
        setRadioButtonEnabled(true, true, true);
      }

      setComboBoxEnabled(false, false, false);

    } else if (e.getSource() == detailsControlPanel.getCancelButton()) {
      log.info("Cancel button clicked");

      cancelToSaveNewMetric();

      setButtonEnabled(false, true, false);
      setRadioButtonEnabled(false, false, false);
      setComboBoxEnabled(false, false, false);
    }
  }

  public void clearAll() {
    setYAxis(null);
    setGroup(null);
    setChartType(ChartType.NONE);

    detailsControlPanel.getButtonGroupFunction().clearSelection();
  }

  public void loadMetricToDetails(Metric metric) {
    this.metric = metric;

    setMetricInUI(metric);

    setButtonEnabled(false, false, false);
    setRadioButtonEnabled(false, false, false);
    setComboBoxEnabled(false, false, false);
  }

  public void loadColumnToDetails(Metric metric) {
    this.metric = metric;

    loadMetricToDetails(metric);

    setButtonEnabled(false, true, false);
    setRadioButtonEnabled(false, false, false);
    setComboBoxEnabled(false, false, false);
  }

  public void setMetricInUI(Metric metric) {
    setYAxis(metric.getYAxis().getColName());
    setGroup(metric.getGroup().getColName());
    setChartType(metric.getChartType());

    setSelectedRadioButton(metric.getMetricFunction());
  }

  public void saveNewMetric(String metricName) {
    log.info("Metric name to create: " + metricName);
    this.status = LifeCycleStatus.NONE;
  }

  public void cancelToSaveNewMetric() {
    this.status = LifeCycleStatus.NONE;

    setMetricInUI(metric);
  }

  public void setSelectedRadioButton(MetricFunction metricFunction) {
    detailsControlPanel.setSelectedRadioButton(metricFunction);
  }

  private void setYAxis(String value) {
    detailsControlPanel.getYAxis().setSelectedItem(value);
  }

  private void setGroup(String value) {
    detailsControlPanel.getGroup().setSelectedItem(value);
  }

  private void setChartType(ChartType chartType) {
    detailsControlPanel.getChartType().setSelectedItem(chartType.toString());
  }

  private void setButtonEnabled(boolean save,
                                boolean edit,
                                boolean cancel) {
    detailsControlPanel.getSaveButton().setEnabled(save);
    detailsControlPanel.getEditButton().setEnabled(edit);
    detailsControlPanel.getCancelButton().setEnabled(cancel);
  }

  private void setRadioButtonEnabled(boolean count,
                                     boolean sum,
                                     boolean average) {
    detailsControlPanel.getButtonGroupFunction().getCount().setEnabled(count);
    detailsControlPanel.getButtonGroupFunction().getSum().setEnabled(sum);
    detailsControlPanel.getButtonGroupFunction().getAverage().setEnabled(average);
  }

  private void setComboBoxEnabled(boolean yAxis,
                                  boolean group,
                                  boolean chartType) {
    detailsControlPanel.getYAxis().setEnabled(yAxis);
    detailsControlPanel.getGroup().setEnabled(group);
    detailsControlPanel.getChartType().setEnabled(chartType);
  }

  private class RadioListenerDetailsUI implements ActionListener {

    public RadioListenerDetailsUI() {
    }

    public void actionPerformed(ActionEvent e) {
      JRadioButton button = (JRadioButton) e.getSource();

      RadioButtonActionExecutor.execute(button, metricFunction -> {
        if (MetricFunction.COUNT.equals(metricFunction)) {
          setChartType(ChartType.STACKED);
        } else {
          setChartType(ChartType.LINEAR);
        }
      });
    }
  }
}
