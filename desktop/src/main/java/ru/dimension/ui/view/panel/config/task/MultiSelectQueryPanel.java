package ru.dimension.ui.view.panel.config.task;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import jakarta.inject.Named;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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
public class MultiSelectQueryPanel extends JPanel {

  private final JXTableCase queryListCase;
  private final JXTableCase selectedQueryCase;
  private final JXTableCase templateListQueryCase;
  private final JButton pickAllBtn;
  private final JButton pickBtn;
  private final JButton unPickBtn;
  private final JButton unPickAllBtn;
  private final JTabbedPane jTabbedPaneQuery;

  public MultiSelectQueryPanel(@Named("queryListCase") JXTableCase queryListCase,
                               @Named("selectedQueryCase") JXTableCase selectedQueryCase,
                               @Named("templateListQueryCase") JXTableCase templateListQueryCase) {
    this.queryListCase = queryListCase;
    this.selectedQueryCase = selectedQueryCase;
    this.templateListQueryCase = templateListQueryCase;
    Font fontBtn = new Font("TimesRoman", Font.BOLD, 12);
    this.pickBtn = new JButton(">");
    this.unPickBtn = new JButton("<");
    this.pickAllBtn = new JButton(">>");
    this.unPickAllBtn = new JButton("<<");
    this.jTabbedPaneQuery = new JTabbedPane();
    jTabbedPaneQuery.add("Configuration", this.queryListCase.getJScrollPane());
    jTabbedPaneQuery.add("Template", this.templateListQueryCase.getJScrollPane());
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

    Border finalBorder = GUIHelper.getGrayBorder();
    this.queryListCase.getJxTable().setBorder(finalBorder);
    this.selectedQueryCase.getJxTable().setBorder(finalBorder);

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
        .cellX(new JXTitledSeparator("Query list"), 2).fillX(6)
        .cellX(new JLabel(), 1).fillX(1)
        .cellX(new JXTitledSeparator("Selected query"), 2).fillX(6);

    gbl.row()
        .cellX(jTabbedPaneQuery, 2).fillXY(6, 5)
        .cellX(btnPanel, 1).fillX(1)
        .cellX(this.selectedQueryCase.getJScrollPane(), 2).fillXY(6, 5);

    gbl.doneAndPushEverythingToTop();
  }
}


