package ru.dimension.ui.view.panel.config.profile;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
public class MultiSelectTaskPanel extends JPanel {

  private final JXTableCase taskListCase;
  private final JXTableCase selectedTaskCase;
  private final JXTableCase templateListTaskCase;
  private final JButton pickAllBtn;
  private final JButton pickBtn;
  private final JButton unPickBtn;
  private final JButton unPickAllBtn;
  private final JTabbedPane jTabbedPaneTask;

  public MultiSelectTaskPanel(@Named("taskListCase") JXTableCase taskListCase,
                              @Named("selectedTaskCase") JXTableCase selectedTaskCase,
                              @Named("templateListTaskCase") JXTableCase templateListTaskCase) {
    this.taskListCase = taskListCase;
    this.selectedTaskCase = selectedTaskCase;
    this.templateListTaskCase = templateListTaskCase;
    this.pickBtn = new JButton(">");
    this.unPickBtn = new JButton("<");
    this.pickAllBtn = new JButton(">>");
    this.unPickAllBtn = new JButton("<<");
    this.jTabbedPaneTask = new JTabbedPane();
    jTabbedPaneTask.add("Configuration", this.taskListCase.getJScrollPane());
    jTabbedPaneTask.add("Template", this.templateListTaskCase.getJScrollPane());
    pickBtn.setEnabled(false);
    unPickBtn.setEnabled(false);
    unPickAllBtn.setEnabled(false);
    pickAllBtn.setEnabled(false);
    pickBtn.setPreferredSize(new Dimension(2, 30));
    unPickBtn.setPreferredSize(new Dimension(2, 30));
    unPickAllBtn.setPreferredSize(new Dimension(2, 30));
    pickAllBtn.setPreferredSize(new Dimension(2, 30));
    this.setBorder(new EtchedBorder());

    Border finalBorder = GUIHelper.getGrayBorder();
    this.taskListCase.getJxTable().setBorder(finalBorder);
    this.selectedTaskCase.getJxTable().setBorder(finalBorder);

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
        .cellX(new JXTitledSeparator("Task list"), 2).fillX()
        .cellX(new JLabel(), 1).fillX(1)
        .cellX(new JXTitledSeparator("Selected task"), 2).fillX();
    gbl.row()
        .cellX(jTabbedPaneTask, 2).fillXY(4, 5)
        .cellX(btnPanel, 1).fillX(1)
        .cellX(this.selectedTaskCase.getJScrollPane(), 2).fillXY(4, 5);

    gbl.doneAndPushEverythingToTop();
  }
}


