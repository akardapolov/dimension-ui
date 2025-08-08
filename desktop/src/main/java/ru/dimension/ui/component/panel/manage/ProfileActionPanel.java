package ru.dimension.ui.component.panel.manage;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.Insets;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.ActionName;
import ru.dimension.ui.model.RunStatus;

@Log4j2
@Data
public class ProfileActionPanel extends JPanel {

  private final JXTitledSeparator status;

  private final JButton start;
  private final JButton stop;
  private final JButton preview;

  JSeparator verticalSeparator = GUIHelper.verticalSeparator();

  public ProfileActionPanel() {
    this.status = new JXTitledSeparator("");

    ImageIcon startIcon = GUIHelper.loadIcon("/icons/start.png");
    ImageIcon stopIcon = GUIHelper.loadIcon("/icons/stop.png");
    ImageIcon previewIcon = GUIHelper.loadIcon("/icons/preview.png");

    this.start = new JButton(ActionName.START.getDescription(), startIcon);
    this.stop = new JButton(ActionName.STOP.getDescription(), stopIcon);
    this.preview = new JButton(ActionName.PREVIEW.getDescription(), previewIcon);

    this.start.setMargin(new Insets(5, 10, 5, 10));
    this.stop.setMargin(new Insets(5, 10, 5, 10));
    this.preview.setMargin(new Insets(5, 10, 5, 10));

    this.start.setToolTipText("Start loading data for the selected profile");
    this.stop.setToolTipText("Stop loading data for the selected profile");
    this.preview.setToolTipText("Load a realtime preview based on the selected query");

    start.setActionCommand(ActionName.START.name());
    stop.setActionCommand(ActionName.STOP.name());
    preview.setActionCommand(ActionName.PREVIEW.name());

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    gbl.row()
        .cell(start)
        .cell(stop)
        .cell(new JLabel("  "))
        .cell(verticalSeparator)
        .cell(new JLabel("  "))
        .cell(preview)
        .cellXRemainder(new JLabel()).fillX();

    PGHelper.setConstrainsInsets(gbl, start, 0);
    PGHelper.setConstrainsInsets(gbl, stop, 0);

    gbl.done();
  }

  public void setButtonState(String profileName, RunStatus runStatus) {
    status.setTitle(profileName + " >> " + runStatus.getDescription());
    if (RunStatus.RUNNING.equals(runStatus)) {
      start.setEnabled(false);
      stop.setEnabled(true);
    } else {
      start.setEnabled(true);
      stop.setEnabled(false);
    }
  }
}
