package ru.dimension.ui.component.panel.function;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.function.PercentileFunction;

@Log4j2
@Data
public class PercentileFunctionPanel extends JPanel {

  private final JRadioButton none;
  private final JRadioButton p50;
  private final JRadioButton p90;
  private final JRadioButton p95;
  private final JRadioButton p99;
  private final ButtonGroup buttonGroup;

  private BiConsumer<String, PercentileFunction> runAction;
  private BiConsumer<PercentileFunction, String> hoverAction;

  public PercentileFunctionPanel() {
    this.none = new JRadioButton(PercentileFunction.NONE.getName(), true);
    this.p50  = new JRadioButton(PercentileFunction.P50.getName(),  false);
    this.p90  = new JRadioButton(PercentileFunction.P90.getName(),  false);
    this.p95  = new JRadioButton(PercentileFunction.P95.getName(),  false);
    this.p99  = new JRadioButton(PercentileFunction.P99.getName(),  false);

    this.buttonGroup = new ButtonGroup();
    buttonGroup.add(none);
    buttonGroup.add(p50);
    buttonGroup.add(p90);
    buttonGroup.add(p95);
    buttonGroup.add(p99);

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);
    gbl.row()
        .cell(none).cell(p50).cell(p90).cell(p95).cell(p99)
        .cellXRemainder(new JLabel()).fillX();

    PGHelper.setConstrainsInsets(gbl, none, 0);
    PGHelper.setConstrainsInsets(gbl, p50,  0);
    PGHelper.setConstrainsInsets(gbl, p90,  0);
    PGHelper.setConstrainsInsets(gbl, p95,  0);
    PGHelper.setConstrainsInsets(gbl, p99,  0);

    gbl.done();

    MouseAdapter hoverAdapter = new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        if (hoverAction == null) return;
        if      (e.getSource() == none) hoverAction.accept(PercentileFunction.NONE, getDescription(PercentileFunction.NONE));
        else if (e.getSource() == p50)  hoverAction.accept(PercentileFunction.P50,  getDescription(PercentileFunction.P50));
        else if (e.getSource() == p90)  hoverAction.accept(PercentileFunction.P90,  getDescription(PercentileFunction.P90));
        else if (e.getSource() == p95)  hoverAction.accept(PercentileFunction.P95,  getDescription(PercentileFunction.P95));
        else if (e.getSource() == p99)  hoverAction.accept(PercentileFunction.P99,  getDescription(PercentileFunction.P99));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        if (hoverAction != null) hoverAction.accept(null, null);
      }
    };

    none.addMouseListener(hoverAdapter);
    p50.addMouseListener(hoverAdapter);
    p90.addMouseListener(hoverAdapter);
    p95.addMouseListener(hoverAdapter);
    p99.addMouseListener(hoverAdapter);

    none.addActionListener(e -> fire(PercentileFunction.NONE));
    p50.addActionListener(e ->  fire(PercentileFunction.P50));
    p90.addActionListener(e ->  fire(PercentileFunction.P90));
    p95.addActionListener(e ->  fire(PercentileFunction.P95));
    p99.addActionListener(e ->  fire(PercentileFunction.P99));
  }

  private void fire(PercentileFunction function) {
    if (runAction  != null) runAction.accept("percentileFunctionChanged", function);
    if (hoverAction != null) hoverAction.accept(function, getDescription(function));
  }

  public void setSelected(PercentileFunction function) {
    switch (function) {
      case NONE -> none.setSelected(true);
      case P50  -> p50.setSelected(true);
      case P90  -> p90.setSelected(true);
      case P95  -> p95.setSelected(true);
      case P99  -> p99.setSelected(true);
    }
  }

  public PercentileFunction getSelectedFunction() {
    if (p50.isSelected()) return PercentileFunction.P50;
    if (p90.isSelected()) return PercentileFunction.P90;
    if (p95.isSelected()) return PercentileFunction.P95;
    if (p99.isSelected()) return PercentileFunction.P99;
    return PercentileFunction.NONE;
  }

  private String getDescription(PercentileFunction function) {
    return switch (function) {
      case NONE -> "None: percentile disabled, standard aggregation";
      case P50  -> "P50: median (50th percentile)";
      case P90  -> "P90: 90th percentile";
      case P95  -> "P95: 95th percentile";
      case P99  -> "P99: 99th percentile";
    };
  }
}
