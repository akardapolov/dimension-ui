package ru.dimension.ui.view.panel;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import lombok.Data;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;

@Data
public class TimeRangeAbsolutePanel extends JPanel {

  private final JPanel jPanelFrom;
  private final JPanel jPanelTo;
  private final JLabel jLabelFrom;
  private final JLabel jLabelTo;
  private final JButton jButtonFrom;
  private final JButton jButtonTo;
  private final JLabel jLabelPromptTextFrom;
  private final JLabel jLabelPromptTextTo;
  private final DatePickerSettings datePickerSettingsFrom;
  private final DatePickerSettings datePickerSettingsTo;
  private final DatePicker datePickerFrom;
  private final DatePicker datePickerTo;
  private final JButton jButtonGo;
  private final JPanel jPanelGo;
  private final JPanel jPanel1LineFrom;
  private final JPanel jPanel1LineTo;
  private final JPanel jPanel2LineFrom;
  private final JPanel jPanel2LineTo;
  private final JPanel jPanel3LineFrom;
  private final JPanel jPanel3LineTo;
  private final JPanel jPanelButton;


  public TimeRangeAbsolutePanel() {
    this.jPanelFrom = new JPanel();
    this.jPanelTo = new JPanel();
    this.jPanelGo = new JPanel();

    this.jPanel1LineFrom = new JPanel(new GridLayout(1, 2));
    this.jPanel1LineTo = new JPanel(new GridLayout(1, 2));
    this.jPanel2LineFrom = new JPanel(new GridLayout(1, 2, 2, 2));
    this.jPanel2LineTo = new JPanel(new GridLayout(1, 2, 2, 2));
    this.jPanel3LineFrom = new JPanel(new GridLayout(1, 2));
    this.jPanel3LineTo = new JPanel(new GridLayout(1, 2));
    this.jPanelButton = new JPanel(new FlowLayout(FlowLayout.CENTER));

    this.jLabelFrom = new JLabel("From");
    Font font = new Font(null, Font.BOLD, 13);
    jLabelFrom.setFont(font);
    this.jLabelTo = new JLabel("To");
    jLabelTo.setFont(font);
    this.jButtonFrom = new JButton("Set To Now");
    jButtonFrom.setFocusPainted(false);
    jButtonFrom.setMargin(new Insets(0, 0, 0, 0));
    jButtonFrom.setContentAreaFilled(false);
    jButtonFrom.setBorderPainted(false);
    jButtonFrom.setOpaque(false);
    jButtonFrom.setForeground(new Color(100, 185, 250));
    jButtonFrom.setHorizontalAlignment(SwingConstants.RIGHT);
    jButtonFrom.setVerticalAlignment(SwingConstants.TOP);

    this.jButtonTo = new JButton("Set To Now");
    jButtonTo.setFocusPainted(false);
    jButtonTo.setMargin(new Insets(0, 0, 0, 0));
    jButtonTo.setContentAreaFilled(false);
    jButtonTo.setBorderPainted(false);
    jButtonTo.setOpaque(false);
    jButtonTo.setForeground(new Color(100, 185, 250));
    jButtonTo.setHorizontalAlignment(SwingConstants.RIGHT);
    jButtonTo.setVerticalAlignment(SwingConstants.TOP);

    this.jLabelPromptTextFrom = new JLabel("DD-MM-YYYY HH:mm:ss");
    jLabelPromptTextFrom.setForeground(Color.GRAY);
    this.jLabelPromptTextTo = new JLabel("DD-MM-YYYY HH:mm:ss");
    jLabelPromptTextTo.setForeground(Color.GRAY);

    this.datePickerSettingsFrom = new DatePickerSettings();
    datePickerSettingsFrom.setAllowKeyboardEditing(true);
    datePickerSettingsFrom.setWeekNumbersDisplayed(true, true);
    datePickerSettingsFrom.setVisibleClearButton(false);
    this.datePickerFrom = new DatePicker(datePickerSettingsFrom);
    datePickerFrom.setPreferredSize(new Dimension(200, 25));
    datePickerFrom.setLocale(Locale.ENGLISH);

    this.datePickerSettingsTo = new DatePickerSettings();
    datePickerSettingsTo.setAllowKeyboardEditing(true);
    datePickerSettingsTo.setWeekNumbersDisplayed(true, true);
    datePickerSettingsTo.setVisibleClearButton(false);
    this.datePickerTo = new DatePicker(datePickerSettingsTo);
    datePickerTo.setPreferredSize(new Dimension(200, 25));
    datePickerTo.setLocale(Locale.ENGLISH);

    this.jButtonGo = new JButton("Go");
    jButtonGo.setPreferredSize(new Dimension(50, 25));

    jPanelGo.setLayout(new BorderLayout());
    jPanelGo.setBorder(BorderFactory.createEmptyBorder(2, 2, 3, 7));
    jPanelGo.add(jButtonGo, BorderLayout.SOUTH);

    PainlessGridBag gblFrom = new PainlessGridBag(jPanelFrom, PGHelper.getPGConfig(0), false);
    jPanel1LineFrom.add(jLabelFrom);
    jPanel1LineFrom.add(jButtonFrom);
    jPanel2LineFrom.add(datePickerFrom);
    jPanel3LineFrom.add(jLabelPromptTextFrom);
    gblFrom.row().cell(jPanel1LineFrom).fillX();
    gblFrom.row().cell(jPanel3LineFrom).fillX();
    gblFrom.row().cell(jPanel2LineFrom).fillX();
    gblFrom.done();

    PainlessGridBag gblTo = new PainlessGridBag(jPanelTo, PGHelper.getPGConfig(0), false);
    jPanel1LineTo.add(jLabelTo);
    jPanel1LineTo.add(jButtonTo);
    jPanel2LineTo.add(datePickerTo);
    jPanel3LineTo.add(jLabelPromptTextTo);

    gblTo.row().cell(jPanel1LineTo).fillX();
    gblTo.row().cell(jPanel3LineTo).fillX();
    gblTo.row().cell(jPanel2LineTo).fillX();
    gblTo.done();

    PainlessGridBag gblGeneral = new PainlessGridBag(this, PGHelper.getPGConfig(0), false);

    gblGeneral.row()
        .cell(new JLabel()).cell(new JLabel()).cell(new JLabel()).cell(new JLabel())
        .cell(new JLabel()).fillX()
        .cell(new JLabel()).cell(new JLabel()).cell(new JLabel()).cell(new JLabel())
        .cell(new JLabel()).fillX()
        .cell(new JLabel()).fillX();

    gblGeneral.row()
        .cellX(jPanelFrom, 5).fillX()
        .cellX(jPanelTo, 5).fillX()
        .cellY(jPanelGo, 1).fillXY();

    gblGeneral.row().cellXYRemainder(new JLabel()).fillXY();

    gblGeneral.done();

  }

}
