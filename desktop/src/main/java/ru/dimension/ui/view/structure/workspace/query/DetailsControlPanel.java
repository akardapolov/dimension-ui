package ru.dimension.ui.view.structure.workspace.query;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.Data;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.view.panel.report.ButtonGroupFunction;

@Data
public class DetailsControlPanel extends JPanel {

  private final JComboBox<String> yAxis;
  private final JComboBox<String> group;
  private final JComboBox<?> chartType;

  private final JButton saveButton;
  private final JButton editButton;
  private final JButton cancelButton;

  private final ButtonGroupFunction buttonGroupFunction;

  public DetailsControlPanel() {
    this.yAxis = new JComboBox<>();
    this.group = new JComboBox<>();
    this.chartType = new JComboBox<>(ChartType.values());

    AutoCompleteDecorator.decorate(yAxis);
    AutoCompleteDecorator.decorate(group);
    AutoCompleteDecorator.decorate(chartType);

    this.saveButton = new JButton("Save");
    this.editButton = new JButton("Edit");
    this.cancelButton = new JButton("Cancel");

    buttonGroupFunction = new ButtonGroupFunction();

    Box boxFunction = Box.createHorizontalBox();
    boxFunction.add(buttonGroupFunction.getCount());
    boxFunction.add(buttonGroupFunction.getSum());
    boxFunction.add(buttonGroupFunction.getAverage());

    JPanel jPanelSettings = new JPanel();
    PainlessGridBag gblSettings = new PainlessGridBag(jPanelSettings, PGHelper.getPGConfig(), false);
    gblSettings.row()
        .cell(new JLabel("Y Axis")).cell(yAxis).fillX();
    gblSettings.row()
        .cell(new JLabel("Group")).cell(group).fillX();
    gblSettings.row()
        .cell(new JLabel("Chart")).cell(chartType).fillX();
    gblSettings.row()
        .cell(new JLabel()).cell(new JLabel()).fillXY();

    setConstrainsInsets(gblSettings, yAxis, 1);
    setConstrainsInsets(gblSettings, group, 1);
    setConstrainsInsets(gblSettings, chartType, 1);
    gblSettings.done();

    JPanel jPanelFunction = new JPanel();
    PainlessGridBag gblFunction = new PainlessGridBag(jPanelFunction, PGHelper.getPGConfig(), false);
    gblFunction.row()
        .cell(buttonGroupFunction.getCount()).fillX();
    gblFunction.row()
        .cell(buttonGroupFunction.getSum()).fillX();
    gblFunction.row()
        .cell(buttonGroupFunction.getAverage()).fillX();
    gblFunction.row()
        .cell(new JLabel()).fillXY();

    setConstrainsInsets(gblFunction, buttonGroupFunction.getCount(), 0);
    setConstrainsInsets(gblFunction, buttonGroupFunction.getSum(), 0);
    setConstrainsInsets(gblFunction, buttonGroupFunction.getAverage(), 0);
    gblFunction.done();

    JPanel jPanelButton = new JPanel();
    PainlessGridBag gblButton = new PainlessGridBag(jPanelButton, PGHelper.getPGConfig(), false);
    gblButton.row()
        .cell(saveButton).fillX();
    gblButton.row()
        .cell(editButton).fillX();
    gblButton.row()
        .cell(cancelButton).fillX();
    gblButton.row()
        .cell(new JLabel()).fillXY();

    setConstrainsInsets(gblButton, saveButton, 1);
    setConstrainsInsets(gblButton, editButton, 1);
    setConstrainsInsets(gblButton, cancelButton, 1);
    gblButton.done();

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row()
        .cellX(jPanelFunction, 1).fillXY(1, 1)
        .cellX(jPanelSettings, 5).fillXY(5, 5)
        .cellX(jPanelButton, 1).fillXY(1, 1);
    gbl.done();

    setEnabled(false);
  }

  public void setEnabled(boolean flag) {
    yAxis.setEnabled(flag);
    group.setEnabled(flag);
    chartType.setEnabled(flag);

    buttonGroupFunction.getCount().setEnabled(flag);
    buttonGroupFunction.getSum().setEnabled(flag);
    buttonGroupFunction.getCount().setEnabled(flag);

    saveButton.setEnabled(flag);
    editButton.setEnabled(flag);
    cancelButton.setEnabled(flag);
  }

  public void setSelectedRadioButton(MetricFunction metricFunction) {
    buttonGroupFunction.setSelectedRadioButton(metricFunction);
  }

  private void setConstrainsInsets(PainlessGridBag gbl,
                                   JComponent component,
                                   int value) {
    gbl.constraints(component).insets.top = value;
    gbl.constraints(component).insets.bottom = value;
    gbl.constraints(component).insets.left = value;
    gbl.constraints(component).insets.right = value;
  }
}
