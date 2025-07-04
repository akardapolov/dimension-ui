package ru.dimension.ui.view.panel;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import lombok.Data;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;

@Data
public class RangeChartCustomPanel extends JPanel {

  private final JToggleButton jToggleButtonStart;

  private final JToggleButton jToggleButtonEnd;
  private final ButtonGroup buttonGroup;
  private final JTextField dateStartPanel;
  private final JTextField dateEndPanel;
  private int pullTimeout;

  public RangeChartCustomPanel() {
    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    this.jToggleButtonStart = new JToggleButton("START", true);
    this.jToggleButtonEnd = new JToggleButton("END", false);
    this.dateStartPanel = new JTextField();
    this.dateEndPanel = new JTextField();

    Dimension dimension = new Dimension(50, 30);
    jToggleButtonStart.setPreferredSize(dimension);
    jToggleButtonEnd.setPreferredSize(dimension);
    dateStartPanel.setPreferredSize(dimension);
    dateEndPanel.setPreferredSize(dimension);

    buttonGroup = new ButtonGroup();
    buttonGroup.add(jToggleButtonStart);
    buttonGroup.add(jToggleButtonEnd);

    JPanel btnPanel = new JPanel();
    btnPanel.add(jToggleButtonStart);
    btnPanel.add(dateStartPanel);
    btnPanel.add(jToggleButtonEnd);
    btnPanel.add(dateEndPanel);
    btnPanel.setLayout(new GridLayout(1, 4, 5, 0));

    setButtonView();
    gbl.row()
        .cell(btnPanel).cell(new JLabel()).fillX();
    gbl.done();
  }

  public void setButtonNotView() {
    jToggleButtonStart.setEnabled(false);
    jToggleButtonEnd.setEnabled(false);
  }

  public void setButtonView() {
    jToggleButtonStart.setEnabled(true);
    jToggleButtonEnd.setEnabled(true);
  }

  public void addActionListener(ActionListener x) {
    jToggleButtonStart.addActionListener(x);
    jToggleButtonEnd.addActionListener(x);
  }

}
