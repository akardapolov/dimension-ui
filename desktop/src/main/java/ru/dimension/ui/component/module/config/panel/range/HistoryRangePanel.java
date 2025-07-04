package ru.dimension.ui.component.module.config.panel.range;

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
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;

@Data
public class HistoryRangePanel extends JPanel {
  private final JRadioButton day;
  private final JRadioButton week;
  private final JRadioButton month;
  private final ButtonGroup buttonGroup;
  private BiConsumer<String, RangeHistory> runAction;

  public HistoryRangePanel() {
    this(null);
  }

  public HistoryRangePanel(JLabel label) {
    this.day = new JRadioButton(RangeHistory.DAY.getName(), true);
    this.week = new JRadioButton(RangeHistory.WEEK.getName(), false);
    this.month = new JRadioButton(RangeHistory.MONTH.getName(), false);

    this.buttonGroup = new ButtonGroup();
    buttonGroup.add(day);
    buttonGroup.add(week);
    buttonGroup.add(month);

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    if (label != null) {
      gbl.row()
          .cell(label).cell(day).cell(week).cell(month)
          .cellXRemainder(new JLabel()).fillX();

      PGHelper.setConstrainsInsets(gbl, label, 0, 10);
    } else {
      gbl.row()
          .cell(day).cell(week).cell(month)
          .cellXRemainder(new JLabel()).fillX();
    }

    PGHelper.setConstrainsInsets(gbl, day, 0);
    PGHelper.setConstrainsInsets(gbl, week, 0);
    PGHelper.setConstrainsInsets(gbl, month, 0);

    gbl.done();

    colorButton(RangeHistory.DAY);

    // Add action listeners
    day.addActionListener(e -> {
      if (runAction != null) runAction.accept("rangeChanged", RangeHistory.DAY);
      colorButton(RangeHistory.DAY);
    });
    week.addActionListener(e -> {
      if (runAction != null) runAction.accept("rangeChanged", RangeHistory.WEEK);
      colorButton(RangeHistory.WEEK);
    });
    month.addActionListener(e -> {
      if (runAction != null) runAction.accept("rangeChanged", RangeHistory.MONTH);
      colorButton(RangeHistory.MONTH);
    });
  }

  public void colorButton(RangeHistory selectedRange) {
    Color colorSelected = LaF.getBackgroundColor(RANGE_SELECTED_FONT_COLOR, LaF.getLafType());
    Color colorNotSelected = LaF.getBackgroundColor(RANGE_NOT_SELECTED_FONT_COLOR, LaF.getLafType());

    day.setForeground(selectedRange == RangeHistory.DAY ? colorSelected : colorNotSelected);
    week.setForeground(selectedRange == RangeHistory.WEEK ? colorSelected : colorNotSelected);
    month.setForeground(selectedRange == RangeHistory.MONTH ? colorSelected : colorNotSelected);
  }

  public void setSelectedRange(RangeHistory range) {
    switch (range) {
      case DAY -> day.setSelected(true);
      case WEEK -> week.setSelected(true);
      case MONTH -> month.setSelected(true);
    }
    colorButton(range);
  }
}