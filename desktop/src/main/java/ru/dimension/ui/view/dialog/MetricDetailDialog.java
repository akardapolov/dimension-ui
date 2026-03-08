package ru.dimension.ui.view.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.Getter;
import org.jdesktop.swingx.JXTextField;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.prompt.Internationalization;

@Getter
public class MetricDetailDialog extends JDialog {

  private final JXTextField nameMetric;
  private final JCheckBox defaultCheckBox;
  private final JXTextField xTextFile;
  private final JComboBox<String> yComboBox;
  private final JComboBox<String> dimensionComboBox;
  private final JComboBox<?> groupFunction;
  private final JComboBox<?> chartType;
  private final JButton btnSave;
  private final JButton btnCancel;

  public MetricDetailDialog(Window owner,
                            JXTextField nameMetric,
                            JCheckBox defaultCheckBox,
                            JXTextField xTextFile,
                            JComboBox<String> yComboBox,
                            JComboBox<String> dimensionComboBox,
                            JComboBox<?> groupFunction,
                            JComboBox<?> chartType) {
    super(owner, "Metric Detail", ModalityType.APPLICATION_MODAL);

    ResourceBundle bundleDefault = Internationalization.getInternationalizationBundle();

    this.nameMetric = nameMetric;
    this.defaultCheckBox = defaultCheckBox;
    this.xTextFile = xTextFile;
    this.yComboBox = yComboBox;
    this.dimensionComboBox = dimensionComboBox;
    this.groupFunction = groupFunction;
    this.chartType = chartType;

    this.btnSave = new JButton("Save");
    this.btnCancel = new JButton("Cancel");

    JPanel content = new JPanel();
    PainlessGridBag gbl = new PainlessGridBag(content, PGHelper.getPGConfig(2), false);

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
        .cell(new JLabel("Function")).cell(groupFunction).fillX();
    gbl.row()
        .cell(new JLabel("Chart")).cell(chartType).fillX();

    JPanel buttonRow = new JPanel();
    buttonRow.add(btnSave);
    buttonRow.add(btnCancel);

    gbl.row()
        .cellXRemainder(buttonRow).fillX();
    gbl.done();

    setLayout(new BorderLayout());
    add(content, BorderLayout.CENTER);

    setMinimumSize(new Dimension(450, 320));
    setPreferredSize(new Dimension(500, 350));
    pack();
    setLocationRelativeTo(owner);
  }
}