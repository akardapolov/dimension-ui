package ru.dimension.ui.component.module.config.panel.range;

import static ru.dimension.ui.model.view.RangeRealTime.FIVE_MIN;
import static ru.dimension.ui.model.view.RangeRealTime.SIXTY_MIN;
import static ru.dimension.ui.model.view.RangeRealTime.TEN_MIN;
import static ru.dimension.ui.model.view.RangeRealTime.THIRTY_MIN;
import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;
import static ru.dimension.ui.laf.LafColorGroup.RANGE_NOT_SELECTED_FONT_COLOR;
import static ru.dimension.ui.laf.LafColorGroup.RANGE_SELECTED_FONT_COLOR;

import java.awt.Color;
import java.util.function.BiConsumer;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import lombok.Data;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;

@Data
public class RealTimeRangePanel extends JPanel {
  private final JRadioButton fiveMin;
  private final JRadioButton tenMin;
  private final JRadioButton thirtyMin;
  private final JRadioButton sixtyMin;
  private final ButtonGroup buttonGroup;
  private BiConsumer<String, RangeRealTime> runAction;

  public RealTimeRangePanel() {
    this(null);
  }

  public RealTimeRangePanel(JLabel label) {
    this.fiveMin = new JRadioButton(FIVE_MIN.getName(), false);
    this.tenMin = new JRadioButton(TEN_MIN.getName(), true);
    this.thirtyMin = new JRadioButton(THIRTY_MIN.getName(), false);
    this.sixtyMin = new JRadioButton(SIXTY_MIN.getName(), false);

    this.buttonGroup = new ButtonGroup();
    buttonGroup.add(fiveMin);
    buttonGroup.add(tenMin);
    buttonGroup.add(thirtyMin);
    buttonGroup.add(sixtyMin);

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    if (label != null) {
      gbl.row()
          .cell(label).cell(fiveMin).cell(tenMin).cell(thirtyMin).cell(sixtyMin)
          .cellXRemainder(new JLabel()).fillX();

      PGHelper.setConstrainsInsets(gbl, label, 0, 10);
    } else {
      gbl.row()
          .cell(fiveMin).cell(tenMin).cell(thirtyMin).cell(sixtyMin)
          .cellXRemainder(new JLabel()).fillX();
    }

    PGHelper.setConstrainsInsets(gbl, fiveMin, 0);
    PGHelper.setConstrainsInsets(gbl, tenMin, 0);
    PGHelper.setConstrainsInsets(gbl, thirtyMin, 0);
    PGHelper.setConstrainsInsets(gbl, sixtyMin, 0);

    gbl.done();

    colorButton(10);

    // Add action listeners
    fiveMin.addActionListener(e -> {
      if (runAction != null) runAction.accept("rangeChanged", FIVE_MIN);
      colorButton(5);
    });
    tenMin.addActionListener(e -> {
      if (runAction != null) runAction.accept("rangeChanged", TEN_MIN);
      colorButton(10);
    });
    thirtyMin.addActionListener(e -> {
      if (runAction != null) runAction.accept("rangeChanged", THIRTY_MIN);
      colorButton(30);
    });
    sixtyMin.addActionListener(e -> {
      if (runAction != null) runAction.accept("rangeChanged", SIXTY_MIN);
      colorButton(60);
    });
  }

  public void colorButton(int numMin) {
    Color colorSelected = LaF.getBackgroundColor(RANGE_SELECTED_FONT_COLOR, LaF.getLafType());
    Color colorNotSelected = LaF.getBackgroundColor(RANGE_NOT_SELECTED_FONT_COLOR, LaF.getLafType());

    switch (numMin) {
      case 5 -> setButtonColor(colorSelected, colorNotSelected, colorNotSelected, colorNotSelected);
      case 10 -> setButtonColor(colorNotSelected, colorSelected, colorNotSelected, colorNotSelected);
      case 30 -> setButtonColor(colorNotSelected, colorNotSelected, colorSelected, colorNotSelected);
      case 60 -> setButtonColor(colorNotSelected, colorNotSelected, colorNotSelected, colorSelected);
    }
  }

  public void setSelectedRange(RangeRealTime range) {
    switch (range) {
      case FIVE_MIN -> fiveMin.setSelected(true);
      case TEN_MIN -> tenMin.setSelected(true);
      case THIRTY_MIN -> thirtyMin.setSelected(true);
      case SIXTY_MIN -> sixtyMin.setSelected(true);
    }
    colorButton(range.getMinutes());
  }

  private void setButtonColor(Color b5, Color b10, Color b30, Color b60) {
    fiveMin.setForeground(b5);
    tenMin.setForeground(b10);
    thirtyMin.setForeground(b30);
    sixtyMin.setForeground(b60);
  }
}