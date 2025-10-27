package ru.dimension.ui.view.panel.config.connection;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.inject.Named;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.table.JXTableCase;

@Data
@EqualsAndHashCode(callSuper = false)
public class MultiSelectConnectionPanel extends JPanel {

  private final JXTableCase listCase;
  private final JXTableCase selectedCase;
  private final JButton pickAllBtn;
  private final JButton pickBtn;
  private final JButton unPickBtn;
  private final JButton unPickAllBtn;

  public MultiSelectConnectionPanel(@Named("listCase") JXTableCase listCase,
                                    @Named("selectedCase") JXTableCase selectedCase) {
    this.listCase = listCase;
    this.selectedCase = selectedCase;
    Font fontBtn = new Font("TimesRoman", Font.BOLD, 12);
    this.pickBtn = new JButton(">");
    this.unPickBtn = new JButton("<");
    this.pickAllBtn = new JButton(">>");
    this.unPickAllBtn = new JButton("<<");

    pickBtn.setEnabled(false);
    unPickBtn.setEnabled(false);
    pickAllBtn.setEnabled(false);
    unPickAllBtn.setEnabled(false);
    pickBtn.setFont(fontBtn);
    unPickBtn.setFont(fontBtn);
    pickAllBtn.setFont(fontBtn);
    unPickAllBtn.setFont(fontBtn);
    pickBtn.setPreferredSize(new Dimension(25, 30));
    unPickBtn.setPreferredSize(new Dimension(25, 30));
    unPickAllBtn.setPreferredSize(new Dimension(25, 30));
    unPickAllBtn.setPreferredSize(new Dimension(25, 30));

    this.setBorder(new EtchedBorder());

    this.listCase.getJxTable().getColumnExt(0).setVisible(false);
    this.selectedCase.getJxTable().getColumnExt(0).setVisible(false);

    Border finalBorder = GUIHelper.getGrayBorder();
    this.listCase.getJxTable().setBorder(finalBorder);
    this.selectedCase.getJxTable().setBorder(finalBorder);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    JPanel btnPanel = new JPanel();
    btnPanel.setLayout(new BorderLayout());
    PainlessGridBag gblBtn = new PainlessGridBag(btnPanel, PGHelper.getPGConfig(), false);

    gblBtn.row()
        .cell(pickAllBtn).fillX();
    gblBtn.row()
        .cell(pickBtn).fillX();
    gblBtn.row()
        .cell(unPickBtn).fillX();
    gblBtn.row()
        .cell(unPickAllBtn).fillX();
    gblBtn.done();

    gbl.row()
        .cellX(new JXTitledSeparator("List"), 2).fillX(6)
        .cellX(new JLabel(), 1).fillX(1)
        .cellX(new JXTitledSeparator("Selected"), 2).fillX(6);

    gbl.row()
        .cellX(this.listCase.getJScrollPane(), 2).fillXY(6, 5)
        .cellX(btnPanel, 1).fillX(1)
        .cellX(this.selectedCase.getJScrollPane(), 2).fillXY(6, 5);

    gbl.doneAndPushEverythingToTop();
  }
}


