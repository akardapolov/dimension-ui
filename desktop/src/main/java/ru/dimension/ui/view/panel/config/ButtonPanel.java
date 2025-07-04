package ru.dimension.ui.view.panel.config;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;

@Data
@EqualsAndHashCode(callSuper = false)
public class ButtonPanel extends JPanel {

  private final JButton btnNew;
  private final JButton btnCopy;
  private final JButton btnDel;
  private final JButton btnEdit;
  private final JButton btnSave;
  private final JButton btnCancel;

  public ButtonPanel() {
    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    this.btnNew = new JButton("New");
    btnNew.setMnemonic('N');
    this.btnCopy = new JButton("Copy");
    btnCopy.setMnemonic('C');
    this.btnDel = new JButton("Delete");
    btnDel.setMnemonic('D');
    this.btnEdit = new JButton("Edit");
    btnEdit.setMnemonic('E');
    this.btnSave = new JButton("Save");
    btnSave.setMnemonic('S');
    this.btnCancel = new JButton("Cancel");
    btnCancel.setMnemonic('C');

    btnSave.setEnabled(false);
    btnCancel.setEnabled(false);

    gbl.row()
        .cell(btnNew)
        .cell(btnCopy)
        .cell(btnDel)
        .cell(btnEdit)
        .cell(btnSave)
        .cell(btnCancel)
        .cell(new JLabel())
        .fillX();
    gbl.doneAndPushEverythingToTop();
  }

  public void setButtonView(Boolean isSelected) {
    btnNew.setEnabled(isSelected);
    btnCopy.setEnabled(isSelected);
    btnDel.setEnabled(isSelected);
    btnEdit.setEnabled(isSelected);
    btnSave.setEnabled(!isSelected);
    btnCancel.setEnabled(!isSelected);
  }

}
