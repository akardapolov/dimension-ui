package ru.dimension.ui.component.panel.range;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;
import static ru.dimension.ui.laf.LafColorGroup.RANGE_NOT_SELECTED_FONT_COLOR;
import static ru.dimension.ui.laf.LafColorGroup.RANGE_SELECTED_FONT_COLOR;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import lombok.Data;
import org.jdesktop.swingx.plaf.basic.CalendarHeaderHandler;
import org.jdesktop.swingx.plaf.basic.SpinningCalendarHeaderHandler;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.panel.popup.ConfigPopupPanel;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.view.panel.DateTimePicker;

@Data
public class HistoryRangePanel extends JPanel {
  private final JRadioButton day;
  private final JRadioButton week;
  private final JRadioButton month;
  private final JRadioButton custom;
  private final ButtonGroup buttonGroup;
  private final ConfigPopupPanel configPopupPanel;
  private final JLabel labelFrom;
  private final JLabel labelTo;
  private final DateTimePicker dateTimePickerFrom;
  private final DateTimePicker dateTimePickerTo;
  private final JButton buttonApplyRange;

  private BiConsumer<String, RangeHistory> runAction;

  public HistoryRangePanel() {
    this(null);
  }

  public HistoryRangePanel(JLabel label) {
    this.day = new JRadioButton(RangeHistory.DAY.getName(), true);
    this.week = new JRadioButton(RangeHistory.WEEK.getName(), false);
    this.month = new JRadioButton(RangeHistory.MONTH.getName(), false);
    this.custom = new JRadioButton(RangeHistory.CUSTOM.getName(), false);

    this.labelFrom = new JLabel("From");
    this.labelTo = new JLabel("To");

    SimpleDateFormat format = new SimpleDateFormat(DateHelper.formatPattern);

    this.dateTimePickerFrom = new DateTimePicker();
    this.dateTimePickerFrom.setFormats(format);
    this.dateTimePickerFrom.setTimeFormat(format);
    UIManager.put(CalendarHeaderHandler.uiControllerID, SpinningCalendarHeaderHandler.class.getName());
    this.dateTimePickerFrom.getMonthView().setZoomable(true);

    this.dateTimePickerTo = new DateTimePicker();
    this.dateTimePickerTo.setFormats(format);
    this.dateTimePickerTo.setTimeFormat(format);
    UIManager.put(CalendarHeaderHandler.uiControllerID, SpinningCalendarHeaderHandler.class.getName());
    this.dateTimePickerTo.getMonthView().setZoomable(true);

    Map.Entry<Date, Date> range = DateHelper.getRangeDate();
    dateTimePickerFrom.setDate(range.getKey());
    dateTimePickerTo.setDate(range.getValue());

    this.configPopupPanel = new ConfigPopupPanel(this::createPopupContent);

    this.buttonApplyRange = new JButton("Apply");

    this.buttonGroup = new ButtonGroup();
    buttonGroup.add(day);
    buttonGroup.add(week);
    buttonGroup.add(month);
    buttonGroup.add(custom);

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    if (label != null) {
      gbl.row()
          .cell(label).cell(day).cell(week).cell(month).cell(custom).cell(configPopupPanel)
          .cellXRemainder(new JLabel()).fillX();

      PGHelper.setConstrainsInsets(gbl, label, 0, 10);
    } else {
      gbl.row()
          .cell(day).cell(week).cell(month).cell(custom).cell(configPopupPanel)
          .cellXRemainder(new JLabel()).fillX();
    }

    PGHelper.setConstrainsInsets(gbl, day, 0);
    PGHelper.setConstrainsInsets(gbl, week, 0);
    PGHelper.setConstrainsInsets(gbl, month, 0);
    PGHelper.setConstrainsInsets(gbl, custom, 0);
    PGHelper.setConstrainsInsets(gbl, configPopupPanel, 0);

    gbl.done();

    colorButton(RangeHistory.DAY);

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
    custom.addActionListener(e -> {
      if (runAction != null) runAction.accept("rangeChanged", RangeHistory.CUSTOM);
      colorButton(RangeHistory.CUSTOM);
    });
  }

  private JPanel createPopupContent() {
    JPanel popupPanel = new JPanel();
    PainlessGridBag gbl = new PainlessGridBag(popupPanel, PGHelper.getPGConfig(), false);

    gbl.row().cell(labelFrom).cell(dateTimePickerFrom).fillX();
    gbl.row().cell(labelTo).cell(dateTimePickerTo).fillX();
    gbl.row().cell(new JLabel("")).cell(buttonApplyRange).fillX();
    gbl.done();

    return popupPanel;
  }

  public void colorButton(RangeHistory selectedRange) {
    Color colorSelected = LaF.getBackgroundColor(RANGE_SELECTED_FONT_COLOR, LaF.getLafType());
    Color colorNotSelected = LaF.getBackgroundColor(RANGE_NOT_SELECTED_FONT_COLOR, LaF.getLafType());

    day.setForeground(selectedRange == RangeHistory.DAY ? colorSelected : colorNotSelected);
    week.setForeground(selectedRange == RangeHistory.WEEK ? colorSelected : colorNotSelected);
    month.setForeground(selectedRange == RangeHistory.MONTH ? colorSelected : colorNotSelected);
    custom.setForeground(selectedRange == RangeHistory.CUSTOM ? colorSelected : colorNotSelected);
  }

  public void setSelectedRange(RangeHistory range) {
    switch (range) {
      case DAY -> day.setSelected(true);
      case WEEK -> week.setSelected(true);
      case MONTH -> month.setSelected(true);
      case CUSTOM -> custom.setSelected(true);
    }
    colorButton(range);
  }
}