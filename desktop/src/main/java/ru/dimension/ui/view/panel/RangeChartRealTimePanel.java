package ru.dimension.ui.view.panel;

import static ru.dimension.ui.model.view.RangeChartRealTime.FIVE_MIN;
import static ru.dimension.ui.model.view.RangeChartRealTime.SIXTY_MIN;
import static ru.dimension.ui.model.view.RangeChartRealTime.TEN_MIN;
import static ru.dimension.ui.model.view.RangeChartRealTime.THIRTY_MIN;

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
public class RangeChartRealTimePanel extends JPanel {

  private final JToggleButton jToggleButton5;
  private final JToggleButton jToggleButton10;
  private final JToggleButton jToggleButton30;
  private final JToggleButton jToggleButton60;
  private final ButtonGroup buttonGroup;

  private int pullTimeout;

  public RangeChartRealTimePanel() {
    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    this.jToggleButton5 = new JToggleButton("Last " + FIVE_MIN.getName(), false);
    this.jToggleButton10 = new JToggleButton("Last " + TEN_MIN.getName(), true);
    this.jToggleButton10.setForeground(Color.BLACK);
    this.jToggleButton30 = new JToggleButton("Last " + THIRTY_MIN.getName(), false);
    this.jToggleButton60 = new JToggleButton("Last " + SIXTY_MIN.getName(), false);

    Dimension dimension = new Dimension(50, 20);
    jToggleButton5.setPreferredSize(dimension);
    jToggleButton10.setPreferredSize(dimension);
    jToggleButton30.setPreferredSize(dimension);
    jToggleButton60.setPreferredSize(dimension);

    setButtonView();

    jToggleButton5.setForeground(new Color(100, 185, 250));
    jToggleButton30.setForeground(new Color(100, 185, 250));
    jToggleButton60.setForeground(new Color(100, 185, 250));

    buttonEmptyBorder(jToggleButton5);
    buttonEmptyBorder(jToggleButton10);
    buttonEmptyBorder(jToggleButton30);
    buttonEmptyBorder(jToggleButton60);

    buttonGroup = new ButtonGroup();
    buttonGroup.add(jToggleButton5);
    buttonGroup.add(jToggleButton10);
    buttonGroup.add(jToggleButton30);
    buttonGroup.add(jToggleButton60);

    JPanel btnPanel = new JPanel();
    btnPanel.add(jToggleButton5);
    btnPanel.add(jToggleButton10);
    btnPanel.add(jToggleButton30);
    btnPanel.add(jToggleButton60);
    btnPanel.setLayout(new GridLayout(4, 1));

    setButtonView();

    gbl.row()
        .cell(btnPanel).fillX();
    gbl.row()
        .cell(new JLabel()).fillXY();
    gbl.done();
  }

  public void setButtonColor(Color b5,
                             Color b10,
                             Color b30,
                             Color b60) {
    jToggleButton5.setForeground(b5);
    jToggleButton10.setForeground(b10);
    jToggleButton30.setForeground(b30);
    jToggleButton60.setForeground(b60);
  }

  public void setButtonView() {
    jToggleButton5.setEnabled(true);
    jToggleButton10.setEnabled(true);
    jToggleButton30.setEnabled(true);
    jToggleButton60.setEnabled(true);
  }

  public void addActionListener(ActionListener x) {
    jToggleButton5.addActionListener(x);
    jToggleButton10.addActionListener(x);
    jToggleButton30.addActionListener(x);
    jToggleButton60.addActionListener(x);
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
