package ru.dimension.ui.component.module.config;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTitledSeparator;
import ru.dimension.ui.component.panel.CollapseCardPanel;
import ru.dimension.ui.component.panel.DetailShowHidePanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.SwitchToTabPanel;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
import ru.dimension.ui.component.panel.range.RealTimeRangePanel;
import ru.dimension.ui.laf.LaF;

@Log4j2
public class ConfigView extends JPanel {

  @Getter
  private final SwitchToTabPanel switchToTabPanel;
  @Getter
  private final RealTimeRangePanel realTimePanel;
  @Getter
  private final HistoryRangePanel historyPanel;
  @Getter
  private final LegendPanel legendPanel;
  @Getter
  private final DetailShowHidePanel detailShowHidePanel;
  @Getter
  private final CollapseCardPanel collapseCardPanel;

  public ConfigView() {
    this.switchToTabPanel = new SwitchToTabPanel();
    this.realTimePanel = new RealTimeRangePanel();
    this.historyPanel = new HistoryRangePanel();
    this.legendPanel = new LegendPanel();
    this.detailShowHidePanel = new DetailShowHidePanel();
    this.collapseCardPanel = new CollapseCardPanel();

    setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    initComponents();
  }

  private void initComponents() {
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    add(createSection("Switch to", switchToTabPanel));
    add(Box.createRigidArea(new Dimension(10, 0)));
    add(createSection("Real-time", realTimePanel));
    add(Box.createRigidArea(new Dimension(10, 0)));
    add(createSection("History", historyPanel));
    add(Box.createRigidArea(new Dimension(10, 0)));
    add(createSection("Legend", legendPanel));
    add(Box.createHorizontalGlue());
    add(createSection("Detail", detailShowHidePanel));
    add(Box.createRigidArea(new Dimension(10, 0)));
    add(createSection("Dashboard", collapseCardPanel));
  }

  private JPanel createSection(String title, JPanel content) {
    JPanel sectionPanel = new JPanel();
    sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
    sectionPanel.setOpaque(false);

    JXTitledSeparator separator = new JXTitledSeparator(title);
    separator.setAlignmentX(LEFT_ALIGNMENT);

    content.setAlignmentX(LEFT_ALIGNMENT);

    sectionPanel.add(separator);
    sectionPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    sectionPanel.add(content);

    return sectionPanel;
  }
}