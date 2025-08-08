package ru.dimension.ui.component.module.manage;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.panel.manage.ProfileActionPanel;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;

@Log4j2
public class ManageView extends JPanel {

  @Getter
  private final ProfileActionPanel profileActionPanel;

  public ManageView() {
    this.profileActionPanel = new ProfileActionPanel();

    setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    gbl.row()
        .cellXRemainder(profileActionPanel.getStatus()).fillX();
    gbl.row()
        .cellXRemainder(profileActionPanel).fillX();

    gbl.done();
  }
}
