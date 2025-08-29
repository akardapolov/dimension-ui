package ru.dimension.ui.component.panel.function;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

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
import ru.dimension.ui.model.function.TimeRangeFunction;

@Log4j2
@Data
public class TimeRangeFunctionPanel extends JPanel {
  private final JRadioButton auto;
  private final JRadioButton minute;
  private final JRadioButton hour;
  private final JRadioButton day;
  private final JRadioButton month;
  private final ButtonGroup buttonGroup;
  private BiConsumer<String, TimeRangeFunction> runAction;

  public TimeRangeFunctionPanel() {
    this.auto = new JRadioButton(TimeRangeFunction.AUTO.getName(), true);
    this.minute = new JRadioButton(TimeRangeFunction.MINUTE.getName(), false);
    this.hour = new JRadioButton(TimeRangeFunction.HOUR.getName(), false);
    this.day = new JRadioButton(TimeRangeFunction.DAY.getName(), false);
    this.month = new JRadioButton(TimeRangeFunction.MONTH.getName(), false);

    this.buttonGroup = new ButtonGroup();
    buttonGroup.add(auto);
    buttonGroup.add(minute);
    buttonGroup.add(hour);
    buttonGroup.add(day);
    buttonGroup.add(month);

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    gbl.row()
        .cell(auto).cell(minute).cell(hour).cell(day).cell(month)
        .cellXRemainder(new JLabel()).fillX();

    PGHelper.setConstrainsInsets(gbl, auto, 0);
    PGHelper.setConstrainsInsets(gbl, minute, 0);
    PGHelper.setConstrainsInsets(gbl, hour, 0);
    PGHelper.setConstrainsInsets(gbl, day, 0);
    PGHelper.setConstrainsInsets(gbl, month, 0);

    gbl.done();

    auto.addActionListener(e -> {
      if (runAction != null) runAction.accept("timeRangeChanged", TimeRangeFunction.AUTO);
    });
    minute.addActionListener(e -> {
      if (runAction != null) runAction.accept("timeRangeChanged", TimeRangeFunction.MINUTE);
    });
    hour.addActionListener(e -> {
      if (runAction != null) runAction.accept("timeRangeChanged", TimeRangeFunction.HOUR);
    });
    day.addActionListener(e -> {
      if (runAction != null) runAction.accept("timeRangeChanged", TimeRangeFunction.DAY);
    });
    month.addActionListener(e -> {
      if (runAction != null) runAction.accept("timeRangeChanged", TimeRangeFunction.MONTH);
    });
  }

  public void setSelected(TimeRangeFunction function) {
    switch (function) {
      case AUTO -> auto.setSelected(true);
      case MINUTE -> minute.setSelected(true);
      case HOUR -> hour.setSelected(true);
      case DAY -> day.setSelected(true);
      case MONTH -> month.setSelected(true);
    }
  }
}
