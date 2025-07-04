package ru.dimension.ui.component.module.config.panel;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.util.function.BiConsumer;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import lombok.Data;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.function.MetricFunction;

@Data
public class MetricFunctionPanel extends JPanel {
  private final JRadioButton count;
  private final JRadioButton sum;
  private final JRadioButton avg;
  private final ButtonGroup buttonGroup;
  private BiConsumer<String, MetricFunction> runAction;

  public MetricFunctionPanel() {
    this(null);
  }

  public MetricFunctionPanel(JLabel label) {
    this.count = new JRadioButton(MetricFunction.COUNT.name(), true);
    this.sum = new JRadioButton(MetricFunction.SUM.name(), false);
    this.avg = new JRadioButton(MetricFunction.AVG.name(), false);

    this.buttonGroup = new ButtonGroup();
    buttonGroup.add(count);
    buttonGroup.add(sum);
    buttonGroup.add(avg);

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    if (label != null) {
      gbl.row()
          .cell(label).cell(count).cell(sum).cell(avg)
          .cellXRemainder(new JLabel()).fillX();

      PGHelper.setConstrainsInsets(gbl, label, 0, 10);
    } else {
      gbl.row()
          .cell(count).cell(sum).cell(avg)
          .cellXRemainder(new JLabel()).fillX();
    }

    PGHelper.setConstrainsInsets(gbl, count, 0);
    PGHelper.setConstrainsInsets(gbl, sum, 0);
    PGHelper.setConstrainsInsets(gbl, avg, 0);

    gbl.done();

    // Add action listeners
    count.addActionListener(e -> {
      if (runAction != null) runAction.accept("functionChanged", MetricFunction.COUNT);
    });
    sum.addActionListener(e -> {
      if (runAction != null) runAction.accept("functionChanged", MetricFunction.SUM);
    });
    avg.addActionListener(e -> {
      if (runAction != null) runAction.accept("functionChanged", MetricFunction.AVG);
    });
  }

  public void setSelected(MetricFunction function) {
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