package ru.dimension.ui.view.panel;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.view.workspase.RelativeDatePane;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class TimeRangeRelativePanel extends JPanel {

  private final JPanel jPanelFrom;
  private final JPanel jPanelTo;
  private final JLabel jLabelFrom;
  private final JLabel jLabelTo;
  private final JButton jButtonFrom;
  private final JButton jButtonTo;
  private final JLabel jLabelPromptTextFrom;
  private final JLabel jLabelPromptTextTo;
  private final JButton jButtonGo;
  private final JPanel jPanelGo;

  private final JSpinner jSpinnerFrom;
  private final JComboBox<String> jComboBoxFrom;
  private final JPanel jPanelSecondRowFrom;
  private final JCheckBox jCheckBoxFrom;
  private final JSpinner jSpinnerTo;
  private final JComboBox<String> jComboBoxTo;
  private final JPanel jPanelSecondRowTo;
  private final JCheckBox jCheckBoxTo;
  private final JPanel jPanel1LineFrom;
  private final JPanel jPanel1LineTo;
  private final JPanel jPanel2LineFrom;
  private final JPanel jPanel2LineTo;
  private final JPanel jPanel3LineFrom;
  private final JPanel jPanel3LineTo;
  private final JPanel jPanel4LineFrom;
  private final JPanel jPanel4LineTo;


  private String selectedFrom;
  private LocalDateTime dateSelectedFrom;
  private LocalDateTime dateChangedFrom;
  private String selectedTo;
  private LocalDateTime dateSelectedTo;
  private LocalDateTime dateChangedTo;
  private int addCountFrom;
  private int countFrom;
  private int previousValueFrom;
  private int addCountTo;
  private int countTo;
  private int previousValueTo;

  @Inject
  public TimeRangeRelativePanel() {

    this.jPanelFrom = new JPanel();
    this.jPanelTo = new JPanel();
    this.jPanelGo = new JPanel();

    this.jPanel1LineFrom = new JPanel(new GridLayout(1, 2));
    this.jPanel1LineTo = new JPanel(new GridLayout(1, 2));
    this.jPanel2LineFrom = new JPanel(new GridLayout(1, 2));
    this.jPanel2LineTo = new JPanel(new GridLayout(1, 2));
    this.jPanel3LineFrom = new JPanel(new GridLayout(1, 2, 2, 2));
    this.jPanel3LineTo = new JPanel(new GridLayout(1, 2, 2, 2));
    this.jPanel4LineFrom = new JPanel(new GridLayout(1, 2));
    this.jPanel4LineTo = new JPanel(new GridLayout(1, 2));

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
    this.jButtonTo = new JButton("Set To Now");
    jButtonTo.setFocusPainted(false);
    jButtonTo.setMargin(new Insets(0, 0, 0, 0));
    jButtonTo.setContentAreaFilled(false);
    jButtonTo.setBorderPainted(false);
    jButtonTo.setOpaque(false);
    jButtonTo.setForeground(new Color(100, 185, 250));

    LocalDateTime dateNow = LocalDateTime.now();

    DateTimeFormatter formatForDateNow = DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH);
    String dateNowFormat = dateNow.format(formatForDateNow);

    this.jLabelPromptTextFrom = new JLabel(dateNowFormat);
    jLabelPromptTextFrom.setForeground(Color.GRAY);
    this.jLabelPromptTextTo = new JLabel("Now");
    jLabelPromptTextTo.setForeground(Color.GRAY);

    this.jSpinnerFrom = new JSpinner();
    SpinnerModel spinnerModelFrom = new SpinnerNumberModel(0, 0, 59, 1);
    jSpinnerFrom.setModel(spinnerModelFrom);
    this.jComboBoxFrom = new JComboBox<>();
    this.jPanelSecondRowFrom = new JPanel();
    this.jCheckBoxFrom = new JCheckBox("round to the second");

    this.jSpinnerTo = new JSpinner();
    SpinnerModel spinnerModelTo = new SpinnerNumberModel(0, 0, 59, 1);
    jSpinnerTo.setModel(spinnerModelTo);
    this.jComboBoxTo = new JComboBox<>();
    this.jPanelSecondRowTo = new JPanel();
    this.jCheckBoxTo = new JCheckBox("round to the second");

    jSpinnerFrom.setPreferredSize(new Dimension(70, 25));
    jComboBoxFrom.setPreferredSize(new Dimension(70, 25));

    PainlessGridBag gblFrom = new PainlessGridBag(jPanelFrom, PGHelper.getPGConfig(0), false);
    jPanel1LineFrom.add(jLabelFrom);
    jPanel1LineFrom.add(jButtonFrom);
    jButtonFrom.setHorizontalAlignment(SwingConstants.RIGHT);
    jButtonFrom.setVerticalAlignment(SwingConstants.TOP);
    jPanel2LineFrom.add(jLabelPromptTextFrom);
    jPanel3LineFrom.add(jSpinnerFrom);
    jPanel3LineFrom.add(jComboBoxFrom);
    gblFrom.row().cell(jPanel1LineFrom).fillX();
    gblFrom.row().cell(jPanel2LineFrom).fillX();
    gblFrom.row().cell(jPanel3LineFrom).fillX();
    gblFrom.done();

    jSpinnerTo.setPreferredSize(new Dimension(70, 25));
    jComboBoxTo.setPreferredSize(new Dimension(70, 25));

    this.jButtonGo = new JButton("Go");
    jButtonGo.setPreferredSize(new Dimension(40, 25));

    jPanelGo.setLayout(new BorderLayout());
    jPanelGo.setBorder(BorderFactory.createEmptyBorder(2, 2, 12, 7));
    jPanelGo.add(jButtonGo, BorderLayout.SOUTH);

    PainlessGridBag gblTo = new PainlessGridBag(jPanelTo, PGHelper.getPGConfig(0), false);
    jPanel1LineTo.add(jLabelTo);
    jPanel1LineTo.add(jButtonTo);
    jButtonTo.setHorizontalAlignment(SwingConstants.RIGHT);
    jButtonTo.setVerticalAlignment(SwingConstants.TOP);
    jPanel2LineTo.add(jLabelPromptTextTo);
    jPanel3LineTo.add(jSpinnerTo);
    jPanel3LineTo.add(jComboBoxTo);
    gblTo.row().cell(jPanel1LineTo).fillX();
    gblTo.row().cell(jPanel2LineTo).fillX();
    gblTo.row().cell(jPanel3LineTo).fillX();
    gblTo.done();

    PainlessGridBag gblGeneral = new PainlessGridBag(this, PGHelper.getPGConfig(0), false);

    gblGeneral.row()
        .cellX(jPanelFrom, 3).fillX()
        .cellX(jPanelTo, 3).fillX()
        .cellY(jPanelGo, 1).fillXY();

    gblGeneral.done();

    this.jComboBoxFrom.addItem(RelativeDatePane.SECONDS.getName());
    this.jComboBoxFrom.addItem(RelativeDatePane.MINUTES.getName());
    this.jComboBoxFrom.addItem(RelativeDatePane.HOURS.getName());
    this.jComboBoxFrom.addItem(RelativeDatePane.DAYS.getName());

    this.jComboBoxTo.addItem(RelativeDatePane.SECONDS.getName());
    this.jComboBoxTo.addItem(RelativeDatePane.MINUTES.getName());
    this.jComboBoxTo.addItem(RelativeDatePane.HOURS.getName());
    this.jComboBoxTo.addItem(RelativeDatePane.DAYS.getName());

    this.selectedFrom = jLabelPromptTextFrom.getText();
    this.dateSelectedFrom = LocalDateTime.parse(selectedFrom, DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH));
    this.dateChangedFrom = dateSelectedFrom;

    this.selectedTo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH));
    this.dateSelectedTo = LocalDateTime.parse(selectedTo, DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH));
    this.dateChangedTo = dateSelectedTo;

    this.countFrom = 0;
    this.previousValueFrom = 0;
    this.countTo = 0;
    this.previousValueTo = 0;
  }
}
