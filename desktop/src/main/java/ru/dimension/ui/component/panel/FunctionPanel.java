package ru.dimension.ui.component.panel;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.Dimension;
import java.util.function.BiConsumer;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import lombok.Data;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.panel.popup.ConfigPopupPanel;
import ru.dimension.ui.component.panel.function.NormFunctionPanel;
import ru.dimension.ui.component.panel.function.TimeRangeFunctionPanel;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.function.GroupFunction;

@Data
public class FunctionPanel extends JPanel {
  private final JRadioButton count;
  private final JRadioButton sum;
  private final JRadioButton avg;
  private final ButtonGroup buttonGroup;
  private final ConfigPopupPanel configPopupPanel;
  private BiConsumer<String, GroupFunction> runAction;

  public FunctionPanel(JLabel label, TimeRangeFunctionPanel timeRangeFunctionPanel) {
    this(label);
    this.configPopupPanel.updateContent(() -> createPopupContent(timeRangeFunctionPanel));
  }

  public FunctionPanel(JLabel label, TimeRangeFunctionPanel timeRangeFunctionPanel, NormFunctionPanel normFunctionPanel) {
    this(label);
    this.configPopupPanel.updateContent(() -> createPopupContent(timeRangeFunctionPanel, normFunctionPanel));
  }

  public FunctionPanel(JLabel label) {
    this.count = new JRadioButton(GroupFunction.COUNT.name(), true);
    this.sum = new JRadioButton(GroupFunction.SUM.name(), false);
    this.avg = new JRadioButton(GroupFunction.AVG.name(), false);
    this.configPopupPanel = new ConfigPopupPanel(this::createPopupContent);

    this.buttonGroup = new ButtonGroup();
    buttonGroup.add(count);
    buttonGroup.add(sum);
    buttonGroup.add(avg);

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    if (label != null) {
      gbl.row()
          .cell(label).cell(count).cell(sum).cell(avg).cell(configPopupPanel)
          .cellXRemainder(new JLabel()).fillX();

      PGHelper.setConstrainsInsets(gbl, label, 0, 10);
    } else {
      gbl.row()
          .cell(count).cell(sum).cell(avg).cell(configPopupPanel)
          .cellXRemainder(new JLabel()).fillX();
    }

    PGHelper.setConstrainsInsets(gbl, count, 0);
    PGHelper.setConstrainsInsets(gbl, sum, 0);
    PGHelper.setConstrainsInsets(gbl, avg, 0);
    PGHelper.setConstrainsInsets(gbl, configPopupPanel, 0);

    gbl.done();

    count.addActionListener(e -> {
      if (runAction != null) runAction.accept("functionChanged", GroupFunction.COUNT);
    });
    sum.addActionListener(e -> {
      if (runAction != null) runAction.accept("functionChanged", GroupFunction.SUM);
    });
    avg.addActionListener(e -> {
      if (runAction != null) runAction.accept("functionChanged", GroupFunction.AVG);
    });
  }

  private JPanel createPopupContent() {
    JPanel panel = new JPanel();
    panel.add(new JLabel("Module is under development"));
    panel.setPreferredSize(new Dimension(200, 200));
    return panel;
  }

  private JPanel createPopupContent(TimeRangeFunctionPanel timeRangeFunctionPanel) {
    JPanel panel = new JPanel();

    LaF.setBackgroundConfigPanel(CHART_PANEL, panel);

    PainlessGridBag gbl = new PainlessGridBag(panel, PGHelper.getPGConfig(5), false);

    JXTitledSeparator history = new JXTitledSeparator("Time range");

    gbl.row()
        .cellX(history, 2).fillX(2)
        .cellXRemainder(new JXTitledSeparator("")).fillX();
    gbl.row()
        .cellX(timeRangeFunctionPanel, 2).fillX(2)
        .cellX(new JLabel(), 10).fillX(10);

    gbl.done();

    return panel;
  }

  private JPanel createPopupContent(TimeRangeFunctionPanel timeRangeFunctionPanel, NormFunctionPanel normFunctionPanel) {
    JPanel panel = new JPanel();

    LaF.setBackgroundConfigPanel(CHART_PANEL, panel);

    PainlessGridBag gbl = new PainlessGridBag(panel, PGHelper.getPGConfig(5), false);

    JXTitledSeparator timeRangeSeparator = new JXTitledSeparator("Time range");
    JXTitledSeparator normFunctionSeparator = new JXTitledSeparator("Normalization");

    gbl.row()
        .cellX(timeRangeSeparator, 2).fillX(2)
        .cellXRemainder(new JXTitledSeparator("")).fillX();
    gbl.row()
        .cellX(timeRangeFunctionPanel, 2).fillX(2)
        .cellX(new JLabel(), 10).fillX(10);

    gbl.row()
        .cellX(normFunctionSeparator, 2).fillX(2)
        .cellXRemainder(new JXTitledSeparator("")).fillX();
    gbl.row()
        .cellX(normFunctionPanel, 2).fillX(2)
        .cellX(new JLabel(), 10).fillX(10);

    gbl.done();

    return panel;
  }

  public void setSelected(GroupFunction function) {
    switch (function) {
      case COUNT -> count.setSelected(true);
      case SUM -> sum.setSelected(true);
      case AVG -> avg.setSelected(true);
    }
  }

  public void setEnabled(boolean bCount, boolean bSum, boolean bAverage) {
    count.setEnabled(bCount);
    sum.setEnabled(bSum);
    avg.setEnabled(bAverage);
  }
}