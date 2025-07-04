package ru.dimension.ui.view.panel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import lombok.Data;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;

@Data
public class QuerySearchButtonPanel extends JPanel {

  private final JLabel lblSearch;
  private final JTextField jTextFieldSearch;
  private final JButton jButtonSearch;

  public QuerySearchButtonPanel() {
    this.lblSearch = new JLabel("Search");
    this.jTextFieldSearch = new JTextField(20);
    this.jButtonSearch = new JButton("Go");

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(0), false);

    gbl.row()
        .cell(getButtonPanel())
        .fillX();

    gbl.done();
  }

  private JPanel getButtonPanel() {
    JPanel buttonPanel = new JPanel();

    buttonPanel.setBorder(new EtchedBorder());
    PainlessGridBag gblButton = new PainlessGridBag(buttonPanel, PGHelper.getPGConfig(), false);

    gblButton.row()
        .cell(lblSearch)
        .cell(jTextFieldSearch).fillX()
        .cell(jButtonSearch)
        .cellXRemainder(new JLabel()).fillX();

    gblButton.done();

    return buttonPanel;
  }
}
