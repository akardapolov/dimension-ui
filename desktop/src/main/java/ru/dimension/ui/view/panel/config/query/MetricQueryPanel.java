package ru.dimension.ui.view.panel.config.query;

import java.util.ResourceBundle;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jdesktop.swingx.JXTextField;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.view.panel.config.ButtonPanel;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class MetricQueryPanel extends JPanel {

  private final ButtonPanel metricQueryButtonPanel;
  private final JXTextField nameMetric;
  private final JCheckBox defaultCheckBox;
  private final JXTextField xTextFile;
  private final JComboBox<String> yComboBox;
  private final JComboBox<String> dimensionComboBox;
  private final JComboBox<?> groupFunction;
  private final JComboBox<?> chartType;
  private final JXTableCase configMetricCase;
  private final ResourceBundle bundleDefault;

  @Inject
  public MetricQueryPanel(@Named("metricQueryButtonPanel") ButtonPanel metricQueryButtonPanel,
                          @Named("configMetricCase") JXTableCase configMetricCase,
                          @Named("groupFunction") JComboBox<?> groupFunction,
                          @Named("chartType") JComboBox<?> chartType) {
    this.metricQueryButtonPanel = metricQueryButtonPanel;
    this.bundleDefault = Internationalization.getInternationalizationBundle();
    this.nameMetric = new JXTextField();
    this.nameMetric.setPrompt(bundleDefault.getString("metricName"));
    this.defaultCheckBox = new JCheckBox(bundleDefault.getString("metricDef"), false);
    this.xTextFile = new JXTextField();
    this.yComboBox = new JComboBox<String>();
    this.dimensionComboBox = new JComboBox<>();
    this.groupFunction = groupFunction;
    this.chartType = chartType;
    this.configMetricCase = configMetricCase;

    Border finalBorder = GUIHelper.getBorder();
    this.nameMetric.setBorder(finalBorder);
    this.xTextFile.setBorder(finalBorder);
    this.yComboBox.setBorder(finalBorder);
    this.dimensionComboBox.setBorder(finalBorder);
    this.groupFunction.setBorder(finalBorder);
    this.chartType.setBorder(finalBorder);

    AutoCompleteDecorator.decorate(yComboBox);
    AutoCompleteDecorator.decorate(dimensionComboBox);
    AutoCompleteDecorator.decorate(this.groupFunction);
    AutoCompleteDecorator.decorate(this.chartType);

    defaultCheckBox.setEnabled(false);
    nameMetric.setEditable(false);
    xTextFile.setEnabled(false);
    yComboBox.setEnabled(false);
    dimensionComboBox.setEnabled(false);
    this.groupFunction.setEnabled(false);
    this.chartType.setEnabled(false);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row()
        .cellXRemainder(metricQueryButtonPanel).fillX();

    gbl.row()
        .cell(defaultCheckBox).cell(new JLabel()).fillX();

    gbl.row()
        .cell(new JLabel("Name")).cell(nameMetric).fillX();
    gbl.row()
        .cell(new JLabel(bundleDefault.getString("xAxis"))).cell(xTextFile).fillX();
    gbl.row()
        .cell(new JLabel("Y axis")).cell(yComboBox).fillX();
    gbl.row()
        .cell(new JLabel("Group")).cell(dimensionComboBox).fillX();

    gbl.row()
        .cell(new JLabel("Function"))
        .cell(groupFunction).fillX();

    gbl.row()
        .cell(new JLabel("Chart"))
        .cell(chartType).fillX();

    gbl.row()
        .cellXYRemainder(this.configMetricCase.getJScrollPane())
        .fillXY();

    gbl.done();
  }
}
