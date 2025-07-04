package ru.dimension.ui.view.panel;

import static ru.dimension.ui.model.view.RangeHistory.CUSTOM;
import static ru.dimension.ui.model.view.RangeHistory.DAY;
import static ru.dimension.ui.model.view.RangeHistory.MONTH;
import static ru.dimension.ui.model.view.RangeHistory.WEEK;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import lombok.Data;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;

@Data
public class RangeChartHistoryPanel extends JPanel {

  private final JToggleButton jToggleButtonDay;
  private final JToggleButton jToggleButtonWeek;
  private final JToggleButton jToggleButtonMonth;
  private final JToggleButton jToggleButtonCustom;
  private final ButtonGroup buttonGroup;

  private int pullTimeout;

  public RangeChartHistoryPanel() {
    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    jToggleButtonDay = new JToggleButton("Last " + DAY.getName(), true);
    jToggleButtonDay.setForeground(Color.BLACK);
    jToggleButtonWeek = new JToggleButton("Last " + WEEK.getName(), false);
    jToggleButtonMonth = new JToggleButton("Last " + MONTH.getName(), false);
    jToggleButtonCustom = new JToggleButton(CUSTOM.getName(), false);

    Dimension dimension = new Dimension(50, 20);
    jToggleButtonDay.setPreferredSize(dimension);
    jToggleButtonWeek.setPreferredSize(dimension);
    jToggleButtonMonth.setPreferredSize(dimension);
    jToggleButtonCustom.setPreferredSize(dimension);

    setButtonView();

    jToggleButtonWeek.setForeground(new Color(100, 185, 250));
    jToggleButtonMonth.setForeground(new Color(100, 185, 250));
    jToggleButtonCustom.setForeground(new Color(100, 185, 250));

    buttonEmptyBorder(jToggleButtonDay);
    buttonEmptyBorder(jToggleButtonWeek);
    buttonEmptyBorder(jToggleButtonMonth);
    buttonEmptyBorder(jToggleButtonCustom);

    buttonGroup = new ButtonGroup();
    buttonGroup.add(jToggleButtonDay);
    buttonGroup.add(jToggleButtonWeek);
    buttonGroup.add(jToggleButtonMonth);
    buttonGroup.add(jToggleButtonCustom);

    JPanel btnPanel = new JPanel();
    btnPanel.add(jToggleButtonDay);
    btnPanel.add(jToggleButtonWeek);
    btnPanel.add(jToggleButtonMonth);
    btnPanel.add(jToggleButtonCustom);
    btnPanel.setLayout(new GridLayout(4, 1));

    gbl.row()
        .cell(btnPanel).fillX();
    gbl.row()
        .cell(new JLabel()).fillXY();

    gbl.done();
  }

  public void setButtonColor(Color day,
                             Color week,
                             Color month,
                             Color custom) {
    jToggleButtonDay.setForeground(day);
    jToggleButtonWeek.setForeground(week);
    jToggleButtonMonth.setForeground(month);
    jToggleButtonCustom.setForeground(custom);
  }

  public void setButtonView() {
    jToggleButtonDay.setEnabled(true);
    jToggleButtonWeek.setEnabled(true);
    jToggleButtonMonth.setEnabled(true);
    jToggleButtonCustom.setEnabled(true);
  }

  public void addActionListener(ActionListener x) {
    jToggleButtonDay.addActionListener(x);
    jToggleButtonWeek.addActionListener(x);
    jToggleButtonMonth.addActionListener(x);
    jToggleButtonCustom.addActionListener(x);
  }

  private void buttonEmptyBorder(JToggleButton jToggleButton) {
    jToggleButton.setFocusPainted(false);
    jToggleButton.setMargin(new Insets(0, 0, 0, 0));
    jToggleButton.setContentAreaFilled(false);
    jToggleButton.setBorderPainted(false);
    jToggleButton.setOpaque(false);

    jToggleButton.setVerticalAlignment(SwingConstants.TOP);
    jToggleButton.setHorizontalAlignment(SwingConstants.LEFT);
    jToggleButton.setHorizontalTextPosition(SwingConstants.RIGHT);
    jToggleButton.setVerticalTextPosition(SwingConstants.CENTER);
    jToggleButton.setIconTextGap(0);
  }
}
