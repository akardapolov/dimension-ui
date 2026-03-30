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
import ru.dimension.ui.model.function.NormFunction;

@Log4j2
@Data
public class NormFunctionPanel extends JPanel {
  private final JRadioButton none;
  private final JRadioButton perSecond;
  private final JRadioButton perMinute;
  private final JRadioButton perHour;
  private final JRadioButton perDay;
  private final ButtonGroup buttonGroup;
  private BiConsumer<String, NormFunction> runAction;
  private BiConsumer<NormFunction, String> hoverAction;

  public NormFunctionPanel() {
    this.none = new JRadioButton(NormFunction.NONE.getName(), false);
    this.perSecond = new JRadioButton(NormFunction.SECOND.getName(), true);
    this.perMinute = new JRadioButton(NormFunction.MINUTE.getName(), false);
    this.perHour = new JRadioButton(NormFunction.HOUR.getName(), false);
    this.perDay = new JRadioButton(NormFunction.DAY.getName(), false);

    this.buttonGroup = new ButtonGroup();
    buttonGroup.add(none);
    buttonGroup.add(perSecond);
    buttonGroup.add(perMinute);
    buttonGroup.add(perHour);
    buttonGroup.add(perDay);

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    gbl.row()
        .cell(none).cell(perSecond).cell(perMinute).cell(perHour).cell(perDay)
        .cellXRemainder(new JLabel()).fillX();

    PGHelper.setConstrainsInsets(gbl, none, 0);
    PGHelper.setConstrainsInsets(gbl, perSecond, 0);
    PGHelper.setConstrainsInsets(gbl, perMinute, 0);
    PGHelper.setConstrainsInsets(gbl, perHour, 0);
    PGHelper.setConstrainsInsets(gbl, perDay, 0);

    gbl.done();

    MouseAdapter hoverAdapter = new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        if (hoverAction != null) {
          if (e.getSource() == none) hoverAction.accept(NormFunction.NONE, getDescription(NormFunction.NONE));
          else if (e.getSource() == perSecond) hoverAction.accept(NormFunction.SECOND, getDescription(NormFunction.SECOND));
          else if (e.getSource() == perMinute) hoverAction.accept(NormFunction.MINUTE, getDescription(NormFunction.MINUTE));
          else if (e.getSource() == perHour) hoverAction.accept(NormFunction.HOUR, getDescription(NormFunction.HOUR));
          else if (e.getSource() == perDay) hoverAction.accept(NormFunction.DAY, getDescription(NormFunction.DAY));
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        if (hoverAction != null) {
          hoverAction.accept(null, null);
        }
      }
    };

    none.addMouseListener(hoverAdapter);
    perSecond.addMouseListener(hoverAdapter);
    perMinute.addMouseListener(hoverAdapter);
    perHour.addMouseListener(hoverAdapter);
    perDay.addMouseListener(hoverAdapter);

    none.addActionListener(e -> {
      if (runAction != null) runAction.accept("normFunctionChanged", NormFunction.NONE);
      if (hoverAction != null) hoverAction.accept(NormFunction.NONE, getDescription(NormFunction.NONE));
    });
    perSecond.addActionListener(e -> {
      if (runAction != null) runAction.accept("normFunctionChanged", NormFunction.SECOND);
      if (hoverAction != null) hoverAction.accept(NormFunction.SECOND, getDescription(NormFunction.SECOND));
    });
    perMinute.addActionListener(e -> {
      if (runAction != null) runAction.accept("normFunctionChanged", NormFunction.MINUTE);
      if (hoverAction != null) hoverAction.accept(NormFunction.MINUTE, getDescription(NormFunction.MINUTE));
    });
    perHour.addActionListener(e -> {
      if (runAction != null) runAction.accept("normFunctionChanged", NormFunction.HOUR);
      if (hoverAction != null) hoverAction.accept(NormFunction.HOUR, getDescription(NormFunction.HOUR));
    });
    perDay.addActionListener(e -> {
      if (runAction != null) runAction.accept("normFunctionChanged", NormFunction.DAY);
      if (hoverAction != null) hoverAction.accept(NormFunction.DAY, getDescription(NormFunction.DAY));
    });
  }

  public void setSelected(NormFunction function) {
    switch (function) {
      case NONE -> none.setSelected(true);
      case SECOND -> perSecond.setSelected(true);
      case MINUTE -> perMinute.setSelected(true);
      case HOUR -> perHour.setSelected(true);
      case DAY -> perDay.setSelected(true);
    }
  }

  public NormFunction getSelectedFunction() {
    if (perSecond.isSelected()) return NormFunction.SECOND;
    if (perMinute.isSelected()) return NormFunction.MINUTE;
    if (perHour.isSelected()) return NormFunction.HOUR;
    if (perDay.isSelected()) return NormFunction.DAY;
    return NormFunction.NONE;
  }

  private String getDescription(NormFunction function) {
    return switch (function) {
      case NONE -> "None: normalization disabled";
      case SECOND -> "Second: values normalized per second";
      case MINUTE -> "Minute: values normalized per minute";
      case HOUR -> "Hour: values normalized per hour";
      case DAY -> "Day: values normalized per day";
    };
  }
}